package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.entity.AuditEvent;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.repository.AuditEventRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.auth.entity.BaseUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {
    
    private final AuditEventRepository auditEventRepository;
    private final RealTimeNotificationService realTimeNotificationService;
    
    @Override
    @Transactional
    public void createGradeAuditEvent(AuditEventType eventType, Long gradeId, String summary, String details, BaseUser actedBy) {
        createAuditEvent(eventType, "Grade", gradeId, summary, details, actedBy);
    }
    
    @Override
    @Transactional
    public void createGradeAuditEvent(AuditEventType eventType, Long gradeId, String summary, String details, BaseUser actedBy, String ipAddress, String userAgent) {
        createAuditEvent(eventType, "Grade", gradeId, summary, details, actedBy, ipAddress, userAgent);
    }
    
    @Override
    @Transactional
    public void createAuditEvent(AuditEventType eventType, String entityType, Long entityId, String summary, String details, BaseUser actedBy) {
        createAuditEvent(eventType, entityType, entityId, summary, details, actedBy, null, null);
    }
    
    @Override
    @Transactional
    public void createAuditEvent(AuditEventType eventType, String entityType, Long entityId, String summary, String details, BaseUser actedBy, String ipAddress, String userAgent) {
        try {
            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setEventType(eventType);
            auditEvent.setEntityType(entityType);
            auditEvent.setEntityId(entityId);
            auditEvent.setSummary(summary);
            auditEvent.setDetails(details);
            auditEvent.setActedBy(actedBy);
            auditEvent.setCreatedAt(LocalDateTime.now());
            auditEvent.setIpAddress(ipAddress);
            auditEvent.setUserAgent(userAgent);
            
            auditEventRepository.save(auditEvent);
            
            // Broadcast real-time admin feed notification
            try {
                String performedBy = actedBy != null ? actedBy.getFirstName() + " " + actedBy.getLastName() : "System";
                realTimeNotificationService.broadcastAdminFeed(
                    eventType, summary, details, performedBy, entityType, entityId
                );
            } catch (Exception notificationError) {
                log.warn("Failed to broadcast real-time notification for audit event: {}", notificationError.getMessage());
            }
            
            log.info("Audit event created: {} for {} {} by user {}", 
                eventType, entityType, entityId, actedBy.getEmail());
        } catch (Exception e) {
            log.error("Failed to create audit event: {} for {} {}", eventType, entityType, entityId, e);
            // Don't throw exception to avoid disrupting main business logic
        }
    }
    
    @Override
    public List<AuditEvent> getGradeAuditHistory(Long gradeId) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("Grade", gradeId);
    }
    
    @Override
    public List<AuditEvent> getEntityAuditHistory(String entityType, Long entityId) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }
    
    @Override
    public List<AuditEvent> getUserAuditHistory(Long userId) {
        return auditEventRepository.findByActedByIdOrderByCreatedAtDesc(userId);
    }
} 