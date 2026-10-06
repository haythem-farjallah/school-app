package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.feature.academic.dto.CreateTeachingAssignmentDto;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.mapper.TeachingAssignmentMapper;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.commons.service.GenericFilterService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TeachingAssignmentBulkFailureTest {
    private final TeacherRepository teachers = mock(TeacherRepository.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final ClassRepository classes = mock(ClassRepository.class);
    private final CurrentSchoolResolver currentSchool = mock(CurrentSchoolResolver.class);
    private final TeachingAssignmentServiceImpl service = spy(new TeachingAssignmentServiceImpl(
            mock(TeachingAssignmentRepository.class), teachers, courses, classes,
            mock(TeachingAssignmentMapper.class), mock(GenericFilterService.class), currentSchool));
    private final CreateTeachingAssignmentDto first = new CreateTeachingAssignmentDto(10L, 20L, 30L, 2);
    private final CreateTeachingAssignmentDto second = new CreateTeachingAssignmentDto(10L, 21L, 30L, 2);

    @BeforeEach
    void prepareSchoolReferences() {
        School school = mock(School.class);
        when(school.getId()).thenReturn(1L);
        when(currentSchool.resolve()).thenReturn(school);
        when(teachers.findByIdAndSchoolId(10L, 1L)).thenReturn(Optional.of(new Teacher()));
        when(courses.findByIdAndSchoolId(20L, 1L)).thenReturn(Optional.of(new Course()));
        when(courses.findByIdAndSchoolId(21L, 1L)).thenReturn(Optional.of(new Course()));
        when(classes.findByIdAndAcademicYearSchoolId(30L, 1L)).thenReturn(Optional.of(new ClassEntity()));
    }

    @Test
    void unexpectedWriteFailurePropagatesAndStopsFurtherCommands() {
        var failure = new DataAccessResourceFailureException("database unavailable");
        doThrow(failure).when(service).create(first);

        assertThatThrownBy(() -> service.bulkAssignTeachersToCourses(List.of(first, second))).isSameAs(failure);
        verify(service, never()).create(second);
    }

    @Test
    void knownDuplicateStillAllowsRemainingAssignments() {
        doThrow(new ConflictException("Assignment already exists")).when(service).create(first);
        doReturn(null).when(service).create(second);

        service.bulkAssignTeachersToCourses(List.of(first, second));

        verify(service).create(second);
    }
}
