package com.manas.staysearch.search;

import com.manas.staysearch.ingest.ListingCsvLoader;
import com.manas.staysearch.model.Listing;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scores {@code QueryParser}'s per-slot extraction (price, bedroom, amenity, neighbourhood)
 * against {@code src/test/resources/slot_eval_queries.csv}: 300 queries expanded from 30
 * hand-written templates (`python/slot_eval_templates.py`), each hand-labeled with the single
 * correct extraction outcome, grounded on 10 real listings from the committed sample. See
 * {@code generate_slot_eval_queries.py}'s module docstring for how the set was built.
 */
class SlotExtractionEvaluationTest {

    private record Row(String queryText, boolean expectPriceHit, Long expectPriceCents,
                        boolean expectBedroomHit, Integer expectBedrooms,
                        boolean expectAmenityHit, List<String> expectAmenityKeywords,
                        boolean expectNeighbourhoodHit, String expectNeighbourhood) {
    }

    private static List<Row> loadRows() throws IOException {
        List<Row> rows = new ArrayList<>();
        Path csvPath = Path.of("src/test/resources/slot_eval_queries.csv");
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try (Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord r : parser) {
                boolean priceHit = Boolean.parseBoolean(r.get("expected_price_hit"));
                boolean bedroomHit = Boolean.parseBoolean(r.get("expected_bedroom_hit"));
                boolean amenityHit = Boolean.parseBoolean(r.get("expected_amenity_hit"));
                boolean neighbourhoodHit = Boolean.parseBoolean(r.get("expected_neighbourhood_hit"));
                rows.add(new Row(
                        r.get("query_text"),
                        priceHit, priceHit ? Long.parseLong(r.get("expected_price_cents")) : null,
                        bedroomHit, bedroomHit ? Integer.parseInt(r.get("expected_bedrooms")) : null,
                        amenityHit, amenityHit ? List.of(r.get("expected_amenity_keywords").split(";")) : List.of(),
                        neighbourhoodHit, neighbourhoodHit ? r.get("expected_neighbourhood") : null));
            }
        }
        return rows;
    }

    private static final class Counts {
        int tp, fp, fn, tn, valueMismatch;

        double precision() {
            return (tp + fp) == 0 ? 1.0 : (double) tp / (tp + fp);
        }

        double recall() {
            return (tp + fn) == 0 ? 1.0 : (double) tp / (tp + fn);
        }
    }

    @Test
    void scoresPerSlotExtractionOnTheHandLabeledSet() throws IOException {
        List<Row> rows = loadRows();
        assertEquals(300, rows.size());

        List<Listing> listings = ListingCsvLoader.load(Path.of("src/test/resources/listings_sample.csv"));
        NeighbourhoodMatcher matcher = new NeighbourhoodMatcher(listings);

        Counts price = new Counts();
        Counts bedroom = new Counts();
        Counts amenity = new Counts();
        Counts neighbourhood = new Counts();

        for (Row row : rows) {
            ParsedQuery parsed = QueryParser.parse(row.queryText(), matcher);

            score(price, row.expectPriceHit(), parsed.maxPriceCents().isPresent(),
                    row.expectPriceHit() && parsed.maxPriceCents().isPresent()
                            && !row.expectPriceCents().equals(parsed.maxPriceCents().get()));

            score(bedroom, row.expectBedroomHit(), parsed.minBedrooms().isPresent(),
                    row.expectBedroomHit() && parsed.minBedrooms().isPresent()
                            && !row.expectBedrooms().equals(parsed.minBedrooms().get()));

            boolean amenityPredictedHit = !parsed.requestedAmenityKeywords().isEmpty();
            score(amenity, row.expectAmenityHit(), amenityPredictedHit,
                    row.expectAmenityHit() && amenityPredictedHit
                            && !row.expectAmenityKeywords().equals(parsed.requestedAmenityKeywords()));

            Optional<String> matchedNeighbourhood = parsed.matchedNeighbourhood();
            score(neighbourhood, row.expectNeighbourhoodHit(), matchedNeighbourhood.isPresent(),
                    row.expectNeighbourhoodHit() && matchedNeighbourhood.isPresent()
                            && !row.expectNeighbourhood().equals(matchedNeighbourhood.get()));
        }

        System.out.println("SLOT_EXTRACTION_BENCHMARK");
        printSlot("price", price, rows.size());
        printSlot("bedroom", bedroom, rows.size());
        printSlot("amenity", amenity, rows.size());
        printSlot("neighbourhood", neighbourhood, rows.size());

        // The evaluation must run over the full hand-labeled set and classify every row one way;
        // whether a given slot's recall is high or low is the measured result, not an assertion.
        assertTrue(price.tp + price.fp + price.fn + price.tn == rows.size());
        assertTrue(bedroom.tp + bedroom.fp + bedroom.fn + bedroom.tn == rows.size());
        assertTrue(amenity.tp + amenity.fp + amenity.fn + amenity.tn == rows.size());
        assertTrue(neighbourhood.tp + neighbourhood.fp + neighbourhood.fn + neighbourhood.tn == rows.size());
    }

    private static void score(Counts c, boolean expectedHit, boolean predictedHit, boolean valueMismatch) {
        if (expectedHit && predictedHit) {
            c.tp++;
            if (valueMismatch) {
                c.valueMismatch++;
            }
        } else if (!expectedHit && predictedHit) {
            c.fp++;
        } else if (expectedHit && !predictedHit) {
            c.fn++;
        } else {
            c.tn++;
        }
    }

    private static void printSlot(String name, Counts c, int total) {
        System.out.printf(
                "%s: tp=%d fp=%d fn=%d tn=%d value_mismatch_on_hit=%d precision=%.4f recall=%.4f%n",
                name, c.tp, c.fp, c.fn, c.tn, c.valueMismatch, c.precision(), c.recall());
    }
}
