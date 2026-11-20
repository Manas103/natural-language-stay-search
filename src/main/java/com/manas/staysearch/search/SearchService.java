package com.manas.staysearch.search;

import com.manas.staysearch.model.Listing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns a free-text query into structured filters plus a semantic leg ({@code QueryParser} and
 * {@code Bm25Index}), then reranks the BM25 candidate set with {@code LogisticReranker}. If the
 * structured filters eliminate every BM25 candidate (a real possibility: a price ceiling plus a
 * rare amenity ask can be empty), this falls back to the unfiltered candidate set rather than
 * returning nothing, a deliberate product choice, not an oversight; it is called out once here so
 * it is not mistaken for a bug when a filtered search is empty.
 */
public final class SearchService {

    private final ListingCorpus corpus;
    private final LogisticReranker reranker;

    public SearchService(ListingCorpus corpus, LogisticReranker reranker) {
        this.corpus = corpus;
        this.reranker = reranker;
    }

    public List<SearchResult> search(String queryText, int limit) {
        ParsedQuery parsed = QueryParser.parse(queryText, corpus.neighbourhoodMatcher());
        List<String> tokens = Tokenizer.tokenize(queryText);
        List<Integer> pool = filteredCandidatePool(tokens, parsed);

        List<SearchResult> results = new ArrayList<>();
        for (int docIndex : pool) {
            Listing listing = corpus.byIndex(docIndex);
            double bm25 = corpus.bm25Index().score(tokens, docIndex);
            double[] features = FeatureExtractor.extract(listing, parsed, bm25);
            double rerankScore = reranker.score(features);
            results.add(new SearchResult(listing, bm25, rerankScore));
        }
        results.sort(Comparator.comparingDouble(SearchResult::rerankScore).reversed());
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    /** BM25-only ranking (no reranker), used by the NDCG benchmark as the baseline to beat. */
    public List<SearchResult> searchBm25Only(String queryText, int limit) {
        ParsedQuery parsed = QueryParser.parse(queryText, corpus.neighbourhoodMatcher());
        List<String> tokens = Tokenizer.tokenize(queryText);
        List<Integer> pool = filteredCandidatePool(tokens, parsed);

        List<SearchResult> results = new ArrayList<>();
        for (int docIndex : pool) {
            double bm25 = corpus.bm25Index().score(tokens, docIndex);
            results.add(new SearchResult(corpus.byIndex(docIndex), bm25, bm25));
        }
        results.sort(Comparator.comparingDouble(SearchResult::bm25Score).reversed());
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    private List<Integer> filteredCandidatePool(List<String> tokens, ParsedQuery parsed) {
        List<Integer> candidates = corpus.bm25Index().candidateDocIndices(tokens);
        List<Integer> filtered = new ArrayList<>();
        for (int docIndex : candidates) {
            if (satisfiesFilters(corpus.byIndex(docIndex), parsed)) {
                filtered.add(docIndex);
            }
        }
        return filtered.isEmpty() ? candidates : filtered;
    }

    private static boolean satisfiesFilters(Listing listing, ParsedQuery parsed) {
        if (parsed.maxPriceCents().isPresent() && listing.priceCents() > parsed.maxPriceCents().get()) {
            return false;
        }
        if (parsed.minBedrooms().isPresent() && listing.bedrooms() < parsed.minBedrooms().get()) {
            return false;
        }
        if (!parsed.requestedAmenityKeywords().isEmpty()) {
            String amenities = listing.amenitiesRaw() == null ? "" : listing.amenitiesRaw().toLowerCase();
            if (parsed.requestedAmenityKeywords().stream().noneMatch(amenities::contains)) {
                return false;
            }
        }
        if (parsed.matchedNeighbourhood().isPresent()) {
            String neighbourhood = listing.neighbourhood() == null ? "" : listing.neighbourhood().toLowerCase();
            if (!neighbourhood.equals(parsed.matchedNeighbourhood().get())) {
                return false;
            }
        }
        return true;
    }
}
