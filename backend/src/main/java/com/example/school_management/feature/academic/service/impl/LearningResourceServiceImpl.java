package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.dto.CreateLearningResourceRequest;
import com.example.school_management.feature.academic.dto.LearningResourceDto;
import com.example.school_management.feature.academic.dto.UpdateLearningResourceRequest;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.mapper.LearningResourceMapper;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.academic.service.LearningResourceService;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.commons.security.FileSecurityService;
import com.example.school_management.commons.security.FileSecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LearningResourceServiceImpl implements LearningResourceService {

    private final LearningResourceRepository repository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final LearningResourceMapper mapper;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;
    private final FileSecurityService fileSecurityService;

    @Value("${app.file.upload.path:uploads/learning-resources}")
    private String uploadPath;

    @Value("${app.file.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    @Override
    public LearningResourceDto create(CreateLearningResourceRequest request) {
        log.debug("Creating learning resource: {}", request);
        
        LearningResource resource = new LearningResource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setUrl(request.getUrl());
        resource.setType(request.getType());
        resource.setThumbnailUrl(request.getThumbnailUrl());
        resource.setDuration(request.getDuration());
        resource.setPublic(request.getIsPublic());
        
        // Set current teacher as creator
        Teacher currentTeacher = getCurrentTeacher();
        resource.addTeacher(currentTeacher);
        
        // Set target classes if provided
        if (request.getClassIds() != null) {
            request.getClassIds().forEach(classId -> {
                ClassEntity classEntity = classRepository.findById(classId)
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
                resource.addTargetClass(classEntity);
            });
        }
        
        // Set target courses if provided
        if (request.getCourseIds() != null) {
            request.getCourseIds().forEach(courseId -> {
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
                resource.addTargetCourse(course);
            });
        }
        
        LearningResource saved = repository.save(resource);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Learning resource created";
            String details = String.format("Learning resource created: %s (Type: %s, Public: %s)", 
                saved.getTitle(), saved.getType(), saved.isPublic());
            
            auditService.createAuditEvent(
                AuditEventType.RESOURCE_UPLOADED,
                "LearningResource",
                saved.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for resource creation: {}", e.getMessage());
        }
        
        return mapper.toDto(saved);
    }

    @Override
    public LearningResourceDto uploadResource(MultipartFile file, CreateLearningResourceRequest request) {
        return uploadResourceWithVisibility(file, request, true); // Default to public
    }

    public LearningResourceDto uploadResourceWithVisibility(MultipartFile file, CreateLearningResourceRequest request, Boolean isPublic) {
        log.debug("Uploading learning resource file: {}", file.getOriginalFilename());
        
        // Determine resource type first
        ResourceType resourceType = determineResourceType(StringUtils.getFilenameExtension(file.getOriginalFilename()));
        
        // Validate file with comprehensive security checks
        FileSecurityService.FileValidationResult validationResult = validateFile(file, resourceType);
        
        // Use sanitized filename from validation
        String sanitizedFilename = validationResult.getSanitizedFilename();
        String fileExtension = StringUtils.getFilenameExtension(sanitizedFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
        
        // Create upload directory if it doesn't exist
        Path uploadDir = Paths.get(uploadPath);
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory", e);
            throw new RuntimeException("Failed to create upload directory", e);
        }
        
        // Save file
        Path filePath = uploadDir.resolve(uniqueFilename);
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save uploaded file", e);
            throw new RuntimeException("Failed to save uploaded file", e);
        }
        
        // Resource type already determined during validation
        // File hash available for audit: validationResult.getFileHash()
        
        // Create resource with file URL
        String fileUrl = "/api/v1/learning-resources/files/" + uniqueFilename;
        
        // Create resource directly with file URL
        LearningResource resource = new LearningResource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setUrl(fileUrl);
        resource.setType(resourceType);
        resource.setThumbnailUrl(request.getThumbnailUrl());
        resource.setDuration(request.getDuration());
        resource.setPublic(isPublic != null ? isPublic : true); // Use provided visibility or default to true
        
        // Set current teacher as creator
        Teacher currentTeacher = getCurrentTeacher();
        resource.addTeacher(currentTeacher);
        
        // Set target classes if provided
        if (request.getClassIds() != null) {
            request.getClassIds().forEach(classId -> {
                ClassEntity classEntity = classRepository.findById(classId)
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
                resource.addTargetClass(classEntity);
            });
        }
        
        // Set target courses if provided
        if (request.getCourseIds() != null) {
            request.getCourseIds().forEach(courseId -> {
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
                resource.addTargetCourse(course);
            });
        }
        
        LearningResource saved = repository.save(resource);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Learning resource uploaded";
            String details = String.format("File uploaded: %s -> %s (Type: %s, Size: %d bytes, Public: %s)", 
                sanitizedFilename, uniqueFilename, resourceType, file.getSize(), isPublic);
            
            auditService.createAuditEvent(
                AuditEventType.RESOURCE_UPLOADED,
                "LearningResource",
                saved.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for resource upload: {}", e.getMessage());
        }
        
        return mapper.toDto(saved);
    }

    @Override
    public LearningResourceDto update(Long id, UpdateLearningResourceRequest request) {
        log.debug("Updating learning resource {} with {}", id, request);
        
        LearningResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + id));
        
        // Check if current teacher is the creator
        Teacher currentTeacher = getCurrentTeacher();
        if (!resource.getCreatedBy().contains(currentTeacher)) {
            throw new RuntimeException("You can only update resources you created");
        }
        
        if (request.getTitle() != null) resource.setTitle(request.getTitle());
        if (request.getDescription() != null) resource.setDescription(request.getDescription());
        if (request.getUrl() != null) resource.setUrl(request.getUrl());
        if (request.getType() != null) resource.setType(request.getType());
        if (request.getThumbnailUrl() != null) resource.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getDuration() != null) resource.setDuration(request.getDuration());
        if (request.getIsPublic() != null) resource.setPublic(request.getIsPublic());
        
        // Update class assignments if provided
        if (request.getClassIds() != null) {
            // Clear existing classes
            resource.getTargetClasses().clear();
            // Add new classes
            request.getClassIds().forEach(classId -> {
                ClassEntity classEntity = classRepository.findById(classId)
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
                resource.addTargetClass(classEntity);
            });
        }
        
        // Update course assignments if provided
        if (request.getCourseIds() != null) {
            // Clear existing courses
            resource.getTargetCourses().clear();
            // Add new courses
            request.getCourseIds().forEach(courseId -> {
                Course course = courseRepository.findById(courseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
                resource.addTargetCourse(course);
            });
        }
        
        LearningResource updated = repository.save(resource);
        return mapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        log.debug("Deleting learning resource {}", id);
        
        LearningResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + id));
        
        // Check if current teacher is the creator
        Teacher currentTeacher = getCurrentTeacher();
        if (!resource.getCreatedBy().contains(currentTeacher)) {
            throw new RuntimeException("You can only delete resources you created");
        }
        
        // Delete associated file if it exists
        if (resource.getUrl() != null && resource.getUrl().startsWith("/api/v1/learning-resources/files/")) {
            String filename = resource.getUrl().substring(resource.getUrl().lastIndexOf('/') + 1);
            deleteFile(filename);
        }
        
        repository.delete(resource);
    }

    @Override
    public LearningResourceDto get(Long id) {
        log.debug("Getting learning resource {}", id);
        
        LearningResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + id));
        
        return mapper.toDto(resource);
    }

    @Override
    public Page<LearningResourceDto> list(Pageable pageable) {
        log.debug("Listing learning resources with pageable: {}", pageable);
        return repository.findAllSorted(pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> findByType(ResourceType type, Pageable pageable) {
        log.debug("Finding learning resources by type: {}", type);
        return repository.findByType(type, pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> findByTeacherId(Long teacherId, Pageable pageable) {
        log.debug("Finding learning resources by teacher ID: {}", teacherId);
        return repository.findByTeacherId(teacherId, pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> findByClassId(Long classId, Pageable pageable) {
        log.debug("Finding learning resources by class ID: {}", classId);
        return repository.findByClassId(classId, pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> findByCourseId(Long courseId, Pageable pageable) {
        log.debug("Finding learning resources by course ID: {}", courseId);
        return repository.findByCourseId(courseId, pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> searchByTitleOrDescription(String title, String description, Pageable pageable) {
        log.debug("Searching learning resources by search term: {}", title);
        // Use the title parameter as the search term (description parameter is ignored now)
        return repository.searchByTitleOrDescription(title, pageable).map(mapper::toDto);
    }

    @Override
    public void addTargetClasses(Long resourceId, Set<Long> classIds) {
        log.debug("Adding target classes {} to resource {}", classIds, resourceId);
        
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + resourceId));
        
        classIds.forEach(classId -> {
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
            resource.addTargetClass(classEntity);
        });
        
        repository.save(resource);
    }

    @Override
    public void removeTargetClasses(Long resourceId, Set<Long> classIds) {
        log.debug("Removing target classes {} from resource {}", classIds, resourceId);
        
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + resourceId));
        
        classIds.forEach(classId -> {
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
            resource.removeTargetClass(classEntity);
        });
        
        repository.save(resource);
    }

    @Override
    public void addTargetCourses(Long resourceId, Set<Long> courseIds) {
        log.debug("Adding target courses {} to resource {}", courseIds, resourceId);
        
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + resourceId));
        
        courseIds.forEach(courseId -> {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            resource.addTargetCourse(course);
        });
        
        repository.save(resource);
    }

    @Override
    public void removeTargetCourses(Long resourceId, Set<Long> courseIds) {
        log.debug("Removing target courses {} from resource {}", courseIds, resourceId);
        
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + resourceId));
        
        courseIds.forEach(courseId -> {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            resource.removeTargetCourse(course);
        });
        
        repository.save(resource);
    }

    @Override
    public void addTeachers(Long resourceId, Set<Long> teacherIds) {
        log.debug("Adding teachers {} to resource {}", teacherIds, resourceId);
        
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + resourceId));
        
        teacherIds.forEach(teacherId -> {
            Teacher teacher = teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
            resource.addTeacher(teacher);
        });
        
        repository.save(resource);
    }

    @Override
    public void removeTeachers(Long resourceId, Set<Long> teacherIds) {
        log.debug("Removing teachers {} from resource {}", teacherIds, resourceId);
        
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + resourceId));
        
        teacherIds.forEach(teacherId -> {
            Teacher teacher = teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
            resource.removeTeacher(teacher);
        });
        
        repository.save(resource);
    }

    private Teacher getCurrentTeacher() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = userDetails.getUsername(); // Username is actually the email
        
        return teacherRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user is not a teacher"));
    }

    private BaseUser getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = userDetails.getUsername(); // Username is actually the email
        
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    private FileSecurityService.FileValidationResult validateFile(MultipartFile file, ResourceType resourceType) {
        // Use comprehensive security validation
        String typeString = resourceType.name();
        FileSecurityService.FileValidationResult result = fileSecurityService.validateFile(file, typeString);
        
        if (!result.isValid()) {
            log.warn("File validation failed: {}", result.getErrorMessage());
            throw new FileSecurityException("File validation failed: " + result.getErrorMessage());
        }
        
        log.info("File validation successful for: {} (hash: {})", 
            result.getSanitizedFilename(), result.getFileHash());
        
        return result;
    }

    private ResourceType determineResourceType(String fileExtension) {
        if (fileExtension == null) return ResourceType.LINK;
        
        String ext = fileExtension.toLowerCase();
        
        // Video files
        if (ext.matches("mp4|avi|mov|wmv|flv|webm|mkv")) {
            return ResourceType.VIDEO;
        }
        
        // Document files
        if (ext.matches("pdf|doc|docx|ppt|pptx|xls|xlsx|txt|rtf")) {
            return ResourceType.DOCUMENT;
        }
        
        // Image files
        if (ext.matches("jpg|jpeg|png|gif|bmp|svg|webp")) {
            return ResourceType.IMAGE;
        }
        
        // Audio files
        if (ext.matches("mp3|wav|ogg|aac|flac|wma")) {
            return ResourceType.AUDIO;
        }
        
        return ResourceType.LINK;
    }

    private void deleteFile(String filename) {
        try {
            Path filePath = Paths.get(uploadPath).resolve(filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filename, e);
        }
    }
    
    @Override
    @Transactional
    public void incrementViewCount(String filename) {
        log.info("=== INCREMENTING VIEW COUNT ===");
        log.info("Looking for filename: {}", filename);
        
        // Debug: Let's see what URLs we have in the database
        var allResources = repository.findAll();
        log.info("Total resources in database: {}", allResources.size());
        for (LearningResource res : allResources) {
            log.info("Resource {}: URL = {}", res.getId(), res.getUrl());
        }
        
        var resourceOpt = repository.findByFilename(filename);
        if (resourceOpt.isPresent()) {
            LearningResource resource = resourceOpt.get();
            Long oldCount = resource.getViewCount();
            resource.setViewCount(resource.getViewCount() + 1);
            LearningResource saved = repository.save(resource);
            repository.flush(); // Force immediate database write
            log.info("✅ View count incremented for resource {} ({}). Old count: {}, New count: {}", 
                saved.getId(), saved.getTitle(), oldCount, saved.getViewCount());
        } else {
            log.warn("❌ No resource found with filename: {}", filename);
            log.warn("Available URLs in database:");
            allResources.forEach(res -> log.warn("  - {}", res.getUrl()));
        }
    }
    
    @Override
    @Transactional
    public void incrementDownloadCount(String filename) {
        log.info("=== INCREMENTING DOWNLOAD COUNT ===");
        log.info("Looking for filename: {}", filename);
        
        var resourceOpt = repository.findByFilename(filename);
        if (resourceOpt.isPresent()) {
            LearningResource resource = resourceOpt.get();
            Long oldCount = resource.getDownloadCount();
            resource.setDownloadCount(resource.getDownloadCount() + 1);
            LearningResource saved = repository.save(resource);
            repository.flush(); // Force immediate database write
            log.info("✅ Download count incremented for resource {} ({}). Old count: {}, New count: {}", 
                saved.getId(), saved.getTitle(), oldCount, saved.getDownloadCount());
        } else {
            log.warn("❌ No resource found with filename: {}", filename);
        }
    }
}