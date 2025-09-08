package com.example.school_management.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
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
    
    // Constructors
    public FilterCriteria() {}
    
    public FilterCriteria(String searchQuery, Map<String, String> textFilters, Map<String, List<String>> multiTextFilters,
                         Map<String, Object> equalFilters, Map<String, Object> notEqualFilters,
                         Map<String, Comparable<?>> greaterThanFilters, Map<String, Comparable<?>> greaterThanOrEqualFilters,
                         Map<String, Comparable<?>> lessThanFilters, Map<String, Comparable<?>> lessThanOrEqualFilters,
                         Map<String, DateRange> dateRangeFilters, Map<String, NumericRange> numericRangeFilters,
                         Map<String, List<Object>> inFilters, Map<String, List<Object>> notInFilters,
                         Map<String, Boolean> booleanFilters, List<String> isNullFields, List<String> isNotNullFields,
                         Map<String, FilterCriteria> joinFilters, Boolean fuzzySearch, Integer maxResults,
                         Boolean distinctResults, List<SortCriteria> sortCriteria) {
        this.searchQuery = searchQuery;
        this.textFilters = textFilters;
        this.multiTextFilters = multiTextFilters;
        this.equalFilters = equalFilters;
        this.notEqualFilters = notEqualFilters;
        this.greaterThanFilters = greaterThanFilters;
        this.greaterThanOrEqualFilters = greaterThanOrEqualFilters;
        this.lessThanFilters = lessThanFilters;
        this.lessThanOrEqualFilters = lessThanOrEqualFilters;
        this.dateRangeFilters = dateRangeFilters;
        this.numericRangeFilters = numericRangeFilters;
        this.inFilters = inFilters;
        this.notInFilters = notInFilters;
        this.booleanFilters = booleanFilters;
        this.isNullFields = isNullFields;
        this.isNotNullFields = isNotNullFields;
        this.joinFilters = joinFilters;
        this.fuzzySearch = fuzzySearch;
        this.maxResults = maxResults;
        this.distinctResults = distinctResults;
        this.sortCriteria = sortCriteria;
    }

    // Getters and Setters
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    
    public Map<String, String> getTextFilters() { return textFilters; }
    public void setTextFilters(Map<String, String> textFilters) { this.textFilters = textFilters; }
    
    public Map<String, List<String>> getMultiTextFilters() { return multiTextFilters; }
    public void setMultiTextFilters(Map<String, List<String>> multiTextFilters) { this.multiTextFilters = multiTextFilters; }
    
    public Map<String, Object> getEqualFilters() { return equalFilters; }
    public void setEqualFilters(Map<String, Object> equalFilters) { this.equalFilters = equalFilters; }
    
    public Map<String, Object> getNotEqualFilters() { return notEqualFilters; }
    public void setNotEqualFilters(Map<String, Object> notEqualFilters) { this.notEqualFilters = notEqualFilters; }
    
    public Map<String, Comparable<?>> getGreaterThanFilters() { return greaterThanFilters; }
    public void setGreaterThanFilters(Map<String, Comparable<?>> greaterThanFilters) { this.greaterThanFilters = greaterThanFilters; }
    
    public Map<String, Comparable<?>> getGreaterThanOrEqualFilters() { return greaterThanOrEqualFilters; }
    public void setGreaterThanOrEqualFilters(Map<String, Comparable<?>> greaterThanOrEqualFilters) { this.greaterThanOrEqualFilters = greaterThanOrEqualFilters; }
    
    public Map<String, Comparable<?>> getLessThanFilters() { return lessThanFilters; }
    public void setLessThanFilters(Map<String, Comparable<?>> lessThanFilters) { this.lessThanFilters = lessThanFilters; }
    
    public Map<String, Comparable<?>> getLessThanOrEqualFilters() { return lessThanOrEqualFilters; }
    public void setLessThanOrEqualFilters(Map<String, Comparable<?>> lessThanOrEqualFilters) { this.lessThanOrEqualFilters = lessThanOrEqualFilters; }
    
    public Map<String, DateRange> getDateRangeFilters() { return dateRangeFilters; }
    public void setDateRangeFilters(Map<String, DateRange> dateRangeFilters) { this.dateRangeFilters = dateRangeFilters; }
    
    public Map<String, NumericRange> getNumericRangeFilters() { return numericRangeFilters; }
    public void setNumericRangeFilters(Map<String, NumericRange> numericRangeFilters) { this.numericRangeFilters = numericRangeFilters; }
    
    public Map<String, List<Object>> getInFilters() { return inFilters; }
    public void setInFilters(Map<String, List<Object>> inFilters) { this.inFilters = inFilters; }
    
    public Map<String, List<Object>> getNotInFilters() { return notInFilters; }
    public void setNotInFilters(Map<String, List<Object>> notInFilters) { this.notInFilters = notInFilters; }
    
    public Map<String, Boolean> getBooleanFilters() { return booleanFilters; }
    public void setBooleanFilters(Map<String, Boolean> booleanFilters) { this.booleanFilters = booleanFilters; }
    
    public List<String> getIsNullFields() { return isNullFields; }
    public void setIsNullFields(List<String> isNullFields) { this.isNullFields = isNullFields; }
    
    public List<String> getIsNotNullFields() { return isNotNullFields; }
    public void setIsNotNullFields(List<String> isNotNullFields) { this.isNotNullFields = isNotNullFields; }
    
    public Map<String, FilterCriteria> getJoinFilters() { return joinFilters; }
    public void setJoinFilters(Map<String, FilterCriteria> joinFilters) { this.joinFilters = joinFilters; }
    
    public Boolean getFuzzySearch() { return fuzzySearch; }
    public void setFuzzySearch(Boolean fuzzySearch) { this.fuzzySearch = fuzzySearch; }
    
    public Integer getMaxResults() { return maxResults; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
    
    public Boolean getDistinctResults() { return distinctResults; }
    public void setDistinctResults(Boolean distinctResults) { this.distinctResults = distinctResults; }
    
    public List<SortCriteria> getSortCriteria() { return sortCriteria; }
    public void setSortCriteria(List<SortCriteria> sortCriteria) { this.sortCriteria = sortCriteria; }

    // Simple Builder
    public static FilterCriteriaBuilder builder() {
        return new FilterCriteriaBuilder();
    }
    
    public static class FilterCriteriaBuilder {
        private FilterCriteria criteria = new FilterCriteria();
        
        public FilterCriteriaBuilder searchQuery(String searchQuery) {
            criteria.searchQuery = searchQuery;
            return this;
        }
        
        public FilterCriteriaBuilder textFilters(Map<String, String> textFilters) {
            criteria.textFilters = textFilters;
            return this;
        }
        
        public FilterCriteriaBuilder equalFilters(Map<String, Object> equalFilters) {
            criteria.equalFilters = equalFilters;
            return this;
        }
        
        public FilterCriteriaBuilder sortCriteria(List<SortCriteria> sortCriteria) {
            criteria.sortCriteria = sortCriteria;
            return this;
        }
        
        public FilterCriteriaBuilder fuzzySearch(boolean fuzzySearch) {
            criteria.fuzzySearch = fuzzySearch;
            return this;
        }
        
        public FilterCriteriaBuilder distinctResults(boolean distinctResults) {
            criteria.distinctResults = distinctResults;
            return this;
        }
        
        public FilterCriteriaBuilder maxResults(int maxResults) {
            criteria.maxResults = maxResults;
            return this;
        }
        
        public FilterCriteriaBuilder notEqualFilters(java.util.Map<String, Object> notEqualFilters) {
            criteria.notEqualFilters = notEqualFilters;
            return this;
        }
        
        public FilterCriteriaBuilder greaterThanFilters(java.util.Map<String, Comparable<?>> greaterThanFilters) {
            criteria.greaterThanFilters = greaterThanFilters;
            return this;
        }
        
        public FilterCriteriaBuilder greaterThanOrEqualFilters(java.util.Map<String, Comparable<?>> greaterThanOrEqualFilters) {
            criteria.greaterThanOrEqualFilters = greaterThanOrEqualFilters;
            return this;
        }
        
        public FilterCriteriaBuilder lessThanFilters(java.util.Map<String, Comparable<?>> lessThanFilters) {
            criteria.lessThanFilters = lessThanFilters;
            return this;
        }
        
        public FilterCriteriaBuilder lessThanOrEqualFilters(java.util.Map<String, Comparable<?>> lessThanOrEqualFilters) {
            criteria.lessThanOrEqualFilters = lessThanOrEqualFilters;
            return this;
        }
        
        public FilterCriteriaBuilder inFilters(java.util.Map<String, java.util.List<Object>> inFilters) {
            criteria.inFilters = inFilters;
            return this;
        }
        
        public FilterCriteriaBuilder notInFilters(java.util.Map<String, java.util.List<Object>> notInFilters) {
            criteria.notInFilters = notInFilters;
            return this;
        }
        
        public FilterCriteriaBuilder isNullFields(java.util.List<String> isNullFields) {
            criteria.isNullFields = isNullFields;
            return this;
        }
        
        public FilterCriteriaBuilder isNotNullFields(java.util.List<String> isNotNullFields) {
            criteria.isNotNullFields = isNotNullFields;
            return this;
        }
        
        public FilterCriteriaBuilder booleanFilters(java.util.Map<String, Boolean> booleanFilters) {
            criteria.booleanFilters = booleanFilters;
            return this;
        }
        
        public FilterCriteriaBuilder dateRangeFilters(java.util.Map<String, DateRange> dateRangeFilters) {
            criteria.dateRangeFilters = dateRangeFilters;
            return this;
        }
        
        public FilterCriteria build() {
            return criteria;
        }
    }
    
    public static class DateRange {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime startDateTime;
        private LocalDateTime endDateTime;
        
        public DateRange() {}
        
        public DateRange(LocalDate startDate, LocalDate endDate, LocalDateTime startDateTime, LocalDateTime endDateTime) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;
        }
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public LocalDateTime getStartDateTime() { return startDateTime; }
        public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
        public LocalDateTime getEndDateTime() { return endDateTime; }
        public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
        
        public static DateRangeBuilder builder() { return new DateRangeBuilder(); }
        
        public static class DateRangeBuilder {
            private DateRange range = new DateRange();
            public DateRangeBuilder startDate(LocalDate startDate) { range.startDate = startDate; return this; }
            public DateRangeBuilder endDate(LocalDate endDate) { range.endDate = endDate; return this; }
            public DateRangeBuilder startDateTime(LocalDateTime startDateTime) { range.startDateTime = startDateTime; return this; }
            public DateRangeBuilder endDateTime(LocalDateTime endDateTime) { range.endDateTime = endDateTime; return this; }
            public DateRange build() { return range; }
        }
    }
    
    public static class NumericRange {
        private Number minValue;
        private Number maxValue;
        private Boolean minInclusive = true;
        private Boolean maxInclusive = true;
        
        public NumericRange() {}
        
        public NumericRange(Number minValue, Number maxValue, Boolean minInclusive, Boolean maxInclusive) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.minInclusive = minInclusive;
            this.maxInclusive = maxInclusive;
        }
        
        public Number getMinValue() { return minValue; }
        public void setMinValue(Number minValue) { this.minValue = minValue; }
        public Number getMaxValue() { return maxValue; }
        public void setMaxValue(Number maxValue) { this.maxValue = maxValue; }
        public Boolean getMinInclusive() { return minInclusive; }
        public void setMinInclusive(Boolean minInclusive) { this.minInclusive = minInclusive; }
        public Boolean getMaxInclusive() { return maxInclusive; }
        public void setMaxInclusive(Boolean maxInclusive) { this.maxInclusive = maxInclusive; }
        
        public static NumericRangeBuilder builder() { return new NumericRangeBuilder(); }
        
        public static class NumericRangeBuilder {
            private NumericRange range = new NumericRange();
            public NumericRangeBuilder minValue(Number minValue) { range.minValue = minValue; return this; }
            public NumericRangeBuilder maxValue(Number maxValue) { range.maxValue = maxValue; return this; }
            public NumericRange build() { return range; }
        }
    }
    
    public static class SortCriteria {
        private String field;
        private SortDirection direction = SortDirection.ASC;
        private Integer priority = 0; // for multi-field sorting
        
        public SortCriteria() {}
        
        public SortCriteria(String field, SortDirection direction, Integer priority) {
            this.field = field;
            this.direction = direction;
            this.priority = priority;
        }
        
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public SortDirection getDirection() { return direction; }
        public void setDirection(SortDirection direction) { this.direction = direction; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        
        public static SortCriteriaBuilder builder() { return new SortCriteriaBuilder(); }
        
        public static class SortCriteriaBuilder {
            private SortCriteria criteria = new SortCriteria();
            public SortCriteriaBuilder field(String field) { criteria.field = field; return this; }
            public SortCriteriaBuilder direction(SortDirection direction) { criteria.direction = direction; return this; }
            public SortCriteriaBuilder priority(Integer priority) { criteria.priority = priority; return this; }
            public SortCriteria build() { return criteria; }
        }
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