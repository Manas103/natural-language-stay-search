package com.manas.staysearch.model;

/** One real Inside Airbnb listing, trimmed to the columns this project's search actually uses. */
public record Listing(
        String id,
        String city,
        String name,
        String description,
        String neighborhoodOverview,
        String neighbourhood,
        String propertyType,
        String roomType,
        int accommodates,
        int bedrooms,
        String amenitiesRaw,
        long priceCents,
        int numberOfReviews,
        double reviewScoreRating,
        boolean instantBookable,
        boolean hostIsSuperhost) {

    /** The text BM25 indexes: title, description, neighbourhood overview and amenities, lowercased by the tokenizer. */
    public String searchableText() {
        return String.join(" ",
                nullToEmpty(name), nullToEmpty(description), nullToEmpty(neighborhoodOverview),
                nullToEmpty(neighbourhood), nullToEmpty(propertyType), nullToEmpty(amenitiesRaw));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
