package com.example.school_management.commons.utils;

import com.example.school_management.commons.dto.FilterCriteria;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class FilterCriteriaParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Parse request parameters into FilterCriteria
     * Supports various parameter patterns:
     * - search: global search query
     * - field_like: text search on specific field
     * - field_eq: exact match on field
     * - field_ne: not equal on field
     * - field_gt: greater than on field
     * - field_gte: greater than or equal on field
     * - field_lt: less than on field
     * - field_lte: less than or equal on field
     * - field_in: value in list (comma-separated)
     * - field_notin: value not in list (comma-separated)
     * - field_null: field is null (true/false)
     * - field_from: range start date/datetime
     * - field_to: range end date/datetime
     * - sort: sorting (field:asc or field:desc, comma-separated)
     */
    public static FilterCriteria parseRequestParams(Map<String, String[]> parameterMap) {
        FilterCriteria.FilterCriteriaBuilder builder = FilterCriteria.builder();

        // Process each parameter
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] values = entry.getValue();
            
            if (values == null || values.length == 0) {
                continue;
            }
            
            String value = values[0]; // Take first value
            
            if (!StringUtils.hasText(value)) {
                continue;
            }

            // Global search
            if ("search".equals(paramName) || "q".equals(paramName)) {
                builder.searchQuery(value);
                continue;
            }

            // Fuzzy search
            if ("fuzzy".equals(paramName)) {
                builder.fuzzySearch(Boolean.parseBoolean(value));
                continue;
            }

            // Distinct results
            if ("distinct".equals(paramName)) {
                builder.distinctResults(Boolean.parseBoolean(value));
                continue;
            }

            // Max results
            if ("limit".equals(paramName) || "maxResults".equals(paramName)) {
                try {
                    builder.maxResults(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    // Ignore invalid numbers
                }
                continue;
            }

            // Sorting
            if ("sort".equals(paramName)) {
                parseSorting(value, builder);
                continue;
            }

            // Field-specific filters
            if (paramName.contains("_")) {
                parseFieldFilter(paramName, value, builder);
            }
        }

        return builder.build();
    }

    private static void parseFieldFilter(String paramName, String value, FilterCriteria.FilterCriteriaBuilder builder) {
        String[] parts = paramName.split("_", 2);
        if (parts.length != 2) {
            return;
        }

        String fieldName = parts[0];
        String operation = parts[1];

        switch (operation.toLowerCase()) {
            case "like", "contains" -> addTextFilter(builder, fieldName, value);
            case "eq", "equal" -> addEqualFilter(builder, fieldName, value);
            case "ne", "notequal" -> addNotEqualFilter(builder, fieldName, value);
            case "gt", "greaterthan" -> addGreaterThanFilter(builder, fieldName, value);
            case "gte", "greaterequal" -> addGreaterThanOrEqualFilter(builder, fieldName, value);
            case "lt", "lessthan" -> addLessThanFilter(builder, fieldName, value);
            case "lte", "lessequal" -> addLessThanOrEqualFilter(builder, fieldName, value);
            case "in" -> addInFilter(builder, fieldName, value);
            case "notin", "nin" -> addNotInFilter(builder, fieldName, value);
            case "null", "isnull" -> addNullFilter(builder, fieldName, value);
            case "notnull", "isnotnull" -> addNotNullFilter(builder, fieldName, value);
            case "from", "start" -> addRangeStartFilter(builder, fieldName, value);
            case "to", "end" -> addRangeEndFilter(builder, fieldName, value);
            case "range" -> addRangeFilter(builder, fieldName, value);
            case "bool", "boolean" -> addBooleanFilter(builder, fieldName, value);
        }
    }

    private static void addTextFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getTextFilters() == null) {
            builder.textFilters(new HashMap<>());
        }
        builder.build().getTextFilters().put(fieldName, value);
    }

    private static void addEqualFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getEqualFilters() == null) {
            builder.equalFilters(new HashMap<>());
        }
        Object parsedValue = parseValue(value);
        builder.build().getEqualFilters().put(fieldName, parsedValue);
    }

    private static void addNotEqualFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getNotEqualFilters() == null) {
            builder.notEqualFilters(new HashMap<>());
        }
        Object parsedValue = parseValue(value);
        builder.build().getNotEqualFilters().put(fieldName, parsedValue);
    }

    private static void addGreaterThanFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getGreaterThanFilters() == null) {
            builder.greaterThanFilters(new HashMap<>());
        }
        Comparable<?> parsedValue = (Comparable<?>) parseValue(value);
        if (parsedValue != null) {
            builder.build().getGreaterThanFilters().put(fieldName, parsedValue);
        }
    }

    private static void addGreaterThanOrEqualFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getGreaterThanOrEqualFilters() == null) {
            builder.greaterThanOrEqualFilters(new HashMap<>());
        }
        Comparable<?> parsedValue = (Comparable<?>) parseValue(value);
        if (parsedValue != null) {
            builder.build().getGreaterThanOrEqualFilters().put(fieldName, parsedValue);
        }
    }

    private static void addLessThanFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getLessThanFilters() == null) {
            builder.lessThanFilters(new HashMap<>());
        }
        Comparable<?> parsedValue = (Comparable<?>) parseValue(value);
        if (parsedValue != null) {
            builder.build().getLessThanFilters().put(fieldName, parsedValue);
        }
    }

    private static void addLessThanOrEqualFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getLessThanOrEqualFilters() == null) {
            builder.lessThanOrEqualFilters(new HashMap<>());
        }
        Comparable<?> parsedValue = (Comparable<?>) parseValue(value);
        if (parsedValue != null) {
            builder.build().getLessThanOrEqualFilters().put(fieldName, parsedValue);
        }
    }

    private static void addInFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getInFilters() == null) {
            builder.inFilters(new HashMap<>());
        }
        List<Object> values = Arrays.stream(value.split(","))
                .map(String::trim)
                .map(FilterCriteriaParser::parseValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (!values.isEmpty()) {
            builder.build().getInFilters().put(fieldName, values);
        }
    }

    private static void addNotInFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getNotInFilters() == null) {
            builder.notInFilters(new HashMap<>());
        }
        List<Object> values = Arrays.stream(value.split(","))
                .map(String::trim)
                .map(FilterCriteriaParser::parseValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (!values.isEmpty()) {
            builder.build().getNotInFilters().put(fieldName, values);
        }
    }

    private static void addNullFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (Boolean.parseBoolean(value)) {
            if (builder.build().getIsNullFields() == null) {
                builder.isNullFields(new ArrayList<>());
            }
            builder.build().getIsNullFields().add(fieldName);
        }
    }

    private static void addNotNullFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (Boolean.parseBoolean(value)) {
            if (builder.build().getIsNotNullFields() == null) {
                builder.isNotNullFields(new ArrayList<>());
            }
            builder.build().getIsNotNullFields().add(fieldName);
        }
    }

    private static void addBooleanFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        if (builder.build().getBooleanFilters() == null) {
            builder.booleanFilters(new HashMap<>());
        }
        builder.build().getBooleanFilters().put(fieldName, Boolean.parseBoolean(value));
    }

    private static void addRangeStartFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        // This is simplified - in a real implementation, you'd need to handle both start and end
        LocalDate date = parseDate(value);
        if (date != null) {
            FilterCriteria.DateRange range = FilterCriteria.DateRange.builder()
                    .startDate(date)
                    .build();
            
            if (builder.build().getDateRangeFilters() == null) {
                builder.dateRangeFilters(new HashMap<>());
            }
            builder.build().getDateRangeFilters().put(fieldName, range);
        }
    }

    private static void addRangeEndFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        // This is simplified - in a real implementation, you'd need to handle both start and end
        LocalDate date = parseDate(value);
        if (date != null) {
            FilterCriteria.DateRange range = FilterCriteria.DateRange.builder()
                    .endDate(date)
                    .build();
            
            if (builder.build().getDateRangeFilters() == null) {
                builder.dateRangeFilters(new HashMap<>());
            }
            
            // If there's already a range for this field, merge it
            FilterCriteria.DateRange existingRange = builder.build().getDateRangeFilters().get(fieldName);
            if (existingRange != null) {
                range = FilterCriteria.DateRange.builder()
                        .startDate(existingRange.getStartDate())
                        .endDate(date)
                        .startDateTime(existingRange.getStartDateTime())
                        .endDateTime(existingRange.getEndDateTime())
                        .build();
            }
            
            builder.build().getDateRangeFilters().put(fieldName, range);
        }
    }

    private static void addRangeFilter(FilterCriteria.FilterCriteriaBuilder builder, String fieldName, String value) {
        // Format: "start,end" or "start-end"
        String[] parts = value.contains(",") ? value.split(",") : value.split("-");
        if (parts.length == 2) {
            LocalDate startDate = parseDate(parts[0].trim());
            LocalDate endDate = parseDate(parts[1].trim());
            
            if (startDate != null && endDate != null) {
                FilterCriteria.DateRange range = FilterCriteria.DateRange.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build();
                
                if (builder.build().getDateRangeFilters() == null) {
                    builder.dateRangeFilters(new HashMap<>());
                }
                builder.build().getDateRangeFilters().put(fieldName, range);
            }
        }
    }

    private static void parseSorting(String value, FilterCriteria.FilterCriteriaBuilder builder) {
        List<FilterCriteria.SortCriteria> sortCriteria = new ArrayList<>();
        
        String[] sortParts = value.split(",");
        for (int i = 0; i < sortParts.length; i++) {
            String sortPart = sortParts[i].trim();
            String[] fieldAndDirection = sortPart.split(":");
            
            String field = fieldAndDirection[0].trim();
            FilterCriteria.SortDirection direction = FilterCriteria.SortDirection.ASC;
            
            if (fieldAndDirection.length > 1) {
                String directionStr = fieldAndDirection[1].trim().toLowerCase();
                if ("desc".equals(directionStr) || "descending".equals(directionStr)) {
                    direction = FilterCriteria.SortDirection.DESC;
                }
            }
            
            sortCriteria.add(FilterCriteria.SortCriteria.builder()
                    .field(field)
                    .direction(direction)
                    .priority(i)
                    .build());
        }
        
        if (!sortCriteria.isEmpty()) {
            builder.sortCriteria(sortCriteria);
        }
    }

    private static Object parseValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // Try to parse as different types
        
        // Boolean
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        // Integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Not an integer
        }

        // Long
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // Not a long
        }

        // Double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Not a double
        }

        // Date
        LocalDate date = parseDate(value);
        if (date != null) {
            return date;
        }

        // DateTime
        LocalDateTime dateTime = parseDateTime(value);
        if (dateTime != null) {
            return dateTime;
        }

        // Default to string
        return value;
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    private static LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }
} 