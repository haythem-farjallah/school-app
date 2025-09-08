package com.example.school_management.commons.utils;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class EntitySpecification {

    private EntitySpecification() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Builds a Specification that compares the given attribute (which can be nested,
     * e.g. "level.id" or use the "Id" suffix shortcut "levelId") against the provided value.
     * Supports String (LIKE), Boolean, Long, Integer, Double; easily extendable.
     */
    public static <T> Specification<T> hasAttribute(String attribute, String value) {
        return (root, query, cb) -> {
            if (attribute == null || value == null) {
                return null;
            }

            Path<?> path = resolvePath(root, attribute);
            Class<?> javaType = path.getJavaType();

            // STRING → LIKE %value%
            if (String.class.equals(javaType)) {
                return cb.like(path.as(String.class), "%" + value + "%");

                // BOOLEAN → exact match
            } else if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
                return cb.equal(path.as(Boolean.class), Boolean.valueOf(value));

                // LONG → exact match
            } else if (Long.class.equals(javaType) || long.class.equals(javaType)) {
                return cb.equal(path.as(Long.class), Long.valueOf(value));

                // INTEGER → exact match
            } else if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
                return cb.equal(path.as(Integer.class), Integer.valueOf(value));

                // DOUBLE → exact match
            } else if (Double.class.equals(javaType) || double.class.equals(javaType)) {
                return cb.equal(path.as(Double.class), Double.valueOf(value));

                // OPTIONAL: Add date parsing if you ever need to filter by LocalDate
            } else if (LocalDate.class.equals(javaType)) {
                try {
                    LocalDate date = LocalDate.parse(value);
                    return cb.equal(path.as(LocalDate.class), date);
                } catch (DateTimeParseException ex) {
                    // invalid date format → no predicate
                    return null;
                }

                // fall back: unsupported type
            } else {
                return null;
            }
        };
    }

    /**
     * Resolves either:
     *  - nested properties via dot notation, e.g. "foo.bar.baz"
     *  - the "Id" shortcut, e.g. "fooId" → root.get("foo").get("id")
     *  - or a direct attribute name
     *  - Special handling for ClassEntity teacherId → teachers.id (many-to-many)
     */
    private static <T> Path<?> resolvePath(Root<T> root, String attribute) {
        if (attribute.contains(".")) {
            Path<?> p = root;
            for (String part : attribute.split("\\.")) {
                p = p.get(part);
            }
            return p;
        }
        if (attribute.endsWith("Id")) {
            String rel = attribute.substring(0, attribute.length() - 2);
            
            // Special case: ClassEntity has "teachers" (plural) not "teacher" (singular)
            if ("teacher".equals(rel) && root.getJavaType().getSimpleName().equals("ClassEntity")) {
                return root.join("teachers").get("id");
            }
            
            return root.get(rel).get("id");
        }
        return root.get(attribute);
    }
}
