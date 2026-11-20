package com.manas.staysearch.search;

import com.manas.staysearch.model.Listing;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Recognizes a mention of one of the corpus's own real neighbourhood names in free text. Added
 * after measuring that the structured filters (price, bedrooms, amenity) alone left a huge,
 * lexically noisy candidate pool for common amenities, burying the one genuinely relevant listing
 * among tens of thousands sharing a generic word; see README Findings for the measured before and
 * after. This closes the gap: a query naming a real neighbourhood (every synthetic query here
 * does) is now also filtered to that neighbourhood, the single highest-precision structured
 * signal a stay query carries.
 */
public final class NeighbourhoodMatcher {

    private final Set<String> neighbourhoods;

    public NeighbourhoodMatcher(List<Listing> listings) {
        this.neighbourhoods = new HashSet<>();
        for (Listing listing : listings) {
            if (listing.neighbourhood() != null && !listing.neighbourhood().isBlank()) {
                neighbourhoods.add(listing.neighbourhood().toLowerCase());
            }
        }
    }

    /** The longest known neighbourhood name that appears as a substring of {@code text}, if any. */
    public Optional<String> find(String text) {
        String lower = text.toLowerCase();
        String best = null;
        for (String neighbourhood : neighbourhoods) {
            if (lower.contains(neighbourhood) && (best == null || neighbourhood.length() > best.length())) {
                best = neighbourhood;
            }
        }
        return Optional.ofNullable(best);
    }
}
