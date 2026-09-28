package com.example.school_management.commons.utils;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * The entity property paths one filter endpoint accepts from request parameters.
 * A path is matched whole: allowing "enrollment.id" does not allow any other path
 * under "enrollment". Any other path is rejected with 400.
 */
public record FilterFields(Set<String> filterable, Set<String> sortable) {

    public FilterFields {
        filterable = Set.copyOf(filterable);
        sortable = Set.copyOf(sortable);
    }

    void requireFilterable(String path) {
        if (!filterable.contains(path)) {
            throw rejected("Unsupported filter field '" + path + "'");
        }
    }

    void requireSortable(String path) {
        if (!sortable.contains(path)) {
            throw rejected("Unsupported sort field '" + path + "'");
        }
    }

    /** Rejects a Spring Data sort that names a property outside the sortable paths. */
    public void requireSortable(Sort sort) {
        sort.forEach(order -> requireSortable(order.getProperty()));
    }

    static ResponseStatusException rejected(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
