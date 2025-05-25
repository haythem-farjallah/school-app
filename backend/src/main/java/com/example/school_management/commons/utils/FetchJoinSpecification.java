package com.example.school_management.commons.utils;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;

public final class FetchJoinSpecification {

    public static <T> Specification<T> fetchRelations(List<String> includes) {
        return (root, query, cb) -> {
            // skip count queries
            if (!includes.isEmpty()) {
                assert query != null;
                if (!isCountQuery(query.getResultType())) {
                    for (String path : includes) {
                        try {
                            root.fetch(path, JoinType.LEFT);
                        } catch (IllegalArgumentException ignore) {
                            // not a valid relation on this entity → skip
                        }
                    }
                    query.distinct(true);
                }
            }
            return null;
        };
    }

    public static <T> Specification<T> joinRelations(List<String> includes) {
        return (root, query, cb) -> {
            // only apply to the main query (not the count query)
            assert query != null;
            if (!isCountQuery(query.getResultType())) {
                for (String path : includes) {
                    try {
                        root.join(path, JoinType.LEFT);
                    } catch (IllegalArgumentException ignored) {
                        // if there's no such relation, skip it
                    }
                }
                query.distinct(true);
            }
            return null;
        };
    }
    private static boolean isCountQuery(Class<?> resultType) {
        return Long.class.equals(resultType) ||
                long.class.equals(resultType)  ||
                Integer.class.equals(resultType) ||
                int.class.equals(resultType);
    }
}
