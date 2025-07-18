package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.operational.dto.AdminFeedDto;
import com.example.school_management.feature.operational.entity.AdminFeed;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import com.example.school_management.feature.operational.repository.AdminFeedRepository;
import com.example.school_management.feature.operational.service.AdminFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminFeedServiceImpl implements AdminFeedService {

    private final AdminFeedRepository repository;
    private final UserRepository userRepository;

    @Override
    public AdminFeedDto create(AuditEventType eventType, NotificationType notificationType, 
                             String title, String description, String entityType, 
                             Long entityId, String entityName, String severity, 
                             Long triggeredById, Long targetUserId, String metadata) {
        log.debug("Creating admin feed: {} - {}", eventType, title);
        
        AdminFeed feed = new AdminFeed();
        feed.setEventType(eventType);
        feed.setNotificationType(notificationType);
        feed.setTitle(title);
        feed.setDescription(description);
        feed.setEntityType(entityType);
        feed.setEntityId(entityId);
        feed.setEntityName(entityName);
        feed.setSeverity(severity);
        feed.setMetadata(metadata);
        
        // Set triggered by user
        if (triggeredById != null) {
            BaseUser triggeredBy = userRepository.findById(triggeredById)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + triggeredById));
            feed.setTriggeredBy(triggeredBy);
        }
        
        // Set target user
        if (targetUserId != null) {
            BaseUser targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));
            feed.setTargetUser(targetUser);
        }
        
        AdminFeed saved = repository.save(feed);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminFeedDto get(Long id) {
        log.debug("Fetching admin feed {}", id);
        
        AdminFeed feed = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin feed not found with id: " + id));
        
        return mapToDto(feed);
    }

    @Override
    public void delete(Long id) {
        log.debug("Deleting admin feed {}", id);
        
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Admin feed not found with id: " + id);
        }
        
        repository.deleteById(id);
    }

    @Override
    public void markAsRead(Long id) {
        log.debug("Marking admin feed {} as read", id);
        repository.markAsRead(id);
    }

    @Override
    public void markAllAsRead() {
        log.debug("Marking all admin feeds as read");
        repository.markAllAsRead();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> list(Pageable pageable) {
        return repository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findByEventType(AuditEventType eventType, Pageable pageable) {
        return repository.findByEventType(eventType, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findByNotificationType(NotificationType notificationType, Pageable pageable) {
        return repository.findByNotificationType(notificationType, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findBySeverity(String severity, Pageable pageable) {
        return repository.findBySeverity(severity, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findByEntityType(String entityType, Pageable pageable) {
        return repository.findByEntityType(entityType, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findByTriggeredBy(Long userId, Pageable pageable) {
        return repository.findByTriggeredById(userId, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findByTargetUser(Long userId, Pageable pageable) {
        return repository.findByTargetUserId(userId, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findUnread(Pageable pageable) {
        return repository.findUnread(pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findHighPriority(Pageable pageable) {
        return repository.findHighPriority(pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminFeedDto> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return repository.findByDateRange(startDate, endDate, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminFeedDto> findRecent(int limit) {
        return repository.findRecent(limit).stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread() {
        return repository.countUnread();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByEventType(AuditEventType eventType) {
        return repository.countByEventType(eventType);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBySeverity(String severity) {
        return repository.countBySeverity(severity);
    }

    private AdminFeedDto mapToDto(AdminFeed feed) {
        return new AdminFeedDto(
                feed.getId(),
                feed.getEventType(),
                feed.getNotificationType(),
                feed.getTitle(),
                feed.getDescription(),
                feed.getEntityType(),
                feed.getEntityId(),
                feed.getEntityName(),
                feed.getSeverity(),
                feed.isRead(),
                feed.getTriggeredBy() != null ? feed.getTriggeredBy().getId() : null,
                feed.getTriggeredBy() != null ? 
                    feed.getTriggeredBy().getFirstName() + " " + feed.getTriggeredBy().getLastName() : null,
                feed.getTargetUser() != null ? feed.getTargetUser().getId() : null,
                feed.getTargetUser() != null ? 
                    feed.getTargetUser().getFirstName() + " " + feed.getTargetUser().getLastName() : null,
                feed.getCreatedAt(),
                feed.getMetadata()
        );
    }
} 