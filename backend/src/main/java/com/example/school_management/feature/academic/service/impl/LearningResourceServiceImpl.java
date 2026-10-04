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
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.commons.security.FileSecurityService;
import com.example.school_management.commons.security.FileSecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LearningResourceServiceImpl implements LearningResourceService {

    private static final String FILE_URL_PREFIX = "/api/v1/learning-resources/files/";
    private static final String TARGETS_DENIED = "You can only change the targets of resources you created";

    private final LearningResourceRepository repository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final LearningResourceMapper mapper;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;
    private final FileSecurityService fileSecurityService;
    private final CurrentSchoolResolver currentSchool;
    private final SchoolMembershipRepository memberships;

    @Value("${app.file.upload.path:uploads/learning-resources}")
    private String uploadPath;

    @Override
    public LearningResourceDto create(CreateLearningResourceRequest request) {
        requireNotManagedFileUrl(request.getUrl());

        LearningResource resource = new LearningResource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setUrl(request.getUrl());
        resource.setType(request.getType());
        resource.setThumbnailUrl(request.getThumbnailUrl());
        resource.setDuration(request.getDuration());
        resource.setPublic(request.getIsPublic());
        
        assignOwnershipAndTargets(resource, request);

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
            log.warn("Failed to create audit event for resource creation: {}", e.getClass().getSimpleName());
        }
        
        return mapper.toDto(saved);
    }

    @Override
    public LearningResourceDto uploadResource(MultipartFile file, CreateLearningResourceRequest request) {
        return uploadResourceWithVisibility(file, request, true); // Default to public
    }

    public LearningResourceDto uploadResourceWithVisibility(MultipartFile file, CreateLearningResourceRequest request, Boolean isPublic) {
        // Determine resource type first
        ResourceType resourceType = determineResourceType(StringUtils.getFilenameExtension(file.getOriginalFilename()));
        
        // Validate file with comprehensive security checks
        FileSecurityService.FileValidationResult validationResult = validateFile(file, resourceType);
        
        // Use sanitized filename from validation
        String sanitizedFilename = validationResult.getSanitizedFilename();
        String fileExtension = StringUtils.getFilenameExtension(sanitizedFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
        
        // Resource type already determined during validation
        // File hash available for audit: validationResult.getFileHash()
        
        // Create resource with file URL
        String fileUrl = FILE_URL_PREFIX + uniqueFilename;
        
        // Create resource directly with file URL
        LearningResource resource = new LearningResource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setUrl(fileUrl);
        resource.setType(resourceType);
        resource.setThumbnailUrl(request.getThumbnailUrl());
        resource.setDuration(request.getDuration());
        resource.setPublic(isPublic != null ? isPublic : true); // Use provided visibility or default to true
        
        assignOwnershipAndTargets(resource, request);

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
            log.warn("Failed to create audit event for resource upload: {}", e.getClass().getSimpleName());
        }
        
        return mapper.toDto(saved);
    }

    @Override
    public LearningResourceDto update(Long id, UpdateLearningResourceRequest request) {
        log.debug("Updating learning resource {}", id);
        
        LearningResource resource = requireSchoolResource(id);
        
        requireCreatorOrAdmin(resource, "You can only update resources you created");
        // Resending the resource's own URL is not a change; any other managed URL is refused.
        if (request.getUrl() != null && !request.getUrl().equals(resource.getUrl())) {
            requireNotManagedFileUrl(request.getUrl());
        }
        
        Long schoolId = resource.getSchool().getId();
        List<ClassEntity> classes = request.getClassIds() == null ? null : request.getClassIds().stream()
                .map(classId -> requireSchoolClass(classId, schoolId)).toList();
        List<Course> courses = request.getCourseIds() == null ? null : request.getCourseIds().stream()
                .map(courseId -> requireSchoolCourse(courseId, schoolId)).toList();

        if (request.getTitle() != null) resource.setTitle(request.getTitle());
        if (request.getDescription() != null) resource.setDescription(request.getDescription());
        if (request.getUrl() != null) resource.setUrl(request.getUrl());
        if (request.getType() != null) resource.setType(request.getType());
        if (request.getThumbnailUrl() != null) resource.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getDuration() != null) resource.setDuration(request.getDuration());
        if (request.getIsPublic() != null) resource.setPublic(request.getIsPublic());
        
        if (classes != null) {
            resource.getTargetClasses().clear();
            classes.forEach(resource::addTargetClass);
        }
        if (courses != null) {
            resource.getTargetCourses().clear();
            courses.forEach(resource::addTargetCourse);
        }

        LearningResource updated = repository.save(resource);
        return mapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        log.debug("Deleting learning resource {}", id);
        
        LearningResource resource = requireSchoolResource(id);
        
        requireCreatorOrAdmin(resource, "You can only delete resources you created");
        
        // The global reference count protects storage integrity, including references in other
        // Schools. It is not an authorization/read operation and exposes no foreign details.
        if (resource.getUrl() != null && resource.getUrl().startsWith(FILE_URL_PREFIX)
                && repository.countByUrl(resource.getUrl()) == 1) {
            String filename = resource.getUrl().substring(resource.getUrl().lastIndexOf('/') + 1);
            deleteFile(filename);
        }
        
        repository.delete(resource);
    }

    @Override
    public LearningResourceDto get(Long id) {
        log.debug("Getting learning resource {}", id);
        
        boolean seesPrivate = seesPrivateResources();
        LearningResource resource = requireSchoolResource(id);
        if (!resource.isPublic() && !seesPrivate) {
            throw new AccessDeniedException("This learning resource is not available to you");
        }
        
        return mapper.toDto(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Path resolveReadableFile(String filename) {
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root) || file.equals(root)) {
            throw new ResourceNotFoundException("Learning resource file not found");
        }
        // A file is served only through a resource that references it, under that resource's visibility.
        readableSchoolFileResources(filename);
        return file;
    }

    @Override
    public Page<LearningResourceDto> list(Pageable pageable) {
        log.debug("Listing learning resources with pageable: {}", pageable);
        return repository.findAllSortedBySchoolId(currentSchool.resolve().getId(), !seesPrivateResources(), pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> findByType(ResourceType type, Pageable pageable) {
        log.debug("Finding learning resources by type: {}", type);
        return repository.findByTypeAndSchoolId(type.name(), currentSchool.resolve().getId(), !seesPrivateResources(), pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> findByTeacherId(Long teacherId, Pageable pageable) {
        log.debug("Finding learning resources by teacher ID: {}", teacherId);
        boolean publicOnly = !seesPrivateResources();
        Long schoolId = currentSchool.resolve().getId();
        requireSchoolTeacher(teacherId, schoolId);
        return repository.findByTeacherIdAndSchoolId(teacherId, schoolId, publicOnly, pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> findByClassId(Long classId, Pageable pageable) {
        log.debug("Finding learning resources by class ID: {}", classId);
        boolean publicOnly = !seesPrivateResources();
        Long schoolId = currentSchool.resolve().getId();
        requireSchoolClass(classId, schoolId);
        return repository.findByClassIdAndSchoolId(classId, schoolId, publicOnly, pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> findByCourseId(Long courseId, Pageable pageable) {
        log.debug("Finding learning resources by course ID: {}", courseId);
        boolean publicOnly = !seesPrivateResources();
        Long schoolId = currentSchool.resolve().getId();
        requireSchoolCourse(courseId, schoolId);
        return repository.findByCourseIdAndSchoolId(courseId, schoolId, publicOnly, pageable).map(mapper::toDto);
    }

    @Override
    public Page<LearningResourceDto> searchByTitleOrDescription(String title, String description, Pageable pageable) {
        log.debug("Searching learning resources by search term: {}", title);
        // Use the title parameter as the search term (description parameter is ignored now)
        return repository.searchByTitleOrDescriptionAndSchoolId(title, currentSchool.resolve().getId(), !seesPrivateResources(), pageable).map(mapper::toDto);
    }

    @Override
    public void addTargetClasses(Long resourceId, Set<Long> classIds) {
        log.debug("Adding target classes {} to resource {}", classIds, resourceId);
        
        LearningResource resource = requireSchoolResource(resourceId);
        requireCreatorOrAdmin(resource, TARGETS_DENIED);
        
        List<ClassEntity> targets = classIds.stream()
                .map(id -> requireSchoolClass(id, resource.getSchool().getId())).toList();
        targets.forEach(resource::addTargetClass);

        repository.save(resource);
    }

    @Override
    public void removeTargetClasses(Long resourceId, Set<Long> classIds) {
        log.debug("Removing target classes {} from resource {}", classIds, resourceId);
        
        LearningResource resource = requireSchoolResource(resourceId);
        requireCreatorOrAdmin(resource, TARGETS_DENIED);
        
        List<ClassEntity> targets = classIds.stream()
                .map(id -> requireSchoolClass(id, resource.getSchool().getId())).toList();
        targets.forEach(resource::removeTargetClass);

        repository.save(resource);
    }

    @Override
    public void addTargetCourses(Long resourceId, Set<Long> courseIds) {
        log.debug("Adding target courses {} to resource {}", courseIds, resourceId);
        
        LearningResource resource = requireSchoolResource(resourceId);
        requireCreatorOrAdmin(resource, TARGETS_DENIED);
        
        List<Course> targets = courseIds.stream()
                .map(id -> requireSchoolCourse(id, resource.getSchool().getId())).toList();
        targets.forEach(resource::addTargetCourse);

        repository.save(resource);
    }

    @Override
    public void removeTargetCourses(Long resourceId, Set<Long> courseIds) {
        log.debug("Removing target courses {} from resource {}", courseIds, resourceId);
        
        LearningResource resource = requireSchoolResource(resourceId);
        requireCreatorOrAdmin(resource, TARGETS_DENIED);
        
        List<Course> targets = courseIds.stream()
                .map(id -> requireSchoolCourse(id, resource.getSchool().getId())).toList();
        targets.forEach(resource::removeTargetCourse);

        repository.save(resource);
    }

    @Override
    public void addTeachers(Long resourceId, Set<Long> teacherIds) {
        log.debug("Adding teachers {} to resource {}", teacherIds, resourceId);
        
        LearningResource resource = requireSchoolResource(resourceId);
        requireCurrentAdmin(resource.getSchool().getId());
        
        List<Teacher> targets = teacherIds.stream()
                .map(id -> requireSchoolTeacher(id, resource.getSchool().getId())).toList();
        targets.forEach(resource::addTeacher);

        repository.save(resource);
    }

    @Override
    public void removeTeachers(Long resourceId, Set<Long> teacherIds) {
        log.debug("Removing teachers {} from resource {}", teacherIds, resourceId);
        
        LearningResource resource = requireSchoolResource(resourceId);
        requireCurrentAdmin(resource.getSchool().getId());
        
        List<Teacher> targets = teacherIds.stream()
                .map(id -> requireSchoolTeacher(id, resource.getSchool().getId())).toList();
        targets.forEach(resource::removeTeacher);

        repository.save(resource);
    }

    /**
     * URLs under {@link #FILE_URL_PREFIX} name files stored by the upload pipeline, and deleting a
     * resource deletes the file its managed URL names. Only an upload assigns such a URL, so a
     * resource can never be pointed at, and then delete, a file another resource uploaded.
     */
    private static void requireNotManagedFileUrl(String url) {
        if (url != null && url.startsWith(FILE_URL_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "url: uploaded file URLs are assigned by the upload endpoint");
        }
    }

    private LearningResource requireSchoolResource(Long id) {
        return repository.findByIdAndSchoolId(id, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Learning resource not found with id: " + id));
    }

    private ClassEntity requireSchoolClass(Long id, Long schoolId) {
        return classRepository.findByIdAndAcademicYearSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }

    private Course requireSchoolCourse(Long id, Long schoolId) {
        return courseRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    private Teacher requireSchoolTeacher(Long id, Long schoolId) {
        return teacherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
    }

    private boolean hasMembershipRole(BaseUser user, Long schoolId, MembershipRole role) {
        return memberships.findByUserIdAndSchoolId(user.getId(), schoolId)
                .map(membership -> membership.getRoles().contains(role)).orElse(false);
    }

    private void requireCurrentAdmin(Long schoolId) {
        BaseUser user = getCurrentUser();
        if (user.getRole() != UserRole.ADMIN || !hasMembershipRole(user, schoolId, MembershipRole.ADMIN)) {
            throw new AccessDeniedException("Current-School ADMIN membership is required");
        }
    }

    private void assignOwnershipAndTargets(LearningResource resource, CreateLearningResourceRequest request) {
        resource.setSchool(currentSchool.resolve());
        Long schoolId = resource.getSchool().getId();
        BaseUser user = getCurrentUser(); // Email resolves account identity, never School authorization.
        Teacher creator = null;
        if (user.getRole() == UserRole.ADMIN) {
            requireCurrentAdmin(schoolId);
            // Admin identity is retained by audit; the Teacher creator set remains empty.
        } else {
            if (user.getRole() != UserRole.TEACHER || !hasMembershipRole(user, schoolId, MembershipRole.TEACHER)) {
                throw new AccessDeniedException("Current-School TEACHER membership is required");
            }
            creator = requireSchoolTeacher(user.getId(), schoolId);
        }
        List<ClassEntity> classes = request.getClassIds() == null ? List.of() : request.getClassIds().stream()
                .map(id -> requireSchoolClass(id, schoolId)).toList();
        List<Course> courses = request.getCourseIds() == null ? List.of() : request.getCourseIds().stream()
                .map(id -> requireSchoolCourse(id, schoolId)).toList();

        // Validate the complete request before changing either side of any association.
        if (creator != null) resource.addTeacher(creator);
        classes.forEach(resource::addTargetClass);
        courses.forEach(resource::addTargetCourse);
    }

    /** Called only after resource School ownership has been established. */
    private void requireCreatorOrAdmin(LearningResource resource, String deniedMessage) {
        BaseUser user = getCurrentUser();
        Long schoolId = resource.getSchool().getId();
        if (user.getRole() == UserRole.ADMIN && hasMembershipRole(user, schoolId, MembershipRole.ADMIN)) {
            return;
        }
        boolean creator = user.getRole() == UserRole.TEACHER
                && teacherRepository.findByIdAndSchoolId(user.getId(), schoolId).isPresent()
                && resource.getCreatedBy().stream().anyMatch(teacher -> teacher.getId().equals(user.getId()));
        if (!creator) throw new AccessDeniedException(deniedMessage);
    }

    /**
     * Public means visible to the current School audience. Every caller requires School
     * membership matching their account role. Class/Course targets remain metadata;
     * Student enrollment-target authorization is deferred to a separate product decision.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean seesPrivateResources() {
        BaseUser user = requireCurrentSchoolAudience();
        return user.getRole() == UserRole.TEACHER || user.getRole() == UserRole.ADMIN;
    }

    private BaseUser requireCurrentSchoolAudience() {
        BaseUser user = getCurrentUser();
        MembershipRole role = switch (user.getRole()) {
            case STUDENT -> MembershipRole.STUDENT;
            case TEACHER -> MembershipRole.TEACHER;
            case ADMIN -> MembershipRole.ADMIN;
            default -> throw new AccessDeniedException("Current-School resource audience membership is required");
        };
        if (!hasMembershipRole(user, currentSchool.resolve().getId(), role)) {
            throw new AccessDeniedException("Current-School resource audience membership is required");
        }
        return user;
    }

    private List<LearningResource> readableSchoolFileResources(String filename) {
        boolean seesPrivate = seesPrivateResources();
        List<LearningResource> resources = repository.findByUrlAndSchoolId(FILE_URL_PREFIX + filename,
                currentSchool.resolve().getId());
        if (resources.isEmpty()) throw new ResourceNotFoundException("Learning resource file not found");
        if (resources.stream().noneMatch(LearningResource::isPublic) && !seesPrivate) {
            throw new AccessDeniedException("This learning resource is not available to you");
        }
        return resources;
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
        
        // FileSecurityService logs the rejected rule.
        if (!result.isValid()) {
            throw new FileSecurityException("File validation failed: " + result.getErrorMessage());
        }

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
        if (ext.matches("jpg|jpeg|png|gif|bmp|webp")) {
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
        // Exact match: an unrelated URL that merely contains the file name is not the file's resource.
        for (LearningResource resource : readableSchoolFileResources(filename)) {
            resource.setViewCount(resource.getViewCount() + 1);
        }
    }
    
    @Override
    @Transactional
    public void incrementDownloadCount(String filename) {
        for (LearningResource resource : readableSchoolFileResources(filename)) {
            resource.setDownloadCount(resource.getDownloadCount() + 1);
        }
    }
}
