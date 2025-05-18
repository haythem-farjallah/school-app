package com.example.school_management.feature.unit.academic.services;

import com.example.school_management.feature.academic.dto.CourseDto;
import com.example.school_management.feature.academic.dto.CreateCourseRequest;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.service.CourseService;
import com.example.school_management.feature.academic.service.impl.CourseServiceImpl;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock CourseRepository  courseRepo;
    @Mock
    TeacherRepository teacherRepo;

    AcademicMapper mapper = Mappers.getMapper(AcademicMapper.class);
    CourseService  service;

    @BeforeEach
    void init() {
        service = new CourseServiceImpl(courseRepo, teacherRepo, mapper);
    }

    @Test
    void createCourse_assignsTeacher() {
        Teacher t = new Teacher(); t.setId(9L);

        when(teacherRepo.findById(9L)).thenReturn(Optional.of(t));
        when(courseRepo.existsByNameIgnoreCase("Physics")).thenReturn(false);
        when(courseRepo.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });

        CourseDto dto = service.create(
                new CreateCourseRequest("Physics", "#ffffff", 2.0, 9L));

        assertThat(dto.teacherId()).isEqualTo(9L);
        assertThat(dto.id()).isEqualTo(5L);
        verify(courseRepo).save(any(Course.class));
    }

    @Test
    void list_byTeacher() {
        when(courseRepo.findAll(
                Mockito.<Specification<Course>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        service.list(PageRequest.of(0, 5), 9L, null);

        verify(courseRepo).findAll(
                Mockito.<Specification<Course>>any(),
                eq(PageRequest.of(0, 5)));
    }
}
