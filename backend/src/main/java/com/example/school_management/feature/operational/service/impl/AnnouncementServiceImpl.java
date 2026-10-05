package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StaffRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.service.TeacherClassService;
import com.example.school_management.feature.academic.dto.TeacherClassDto;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnouncementRepository announcementRepo;
    private final StaffRepository staffRepo;
    private final StudentRepository studentRepo;
    private final NotificationRepository notificationRepo;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;
    private final RealTimeNotificationService realTimeNotificationService;
    private final ClassRepository classRepo;
    private final TeacherClassService teacherClassService;
    private final CurrentSchoolResolver currentSchool;
    private final SchoolRepository schoolRepo;
    private final SchoolMembershipRepository memberships;

    @Override
    public AnnouncementDto create(CreateAnnouncementRequest req) {
        BaseUser currentUser = getCurrentUser();
        School school = requireCallerSchool(currentUser);
        requireCreator(currentUser);
        validateDates(req.startDate(), req.endDate());
        validateTargetingPermissions(req, currentUser);
        Set<ClassEntity> targetClasses = resolveClasses(req.targetClassIds(), school);
        if ("CLASSES".equals(req.targetType())) validateTeacherClassAccess(currentUser, targetClasses);
        Set<Staff> publishers = resolvePublishers(req.publisherIds(), school);
        if (currentUser instanceof Staff publisher) {
            requireStaffCompatibility(school);
            publishers.add(publisher);
        }
        // Resolve the full audience before persistence, even when delivery is disabled.
        List<BaseUser> recipients = resolveAudience(req, targetClasses, school);

        Announcement entity = new Announcement();
        entity.setSchool(school);
        entity.setTitle(req.title());
        entity.setBody(req.body());
        entity.setStartDate(req.startDate());
        entity.setEndDate(req.endDate());
        entity.setIsPublic(req.isPublic());
        entity.setImportance(req.importance());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setTargetType(req.targetType());
        entity.setCreatedById(currentUser.getId());
        entity.setCreatedByName(currentUser.getFirstName() + " " + currentUser.getLastName());
        entity.setTargetClasses(targetClasses);
        entity.setPublishers(publishers);
        Announcement saved = announcementRepo.save(entity);
        if (Boolean.TRUE.equals(req.sendNotifications())) deliver(saved, recipients, true);
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
            log.warn("Failed to create audit event for announcement creation: {}", e.getClass().getSimpleName());
        }
        
        return toDto(saved);
    }

    @Override
    public AnnouncementDto update(Long id, UpdateAnnouncementRequest req) {
        BaseUser currentUser = getCurrentUser();
        School school = requireCallerSchool(currentUser);
        requireCreator(currentUser);
        Announcement entity = requireSchoolAnnouncement(id, school);
        requireTeacherIsCreator(entity, currentUser, "update");
        validateDates(req.startDate(), req.endDate());
        Set<Staff> publishers = req.publisherIds() == null ? null : resolvePublishers(req.publisherIds(), school);

        if (req.title() != null) entity.setTitle(req.title());
        if (req.body() != null) entity.setBody(req.body());
        if (req.startDate() != null) entity.setStartDate(req.startDate());
        if (req.endDate() != null) entity.setEndDate(req.endDate());
        if (req.isPublic() != null) entity.setIsPublic(req.isPublic());
        if (req.importance() != null) entity.setImportance(req.importance());
        if (publishers != null) entity.setPublishers(publishers);
        Announcement updatedEntity = announcementRepo.save(entity);
        // Create audit event
        try {
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
            log.warn("Failed to create audit event for announcement update: {}", e.getClass().getSimpleName());
        }
        
        return toDto(updatedEntity);
    }

    @Override
    public void delete(Long id) {
        BaseUser currentUser = getCurrentUser();
        School school = requireCallerSchool(currentUser);
        requireCreator(currentUser);
        Announcement entity = requireSchoolAnnouncement(id, school);
        requireTeacherIsCreator(entity, currentUser, "delete");
        String title = entity.getTitle();
        announcementRepo.delete(entity);
        // Create audit event
        try {
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
            log.warn("Failed to create audit event for announcement deletion: {}", e.getClass().getSimpleName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementDto get(Long id) {
        BaseUser caller = getCurrentUser();
        School school = requireCallerSchool(caller);
        Announcement entity = requireSchoolAnnouncement(id, school);
        if (isPublicOnly(caller) && !Boolean.TRUE.equals(entity.getIsPublic())) {
            throw new AccessDeniedException("You can only access public announcements");
        }
        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementDto> list(Pageable page, String importance, Boolean isPublic) {
        BaseUser caller = getCurrentUser();
        School school = requireCallerSchool(caller);
        Specification<Announcement> spec = inSchool(school);
        if (isPublicOnly(caller)) spec = spec.and((root, q, cb) -> cb.isTrue(root.get("isPublic")));
        if (importance != null && !importance.isBlank()) {
            try {
                AnnouncementImportance value = AnnouncementImportance.valueOf(importance.toUpperCase());
                spec = spec.and((root, q, cb) -> cb.equal(((JpaExpression<?>) root.get("importance")).cast(String.class), value.name()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid importance value: {}", importance);
            }
        }
        if (isPublic != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("isPublic"), isPublic));
        Pageable sorted = PageRequest.of(page.getPageNumber(), page.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return announcementRepo.findAll(spec, sorted).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnouncementDto> getPublicAnnouncements(Pageable page) {
        School school = requireCallerSchool(getCurrentUser());
        LocalDateTime now = LocalDateTime.now();
        Specification<Announcement> spec = inSchool(school).and((root, q, cb) -> cb.and(
                cb.isTrue(root.get("isPublic")),
                cb.or(cb.isNull(root.get("startDate")), cb.lessThanOrEqualTo(root.get("startDate"), now)),
                cb.or(cb.isNull(root.get("endDate")), cb.greaterThanOrEqualTo(root.get("endDate"), now))));
        return announcementRepo.findAll(spec, page).map(this::toDto);
    }

    @Override
    public AnnouncementDto publish(Long id, PublishAnnouncementRequest req) {
        BaseUser caller = getCurrentUser();
        School school = requireCallerSchool(caller);
        if (caller.getRole() != UserRole.ADMIN && caller.getRole() != UserRole.STAFF) {
            throw new AccessDeniedException("Only administrators and staff can publish announcements");
        }
        Announcement announcement = requireSchoolAnnouncement(id, school);
        List<BaseUser> recipients = resolveSpecificUsers(req.userIds(), school);
        deliver(announcement, recipients, false);
        return toDto(announcement);
    }

    private Announcement requireSchoolAnnouncement(Long id, School school) {
        return announcementRepo.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
    }

    private Specification<Announcement> inSchool(School school) {
        return (root, q, cb) -> cb.equal(root.get("school").get("id"), school.getId());
    }

    private School requireCallerSchool(BaseUser caller) {
        // Staff must fail with 403 before the single-School resolver can reject
        // ambiguous/empty database state, and before any resource is inspected.
        if (caller.getRole() == UserRole.STAFF && schoolRepo.count() != 1) {
            throw new AccessDeniedException("Staff access requires single-School compatibility");
        }
        School school = currentSchool.resolve();
        if (caller.getRole() == UserRole.STAFF) {
            if (!singleSchoolCompatibility(school)) throw new AccessDeniedException("Staff access requires single-School compatibility");
        } else {
            MembershipRole role = switch (caller.getRole()) {
                case ADMIN -> MembershipRole.ADMIN;
                case TEACHER -> MembershipRole.TEACHER;
                case STUDENT -> MembershipRole.STUDENT;
                case PARENT -> MembershipRole.GUARDIAN;
                default -> throw new AccessDeniedException("School membership required");
            };
            if (memberships.findByUserIdAndSchoolId(caller.getId(), school.getId())
                    .filter(membership -> membership.getRoles().contains(role)).isEmpty()) {
                throw new AccessDeniedException("School membership with the required role is required");
            }
        }
        return school;
    }

    private boolean singleSchoolCompatibility(School school) {
        return schoolRepo.count() == 1 && schoolRepo.existsById(school.getId());
    }

    private void requireStaffCompatibility(School school) {
        if (!singleSchoolCompatibility(school)) {
            throw new ConflictException("Staff targeting and publishers require single-School compatibility");
        }
    }

    private void requireCreator(BaseUser caller) {
        if (!Set.of(UserRole.ADMIN, UserRole.TEACHER, UserRole.STAFF).contains(caller.getRole())) {
            throw new AccessDeniedException("Only administrators, teachers and staff can manage announcements");
        }
    }

    private boolean isPublicOnly(BaseUser caller) {
        return caller.getRole() == UserRole.STUDENT || caller.getRole() == UserRole.PARENT;
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) throw new ConflictException("Start date cannot be after end date");
    }

    private void validateTargetingPermissions(CreateAnnouncementRequest req, BaseUser caller) {
        if (req.targetType() == null) return;
        switch (req.targetType()) {
            case "CLASSES" -> {
                if (caller.getRole() != UserRole.TEACHER) throw new ConflictException("Only teachers can send announcements to classes");
            }
            case "ALL_STAFF", "ALL_TEACHERS", "ALL_STUDENTS", "WHOLE_SCHOOL" -> {
                if (caller.getRole() != UserRole.ADMIN) throw new ConflictException("Only administrators can send announcements to broad audiences");
            }
            case "SPECIFIC_USERS" -> { }
            default -> throw new ConflictException("Unknown announcement target type");
        }
    }

    private Set<ClassEntity> resolveClasses(Set<Long> ids, School school) {
        Set<ClassEntity> classes = new HashSet<>();
        if (ids != null) for (Long id : ids) {
            classes.add(classRepo.findByIdAndAcademicYearSchoolId(id, school.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found")));
        }
        return classes;
    }

    private void validateTeacherClassAccess(BaseUser teacher, Set<ClassEntity> classes) {
        Set<Long> allowed = teacherClassService.getAllTeacherClasses(teacher.getEmail(), null).stream()
                .map(TeacherClassDto::id).collect(Collectors.toSet());
        if (classes.stream().anyMatch(clazz -> !allowed.contains(clazz.getId()))) {
            throw new ConflictException("Teacher does not have access to the requested classes");
        }
    }

    private Set<Staff> resolvePublishers(Set<Long> ids, School school) {
        Set<Staff> publishers = new HashSet<>();
        if (ids == null || ids.isEmpty()) return publishers;
        requireStaffCompatibility(school);
        if (ids.contains(null)) throw new ResourceNotFoundException("Staff not found");
        // Global Staff identity is usable only after proving single-School state.
        for (Long id : ids) publishers.add(staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found")));
        return publishers;
    }

    private List<BaseUser> resolveSpecificUsers(Set<Long> ids, School school) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<BaseUser> recipients = new ArrayList<>(memberships.findUsersBySchoolIdAndIdsAndRoles(
                school.getId(), ids, Set.of(MembershipRole.STUDENT, MembershipRole.TEACHER, MembershipRole.ADMIN, MembershipRole.GUARDIAN)));
        Set<Long> unresolved = new HashSet<>(ids);
        recipients.forEach(user -> unresolved.remove(user.getId()));
        if (!unresolved.isEmpty() && singleSchoolCompatibility(school)) {
            // Guarded legacy compatibility, never a generic BaseUser lookup.
            for (Staff user : staffRepo.findAllById(unresolved)) {
                if (user.getRole() == UserRole.STAFF) {
                    recipients.add(user);
                    unresolved.remove(user.getId());
                }
            }
        }
        if (!unresolved.isEmpty()) throw new ResourceNotFoundException("Announcement recipient not found");
        return recipients;
    }

    private List<BaseUser> resolveAudience(CreateAnnouncementRequest req, Set<ClassEntity> classes, School school) {
        // Validate any supplied specific IDs, even when another audience is selected.
        List<BaseUser> specific = resolveSpecificUsers(req.targetUserIds(), school);
        if (req.targetType() == null) return List.of();
        return switch (req.targetType()) {
            case "CLASSES" -> classes.isEmpty() ? List.of() : new ArrayList<>(studentRepo.findAnnouncementRecipientsByClassIdsAndSchoolId(
                    classes.stream().map(ClassEntity::getId).toList(), school.getId()));
            case "ALL_TEACHERS" -> memberships.findUsersBySchoolIdAndRoles(school.getId(), Set.of(MembershipRole.TEACHER));
            case "ALL_STUDENTS" -> memberships.findUsersBySchoolIdAndRoles(school.getId(), Set.of(MembershipRole.STUDENT));
            case "ALL_STAFF" -> {
                requireStaffCompatibility(school);
                yield new ArrayList<>(staffRepo.findAll());
            }
            case "WHOLE_SCHOOL" -> {
                requireStaffCompatibility(school);
                List<BaseUser> recipients = new ArrayList<>(memberships.findUsersBySchoolIdAndRoles(school.getId(),
                        Set.of(MembershipRole.TEACHER, MembershipRole.STUDENT, MembershipRole.GUARDIAN)));
                recipients.addAll(staffRepo.findAll());
                yield recipients;
            }
            case "SPECIFIC_USERS" -> specific;
            default -> throw new ConflictException("Unknown announcement target type");
        };
    }

    private void deliver(Announcement announcement, List<BaseUser> recipients, boolean newAnnouncement) {
        Map<Long, BaseUser> unique = new LinkedHashMap<>();
        recipients.forEach(user -> unique.put(user.getId(), user));
        String title = newAnnouncement ? "New Announcement: " + announcement.getTitle() : announcement.getTitle();
        for (BaseUser user : unique.values()) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(announcement.getBody());
            notification.setType(NotificationType.ANNOUNCEMENT_PUBLISHED);
            notification.setEntityType("ANNOUNCEMENT");
            notification.setEntityId(announcement.getId());
            notification.setActionUrl("/announcements/" + announcement.getId());
            notification.setReadStatus(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepo.save(notification);
        }
        String priority = switch (announcement.getImportance()) {
            case URGENT, HIGH -> "HIGH";
            case MEDIUM -> "MEDIUM";
            case LOW -> "LOW";
        };
        if (!unique.isEmpty()) realTimeNotificationService.notifySpecificUsers(title, announcement.getBody(), priority, unique.keySet());
    }

    private AnnouncementDto toDto(Announcement entity) {
        Set<Long> publisherIds = singleSchoolCompatibility(entity.getSchool())
                ? entity.getPublishers().stream().map(Staff::getId).collect(Collectors.toSet()) : Set.of();
        return new AnnouncementDto(entity.getId(), entity.getTitle(), entity.getBody(), entity.getStartDate(), entity.getEndDate(),
                entity.getIsPublic(), entity.getImportance(), entity.getCreatedAt(), entity.getCreatedById(), entity.getCreatedByName(),
                publisherIds, entity.getTargetType(), entity.getTargetClasses().stream().map(ClassEntity::getId).collect(Collectors.toSet()),
                entity.getTargetClasses().stream().map(ClassEntity::getName).collect(Collectors.toSet()));
    }

    @Override
    @Transactional(readOnly = true)
    public Object getTeacherClasses() {
        BaseUser caller = getCurrentUser();
        requireCallerSchool(caller);
        if (caller.getRole() != UserRole.TEACHER) throw new ConflictException("Only teachers can access this endpoint");
        return teacherClassService.getAllTeacherClasses(caller.getEmail(), null).stream().map(tc -> {
            var info = new HashMap<String, Object>();
            info.put("id", tc.id()); info.put("name", tc.name()); info.put("gradeLevel", tc.grade());
            info.put("section", ""); info.put("course", "Multiple Courses");
            return info;
        }).toList();
    }

    private void requireTeacherIsCreator(Announcement announcement, BaseUser caller, String action) {
        if (caller.getRole() == UserRole.TEACHER && !caller.getId().equals(announcement.getCreatedById())) {
            throw new AccessDeniedException("You can only " + action + " announcements you created");
        }
    }

    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email).orElseThrow(() -> new IllegalStateException("Current user not found"));
    }
}
