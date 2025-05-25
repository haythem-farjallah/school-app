package com.example.school_management.commons.utils;


import org.springframework.data.jpa.domain.Specification;
import java.util.*;

public final class SpecificationBuilder<T> {
    private final QueryParams qp;
    private final List<Specification<T>> specs = new ArrayList<>();

    public SpecificationBuilder(QueryParams qp) {
        this.qp = qp;
    }

    public Specification<T> build() {
        qp.getFilters().forEach((attr, values) -> {
            // here we only support single-value equals or LIKE on String
            String v = values.get(0);
            specs.add(EntitySpecification.hasAttribute(attr, v));
        });
        return specs.stream()
                .reduce(Specification.where(null), Specification::and);
    }
}