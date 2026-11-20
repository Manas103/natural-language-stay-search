package com.manas.staysearch.search;

import com.manas.staysearch.ingest.ListingCsvLoader;
import com.manas.staysearch.model.Listing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchServiceTest {

    private static SearchService sampleCorpusSearchService;

    @BeforeAll
    static void setUp() throws IOException {
        var listings = ListingCsvLoader.load(Path.of("src/test/resources/listings_sample.csv"));
        ListingCorpus corpus = new ListingCorpus(listings);
        sampleCorpusSearchService = new SearchService(corpus, new LogisticReranker(FeatureExtractor.featureCount()));
    }

    @Test
    void searchReturnsAtMostTheRequestedLimit() {
        var results = sampleCorpusSearchService.search("balcony apartment", 5);
        assertTrue(results.size() <= 5);
    }

    @Test
    void priceFilterExcludesListingsAboveTheCeilingWhenAnySurvive() {
        Listing cheap = listing("cheap-1", 5000, 1, "wifi, kitchen");
        Listing expensive = listing("expensive-1", 30000, 1, "wifi, kitchen, pool");
        ListingCorpus corpus = new ListingCorpus(List.of(cheap, expensive));
        SearchService service = new SearchService(corpus, new LogisticReranker(FeatureExtractor.featureCount()));

        var results = service.search("apartment with wifi under $100", 10);
        assertEquals(1, results.size());
        assertEquals("cheap-1", results.get(0).listing().id());
    }

    @Test
    void falseBackToUnfilteredWhenFilterEliminatesEveryCandidate() {
        Listing onlyOption = listing("only-1", 30000, 1, "wifi, kitchen");
        ListingCorpus corpus = new ListingCorpus(List.of(onlyOption));
        SearchService service = new SearchService(corpus, new LogisticReranker(FeatureExtractor.featureCount()));

        // the price ceiling excludes the only listing; the service must fall back rather than return nothing
        var results = service.search("apartment with wifi under $10", 10);
        assertEquals(1, results.size());
    }

    @Test
    void emptyCorpusReturnsNoResults() {
        ListingCorpus emptyCorpus = new ListingCorpus(List.of());
        SearchService emptyService = new SearchService(emptyCorpus, new LogisticReranker(FeatureExtractor.featureCount()));
        assertTrue(emptyService.search("anything", 10).isEmpty());
    }

    private static Listing listing(String id, long priceCents, int bedrooms, String amenities) {
        return new Listing(id, "TestCity", "a nice apartment with wifi", "a description", "overview",
                "TestNeighbourhood", "Entire apartment", "Entire home/apt", 2, bedrooms, amenities,
                priceCents, 10, 4.8, true, false);
    }
}
