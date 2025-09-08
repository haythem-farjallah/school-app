package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StaffRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.service.TeacherClassService;
import com.example.school_management.feature.academic.dto.TeacherClassDto;
import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.entity.Announcement;
import com.example.school_management.feature.operational.entity.Notification;
import com.example.school_management.feature.operational.entity.enums.AnnouncementImportance;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import com.example.school_management.feature.operational.repository.AnnouncementRepository;
import com.example.school_management.feature.operational.repository.NotificationRepository;
import com.example.school_management.feature.operational.service.AnnouncementService;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.service.impl.RealTimeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final StaffRepository staffRepo;
    private final TeacherRepository teacherRepo;
    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final TeachingAssignmentRepository teachingAssignmentRepo;
    private final NotificationRepository notificationRepo;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;
    private final RealTimeNotificationService realTimeNotificationService;
    private final ClassRepository classRepo;
    private final CourseRepository courseRepo;
    private final TeacherClassService teacherClassService;

    @Override
    public AnnouncementDto create(CreateAnnouncementRequest req) {
        log.debug("Creating announcement: {}", req);
        
        // Validate dates
        if (req.startDate() != null && req.endDate() != null && req.startDate().isAfter(req.endDate())) {
            throw new ConflictException("Start date cannot be after end date");
        }

        BaseUser currentUser = getCurrentUser();
        
        // Validate targeting permissions
        validateTargetingPermissions(req, currentUser);

        Announcement entity = new Announcement();
        entity.setTitle(req.title());
        entity.setBody(req.body());
        entity.setStartDate(req.startDate());
        entity.setEndDate(req.endDate());
        entity.setIsPublic(req.isPublic());
        entity.setImportance(req.importance());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setTargetType(req.targetType());
        // TODO: Set createdBy after migration is applied
        // entity.setCreatedBy(currentUser);
        
        // Set creator information temporarily
        entity.setCreatedById(currentUser.getId());
        entity.setCreatedByName(currentUser.getFirstName() + " " + currentUser.getLastName());
        
        // Set target classes if specified
        if (req.targetClassIds() != null && !req.targetClassIds().isEmpty()) {
            Set<ClassEntity> targetClasses = new HashSet<>();
            for (Long classId : req.targetClassIds()) {
                ClassEntity classEntity = classRepo.findById(classId)
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
                targetClasses.add(classEntity);
            }
            entity.setTargetClasses(targetClasses);
        }

        // Add publishers - always include the current user as a publisher
        Set<Staff> publishers = new HashSet<>();
        
        // Add current user as publisher if they are staff/teacher
        if (currentUser instanceof Staff) {
            publishers.add((Staff) currentUser);
        } else if (currentUser instanceof Teacher) {
            // Teachers are not Staff, but we need to track who created the announcement
            // For now, we'll handle this in the listing logic
            log.debug("Teacher {} created announcement, will be handled in listing logic", currentUser.getEmail());
        }
        
        // Add additional publishers if specified
        if (req.publisherIds() != null && !req.publisherIds().isEmpty()) {
            for (Long staffId : req.publisherIds()) {
                Staff staff = staffRepo.findById(staffId)
                        .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + staffId));
                publishers.add(staff);
            }
        }
        
        if (!publishers.isEmpty()) {
            entity.setPublishers(publishers);
        }

        Announcement saved = announcementRepo.save(entity);
        log.info("Announcement created id={}", saved.getId());
        
        // Handle targeting and notifications
        if (req.sendNotifications() != null && req.sendNotifications()) {
            handleAnnouncementTargeting(saved, req, currentUser);
        }
        
        // Create audit event
        try {
            String summary = "New announcement created";
            String details = String.format("Announcement created: %s, Importance: %s, Public: %s, Target: %s", 
                saved.getTitle(), saved.getImportance(), saved.getIsPublic(), req.targetType());
            
            auditService.createAuditEvent(
                AuditEventType.ANNOUNCEMENT_CREATED,
                "Announcement",
                saved.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for announcement creation: {}", e.getMessage());
        }
        
        return toDto(saved);
    }

    @Override
    public AnnouncementDto update(Long id, UpdateAnnouncementRequest req) {
        log.debug("Updating announcement {} with {}", id, req);
        
        Announcement entity = announcementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        // Validate dates
        if (req.startDate() != null && req.endDate() != null && req.startDate().isAfter(req.endDate())) {
            throw new ConflictException("Start date cannot be after end date");
        }

        if (req.title() != null) entity.setTitle(req.title());
        if (req.body() != null) entity.setBody(req.body());
        if (req.startDate() != null) entity.setStartDate(req.startDate());
        if (req.endDate() != null) entity.setEndDate(req.endDate());
        if (req.isPublic() != null) entity.setIsPublic(req.isPublic());
        if (req.importance() != null) entity.setImportance(req.importance());

        // Update publishers if specified
        if (req.publisherIds() != null) {
            Set<Staff> publishers = new HashSet<>();
            for (Long staffId : req.publisherIds()) {
                Staff staff = staffRepo.findById(staffId)
                        .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + staffId));
                publishers.add(staff);
            }
            entity.setPublishers(publishers);
        }

        Announcement updatedEntity = announcementRepo.save(entity);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Announcement updated";
            String details = String.format("Announcement updated: %s (ID: %d), Importance: %s, Public: %s", 
                updatedEntity.getTitle(), id, updatedEntity.getImportance(), updatedEntity.getIsPublic());
            
            auditService.createAuditEvent(
                AuditEventType.ANNOUNCEMENT_UPDATED,
                "Announcement",
                id,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for announcement update: {}", e.getMessage());
        }
        
        return toDto(updatedEntity);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting announcement {}", id);
        
        // Get announcement details before deletion for audit
        Announcement entity = announcementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
        String title = entity.getTitle();
        
        announcementRepo.deleteById(id);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Announcement deleted";
            String details = String.format("Announcement deleted: %s (ID: %d)", title, id);
            
            auditService.createAuditEvent(
                AuditEventType.ANNOUNCEMENT_DELETED,
                "Announcement",
                id,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for announcement deletion: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementDto get(Long id) {
        log.debug("Fetching announcement {}", id);
        Announcement entity = announcementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementDto> list(Pageable page, String importance, Boolean isPublic) {
        log.trace("Listing announcements importance={} isPublic={} {}", importance, isPublic, page);

        BaseUser currentUser = getCurrentUser();
        Specification<Announcement> spec = (root, q, cb) -> cb.conjunction();

        // Filter by user role and permissions
        if (currentUser instanceof Teacher) {
            // Teachers see all announcements for now (until createdBy field is added)
            // This allows teachers to see announcements they created and public ones
            log.debug("Teacher {} accessing announcements - showing all for now", currentUser.getEmail());
            // No additional filtering for teachers - they see all announcements
        } else if (currentUser.getRole().name().equals("ADMIN") || currentUser.getRole().name().equals("STAFF")) {
            // Admins and staff see all announcements (no additional filtering)
            log.debug("Admin/Staff {} accessing announcements - showing all", currentUser.getEmail());
        } else {
            // Students and parents see only public announcements
            log.debug("Student/Parent {} accessing announcements - showing only public", currentUser.getEmail());
            spec = spec.and((root, q, cb) -> cb.equal(root.get("isPublic"), true));
        }

        if (importance != null && !importance.isBlank()) {
            try {
                AnnouncementImportance importanceEnum = AnnouncementImportance.valueOf(importance.toUpperCase());
                spec = spec.and((root, q, cb) -> cb.equal(root.get("importance"), importanceEnum));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid importance value: {}", importance);
            }
        }

        if (isPublic != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("isPublic"), isPublic));
        }

        // Add ordering by creation date descending (newest first)
        Pageable pageWithSort = PageRequest.of(page.getPageNumber(), page.getPageSize(), 
            Sort.by(Sort.Direction.DESC, "createdAt"));
        return announcementRepo.findAll(spec, pageWithSort).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementDto> getPublicAnnouncements(Pageable page) {
        log.trace("Listing public announcements {}", page);
        
        Specification<Announcement> spec = (root, q, cb) -> 
            cb.and(
                cb.equal(root.get("isPublic"), true),
                cb.or(
                    cb.isNull(root.get("startDate")),
                    cb.lessThanOrEqualTo(root.get("startDate"), LocalDateTime.now())
                ),
                cb.or(
                    cb.isNull(root.get("endDate")),
                    cb.greaterThanOrEqualTo(root.get("endDate"), LocalDateTime.now())
                )
            );

        return announcementRepo.findAll(spec, page).map(this::toDto);
    }

    @Override
    public AnnouncementDto publish(Long id, PublishAnnouncementRequest req) {
        log.debug("Publishing announcement {} to users {}", id, req.userIds());
        
        Announcement announcement = announcementRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        // Create notifications for each user
        for (Long userId : req.userIds()) {
            Notification notification = new Notification();
            notification.setUser(staffRepo.findById(userId).orElse(null)); // This should be BaseUser, not Staff
            notification.setTitle(announcement.getTitle());
            notification.setMessage(announcement.getBody());
            notification.setType(NotificationType.ANNOUNCEMENT_PUBLISHED);
            notification.setEntityType("ANNOUNCEMENT");
            notification.setEntityId(announcement.getId());
            notification.setActionUrl("/announcements/" + announcement.getId());
            notification.setReadStatus(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepo.save(notification);
        }

        log.info("Announcement {} published to {} users", id, req.userIds().size());
        return toDto(announcement);
    }

    /**
     * Validate that the current user has permission to target the specified audience
     */
    private void validateTargetingPermissions(CreateAnnouncementRequest req, BaseUser currentUser) {
        String targetType = req.targetType();
        if (targetType == null) return;
        
        switch (targetType) {
            case "CLASSES":
                // Only teachers can send to their classes
                if (!(currentUser instanceof Teacher)) {
                    throw new ConflictException("Only teachers can send announcements to classes");
                }
                // Validate that teacher teaches these classes
                if (req.targetClassIds() != null && !req.targetClassIds().isEmpty()) {
                    validateTeacherClassAccess((Teacher) currentUser, req.targetClassIds());
                }
                break;
                
            case "ALL_STAFF":
            case "ALL_TEACHERS":
            case "ALL_STUDENTS":
            case "WHOLE_SCHOOL":
                // Only admins can send to these broad audiences
                if (!currentUser.getRole().name().equals("ADMIN")) {
                    throw new ConflictException("Only administrators can send announcements to " + targetType.toLowerCase().replace("_", " "));
                }
                break;
                
            case "SPECIFIC_USERS":
                // Anyone can send to specific users (with proper validation)
                break;
                
            default:
                log.warn("Unknown target type: {}", targetType);
        }
    }
    
    /**
     * Validate that a teacher has access to the specified classes
     * Uses the same logic as TeacherClassService to ensure consistency
     */
    private void validateTeacherClassAccess(Teacher teacher, Set<Long> classIds) {
        // Get all classes the teacher has access to using the same service as My Classes page
        List<TeacherClassDto> teacherClasses = teacherClassService.getAllTeacherClasses(teacher.getEmail(), null);
        Set<Long> teacherClassIds = teacherClasses.stream()
            .map(tc -> (long) tc.id())
            .collect(Collectors.toSet());
            
        log.debug("Teacher {} has access to classes: {}", teacher.getEmail(), teacherClassIds);
        log.debug("Requested class IDs: {}", classIds);
            
        for (Long classId : classIds) {
            if (!teacherClassIds.contains(classId)) {
                throw new ConflictException("Teacher does not have access to class with ID: " + classId);
            }
        }
    }
    
    /**
     * Handle announcement targeting and send notifications
     */
    private void handleAnnouncementTargeting(Announcement announcement, CreateAnnouncementRequest req, BaseUser sender) {
        String targetType = req.targetType();
        if (targetType == null) return;
        
        List<BaseUser> targetUsers = new ArrayList<>();
        Set<String> targetRoles = new HashSet<>();
        
        switch (targetType) {
            case "CLASSES":
                if (req.targetClassIds() != null && !req.targetClassIds().isEmpty()) {
                    List<Student> students = studentRepo.findByClassIds(new ArrayList<>(req.targetClassIds()));
                    targetUsers.addAll(students);
                    targetRoles.add("STUDENT");
                }
                break;
                
            case "ALL_STAFF":
                List<Staff> allStaff = staffRepo.findAll();
                targetUsers.addAll(allStaff);
                targetRoles.add("STAFF");
                break;
                
            case "ALL_TEACHERS":
                List<Teacher> allTeachers = teacherRepo.findAll();
                targetUsers.addAll(allTeachers);
                targetRoles.add("TEACHER");
                break;
                
            case "ALL_STUDENTS":
                List<Student> allStudents = studentRepo.findAll();
                targetUsers.addAll(allStudents);
                targetRoles.add("STUDENT");
                break;
                
            case "WHOLE_SCHOOL":
                targetUsers.addAll(staffRepo.findAll());
                targetUsers.addAll(teacherRepo.findAll());
                targetUsers.addAll(studentRepo.findAll());
                targetUsers.addAll(parentRepo.findAll());
                targetRoles.addAll(Set.of("STAFF", "TEACHER", "STUDENT", "PARENT"));
                break;
                
            case "SPECIFIC_USERS":
                if (req.targetUserIds() != null && !req.targetUserIds().isEmpty()) {
                    for (Long userId : req.targetUserIds()) {
                        userRepo.findById(userId).ifPresent(targetUsers::add);
                    }
                }
                break;
        }
        
        // Create database notifications
        createNotificationsForUsers(announcement, targetUsers);
        
        // Send real-time notifications
        if (!targetRoles.isEmpty()) {
            realTimeNotificationService.notifyNewAnnouncement(
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getImportance().name(),
                targetRoles
            );
        }
        
        // Send specific user notifications
        if (!targetUsers.isEmpty()) {
            Set<Long> userIds = targetUsers.stream().map(BaseUser::getId).collect(Collectors.toSet());
            realTimeNotificationService.notifySpecificUsers(
                "New Announcement: " + announcement.getTitle(),
                announcement.getBody(),
                announcement.getImportance().name(),
                userIds
            );
        }
        
        log.info("Sent announcement '{}' to {} users and {} roles", 
            announcement.getTitle(), targetUsers.size(), targetRoles.size());
    }
    
    /**
     * Create database notifications for users
     */
    private void createNotificationsForUsers(Announcement announcement, List<BaseUser> users) {
        for (BaseUser user : users) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle("New Announcement: " + announcement.getTitle());
            notification.setMessage(announcement.getBody());
            notification.setType(NotificationType.ANNOUNCEMENT_PUBLISHED);
            notification.setEntityType("ANNOUNCEMENT");
            notification.setEntityId(announcement.getId());
            notification.setActionUrl("/announcements/" + announcement.getId());
            notification.setReadStatus(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepo.save(notification);
        }
    }

    private AnnouncementDto toDto(Announcement entity) {
        Set<Long> publisherIds = entity.getPublishers() != null ? 
            entity.getPublishers().stream().map(Staff::getId).collect(java.util.stream.Collectors.toSet()) : 
            new HashSet<>();
            
        // Get creator info from entity (temporary solution)
        Long createdById = entity.getCreatedById();
        String createdByName = entity.getCreatedByName();
            
        // Get target class information
        Set<Long> targetClassIds = entity.getTargetClasses() != null ?
            entity.getTargetClasses().stream().map(ClassEntity::getId).collect(java.util.stream.Collectors.toSet()) :
            new HashSet<>();
            
        Set<String> targetClassNames = entity.getTargetClasses() != null ?
            entity.getTargetClasses().stream().map(ClassEntity::getName).collect(java.util.stream.Collectors.toSet()) :
            new HashSet<>();
            
        return new AnnouncementDto(
            entity.getId(),
            entity.getTitle(),
            entity.getBody(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.getIsPublic(),
            entity.getImportance(),
            entity.getCreatedAt(),
            createdById,
            createdByName,
            publisherIds,
            entity.getTargetType(),
            targetClassIds,
            targetClassNames
        );
    }
    
        @Override
    public Object getTeacherClasses() {
        BaseUser currentUser = getCurrentUser();
        if (!(currentUser instanceof Teacher)) {
            throw new ConflictException("Only teachers can access this endpoint");
        }

        Teacher teacher = (Teacher) currentUser;
        
        // Use the existing TeacherClassService to get classes (same as My Classes page)
        List<TeacherClassDto> teacherClasses = teacherClassService.getAllTeacherClasses(teacher.getEmail(), null);
        
        // Convert to the format expected by the frontend
        return teacherClasses.stream()
            .map(tc -> {
                var classInfo = new java.util.HashMap<String, Object>();
                classInfo.put("id", tc.id());
                classInfo.put("name", tc.name());
                classInfo.put("gradeLevel", tc.grade());
                classInfo.put("section", ""); // TeacherClassDto doesn't have section, use empty string
                classInfo.put("course", "Multiple Courses"); // TeacherClassDto represents class, not individual courses
                return classInfo;
            })
            .collect(Collectors.toList());
    }

    @Override
    public void createTestTeachingAssignments() {
        BaseUser currentUser = getCurrentUser();
        if (!(currentUser instanceof Teacher)) {
            throw new ConflictException("Only teachers can create test assignments");
        }
        
        Teacher teacher = (Teacher) currentUser;
        
        // Check if teacher already has assignments
        List<TeachingAssignment> existingAssignments = teachingAssignmentRepo.findByTeacherId(teacher.getId());
        if (!existingAssignments.isEmpty()) {
            log.info("Teacher {} already has {} assignments", teacher.getEmail(), existingAssignments.size());
            return;
        }
        
        // Get first 3 classes and first 2 courses
        List<ClassEntity> classes = classRepo.findAll().stream().limit(3).toList();
        List<Course> courses = courseRepo.findAll().stream().limit(2).toList();
        
        if (classes.isEmpty() || courses.isEmpty()) {
            throw new ConflictException("No classes or courses found. Please create some first.");
        }
        
        // Create assignments for each class-course combination
        for (ClassEntity clazz : classes) {
            for (Course course : courses) {
                // Check if assignment already exists for this class-course combination
                if (!teachingAssignmentRepo.existsByClazzIdAndCourseId(clazz.getId(), course.getId())) {
                    TeachingAssignment assignment = new TeachingAssignment();
                    assignment.setClazz(clazz);
                    assignment.setCourse(course);
                    assignment.setTeacher(teacher);
                    assignment.setWeeklyHours(4); // Default 4 hours per week
                    
                    teachingAssignmentRepo.save(assignment);
                    log.info("Created teaching assignment: Teacher {} -> Class {} -> Course {}", 
                            teacher.getEmail(), clazz.getName(), course.getName());
                }
            }
        }
        
        log.info("Test teaching assignments created for teacher: {}", teacher.getEmail());
    }

    /**
     * Get the current authenticated user
     */
    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + email));
    }
} 