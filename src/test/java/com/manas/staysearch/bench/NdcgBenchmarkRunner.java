package com.manas.staysearch.bench;

import com.manas.staysearch.ingest.ListingCsvLoader;
import com.manas.staysearch.ingest.SyntheticLabelLoader;
import com.manas.staysearch.ingest.SyntheticLabelLoader.LabelRow;
import com.manas.staysearch.ingest.SyntheticLabelLoader.QueryRow;
import com.manas.staysearch.search.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measures "NDCG@10 up 31% over a BM25 baseline on a 1,000-query labeled set": the reranker is
 * trained on the train-split rows only (see {@code RerankerTrainer}), then both BM25-only ranking
 * and reranked ranking are measured on the held-out 1,000-query eval split, which the reranker
 * never saw during training.
 *
 * <p>Requires the full, real 199,021-listing corpus and the synthetic query/label files this
 * project's own {@code python/} scripts generate; neither is committed (see
 * {@code docs/data_manifest.md}). Point at them with:
 * {@code -Dapp.bench.listings-csv-path=<path> -Dapp.bench.data-dir=<dir>}
 */
@Tag("benchmark")
class NdcgBenchmarkRunner {

    @Test
    void measureNdcgImprovementOverBm25Baseline() throws Exception {
        String listingsCsvPath = requireProperty("app.bench.listings-csv-path");
        String dataDir = requireProperty("app.bench.data-dir");

        var listings = ListingCsvLoader.load(Path.of(listingsCsvPath));
        ListingCorpus corpus = new ListingCorpus(listings);
        System.out.println("corpus size: " + corpus.size());

        List<QueryRow> allQueries = SyntheticLabelLoader.loadQueries(Path.of(dataDir, "queries.csv"));
        List<LabelRow> allLabels = SyntheticLabelLoader.loadLabels(Path.of(dataDir, "labels.csv"));
        List<QueryRow> trainQueries = allQueries.stream().filter(q -> "train".equals(q.split())).toList();
        List<QueryRow> evalQueries = allQueries.stream().filter(q -> "eval".equals(q.split())).toList();
        System.out.println("train queries: " + trainQueries.size() + ", eval queries: " + evalQueries.size());

        LogisticReranker reranker = RerankerTrainer.train(corpus, trainQueries, allLabels, 300, 0.5);
        SearchService searchService = new SearchService(corpus, reranker);

        Map<String, List<LabelRow>> labelsByQuery = SyntheticLabelLoader.groupByQuery(allLabels);

        double bm25TotalNdcg = 0.0;
        double rerankTotalNdcg = 0.0;
        int evaluated = 0;

        for (QueryRow query : evalQueries) {
            List<LabelRow> judgedRows = labelsByQuery.get(query.queryId());
            if (judgedRows == null || judgedRows.isEmpty()) {
                continue;
            }
            // A listing can legitimately be picked for two different judged roles of the same
            // query (e.g. as both a "strong partial" and a random non-match); keep the higher
            // grade rather than letting Collectors.toMap throw on the duplicate key.
            Map<String, Integer> judgments = judgedRows.stream()
                    .collect(java.util.stream.Collectors.toMap(LabelRow::listingId, LabelRow::grade, Math::max));

            List<String> bm25Ranking = searchService.searchBm25Only(query.text(), 10).stream()
                    .map(r -> r.listing().id()).toList();
            List<String> rerankRanking = searchService.search(query.text(), 10).stream()
                    .map(r -> r.listing().id()).toList();

            bm25TotalNdcg += NdcgCalculator.ndcgAtK(bm25Ranking, judgments, 10);
            rerankTotalNdcg += NdcgCalculator.ndcgAtK(rerankRanking, judgments, 10);
            evaluated++;
        }

        double bm25MeanNdcg = bm25TotalNdcg / evaluated;
        double rerankMeanNdcg = rerankTotalNdcg / evaluated;
        double relativeImprovementPct = ((rerankMeanNdcg - bm25MeanNdcg) / bm25MeanNdcg) * 100;

        System.out.println("NDCG_BENCHMARK");
        System.out.println("evaluated_queries=" + evaluated);
        System.out.println("bm25_mean_ndcg_at_10=" + bm25MeanNdcg);
        System.out.println("rerank_mean_ndcg_at_10=" + rerankMeanNdcg);
        System.out.println("relative_improvement_pct=" + relativeImprovementPct);

        assertTrue(evaluated > 900, "expected close to the full 1,000-query eval set to be judgeable");
    }

    private static String requireProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing required system property " + key + "; see this class's javadoc");
        }
        return value;
    }
}
