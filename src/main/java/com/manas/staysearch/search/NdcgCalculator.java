package com.manas.staysearch.search;

import java.util.List;
import java.util.Map;

/** Standard Jarvik/Kekalainen NDCG@k: a listing not in the judged pool for a query counts as grade 0. */
public final class NdcgCalculator {

    private NdcgCalculator() {
    }

    public static double ndcgAtK(List<String> rankedListingIds, Map<String, Integer> judgments, int k) {
        double dcg = 0.0;
        int limit = Math.min(k, rankedListingIds.size());
        for (int i = 0; i < limit; i++) {
            int grade = judgments.getOrDefault(rankedListingIds.get(i), 0);
            dcg += (Math.pow(2, grade) - 1) / log2(i + 2);
        }

        List<Integer> idealGrades = judgments.values().stream()
                .sorted((a, b) -> b - a)
                .toList();
        double idcg = 0.0;
        int idealLimit = Math.min(k, idealGrades.size());
        for (int i = 0; i < idealLimit; i++) {
            idcg += (Math.pow(2, idealGrades.get(i)) - 1) / log2(i + 2);
        }

        return idcg == 0.0 ? 0.0 : dcg / idcg;
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
