package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.entity.AuditEvent;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.auth.entity.BaseUser;

import java.util.List;

public interface AuditService {
    
    /**
     * Create an audit event for grade operations
     */
    void createGradeAuditEvent(AuditEventType eventType, Long gradeId, String summary, String details, BaseUser actedBy);
    
    /**
     * Create an audit event for grade operations with HTTP context
     */
    void createGradeAuditEvent(AuditEventType eventType, Long gradeId, String summary, String details, BaseUser actedBy, String ipAddress, String userAgent);
    
    /**
     * Create a general audit event
     */
    void createAuditEvent(AuditEventType eventType, String entityType, Long entityId, String summary, String details, BaseUser actedBy);
    
    /**
     * Create a general audit event with HTTP context
     */
    void createAuditEvent(AuditEventType eventType, String entityType, Long entityId, String summary, String details, BaseUser actedBy, String ipAddress, String userAgent);
    
    /**
     * Get audit history for a specific grade
     */
    List<AuditEvent> getGradeAuditHistory(Long gradeId);
    
    /**
     * Get audit history for a specific entity
     */
    List<AuditEvent> getEntityAuditHistory(String entityType, Long entityId);
    
    /**
     * Get all audit events for a user
     */
    List<AuditEvent> getUserAuditHistory(Long userId);
} 