package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StaffRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final StaffRepository staffRepo;
    private final NotificationRepository notificationRepo;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;

    @Override
    public AnnouncementDto create(CreateAnnouncementRequest req) {
        log.debug("Creating announcement: {}", req);
        
        // Validate dates
        if (req.startDate() != null && req.endDate() != null && req.startDate().isAfter(req.endDate())) {
            throw new ConflictException("Start date cannot be after end date");
        }

        Announcement entity = new Announcement();
        entity.setTitle(req.title());
        entity.setBody(req.body());
        entity.setStartDate(req.startDate());
        entity.setEndDate(req.endDate());
        entity.setIsPublic(req.isPublic());
        entity.setImportance(req.importance());
        entity.setCreatedAt(LocalDateTime.now());

        // Add publishers if specified
        if (req.publisherIds() != null && !req.publisherIds().isEmpty()) {
            Set<Staff> publishers = new HashSet<>();
            for (Long staffId : req.publisherIds()) {
                Staff staff = staffRepo.findById(staffId)
                        .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + staffId));
                publishers.add(staff);
            }
            entity.setPublishers(publishers);
        }

        Announcement saved = announcementRepo.save(entity);
        log.info("Announcement created id={}", saved.getId());
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "New announcement created";
            String details = String.format("Announcement created: %s, Importance: %s, Public: %s", 
                saved.getTitle(), saved.getImportance(), saved.getIsPublic());
            
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

        Specification<Announcement> spec = (root, q, cb) -> cb.conjunction();

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

        return announcementRepo.findAll(spec, page).map(this::toDto);
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

    private AnnouncementDto toDto(Announcement entity) {
        Set<Long> publisherIds = entity.getPublishers() != null ? 
            entity.getPublishers().stream().map(Staff::getId).collect(java.util.stream.Collectors.toSet()) : 
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
            publisherIds
        );
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