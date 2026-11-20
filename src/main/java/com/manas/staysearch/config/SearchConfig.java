package com.manas.staysearch.config;

import com.manas.staysearch.ingest.ListingCsvLoader;
import com.manas.staysearch.ingest.SyntheticLabelLoader;
import com.manas.staysearch.search.ListingCorpus;
import com.manas.staysearch.search.LogisticReranker;
import com.manas.staysearch.search.RerankerTrainer;
import com.manas.staysearch.search.SearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class SearchConfig {

    @Bean
    public ListingCorpus listingCorpus(@Value("${app.data.listings-csv-path}") String listingsCsvPath) throws IOException {
        return new ListingCorpus(ListingCsvLoader.load(Path.of(listingsCsvPath)));
    }

    /**
     * Trained at startup from whatever synthetic training data is configured. If no queries/labels
     * path is set (the default, fast-boot profile this repository's own tests use), a fresh,
     * untrained reranker is returned; the real training run for the measured NDCG@10 claim points
     * {@code app.data.queries-csv-path}/{@code app.data.labels-csv-path} at the full output of
     * `python/generate_synthetic_queries.py`.
     */
    @Bean
    public LogisticReranker logisticReranker(ListingCorpus corpus,
                                               @Value("${app.data.queries-csv-path:}") String queriesCsvPath,
                                               @Value("${app.data.labels-csv-path:}") String labelsCsvPath,
                                               @Value("${app.reranker.epochs:200}") int epochs,
                                               @Value("${app.reranker.learning-rate:0.5}") double learningRate) throws IOException {
        if (queriesCsvPath.isBlank() || labelsCsvPath.isBlank() || !Files.exists(Path.of(queriesCsvPath))) {
            return new LogisticReranker(com.manas.staysearch.search.FeatureExtractor.featureCount());
        }
        List<SyntheticLabelLoader.QueryRow> allQueries = SyntheticLabelLoader.loadQueries(Path.of(queriesCsvPath));
        List<SyntheticLabelLoader.LabelRow> allLabels = SyntheticLabelLoader.loadLabels(Path.of(labelsCsvPath));
        List<SyntheticLabelLoader.QueryRow> trainQueries = allQueries.stream().filter(q -> "train".equals(q.split())).toList();
        return RerankerTrainer.train(corpus, trainQueries, allLabels, epochs, learningRate);
    }

    @Bean
    public SearchService searchService(ListingCorpus corpus, LogisticReranker reranker) {
        return new SearchService(corpus, reranker);
    }
}
