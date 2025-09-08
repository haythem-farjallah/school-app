package com.example.school_management.feature.academic.service;

import com.example.school_management.feature.academic.dto.CreateTeachingAssignmentDto;

import com.example.school_management.feature.academic.dto.UpdateTeachingAssignmentDto;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface TeachingAssignmentService {
    
    // Basic CRUD operations
    TeachingAssignment create(CreateTeachingAssignmentDto dto);
    TeachingAssignment find(long id);
    TeachingAssignment patch(long id, UpdateTeachingAssignmentDto dto);
    void delete(long id);
    
    // Listing and searching
    Page<TeachingAssignment> findAll(Pageable pageable);
    Page<TeachingAssignment> search(Pageable pageable, String query);
    Page<TeachingAssignment> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap);
    
    // Bulk operations
    void bulkDelete(List<Long> ids);
    List<TeachingAssignment> findByIds(List<Long> ids);
    List<TeachingAssignment> findAll();
    
    // Teacher-specific operations
    List<TeachingAssignment> findByTeacherId(Long teacherId);
    Page<TeachingAssignment> findByTeacherId(Long teacherId, Pageable pageable);
    
    // Course-specific operations
    List<TeachingAssignment> findByCourseId(Long courseId);
    Page<TeachingAssignment> findByCourseId(Long courseId, Pageable pageable);
    
    // Class-specific operations
    List<TeachingAssignment> findByClassId(Long classId);
    Page<TeachingAssignment> findByClassId(Long classId, Pageable pageable);
    
    // Bulk assignment operations
    void assignTeacherToCourses(Long teacherId, List<Long> courseIds, Long classId);
    void assignTeachersToClass(List<Long> teacherIds, Long classId, Long courseId);
    void bulkAssignTeachersToCourses(List<CreateTeachingAssignmentDto> assignments);
    
    // Validation
    boolean existsByTeacherAndCourseAndClass(Long teacherId, Long courseId, Long classId);
    boolean hasConflictingAssignment(Long teacherId, Long courseId, Long classId);
}
