package com.manas.staysearch.search;

import com.manas.staysearch.model.Listing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The in-memory corpus a {@code SearchService} serves from: the listing list, its BM25 index, and an id-to-index map. */
public final class ListingCorpus {

    private final List<Listing> listings;
    private final Bm25Index bm25Index;
    private final Map<String, Integer> idToIndex;
    private final NeighbourhoodMatcher neighbourhoodMatcher;

    public ListingCorpus(List<Listing> listings) {
        this.listings = listings;
        this.bm25Index = new Bm25Index(listings);
        this.idToIndex = new HashMap<>();
        for (int i = 0; i < listings.size(); i++) {
            idToIndex.put(listings.get(i).id(), i);
        }
        this.neighbourhoodMatcher = new NeighbourhoodMatcher(listings);
    }

    public NeighbourhoodMatcher neighbourhoodMatcher() {
        return neighbourhoodMatcher;
    }

    public List<Listing> listings() {
        return listings;
    }

    public Bm25Index bm25Index() {
        return bm25Index;
    }

    public Listing byIndex(int index) {
        return listings.get(index);
    }

    public Integer indexOf(String listingId) {
        return idToIndex.get(listingId);
    }

    public int size() {
        return listings.size();
    }
}
