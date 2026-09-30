package com.example.school_management.feature.academic;

import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CurrentAcademicYearResolverTest {
    @Test
    void ambiguousActiveYearsFailInsteadOfSelectingOne() {
        var school = mock(School.class);
        when(school.getId()).thenReturn(10L);
        var currentSchool = mock(CurrentSchoolResolver.class);
        when(currentSchool.resolve()).thenReturn(school);
        var years = mock(AcademicYearRepository.class);
        when(years.findAllBySchoolIdAndActiveTrue(10L)).thenReturn(List.of(new AcademicYear(), new AcademicYear()));
        var resolver = new CurrentAcademicYearResolver(currentSchool, years);
        assertThatThrownBy(resolver::resolve).isInstanceOf(IllegalStateException.class).hasMessageContaining("multiple active");
    }
}
