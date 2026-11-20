package com.manas.staysearch.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryParserTest {

    @Test
    void parsesPriceCeiling() {
        ParsedQuery parsed = QueryParser.parse("quiet cabin near a lake, dog friendly, under $150");
        assertEquals(15000L, parsed.maxPriceCents().orElseThrow());
    }

    @Test
    void detectsPetFriendly() {
        assertTrue(QueryParser.parse("a pet friendly apartment").petFriendly());
        assertTrue(QueryParser.parse("dog-friendly house").petFriendly());
        assertFalse(QueryParser.parse("a quiet apartment downtown").petFriendly());
    }

    @Test
    void parsesBedroomCount() {
        ParsedQuery parsed = QueryParser.parse("looking for a cozy 3-bedroom place in Soho");
        assertEquals(3, parsed.minBedrooms().orElseThrow());
    }

    @Test
    void handlesQueryWithNoStructuredFilters() {
        ParsedQuery parsed = QueryParser.parse("a nice place to stay");
        assertTrue(parsed.maxPriceCents().isEmpty());
        assertTrue(parsed.minBedrooms().isEmpty());
        assertFalse(parsed.petFriendly());
    }
}
