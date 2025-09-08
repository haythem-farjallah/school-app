package com.example.school_management.commons.service;

import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Generic filtering service providing advanced filtering capabilities for any entity.
 * Centralizes filter logic and validation.
 */
@Slf4j
@Service
public class GenericFilterService {

    /**
     * Parse request parameters and build a dynamic specification
     * 
     * @param parameterMap HTTP request parameter map
     * @param <T> Entity type
     * @return JPA Specification for the entity
     */
    public <T> Specification<T> buildSpecificationFromParams(Map<String, String[]> parameterMap) {
        log.debug("Building specification from {} parameters", parameterMap.size());
        
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap);
        Specification<T> spec = DynamicSpecificationBuilder.build(criteria);
        
        log.debug("Built specification with {} filter criteria", 
                criteria.getTextFilters() != null ? criteria.getTextFilters().size() : 0);
        
        return spec;
    }

    /**
     * Create a simple text search specification for common fields
     * 
     * @param searchTerm The search term
     * @param searchFields Fields to search in (e.g., "firstName", "lastName", "email")
     * @param <T> Entity type
     * @return JPA Specification for text search
     */
    public <T> Specification<T> buildTextSearchSpecification(String searchTerm, String... searchFields) {
        if (searchTerm == null || searchTerm.isBlank() || searchFields.length == 0) {
            return null;
        }
        
        String normalizedTerm = "%" + searchTerm.toLowerCase() + "%";
        
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            
            for (String field : searchFields) {
                predicates.add(cb.like(cb.lower(root.get(field)), normalizedTerm));
            }
            
            return cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Validate filter parameters and return validation errors
     * 
     * @param parameterMap HTTP request parameter map
     * @return Map of field names to error messages, empty if valid
     */
    public Map<String, String> validateFilterParameters(Map<String, String[]> parameterMap) {
        var errors = new java.util.HashMap<String, String>();
        
        // Add validation logic here
        // For example, check date formats, number ranges, etc.
        
        return errors;
    }

    /**
     * Get available filter operators for a field type
     * 
     * @param fieldType The field type (text, number, date, boolean)
     * @return List of available operators
     */
    public java.util.List<String> getAvailableOperators(String fieldType) {
        return switch (fieldType.toLowerCase()) {
            case "text", "string" -> java.util.List.of("eq", "like", "in", "not_in");
            case "number", "integer", "long", "double" -> java.util.List.of("eq", "gt", "gte", "lt", "lte", "in", "not_in");
            case "date", "datetime" -> java.util.List.of("eq", "gt", "gte", "lt", "lte", "between");
            case "boolean" -> java.util.List.of("eq");
            default -> java.util.List.of("eq");
        };
    }
}
