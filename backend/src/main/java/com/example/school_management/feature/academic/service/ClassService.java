package com.example.school_management.feature.academic.service;

import com.example.school_management.feature.academic.dto.BatchIdsRequest;
import com.example.school_management.feature.academic.dto.ClassDto;
import com.example.school_management.feature.academic.dto.CreateClassRequest;
import com.example.school_management.feature.academic.dto.UpdateClassRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClassService {

    /* ─── CRUD ─────────────────────────────────────────── */
    ClassDto create(CreateClassRequest req);
    ClassDto update(Long id, UpdateClassRequest req);
    void     delete(Long id);
    ClassDto get(Long id);

    /* ─── LIST with optional filters (pagination) ─────── */
    Page<ClassDto> list(Pageable page,
                        Long   levelId,          // nullable → no filter
                        String nameLike);        // nullable LIKE %name%

    /* ─── BATCH ENROLMENT ─────────────────────────────── */
    ClassDto mutateStudents(Long classId, BatchIdsRequest req);   // add/remove many
    ClassDto mutateCourses (Long classId, BatchIdsRequest req);

    /* ─── SINGLE-ITEM SHORTCUTS (checkbox UX) ─────────── */
    ClassDto addStudent   (Long classId, Long studentId);
    ClassDto removeStudent(Long classId, Long studentId);
    ClassDto addCourse    (Long classId, Long courseId);
    ClassDto removeCourse (Long classId, Long courseId);
}
