package com.manas.staysearch.search;

import com.manas.staysearch.ingest.SyntheticLabelLoader.LabelRow;
import com.manas.staysearch.ingest.SyntheticLabelLoader.QueryRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the training set {@code LogisticReranker} fits on: for every (train-split query, judged
 * listing) pair, the 7 features {@code FeatureExtractor} would compute at serve time, labeled 1 if
 * the synthetic grade is &gt;= 2 (the same binarization {@code audit_sample.py} uses) and 0
 * otherwise. Trained with no traffic of its own, exactly as the resume bullet states: every row
 * here comes from `generate_synthetic_queries.py`, never from a real query log.
 */
public final class RerankerTrainer {

    private RerankerTrainer() {
    }

    public static LogisticReranker train(ListingCorpus corpus, List<QueryRow> queries, List<LabelRow> labels,
                                          int epochs, double learningRate) {
        Map<String, String> queryTextById = queries.stream()
                .collect(java.util.stream.Collectors.toMap(QueryRow::queryId, QueryRow::text));

        List<double[]> featureRows = new ArrayList<>();
        List<Integer> binaryLabels = new ArrayList<>();

        for (LabelRow label : labels) {
            String queryText = queryTextById.get(label.queryId());
            if (queryText == null) {
                continue;
            }
            Integer docIndex = corpus.indexOf(label.listingId());
            if (docIndex == null) {
                continue;
            }
            ParsedQuery parsed = QueryParser.parse(queryText);
            List<String> tokens = Tokenizer.tokenize(queryText);
            double bm25 = corpus.bm25Index().score(tokens, docIndex);
            double[] features = FeatureExtractor.extract(corpus.byIndex(docIndex), parsed, bm25);
            featureRows.add(features);
            binaryLabels.add(label.grade() >= 2 ? 1 : 0);
        }

        LogisticReranker reranker = new LogisticReranker(FeatureExtractor.featureCount());
        if (!featureRows.isEmpty()) {
            reranker.train(featureRows, binaryLabels, epochs, learningRate);
        }
        return reranker;
    }
}
