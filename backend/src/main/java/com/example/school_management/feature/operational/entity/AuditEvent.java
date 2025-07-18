package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", columnDefinition = "audit_event_type")
    @ColumnTransformer(write = "?::audit_event_type")
    private AuditEventType eventType;
    
    private String summary;
    private String details;
    private String entityType;
    private Long entityId;
    
    @ManyToOne
    @JoinColumn(name = "acted_by_id", nullable = false)
    private BaseUser actedBy;

    @CreatedDate
    private LocalDateTime createdAt;
    
    private String ipAddress;
    private String userAgent;
} 