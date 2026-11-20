package com.manas.staysearch.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.graphql.test.tester.GraphQlTester;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Served as a GraphQL field": this is the end-to-end proof, a real GraphQL query through the real Spring context. */
@SpringBootTest
@AutoConfigureGraphQlTester
class StaySearchGraphQlIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void searchStaysFieldReturnsResults() {
        String document = """
                query {
                    searchStays(query: "apartment with a balcony", limit: 5) {
                        id
                        name
                        priceCents
                        rerankScore
                    }
                }
                """;

        var results = graphQlTester.document(document)
                .execute()
                .path("searchStays")
                .entityList(Object.class)
                .get();

        assertFalse(results.isEmpty());
    }

    @Test
    void limitArgumentIsRespected() {
        String document = """
                query {
                    searchStays(query: "a quiet apartment", limit: 3) {
                        id
                    }
                }
                """;

        var results = graphQlTester.document(document)
                .execute()
                .path("searchStays")
                .entityList(Object.class)
                .get();

        assertTrue(results.size() <= 3);
    }
}
