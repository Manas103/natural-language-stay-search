package com.manas.staysearch.ingest;

import com.manas.staysearch.model.Listing;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the merged, trimmed Inside Airbnb export this project ships a 200-row sample of
 * (`src/test/resources/listings_sample.csv`) and measures against in full (not committed; see
 * `docs/data_manifest.md`). Apache Commons CSV, not a hand-rolled splitter, because Inside
 * Airbnb's own `description` and `amenities` columns routinely contain embedded commas, quotes
 * and newlines that a naive {@code split(",")} would corrupt silently.
 */
public final class ListingCsvLoader {

    private ListingCsvLoader() {
    }

    public static List<Listing> load(Path csvPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            return load(reader);
        }
    }

    public static List<Listing> load(Reader reader) throws IOException {
        List<Listing> listings = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try (CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                listings.add(toListing(record));
            }
        }
        return listings;
    }

    private static Listing toListing(CSVRecord r) {
        return new Listing(
                get(r, "id"),
                get(r, "city"),
                get(r, "name"),
                get(r, "description"),
                get(r, "neighborhood_overview"),
                get(r, "neighbourhood_cleansed"),
                get(r, "property_type"),
                get(r, "room_type"),
                safeInt(get(r, "accommodates"), 1),
                safeInt(get(r, "bedrooms"), 1),
                get(r, "amenities"),
                safePriceCents(get(r, "price")),
                safeInt(get(r, "number_of_reviews"), 0),
                safeDouble(get(r, "review_scores_rating"), 0.0),
                "t".equalsIgnoreCase(get(r, "instant_bookable")),
                "t".equalsIgnoreCase(get(r, "host_is_superhost")));
    }

    private static String get(CSVRecord r, String column) {
        return r.isSet(column) ? r.get(column) : "";
    }

    private static int safeInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double safeDouble(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long safePriceCents(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        String cleaned = value.replace("$", "").replace(",", "").trim();
        try {
            return Math.round(Double.parseDouble(cleaned) * 100);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
