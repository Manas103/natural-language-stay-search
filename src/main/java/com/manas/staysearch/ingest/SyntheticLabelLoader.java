package com.manas.staysearch.ingest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads the {@code queries.csv} and {@code labels.csv} files `python/generate_synthetic_queries.py` writes. */
public final class SyntheticLabelLoader {

    public record QueryRow(String queryId, String text, String split) {
    }

    public record LabelRow(String queryId, String listingId, int grade) {
    }

    private SyntheticLabelLoader() {
    }

    public static List<QueryRow> loadQueries(Path csvPath) throws IOException {
        List<QueryRow> rows = new ArrayList<>();
        try (var reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            for (CSVRecord r : parser) {
                rows.add(new QueryRow(r.get("query_id"), r.get("query_text"), r.get("split")));
            }
        }
        return rows;
    }

    public static List<LabelRow> loadLabels(Path csvPath) throws IOException {
        List<LabelRow> rows = new ArrayList<>();
        try (var reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            for (CSVRecord r : parser) {
                rows.add(new LabelRow(r.get("query_id"), r.get("listing_id"), Integer.parseInt(r.get("grade"))));
            }
        }
        return rows;
    }

    /** Labels grouped by query id, for NDCG computation (one judged candidate pool per query). */
    public static Map<String, List<LabelRow>> groupByQuery(List<LabelRow> labels) {
        Map<String, List<LabelRow>> byQuery = new HashMap<>();
        for (LabelRow row : labels) {
            byQuery.computeIfAbsent(row.queryId(), k -> new ArrayList<>()).add(row);
        }
        return byQuery;
    }
}
