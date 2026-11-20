package com.manas.staysearch.controller;

import com.manas.staysearch.search.SearchService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/** {@code searchStays} is served as a GraphQL field, not a REST endpoint, per the resume claim. */
@Controller
public class StaySearchGraphQlController {

    private final SearchService searchService;

    public StaySearchGraphQlController(SearchService searchService) {
        this.searchService = searchService;
    }

    @QueryMapping
    public List<StayResultPayload> searchStays(@Argument String query, @Argument Integer limit) {
        int effectiveLimit = limit == null ? 10 : limit;
        return searchService.search(query, effectiveLimit).stream().map(StayResultPayload::from).toList();
    }
}
