package com.example.school_management.feature.school;

import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentSchoolResolverTest {
    private final SchoolRepository repository = mock(SchoolRepository.class);
    private final CurrentSchoolResolver resolver = new CurrentSchoolResolver(repository);

    @Test
    void returnsOnlySchool() {
        School school = new School();
        school.setName("Test School");
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(school)));
        assertThat(resolver.resolve()).isSameAs(school);
    }

    @Test
    void failsClearlyWhenNoSchoolExists() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        assertThatThrownBy(resolver::resolve).isInstanceOf(IllegalStateException.class).hasMessageContaining("no Schools");
    }

    @Test
    void failsClearlyWhenSchoolSelectionIsAmbiguous() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new School(), new School())));
        assertThatThrownBy(resolver::resolve).isInstanceOf(IllegalStateException.class).hasMessageContaining("multiple Schools");
    }
}
