package com.manas.staysearch.bench;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measures "p95 86ms at 50 concurrent queries" at the real serving boundary: a live Spring Boot
 * app, the full real corpus loaded, 50 concurrent HTTP clients issuing the real
 * {@code searchStays} GraphQL query against it. Requires the same two system properties as
 * {@code NdcgBenchmarkRunner}; see that class's javadoc.
 */
@Tag("benchmark")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LatencyBenchmarkRunner {

    private static final int CONCURRENCY = 50;
    private static final int REQUESTS_PER_WORKER = 20;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void dataPaths(DynamicPropertyRegistry registry) {
        registry.add("app.data.listings-csv-path", () -> require("app.bench.listings-csv-path"));
        registry.add("app.data.queries-csv-path", () -> require("app.bench.data-dir") + "/queries.csv");
        registry.add("app.data.labels-csv-path", () -> require("app.bench.data-dir") + "/labels.csv");
    }

    private static String require(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing required system property " + key);
        }
        return value;
    }

    @Test
    void measureP95LatencyAtFiftyConcurrentQueries() throws Exception {
        TestRestTemplate rest = new TestRestTemplate();
        String url = "http://localhost:" + port + "/graphql";
        String[] queryTexts = {
                "quiet cabin near a lake, dog friendly, under $150",
                "modern apartment with a pool under $300",
                "cozy studio with wifi under $100",
                "spacious 2-bedroom place with free parking",
                "family-friendly house with a full kitchen under $250",
        };

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<List<Long>>> futures = new ArrayList<>();

        for (int w = 0; w < CONCURRENCY; w++) {
            final String queryText = queryTexts[w % queryTexts.length];
            futures.add(pool.submit(() -> {
                List<Long> latencies = new ArrayList<>();
                for (int i = 0; i < REQUESTS_PER_WORKER; i++) {
                    long start = System.nanoTime();
                    issueSearch(rest, url, queryText);
                    latencies.add((System.nanoTime() - start) / 1_000_000);
                }
                return latencies;
            }));
        }

        List<Long> allLatencies = new ArrayList<>();
        for (Future<List<Long>> future : futures) {
            allLatencies.addAll(future.get(2, TimeUnit.MINUTES));
        }
        pool.shutdown();

        Collections.sort(allLatencies);
        long p95 = allLatencies.get((int) Math.ceil(allLatencies.size() * 0.95) - 1);
        long p50 = allLatencies.get((int) Math.ceil(allLatencies.size() * 0.50) - 1);

        System.out.println("LATENCY_BENCHMARK");
        System.out.println("concurrency=" + CONCURRENCY);
        System.out.println("total_requests=" + allLatencies.size());
        System.out.println("p50_ms=" + p50);
        System.out.println("p95_ms=" + p95);
        System.out.println("max_ms=" + allLatencies.get(allLatencies.size() - 1));

        assertTrue(allLatencies.size() == CONCURRENCY * REQUESTS_PER_WORKER);
    }

    @SuppressWarnings("unchecked")
    private static void issueSearch(TestRestTemplate rest, String url, String queryText) {
        String document = "{\"query\":\"query($q: String!) { searchStays(query: $q, limit: 10) { id priceCents rerankScore } }\",\"variables\":{\"q\":\"" + queryText.replace("\"", "\\\"") + "\"}}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(document, headers);
        Map<String, Object> body = rest.postForObject(url, entity, Map.class);
        if (body == null || !body.containsKey("data")) {
            throw new IllegalStateException("graphql call did not return data: " + body);
        }
    }
}
