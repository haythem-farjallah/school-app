package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.operational.dto.AutoEnrollmentResultDto;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A failed auto-enrollment reports a generic error; the underlying exception's message, which may
 * carry SQL or internal state, never reaches the caller.
 */
class EnrollmentServiceAutoEnrollmentErrorTest {

    private static final String INTERNAL_DETAIL =
            "could not execute statement [ERROR: relation \"students\" does not exist] [select s1_0.id from students s1_0]";

    private final StudentRepository studentRepo = mock(StudentRepository.class);

    @SuppressWarnings("unchecked")
    private final EnrollmentServiceImpl service = new EnrollmentServiceImpl(
            mock(EnrollmentRepository.class),
            studentRepo,
            mock(ClassRepository.class),
            mock(AuditService.class),
            (BaseUserRepository<BaseUser>) mock(BaseUserRepository.class),
            mock(RealTimeNotificationService.class));

    @Test
    void failureReportsOnlyAGenericError() {
        when(studentRepo.findAll()).thenThrow(new DataAccessResourceFailureException(INTERNAL_DETAIL));

        AutoEnrollmentResultDto result = service.previewAutoEnrollment();

        assertThat(result.success()).isFalse();
        assertThat(result.errors()).containsExactly("Auto-enrollment failed");
        assertThat(result.message()).doesNotContain("students", "select", "ERROR");
    }
}
