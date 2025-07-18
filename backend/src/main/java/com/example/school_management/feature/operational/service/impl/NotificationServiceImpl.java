package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.operational.dto.NotificationDto;
import com.example.school_management.feature.operational.entity.Notification;
import com.example.school_management.feature.operational.repository.NotificationRepository;
import com.example.school_management.feature.operational.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepository;

    private BaseUser getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto get(Long id) {
        log.debug("Fetching notification {}", id);
        Notification entity = notificationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        
        // Ensure user can only access their own notifications
        BaseUser currentUser = getCurrentUser();
        if (!entity.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found with id: " + id);
        }
        
        return toDto(entity);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting notification {}", id);
        Notification entity = notificationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        
        // Ensure user can only delete their own notifications
        BaseUser currentUser = getCurrentUser();
        if (!entity.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found with id: " + id);
        }
        
        notificationRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getMyNotifications(Pageable page, Boolean readStatus) {
        log.trace("Getting notifications for current user readStatus={} {}", readStatus, page);
        
        BaseUser currentUser = getCurrentUser();
        
        Specification<Notification> spec = (root, q, cb) -> 
            cb.equal(root.get("user").get("id"), currentUser.getId());
        
        if (readStatus != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("readStatus"), readStatus));
        }
        
        return notificationRepo.findAll(spec, page).map(this::toDto);
    }

    @Override
    public NotificationDto markAsRead(Long id) {
        log.debug("Marking notification {} as read", id);
        
        Notification entity = notificationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        
        // Ensure user can only mark their own notifications as read
        BaseUser currentUser = getCurrentUser();
        if (!entity.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found with id: " + id);
        }
        
        entity.markAsRead();
        return toDto(notificationRepo.save(entity));
    }

    @Override
    public void markAllAsRead() {
        log.info("Marking all notifications as read for current user");
        
        BaseUser currentUser = getCurrentUser();
        
        Specification<Notification> spec = (root, q, cb) -> 
            cb.and(
                cb.equal(root.get("user").get("id"), currentUser.getId()),
                cb.equal(root.get("readStatus"), false)
            );
        
        notificationRepo.findAll(spec).forEach(notification -> {
            notification.markAsRead();
            notificationRepo.save(notification);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getUnreadCount() {
        log.trace("Getting unread notification count for current user");
        
        BaseUser currentUser = getCurrentUser();
        
        Specification<Notification> spec = (root, q, cb) -> 
            cb.and(
                cb.equal(root.get("user").get("id"), currentUser.getId()),
                cb.equal(root.get("readStatus"), false)
            );
        
        return (int) notificationRepo.count(spec);
    }

    private NotificationDto toDto(Notification entity) {
        return new NotificationDto(
            entity.getId(),
            entity.getTitle(),
            entity.getMessage(),
            entity.getType(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getActionUrl(),
            entity.getReadStatus(),
            entity.getCreatedAt(),
            entity.getReadAt()
        );
    }
} 