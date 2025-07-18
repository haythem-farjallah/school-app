package com.example.school_management.feature.academic.service;

import com.example.school_management.feature.academic.dto.LearningResourceDto;
import com.example.school_management.feature.academic.dto.CreateLearningResourceRequest;
import com.example.school_management.feature.academic.dto.UpdateLearningResourceRequest;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface LearningResourceService {

    LearningResourceDto create(CreateLearningResourceRequest request);
    
    LearningResourceDto update(Long id, UpdateLearningResourceRequest request);
    
    void delete(Long id);
    
    LearningResourceDto get(Long id);
    
    Page<LearningResourceDto> list(Pageable pageable);
    
    Page<LearningResourceDto> findByType(ResourceType type, Pageable pageable);
    
    Page<LearningResourceDto> findByTeacherId(Long teacherId, Pageable pageable);
    
    Page<LearningResourceDto> findByClassId(Long classId, Pageable pageable);
    
    Page<LearningResourceDto> findByCourseId(Long courseId, Pageable pageable);
    
    Page<LearningResourceDto> searchByTitleOrDescription(String title, String description, Pageable pageable);
    
    LearningResourceDto uploadResource(MultipartFile file, CreateLearningResourceRequest request);
    
    void addTargetClasses(Long resourceId, Set<Long> classIds);
    
    void removeTargetClasses(Long resourceId, Set<Long> classIds);
    
    void addTargetCourses(Long resourceId, Set<Long> courseIds);
    
    void removeTargetCourses(Long resourceId, Set<Long> courseIds);
    
    void addTeachers(Long resourceId, Set<Long> teacherIds);
    
    void removeTeachers(Long resourceId, Set<Long> teacherIds);
} 