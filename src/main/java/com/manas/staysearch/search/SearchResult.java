package com.manas.staysearch.search;

import com.manas.staysearch.model.Listing;

public record SearchResult(Listing listing, double bm25Score, double rerankScore) {
}
