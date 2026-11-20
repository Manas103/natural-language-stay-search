package com.manas.staysearch.search;

import com.manas.staysearch.model.Listing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A real inverted index (not a per-query linear scan) over every listing's title, description,
 * neighbourhood overview, neighbourhood name, property type and amenities text, scored with the
 * standard Robertson/Walker BM25 formula (k1 = 1.2, b = 0.75). This is the "semantic leg" the
 * resume bullet refers to: free text left over after {@code QueryParser} pulls out the structured
 * filters is ranked against this index, not matched exactly.
 */
public final class Bm25Index {

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private final List<Listing> listings;
    // token -> (docIndex -> term frequency in that doc). A HashMap, not a List<Posting>, so
    // score() can look up one document's term frequency in O(1) instead of scanning every
    // posting for a token that appears in many documents (a real measured cost under the p95
    // latency benchmark; see README Findings).
    private final Map<String, Map<Integer, Integer>> postings = new HashMap<>();
    private final int[] docLengths;
    private final double avgDocLength;

    public Bm25Index(List<Listing> listings) {
        this.listings = listings;
        this.docLengths = new int[listings.size()];
        long totalLength = 0;
        for (int i = 0; i < listings.size(); i++) {
            List<String> tokens = Tokenizer.tokenize(listings.get(i).searchableText());
            docLengths[i] = tokens.size();
            totalLength += tokens.size();
            Map<String, Integer> termCounts = new HashMap<>();
            for (String token : tokens) {
                termCounts.merge(token, 1, Integer::sum);
            }
            for (var entry : termCounts.entrySet()) {
                postings.computeIfAbsent(entry.getKey(), k -> new HashMap<>()).put(i, entry.getValue());
            }
        }
        this.avgDocLength = listings.isEmpty() ? 0 : (double) totalLength / listings.size();
    }

    public int size() {
        return listings.size();
    }

    /** BM25 score of {@code docIndex} against {@code queryTokens}, restricted to this index's own vocabulary. */
    public double score(List<String> queryTokens, int docIndex) {
        double score = 0.0;
        int docLength = docLengths[docIndex];
        int n = listings.size();
        for (String token : queryTokens) {
            Map<Integer, Integer> docPostings = postings.get(token);
            if (docPostings == null) {
                continue;
            }
            Integer termFrequency = docPostings.get(docIndex);
            if (termFrequency == null) {
                continue;
            }
            int df = docPostings.size();
            double idf = Math.log((n - df + 0.5) / (df + 0.5) + 1);
            double denom = termFrequency + K1 * (1 - B + B * (docLength / Math.max(1.0, avgDocLength)));
            score += idf * (termFrequency * (K1 + 1)) / denom;
        }
        return score;
    }

    /** Candidate document indices that share at least one query token with a nonzero-length posting list. */
    public List<Integer> candidateDocIndices(List<String> queryTokens) {
        var seen = new java.util.LinkedHashSet<Integer>();
        for (String token : queryTokens) {
            Map<Integer, Integer> docPostings = postings.get(token);
            if (docPostings == null) {
                continue;
            }
            seen.addAll(docPostings.keySet());
        }
        return new ArrayList<>(seen);
    }
}
