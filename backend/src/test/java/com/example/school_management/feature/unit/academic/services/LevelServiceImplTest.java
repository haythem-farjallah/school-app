package com.example.school_management.feature.unit.academic.services;

import com.example.school_management.feature.academic.dto.LevelDto;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.Level;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.LevelRepository;
import com.example.school_management.feature.academic.service.LevelService;
import com.example.school_management.feature.academic.service.impl.LevelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LevelServiceImplTest {

    @Mock LevelRepository  levelRepo;
    @Mock CourseRepository courseRepo;

    AcademicMapper mapper = Mappers.getMapper(AcademicMapper.class);
    LevelService   service;

    @BeforeEach
    void init() {
        service = new LevelServiceImpl(levelRepo, courseRepo, mapper);
    }

    @Test
    void addCourse_toLevel() {
        Level lvl = new Level(); lvl.setId(2L);
        when(levelRepo.findById(2L)).thenReturn(Optional.of(lvl));

        Course c = new Course(); c.setId(77L);
        when(courseRepo.findById(77L)).thenReturn(Optional.of(c));

        LevelDto dto = service.addCourse(2L, 77L);

        assertThat(dto.courseIds()).containsExactly(77L);
    }

    @Test
    void list_nameFilter() {
        when(levelRepo.findAll(
                Mockito.<Specification<Level>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(PageRequest.of(0, 20), "Level");

        verify(levelRepo).findAll(
                Mockito.<Specification<Level>>any(),
                eq(PageRequest.of(0, 20)));
    }
}
