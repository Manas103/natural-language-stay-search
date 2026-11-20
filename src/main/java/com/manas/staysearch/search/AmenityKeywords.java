package com.manas.staysearch.search;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The same 12 amenity phrase-to-keyword mappings {@code python/seed_queries.py} uses to ground a
 * synthetic query on a listing that genuinely has the amenity it asks for. {@code QueryParser}
 * uses this to recognize any of the 12 phrases in free text (not only "dog/pet friendly", the one
 * phrase the first version of this parser recognized, a real gap found and fixed; see README
 * Findings), and {@code FeatureExtractor} uses it to check a candidate listing's real amenities
 * text against whichever phrase the query asked for.
 */
public final class AmenityKeywords {

    private static final Map<String, List<String>> PHRASE_TO_KEYWORDS = new LinkedHashMap<>();

    static {
        PHRASE_TO_KEYWORDS.put("dog friendly", List.of("pet", "dog"));
        PHRASE_TO_KEYWORDS.put("pet friendly", List.of("pet", "dog"));
        PHRASE_TO_KEYWORDS.put("pets allowed", List.of("pet", "dog"));
        PHRASE_TO_KEYWORDS.put("with a pool", List.of("pool"));
        PHRASE_TO_KEYWORDS.put("with free parking", List.of("parking"));
        PHRASE_TO_KEYWORDS.put("with a washer", List.of("washer"));
        PHRASE_TO_KEYWORDS.put("with wifi", List.of("wifi", "internet"));
        PHRASE_TO_KEYWORDS.put("with air conditioning", List.of("air conditioning", "ac"));
        PHRASE_TO_KEYWORDS.put("with a full kitchen", List.of("kitchen"));
        PHRASE_TO_KEYWORDS.put("with an elevator", List.of("elevator"));
        PHRASE_TO_KEYWORDS.put("with a balcony", List.of("balcony", "patio"));
        PHRASE_TO_KEYWORDS.put("with a hot tub", List.of("hot tub"));
        PHRASE_TO_KEYWORDS.put("with a workspace", List.of("workspace", "desk"));
        PHRASE_TO_KEYWORDS.put("with a gym", List.of("gym"));
    }

    private AmenityKeywords() {
    }

    public static Map<String, List<String>> phraseToKeywords() {
        return PHRASE_TO_KEYWORDS;
    }
}
