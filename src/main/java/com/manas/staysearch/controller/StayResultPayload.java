package com.manas.staysearch.controller;

import com.manas.staysearch.search.SearchResult;

/** The GraphQL-facing shape of one search result; keeps {@code Listing}'s internal fields (amenities raw text, etc.) off the wire. */
public record StayResultPayload(
        String id, String name, String city, String neighbourhood, String propertyType,
        long priceCents, int bedrooms, double reviewScoreRating, double bm25Score, double rerankScore) {

    public static StayResultPayload from(SearchResult result) {
        var listing = result.listing();
        return new StayResultPayload(
                listing.id(), listing.name(), listing.city(), listing.neighbourhood(), listing.propertyType(),
                listing.priceCents(), listing.bedrooms(), listing.reviewScoreRating(), result.bm25Score(), result.rerankScore());
    }
}
