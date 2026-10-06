package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.*;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.repository.*;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.operational.repository.*;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardTeacherBatchReadTest {
    @Mock BaseUserRepository<BaseUser> users;
    @Mock StudentRepository students;
    @Mock TeacherRepository teachers;
    @Mock ParentRepository parents;
    @Mock ClassRepository classes;
    @Mock CourseRepository courses;
    @Mock AttendanceRepository attendance;
    @Mock EnrollmentRepository enrollments;
    @Mock GradeRepository grades;
    @Mock NotificationRepository notifications;
    @Mock TeachingAssignmentRepository assignments;
    @Mock SchoolMembershipRepository memberships;
    @Mock CurrentSchoolResolver currentSchool;
    @Mock CurrentAcademicYearResolver currentYear;
    @InjectMocks DashboardServiceImpl service;

    @Test
    @SuppressWarnings("unchecked")
    void summariesAndStatisticsShareOneBatchInsteadOfLoadingEveryClassGradesAndCounts() {
        School school = mock(School.class); when(school.getId()).thenReturn(1L);
        when(currentSchool.resolve()).thenReturn(school);
        Teacher teacher = new Teacher(); teacher.setId(5L); teacher.setRole(UserRole.TEACHER);
        when(teachers.findByIdAndSchoolId(5L, 1L)).thenReturn(Optional.of(teacher));
        ClassEntity first = new ClassEntity(); first.setId(10L);
        ClassEntity second = new ClassEntity(); second.setId(11L);
        when(classes.findByTeacherIdAndSchoolId(5L, 1L)).thenReturn(List.of(first, second));
        when(notifications.findByUserIdOrderByCreatedAtDesc(eq(5L), any(Pageable.class))).thenReturn(Page.empty());
        GradeRepository.ClassGradeSummary summary = mock(GradeRepository.ClassGradeSummary.class);
        when(summary.getClassId()).thenReturn(10L);
        when(summary.getAverageGrade()).thenReturn(16.0);
        when(summary.getScoreCount()).thenReturn(3L);
        when(grades.summarizeByClassIdsAndSchoolId(List.of(10L, 11L), 1L)).thenReturn(List.of(summary));
        Map<String, Object> dashboard = (Map<String, Object>) service.getTeacherDashboard(5L);
        Map<String, Object> stats = (Map<String, Object>) dashboard.get("stats");
        assertThat(stats.get("averageClassGrade")).isEqualTo(16.0);
        assertThat(stats).containsEntry("pendingGrades", null);
        verify(grades).summarizeByClassIdsAndSchoolId(List.of(10L, 11L), 1L);
        verify(enrollments).countActiveRosters(List.of(10L, 11L));
        verify(grades, never()).findByClassIdAndSchoolId(anyLong(), anyLong());
        verify(enrollments, never()).countActiveByClassId(anyLong());
    }
}
