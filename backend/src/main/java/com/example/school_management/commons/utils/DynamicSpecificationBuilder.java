package com.example.school_management.commons.utils;

import com.example.school_management.commons.dto.FilterCriteria;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicSpecificationBuilder {

    public static <T> Specification<T> build(FilterCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return criteriaBuilder.conjunction();
            }

            // Text filters (LIKE operations)
            addTextFilters(criteria, root, criteriaBuilder, predicates);

            // Search query (global search)
            addSearchQuery(criteria, root, criteriaBuilder, predicates);

            // Equality filters
            addEqualFilters(criteria, root, criteriaBuilder, predicates);

            // Not equal filters
            addNotEqualFilters(criteria, root, criteriaBuilder, predicates);

            // Comparison filters
            addComparisonFilters(criteria, root, criteriaBuilder, predicates);

            // Range filters
            addRangeFilters(criteria, root, criteriaBuilder, predicates);

            // List filters (IN/NOT IN)
            addListFilters(criteria, root, criteriaBuilder, predicates);

            // Boolean filters
            addBooleanFilters(criteria, root, criteriaBuilder, predicates);

            // Null filters
            addNullFilters(criteria, root, criteriaBuilder, predicates);

            // Join filters
            addJoinFilters(criteria, root, query, criteriaBuilder, predicates);

            // Handle distinct results
            if (Boolean.TRUE.equals(criteria.getDistinctResults())) {
                query.distinct(true);
            }

            // Add sorting
            addSorting(criteria, root, query, criteriaBuilder);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static <T> void addTextFilters(FilterCriteria criteria, Root<T> root,
                                          CriteriaBuilder cb, List<Predicate> predicates) {
        if (criteria.getTextFilters() != null) {
            for (Map.Entry<String, String> entry : criteria.getTextFilters().entrySet()) {
                if (StringUtils.hasText(entry.getValue())) {
                    String field = entry.getKey();
                    String value = entry.getValue();
                    
                    Path<String> fieldPath = getNestedPath(root, field);
                    
                    if (Boolean.TRUE.equals(criteria.getFuzzySearch())) {
                        // Fuzzy search with multiple patterns
                        predicates.add(cb.or(
                            cb.like(cb.lower(fieldPath), "%" + value.toLowerCase() + "%"),
                            cb.like(cb.lower(fieldPath), value.toLowerCase() + "%"),
                            cb.like(cb.lower(fieldPath), "%" + value.toLowerCase())
                        ));
                    } else {
                        predicates.add(cb.like(cb.lower(fieldPath), "%" + value.toLowerCase() + "%"));
                    }
                }
            }
        }

        // Multi-text filters (OR within field, AND across fields)
        if (criteria.getMultiTextFilters() != null) {
            for (Map.Entry<String, List<String>> entry : criteria.getMultiTextFilters().entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    String field = entry.getKey();
                    List<String> values = entry.getValue();
                    
                    Path<String> fieldPath = getNestedPath(root, field);
                    
                    List<Predicate> orPredicates = new ArrayList<>();
                    for (String value : values) {
                        if (StringUtils.hasText(value)) {
                            orPredicates.add(cb.like(cb.lower(fieldPath), "%" + value.toLowerCase() + "%"));
                        }
                    }
                    
                    if (!orPredicates.isEmpty()) {
                        predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
                    }
                }
            }
        }
    }

    private static <T> void addSearchQuery(FilterCriteria criteria, Root<T> root,
                                          CriteriaBuilder cb, List<Predicate> predicates) {
        if (StringUtils.hasText(criteria.getSearchQuery())) {
            String searchQuery = criteria.getSearchQuery().toLowerCase();
            
            // Define searchable fields (can be customized per entity)
            String[] searchableFields = {"firstName", "lastName", "email", "name", "title", "description"};
            
            List<Predicate> searchPredicates = new ArrayList<>();
            for (String field : searchableFields) {
                try {
                    Path<String> fieldPath = getNestedPath(root, field);
                    searchPredicates.add(cb.like(cb.lower(fieldPath), "%" + searchQuery + "%"));
                } catch (Exception e) {
                    // Field doesn't exist in this entity, skip it
                }
            }
            
            if (!searchPredicates.isEmpty()) {
                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));
            }
        }
    }

    private static <T> void addEqualFilters(FilterCriteria criteria, Root<T> root,
                                           CriteriaBuilder cb, List<Predicate> predicates) {
        if (criteria.getEqualFilters() != null) {
            for (Map.Entry<String, Object> entry : criteria.getEqualFilters().entrySet()) {
                if (entry.getValue() != null) {
                    Path<Object> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(cb.equal(fieldPath, entry.getValue()));
                }
            }
        }
    }

    private static <T> void addNotEqualFilters(FilterCriteria criteria, Root<T> root,
                                              CriteriaBuilder cb, List<Predicate> predicates) {
        if (criteria.getNotEqualFilters() != null) {
            for (Map.Entry<String, Object> entry : criteria.getNotEqualFilters().entrySet()) {
                if (entry.getValue() != null) {
                    Path<Object> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(cb.notEqual(fieldPath, entry.getValue()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void addComparisonFilters(FilterCriteria criteria, Root<T> root,
                                                 CriteriaBuilder cb, List<Predicate> predicates) {
        // Greater than
        if (criteria.getGreaterThanFilters() != null) {
            for (Map.Entry<String, Comparable<?>> entry : criteria.getGreaterThanFilters().entrySet()) {
                if (entry.getValue() != null) {
                    Path<Comparable> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(cb.greaterThan(fieldPath, (Comparable) entry.getValue()));
                }
            }
        }

        // Greater than or equal
        if (criteria.getGreaterThanOrEqualFilters() != null) {
            for (Map.Entry<String, Comparable<?>> entry : criteria.getGreaterThanOrEqualFilters().entrySet()) {
                if (entry.getValue() != null) {
                    Path<Comparable> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(cb.greaterThanOrEqualTo(fieldPath, (Comparable) entry.getValue()));
                }
            }
        }

        // Less than
        if (criteria.getLessThanFilters() != null) {
            for (Map.Entry<String, Comparable<?>> entry : criteria.getLessThanFilters().entrySet()) {
                if (entry.getValue() != null) {
                    Path<Comparable> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(cb.lessThan(fieldPath, (Comparable) entry.getValue()));
                }
            }
        }

        // Less than or equal
        if (criteria.getLessThanOrEqualFilters() != null) {
            for (Map.Entry<String, Comparable<?>> entry : criteria.getLessThanOrEqualFilters().entrySet()) {
                if (entry.getValue() != null) {
                    Path<Comparable> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(cb.lessThanOrEqualTo(fieldPath, (Comparable) entry.getValue()));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void addRangeFilters(FilterCriteria criteria, Root<T> root,
                                           CriteriaBuilder cb, List<Predicate> predicates) {
        // Date range filters
        if (criteria.getDateRangeFilters() != null) {
            for (Map.Entry<String, FilterCriteria.DateRange> entry : criteria.getDateRangeFilters().entrySet()) {
                FilterCriteria.DateRange range = entry.getValue();
                
                if (range.getStartDate() != null || range.getEndDate() != null) {
                    Path<LocalDate> fieldPath = getNestedPath(root, entry.getKey());
                    
                    if (range.getStartDate() != null) {
                        predicates.add(cb.greaterThanOrEqualTo(fieldPath, range.getStartDate()));
                    }
                    if (range.getEndDate() != null) {
                        predicates.add(cb.lessThanOrEqualTo(fieldPath, range.getEndDate()));
                    }
                }
                
                if (range.getStartDateTime() != null || range.getEndDateTime() != null) {
                    Path<LocalDateTime> fieldPath = getNestedPath(root, entry.getKey());
                    
                    if (range.getStartDateTime() != null) {
                        predicates.add(cb.greaterThanOrEqualTo(fieldPath, range.getStartDateTime()));
                    }
                    if (range.getEndDateTime() != null) {
                        predicates.add(cb.lessThanOrEqualTo(fieldPath, range.getEndDateTime()));
                    }
                }
            }
        }

        // Numeric range filters
        if (criteria.getNumericRangeFilters() != null) {
            for (Map.Entry<String, FilterCriteria.NumericRange> entry : criteria.getNumericRangeFilters().entrySet()) {
                FilterCriteria.NumericRange range = entry.getValue();
                Path<Number> fieldPath = getNestedPath(root, entry.getKey());
                
                if (range.getMinValue() != null) {
                    if (Boolean.TRUE.equals(range.getMinInclusive())) {
                        predicates.add(cb.ge(fieldPath, range.getMinValue()));
                    } else {
                        predicates.add(cb.gt(fieldPath, range.getMinValue()));
                    }
                }
                
                if (range.getMaxValue() != null) {
                    if (Boolean.TRUE.equals(range.getMaxInclusive())) {
                        predicates.add(cb.le(fieldPath, range.getMaxValue()));
                    } else {
                        predicates.add(cb.lt(fieldPath, range.getMaxValue()));
                    }
                }
            }
        }
    }

    private static <T> void addListFilters(FilterCriteria criteria, Root<T> root,
                                          CriteriaBuilder cb, List<Predicate> predicates) {
        // IN filters
        if (criteria.getInFilters() != null) {
            for (Map.Entry<String, List<Object>> entry : criteria.getInFilters().entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    Path<Object> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(fieldPath.in(entry.getValue()));
                }
            }
        }

        // NOT IN filters
        if (criteria.getNotInFilters() != null) {
            for (Map.Entry<String, List<Object>> entry : criteria.getNotInFilters().entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    Path<Object> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(cb.not(fieldPath.in(entry.getValue())));
                }
            }
        }
    }

    private static <T> void addBooleanFilters(FilterCriteria criteria, Root<T> root,
                                             CriteriaBuilder cb, List<Predicate> predicates) {
        if (criteria.getBooleanFilters() != null) {
            for (Map.Entry<String, Boolean> entry : criteria.getBooleanFilters().entrySet()) {
                if (entry.getValue() != null) {
                    Path<Boolean> fieldPath = getNestedPath(root, entry.getKey());
                    predicates.add(cb.equal(fieldPath, entry.getValue()));
                }
            }
        }
    }

    private static <T> void addNullFilters(FilterCriteria criteria, Root<T> root,
                                          CriteriaBuilder cb, List<Predicate> predicates) {
        if (criteria.getIsNullFields() != null) {
            for (String field : criteria.getIsNullFields()) {
                Path<Object> fieldPath = getNestedPath(root, field);
                predicates.add(cb.isNull(fieldPath));
            }
        }

        if (criteria.getIsNotNullFields() != null) {
            for (String field : criteria.getIsNotNullFields()) {
                Path<Object> fieldPath = getNestedPath(root, field);
                predicates.add(cb.isNotNull(fieldPath));
            }
        }
    }

    private static <T> void addJoinFilters(FilterCriteria criteria, Root<T> root,
                                          CriteriaQuery<?> query, CriteriaBuilder cb,
                                          List<Predicate> predicates) {
        if (criteria.getJoinFilters() != null) {
            for (Map.Entry<String, FilterCriteria> entry : criteria.getJoinFilters().entrySet()) {
                String association = entry.getKey();
                FilterCriteria joinCriteria = entry.getValue();
                
                Join<T, ?> join = root.join(association, JoinType.LEFT);
                Specification<?> joinSpec = build(joinCriteria);
                
                predicates.add(joinSpec.toPredicate((Root) join, query, cb));
            }
        }
    }

    private static <T> void addSorting(FilterCriteria criteria, Root<T> root,
                                      CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (criteria.getSortCriteria() != null && !criteria.getSortCriteria().isEmpty()) {
            List<Order> orders = new ArrayList<>();
            
            criteria.getSortCriteria().stream()
                    .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                    .forEach(sortCriteria -> {
                        Path<Object> fieldPath = getNestedPath(root, sortCriteria.getField());
                        Order order = sortCriteria.getDirection() == FilterCriteria.SortDirection.DESC
                                ? cb.desc(fieldPath)
                                : cb.asc(fieldPath);
                        orders.add(order);
                    });
            
            query.orderBy(orders);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, R> Path<R> getNestedPath(Root<T> root, String fieldPath) {
        String[] fields = fieldPath.split("\\.");
        Path<R> path = (Path<R>) root;
        
        for (String field : fields) {
            path = path.get(field);
        }
        
        return path;
    }
} 