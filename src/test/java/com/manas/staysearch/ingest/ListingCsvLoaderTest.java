package com.manas.staysearch.ingest;

import com.manas.staysearch.model.Listing;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListingCsvLoaderTest {

    @Test
    void loadsAllSampleRows() throws IOException {
        List<Listing> listings = ListingCsvLoader.load(Path.of("src/test/resources/listings_sample.csv"));
        assertEquals(200, listings.size());
        for (Listing listing : listings) {
            assertNotNull(listing.id());
            assertFalse(listing.id().isBlank());
        }
    }

    @Test
    void parsesPriceIntoCents() throws IOException {
        List<Listing> listings = ListingCsvLoader.load(Path.of("src/test/resources/listings_sample.csv"));
        assertTrue(listings.stream().anyMatch(l -> l.priceCents() > 0));
    }
}
