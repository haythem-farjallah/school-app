package com.example.school_management.feature.academic.service;

import com.example.school_management.feature.academic.dto.CourseDto;
import com.example.school_management.feature.academic.dto.CreateCourseRequest;
import com.example.school_management.feature.academic.dto.UpdateCourseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    CourseDto create(CreateCourseRequest req);
    CourseDto update(Long id, UpdateCourseRequest req);
    void      delete(Long id);
    CourseDto get(Long id);

    Page<CourseDto> list(Pageable page,
                         Long teacherId,
                         String nameLike);

}