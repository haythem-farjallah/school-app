package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Failures while loading the current teacher's classes reach the caller instead
 * of being turned into an empty page.
 */
class ClassServiceImplTest {

    private static final String EMAIL = "teacher@school.test";

    private final ClassRepository classes = mock(ClassRepository.class);
    private final TeacherRepository teachers = mock(TeacherRepository.class);
    private final CurrentSchoolResolver currentSchool = mock(CurrentSchoolResolver.class);

    @SuppressWarnings("unchecked")
    private final ClassServiceImpl service = new ClassServiceImpl(
            classes,
            mock(CourseRepository.class),
            mock(EnrollmentRepository.class),
            teachers,
            mock(AcademicMapper.class),
            mock(TeachingAssignmentRepository.class),
            mock(AuditService.class),
            mock(CurrentAcademicYearResolver.class),
            currentSchool,
            (BaseUserRepository<BaseUser>) mock(BaseUserRepository.class));

    @BeforeEach
    void signInAsTeacher() {
        UserDetails principal = User.withUsername(EMAIL).password("unused").roles("TEACHER").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void repositoryFailurePropagates() {
        Teacher teacher = new Teacher();
        teacher.setId(5L);
        when(teachers.findByEmail(EMAIL)).thenReturn(Optional.of(teacher));
        School school = mock(School.class);
        when(school.getId()).thenReturn(1L);
        when(currentSchool.resolve()).thenReturn(school);
        when(classes.findByTeacherIdAndSchoolId(5L, 1L)).thenThrow(new DataAccessResourceFailureException("connection refused"));

        assertThatThrownBy(() -> service.getCurrentTeacherClasses(PageRequest.of(0, 10)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    void principalWithoutTeacherRecordIsNotFound() {
        when(teachers.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentTeacherClasses(PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
