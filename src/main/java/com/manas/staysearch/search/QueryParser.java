package com.manas.staysearch.search;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a free-text stay query ("quiet cabin near a lake, dog friendly, under $150") into the
 * structured filters this project's bullet claims (a price ceiling, a pet-friendly flag, a
 * minimum bedroom count, a recognized amenity ask, a recognized neighbourhood), leaving the rest
 * of the text for {@code Bm25Index}'s semantic leg. This is a rule-based parser, not a trained
 * one: the structured slots a stay query carries are few and regular enough that regex and
 * keyword matching catch them reliably, and a rule-based parser is exactly inspectable, which
 * matters more here than recall on phrasings this project's own query set never produces.
 */
public final class QueryParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$\\s?(\\d+)");
    private static final Pattern BEDROOM_PATTERN = Pattern.compile("(\\d+)[- ]bedroom");
    private static final String[] PET_PHRASES = {"dog friendly", "pet friendly", "pets allowed", "dog-friendly", "pet-friendly"};

    private QueryParser() {
    }

    public static ParsedQuery parse(String text) {
        return parse(text, null);
    }

    public static ParsedQuery parse(String text, NeighbourhoodMatcher neighbourhoodMatcher) {
        String lower = text.toLowerCase();

        Optional<Long> maxPriceCents = Optional.empty();
        Matcher priceMatcher = PRICE_PATTERN.matcher(text);
        if (priceMatcher.find()) {
            maxPriceCents = Optional.of(Long.parseLong(priceMatcher.group(1)) * 100);
        }

        boolean petFriendly = false;
        for (String phrase : PET_PHRASES) {
            if (lower.contains(phrase)) {
                petFriendly = true;
                break;
            }
        }

        Optional<Integer> minBedrooms = Optional.empty();
        Matcher bedroomMatcher = BEDROOM_PATTERN.matcher(lower);
        if (bedroomMatcher.find()) {
            minBedrooms = Optional.of(Integer.parseInt(bedroomMatcher.group(1)));
        }

        List<String> amenityKeywords = List.of();
        for (var entry : AmenityKeywords.phraseToKeywords().entrySet()) {
            if (lower.contains(entry.getKey())) {
                amenityKeywords = entry.getValue();
                break;
            }
        }

        Optional<String> matchedNeighbourhood = neighbourhoodMatcher == null
                ? Optional.empty()
                : neighbourhoodMatcher.find(text);

        return new ParsedQuery(text, maxPriceCents, petFriendly, minBedrooms, amenityKeywords, matchedNeighbourhood);
    }
}
