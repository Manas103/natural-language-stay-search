package com.manas.staysearch.search;

import com.manas.staysearch.model.Listing;

import java.util.Locale;

/**
 * Builds the 4-feature vector {@code LogisticReranker} trains and scores on. Every feature is
 * bounded to roughly [0, 1] so no single feature's raw scale can dominate the learned weights by
 * accident; {@code bm25Score / (bm25Score + 1)} is a standard bounded monotonic transform that
 * avoids needing a corpus-wide maximum to normalize against.
 *
 * <p>A first version of this class also carried {@code reviewScoreRating}, {@code hostIsSuperhost}
 * and {@code instantBookable} as features. Those are real listing attributes, but
 * {@code generate_synthetic_queries.py}'s grading never considers any of them (a label is 3, 2, 1
 * or 0 purely from neighbourhood, price and amenity match), so training on them added three
 * uninformative dimensions that measurably hurt NDCG@10 relative to the BM25 baseline (see README
 * Findings for the measured before/after). They were removed, not reweighted, once that was
 * root-caused: a feature with no relationship to the label is noise, not a weak signal.
 */
public final class FeatureExtractor {

    private FeatureExtractor() {
    }

    public static double[] extract(Listing listing, ParsedQuery query, double bm25Score) {
        double bm25Normalized = bm25Score / (bm25Score + 1.0);

        double priceFit = 1.0;
        if (query.maxPriceCents().isPresent()) {
            long ceiling = query.maxPriceCents().get();
            if (listing.priceCents() > ceiling) {
                double overshoot = (listing.priceCents() - ceiling) / (double) Math.max(1, ceiling);
                priceFit = Math.max(0.0, 1.0 - overshoot);
            }
        }

        double bedroomFit = 1.0;
        if (query.minBedrooms().isPresent()) {
            bedroomFit = listing.bedrooms() >= query.minBedrooms().get() ? 1.0 : 0.0;
        }

        double amenityFit = 1.0;
        if (!query.requestedAmenityKeywords().isEmpty()) {
            String amenities = listing.amenitiesRaw() == null ? "" : listing.amenitiesRaw().toLowerCase(Locale.ROOT);
            boolean hasAmenity = query.requestedAmenityKeywords().stream().anyMatch(amenities::contains);
            amenityFit = hasAmenity ? 1.0 : 0.0;
        }

        return new double[]{bm25Normalized, priceFit, bedroomFit, amenityFit};
    }

    public static int featureCount() {
        return 4;
    }
}
