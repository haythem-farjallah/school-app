package com.example.school_management.commons.utils;

import com.example.school_management.commons.dto.FilterCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DynamicSpecificationSearchFailureTest {
    @SuppressWarnings("unchecked")
    private final Root<Object> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder builder = mock(CriteriaBuilder.class);

    private FilterCriteria search() {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setSearchQuery("algebra");
        return criteria;
    }

    @Test
    void missingPresentationFieldsRemainSkipped() {
        doThrow(new IllegalArgumentException("attribute absent")).when(root).get(anyString());
        Predicate result = mock(Predicate.class);
        when(builder.and(any(Predicate[].class))).thenReturn(result);

        assertThat(DynamicSpecificationBuilder.build(search()).toPredicate(root, query, builder)).isSameAs(result);
        verify(root).get("description");
        verify(builder, never()).like(any(), anyString());
    }

    @Test
    void unexpectedPathFailurePropagates() {
        var failure = new IllegalStateException("unexpected metadata failure");
        doThrow(failure).when(root).get("firstName");

        assertThatThrownBy(() -> DynamicSpecificationBuilder.build(search()).toPredicate(root, query, builder))
                .isSameAs(failure);
    }

    @Test
    void predicateConstructionFailurePropagatesEvenWhenIllegalArgumentException() {
        Path<?> field = mock(Path.class);
        doReturn(field).when(root).get("firstName");
        var failure = new IllegalArgumentException("unexpected SQL expression failure");
        doThrow(failure).when(builder).lower(any());

        assertThatThrownBy(() -> DynamicSpecificationBuilder.build(search()).toPredicate(root, query, builder))
                .isSameAs(failure);
    }
}
