package com.example.school_management.feature.academic.service;

import com.example.school_management.commons.utils.QueryParams;
import com.example.school_management.feature.academic.dto.*;
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
                        String nameLike);        // nullable LIKE %name%

    /* ─── BATCH ENROLMENT ─────────────────────────────── */
    ClassDto mutateStudents(Long classId, BatchIdsRequest req);   // add/remove many
    ClassDto mutateCourses (Long classId, BatchIdsRequest req);

    /* ─── SINGLE-ITEM SHORTCUTS (checkbox UX) ─────────── */
    ClassDto addStudent   (Long classId, Long studentId);
    ClassDto removeStudent(Long classId, Long studentId);
    ClassDto addCourse    (Long classId, Long courseId);
    ClassDto removeCourse (Long classId, Long courseId);

    Page<ClassDto> listClasses(QueryParams qp);
    ClassViewDto getDetails(Long classId);
    Page<ClassCardDto> listCards(QueryParams qp);

    /* ─── ROLE-BASED CLASS RETRIEVAL ─────────────────────── */
    Page<ClassDto> getClassesByTeacherId(Long teacherId, Pageable pageable);
    Page<ClassDto> getClassesByStudentId(Long studentId, Pageable pageable);
    Page<ClassDto> getCurrentTeacherClasses(Pageable pageable);

}
