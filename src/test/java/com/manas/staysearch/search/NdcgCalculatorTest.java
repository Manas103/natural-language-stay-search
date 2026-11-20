package com.manas.staysearch.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NdcgCalculatorTest {

    @Test
    void perfectRankingScoresOne() {
        Map<String, Integer> judgments = Map.of("a", 3, "b", 2, "c", 1, "d", 0);
        double ndcg = NdcgCalculator.ndcgAtK(List.of("a", "b", "c", "d"), judgments, 10);
        assertEquals(1.0, ndcg, 1e-9);
    }

    @Test
    void reversedRankingScoresBelowOne() {
        Map<String, Integer> judgments = Map.of("a", 3, "b", 2, "c", 1, "d", 0);
        double perfect = NdcgCalculator.ndcgAtK(List.of("a", "b", "c", "d"), judgments, 10);
        double reversed = NdcgCalculator.ndcgAtK(List.of("d", "c", "b", "a"), judgments, 10);
        assertEquals(1.0, perfect, 1e-9);
        assertTrue(reversed < 1.0, "worst-to-best order must score strictly below the perfect order");
    }

    @Test
    void noJudgmentsAtAllScoresZero() {
        double ndcg = NdcgCalculator.ndcgAtK(List.of("x", "y"), Map.of(), 10);
        assertEquals(0.0, ndcg, 1e-9);
    }

    @Test
    void unjudgedListingsCountAsGradeZero() {
        Map<String, Integer> judgments = Map.of("a", 3);
        double ndcgWithJunkFirst = NdcgCalculator.ndcgAtK(List.of("unknown", "a"), judgments, 10);
        double ndcgWithGoodFirst = NdcgCalculator.ndcgAtK(List.of("a", "unknown"), judgments, 10);
        assertEquals(1.0, ndcgWithGoodFirst, 1e-9);
        assertTrue(ndcgWithJunkFirst < ndcgWithGoodFirst);
    }
}
