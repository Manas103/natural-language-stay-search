package com.manas.staysearch.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Lowercase, split on non-alphanumeric, drop a small stopword list and single-character tokens. */
public final class Tokenizer {

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "in", "on", "at", "for", "of", "to", "with", "and", "or", "is", "it",
            "near", "under", "per", "night", "stay", "place", "looking", "need", "somewhere");

    private Tokenizer() {
    }

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        for (String raw : text.toLowerCase().split("[^a-z0-9]+")) {
            if (raw.length() > 1 && !STOPWORDS.contains(raw)) {
                tokens.add(raw);
            }
        }
        return tokens;
    }
}
