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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        log.debug("Uploading learning resource file: {}", file.getOriginalFilename());
        
        // Validate file
        validateFile(file);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
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
        
        // Determine resource type based on file extension
        ResourceType resourceType = determineResourceType(fileExtension);
        
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
        
        LearningResource updated = repository.save(resource);
        return mapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningResourceDto get(Long id) {
        log.debug("Fetching learning resource {}", id);
        
        LearningResource resource = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + id));
        
        return mapper.toDto(resource);
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
            String filename = resource.getUrl().substring("/api/v1/learning-resources/files/".length());
            deleteFile(filename);
        }
        
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningResourceDto> list(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningResourceDto> findByType(ResourceType type, Pageable pageable) {
        return repository.findByType(type, pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningResourceDto> findByTeacherId(Long teacherId, Pageable pageable) {
        return repository.findByTeacherId(teacherId, pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningResourceDto> findByClassId(Long classId, Pageable pageable) {
        return repository.findByClassId(classId, pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningResourceDto> findByCourseId(Long courseId, Pageable pageable) {
        return repository.findByCourseId(courseId, pageable).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningResourceDto> searchByTitleOrDescription(String title, String description, Pageable pageable) {
        return repository.searchByTitleOrDescription(title, description, pageable).map(mapper::toDto);
    }

    @Override
    public void addTargetClasses(Long resourceId, Set<Long> classIds) {
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found"));
        
        classIds.forEach(classId -> {
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
            resource.addTargetClass(classEntity);
        });
        
        repository.save(resource);
    }

    @Override
    public void removeTargetClasses(Long resourceId, Set<Long> classIds) {
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found"));
        
        classIds.forEach(classId -> {
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
            resource.removeTargetClass(classEntity);
        });
        
        repository.save(resource);
    }

    @Override
    public void addTargetCourses(Long resourceId, Set<Long> courseIds) {
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found"));
        
        courseIds.forEach(courseId -> {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            resource.addTargetCourse(course);
        });
        
        repository.save(resource);
    }

    @Override
    public void removeTargetCourses(Long resourceId, Set<Long> courseIds) {
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found"));
        
        courseIds.forEach(courseId -> {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            resource.removeTargetCourse(course);
        });
        
        repository.save(resource);
    }

    @Override
    public void addTeachers(Long resourceId, Set<Long> teacherIds) {
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found"));
        
        teacherIds.forEach(teacherId -> {
            Teacher teacher = teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
            resource.addTeacher(teacher);
        });
        
        repository.save(resource);
    }

    @Override
    public void removeTeachers(Long resourceId, Set<Long> teacherIds) {
        LearningResource resource = repository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found"));
        
        teacherIds.forEach(teacherId -> {
            Teacher teacher = teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
            resource.removeTeacher(teacher);
        });
        
        repository.save(resource);
    }

    private Teacher getCurrentTeacher() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return teacherRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current teacher not found"));
    }

    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + email));
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File cannot be empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new RuntimeException("File name cannot be empty");
        }
        
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        if (fileExtension == null) {
            throw new RuntimeException("File must have an extension");
        }
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
} 