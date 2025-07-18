package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.AdminFeedDto;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminFeedService {
    
    AdminFeedDto create(AuditEventType eventType, NotificationType notificationType, 
                       String title, String description, String entityType, 
                       Long entityId, String entityName, String severity, 
                       Long triggeredById, Long targetUserId, String metadata);
    
    AdminFeedDto get(Long id);
    
    void delete(Long id);
    
    void markAsRead(Long id);
    
    void markAllAsRead();
    
    Page<AdminFeedDto> list(Pageable pageable);
    
    Page<AdminFeedDto> findByEventType(AuditEventType eventType, Pageable pageable);
    
    Page<AdminFeedDto> findByNotificationType(NotificationType notificationType, Pageable pageable);
    
    Page<AdminFeedDto> findBySeverity(String severity, Pageable pageable);
    
    Page<AdminFeedDto> findByEntityType(String entityType, Pageable pageable);
    
    Page<AdminFeedDto> findByTriggeredBy(Long userId, Pageable pageable);
    
    Page<AdminFeedDto> findByTargetUser(Long userId, Pageable pageable);
    
    Page<AdminFeedDto> findUnread(Pageable pageable);
    
    Page<AdminFeedDto> findHighPriority(Pageable pageable);
    
    Page<AdminFeedDto> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    List<AdminFeedDto> findRecent(int limit);
    
    long countUnread();
    
    long countByEventType(AuditEventType eventType);
    
    long countBySeverity(String severity);
} 