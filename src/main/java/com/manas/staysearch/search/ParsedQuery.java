package com.manas.staysearch.search;

import java.util.List;
import java.util.Optional;

/**
 * The structured filters {@code QueryParser} pulls out of a free-text query, plus the original
 * text for the semantic leg. {@code requestedAmenityKeywords} is empty when the query did not ask
 * for any of the 12 recognized amenity phrases (see {@code AmenityKeywords}).
 */
public record ParsedQuery(String originalText, Optional<Long> maxPriceCents, boolean petFriendly,
                           Optional<Integer> minBedrooms, List<String> requestedAmenityKeywords,
                           Optional<String> matchedNeighbourhood) {
}
