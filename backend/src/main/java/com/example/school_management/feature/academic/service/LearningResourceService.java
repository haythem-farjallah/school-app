package com.example.school_management.feature.academic.service;

import com.example.school_management.feature.academic.dto.LearningResourceDto;
import com.example.school_management.feature.academic.dto.CreateLearningResourceRequest;
import com.example.school_management.feature.academic.dto.UpdateLearningResourceRequest;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface LearningResourceService {

    LearningResourceDto create(CreateLearningResourceRequest request);
    
    LearningResourceDto update(Long id, UpdateLearningResourceRequest request);
    
    void delete(Long id);
    
    LearningResourceDto get(Long id);

    /** The stored file behind {@code filename}, once a resource referencing it is readable by the caller. */
    Path resolveReadableFile(String filename);

    /**
     * Whether the caller may see private resources and everything attached to them, such as
     * comments. Requires current-School membership matching the caller's account role;
     * valid Student members see only public resources.
     */
    boolean seesPrivateResources();
    
    Page<LearningResourceDto> list(Pageable pageable);
    
    Page<LearningResourceDto> findByType(ResourceType type, Pageable pageable);
    
    Page<LearningResourceDto> findByTeacherId(Long teacherId, Pageable pageable);
    
    Page<LearningResourceDto> findByClassId(Long classId, Pageable pageable);
    
    Page<LearningResourceDto> findByCourseId(Long courseId, Pageable pageable);
    
    Page<LearningResourceDto> searchByTitleOrDescription(String searchTerm, String unused, Pageable pageable);
    
    LearningResourceDto uploadResource(MultipartFile file, CreateLearningResourceRequest request);
    
    void addTargetClasses(Long resourceId, Set<Long> classIds);
    
    void removeTargetClasses(Long resourceId, Set<Long> classIds);
    
    void addTargetCourses(Long resourceId, Set<Long> courseIds);
    
    void removeTargetCourses(Long resourceId, Set<Long> courseIds);
    
    void addTeachers(Long resourceId, Set<Long> teacherIds);
    
    void removeTeachers(Long resourceId, Set<Long> teacherIds);
    
    void incrementViewCount(String filename);
    
    void incrementDownloadCount(String filename);
} 