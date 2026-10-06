package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TeacherClassBatchReadTest {
    private final TeacherRepository teachers = mock(TeacherRepository.class);
    private final TeachingAssignmentRepository assignments = mock(TeachingAssignmentRepository.class);
    private final ClassRepository classes = mock(ClassRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final GradeRepository grades = mock(GradeRepository.class);
    private final CurrentSchoolResolver currentSchool = mock(CurrentSchoolResolver.class);
    private final TeacherClassServiceImpl service = new TeacherClassServiceImpl(
            teachers, assignments, classes, grades, enrollments, currentSchool);
    private final List<ClassEntity> selected = List.of(clazz(10L), clazz(11L));

    @BeforeEach
    void setUp() {
        School school = mock(School.class); when(school.getId()).thenReturn(1L);
        when(currentSchool.resolve()).thenReturn(school);
        Teacher teacher = new Teacher(); teacher.setId(5L);
        when(teachers.findByEmailAndSchoolId("teacher@test", 1L)).thenReturn(Optional.of(teacher));
    }

    @Test
    void pageOnlyBuildsSelectedRowsUsingBatchesAndRetainsDatabaseTotal() {
        PageRequest request = PageRequest.of(1, 2);
        when(classes.findTeacherClassPage(5L, 1L, null, request)).thenReturn(new PageImpl<>(selected, request, 12));
        var result = service.getTeacherClasses("teacher@test", request, null);
        assertThat(result.getTotalElements()).isEqualTo(12);
        assertThat(result.getContent()).extracting(dto -> dto.id()).containsExactly(10L, 11L);
        verify(classes, never()).findTeacherClasses(anyLong(), anyLong(), any());
        verify(classes, never()).findByTeacherIdAndSchoolId(anyLong(), anyLong());
        verifyBatches();
    }

    @Test
    void explicitAllAndStatisticsReuseBatchConstruction() {
        when(classes.findTeacherClasses(5L, 1L, null)).thenReturn(selected);
        assertThat(service.getTeacherClassStats("teacher@test").totalClasses()).isEqualTo(2);
        verifyBatches();
    }

    private void verifyBatches() {
        verify(classes).findWithCoursesByIdsAndSchoolId(List.of(10L, 11L), 1L);
        verify(assignments).findByTeacherIdAndSchoolId(5L, 1L);
        verify(enrollments).countActiveRosters(List.of(10L, 11L));
        verify(grades).summarizeByClassIdsAndSchoolId(List.of(10L, 11L), 1L);
        verify(enrollments, never()).countActiveByClassId(anyLong());
        verify(grades, never()).findByClassIdAndSchoolId(anyLong(), anyLong());
    }

    private static ClassEntity clazz(Long id) {
        ClassEntity clazz = new ClassEntity(); clazz.setId(id); clazz.setName("Class " + id); return clazz;
    }
}
