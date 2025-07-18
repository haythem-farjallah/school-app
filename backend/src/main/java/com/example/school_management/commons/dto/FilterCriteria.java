package com.example.school_management.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterCriteria {
    
    // Text filters
    private String searchQuery;
    private Map<String, String> textFilters; // field -> value
    private Map<String, List<String>> multiTextFilters; // field -> values
    
    // Comparison filters
    private Map<String, Object> equalFilters; // field -> exact value
    private Map<String, Object> notEqualFilters; // field -> exclude value
    private Map<String, Comparable<?>> greaterThanFilters; // field -> min value (exclusive)
    private Map<String, Comparable<?>> greaterThanOrEqualFilters; // field -> min value (inclusive)
    private Map<String, Comparable<?>> lessThanFilters; // field -> max value (exclusive)
    private Map<String, Comparable<?>> lessThanOrEqualFilters; // field -> max value (inclusive)
    
    // Range filters
    private Map<String, DateRange> dateRangeFilters; // field -> date range
    private Map<String, NumericRange> numericRangeFilters; // field -> numeric range
    
    // List filters
    private Map<String, List<Object>> inFilters; // field -> allowed values
    private Map<String, List<Object>> notInFilters; // field -> excluded values
    
    // Boolean filters
    private Map<String, Boolean> booleanFilters; // field -> boolean value
    
    // Null filters
    private List<String> isNullFields; // fields that should be null
    private List<String> isNotNullFields; // fields that should not be null
    
    // Join filters
    private Map<String, FilterCriteria> joinFilters; // association -> nested criteria
    
    // Advanced search
    private Boolean fuzzySearch; // enable fuzzy search for text
    private Integer maxResults; // limit results
    private Boolean distinctResults; // return distinct results
    
    // Sorting
    private List<SortCriteria> sortCriteria;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRange {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NumericRange {
        private Number minValue;
        private Number maxValue;
        private Boolean minInclusive = true;
        private Boolean maxInclusive = true;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SortCriteria {
        private String field;
        private SortDirection direction = SortDirection.ASC;
        private Integer priority = 0; // for multi-field sorting
    }
    
    public enum SortDirection {
        ASC, DESC
    }
    
    // Builder helper methods for common operations
    public static FilterCriteria empty() {
        return FilterCriteria.builder().build();
    }
    
    public FilterCriteria withSearch(String query) {
        this.searchQuery = query;
        return this;
    }
    
    public FilterCriteria withTextFilter(String field, String value) {
        if (this.textFilters == null) {
            this.textFilters = new java.util.HashMap<>();
        }
        this.textFilters.put(field, value);
        return this;
    }
    
    public FilterCriteria withEqualFilter(String field, Object value) {
        if (this.equalFilters == null) {
            this.equalFilters = new java.util.HashMap<>();
        }
        this.equalFilters.put(field, value);
        return this;
    }
    
    public FilterCriteria withDateRange(String field, LocalDate start, LocalDate end) {
        if (this.dateRangeFilters == null) {
            this.dateRangeFilters = new java.util.HashMap<>();
        }
        this.dateRangeFilters.put(field, DateRange.builder()
                .startDate(start)
                .endDate(end)
                .build());
        return this;
    }
    
    public FilterCriteria withNumericRange(String field, Number min, Number max) {
        if (this.numericRangeFilters == null) {
            this.numericRangeFilters = new java.util.HashMap<>();
        }
        this.numericRangeFilters.put(field, NumericRange.builder()
                .minValue(min)
                .maxValue(max)
                .build());
        return this;
    }
    
    public FilterCriteria withInFilter(String field, List<Object> values) {
        if (this.inFilters == null) {
            this.inFilters = new java.util.HashMap<>();
        }
        this.inFilters.put(field, values);
        return this;
    }
    
    public FilterCriteria withSort(String field, SortDirection direction) {
        if (this.sortCriteria == null) {
            this.sortCriteria = new java.util.ArrayList<>();
        }
        this.sortCriteria.add(SortCriteria.builder()
                .field(field)
                .direction(direction)
                .priority(this.sortCriteria.size())
                .build());
        return this;
    }
} 