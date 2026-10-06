package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Unexpected planning failures propagate to the global server-error handler. */
class EnrollmentServiceAutoEnrollmentErrorTest {

    private static final String INTERNAL_DETAIL =
            "could not execute statement [ERROR: relation \"students\" does not exist] [select s1_0.id from students s1_0]";

    private final StudentRepository studentRepo = mock(StudentRepository.class);
    private final CurrentAcademicYearResolver academicYearResolver = mock(CurrentAcademicYearResolver.class);
    private final CurrentSchoolResolver schoolResolver = mock(CurrentSchoolResolver.class);

    @SuppressWarnings("unchecked")
    private final EnrollmentServiceImpl service = new EnrollmentServiceImpl(
            mock(EnrollmentRepository.class),
            studentRepo,
            mock(ClassRepository.class),
            mock(AuditService.class),
            (BaseUserRepository<BaseUser>) mock(BaseUserRepository.class),
            mock(EnrollmentWriteExecutor.class),
            mock(AutoEnrollmentClassWriter.class),
            academicYearResolver,
            schoolResolver,
            mock(SchoolMembershipRepository.class));

    @ParameterizedTest
    @ValueSource(strings = {"all", "grade", "preview"})
    void unexpectedDatabaseFailurePropagates(String operation) {
        AcademicYear year = mock(AcademicYear.class);
        School school = mock(School.class);
        when(year.getId()).thenReturn(1L);
        when(school.getId()).thenReturn(2L);
        when(academicYearResolver.resolve()).thenReturn(year);
        when(schoolResolver.resolve()).thenReturn(school);
        var failure = new DataAccessResourceFailureException(INTERNAL_DETAIL);
        when(studentRepo.findEligibleSchoolStudents(anyLong())).thenThrow(failure);

        assertThatThrownBy(() -> run(operation)).isSameAs(failure);
    }

    @ParameterizedTest
    @ValueSource(strings = {"all", "grade", "preview"})
    void unexpectedResolverFailurePropagates(String operation) {
        var failure = new IllegalStateException("unexpected internal resolver failure");
        when(academicYearResolver.resolve()).thenThrow(failure);

        assertThatThrownBy(() -> run(operation)).isSameAs(failure);
    }

    private void run(String operation) {
        switch (operation) {
            case "all" -> service.autoEnrollAllStudents();
            case "grade" -> service.autoEnrollByGradeLevel("MIDDLE");
            case "preview" -> service.previewAutoEnrollment();
            default -> throw new AssertionError("Unknown operation");
        }
    }
}
