package com.example.school_management.feature.academic.service;

import com.example.school_management.feature.academic.dto.BatchIdsRequest;
import com.example.school_management.feature.academic.dto.CreateLevelRequest;
import com.example.school_management.feature.academic.dto.LevelDto;
import com.example.school_management.feature.academic.dto.UpdateLevelRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface LevelService {

    LevelDto create(CreateLevelRequest req);
    LevelDto update(Long id, UpdateLevelRequest req);
    void     delete(Long id);
    LevelDto get(Long id);

    Page<LevelDto> list(Pageable page, String nameLike);

    /* ─── Batch subject management ────────────────────── */
    LevelDto mutateCourses(Long levelId, BatchIdsRequest req);

    /* Single-item helpers */
    LevelDto addCourse   (Long levelId, Long courseId);
    LevelDto removeCourse(Long levelId, Long courseId);
}