package com.manas.staysearch.search;

import com.manas.staysearch.ingest.ListingCsvLoader;
import com.manas.staysearch.model.Listing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Bm25IndexTest {

    private static List<Listing> listings;
    private static Bm25Index index;

    @BeforeAll
    static void load() throws IOException {
        listings = ListingCsvLoader.load(Path.of("src/test/resources/listings_sample.csv"));
        index = new Bm25Index(listings);
    }

    @Test
    void sizeMatchesCorpus() {
        assertEquals(listings.size(), index.size());
    }

    @Test
    void documentScoresHigherOnItsOwnDistinctiveTokens() {
        // Find a listing with a distinctive, rare word in its name, and confirm it beats a
        // document that does not contain that word at all.
        Listing target = listings.stream()
                .filter(l -> l.name() != null && l.name().toLowerCase().contains("balcon"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("fixture must contain a listing with 'balcony' in its name"));
        int targetIndex = listings.indexOf(target);
        int otherIndex = (targetIndex + 1) % listings.size();

        List<String> queryTokens = Tokenizer.tokenize("balcony");
        double targetScore = index.score(queryTokens, targetIndex);
        assertTrue(targetScore > 0, "the matching document must score above zero");

        boolean otherAlsoMatches = listings.get(otherIndex).searchableText().toLowerCase().contains("balcon");
        if (!otherAlsoMatches) {
            double otherScore = index.score(queryTokens, otherIndex);
            assertTrue(targetScore > otherScore);
        }
    }

    @Test
    void candidateDocIndicesOnlyReturnsDocumentsContainingAQueryToken() {
        List<String> queryTokens = Tokenizer.tokenize("balcony");
        List<Integer> candidates = index.candidateDocIndices(queryTokens);
        for (int docIndex : candidates) {
            assertTrue(listings.get(docIndex).searchableText().toLowerCase().contains("balcon"));
        }
    }

    @Test
    void emptyQueryHasNoCandidates() {
        assertTrue(index.candidateDocIndices(List.of()).isEmpty());
    }
}
