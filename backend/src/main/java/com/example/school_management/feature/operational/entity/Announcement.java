package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.operational.entity.enums.AnnouncementImportance;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String body;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isPublic = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "importance", columnDefinition = "announcement_importance")
    @ColumnTransformer(write = "?::announcement_importance")
    private AnnouncementImportance importance = AnnouncementImportance.MEDIUM;

    @CreatedDate
    private LocalDateTime createdAt;

    // TODO: Add createdBy field after migration is applied
    // @ManyToOne
    // @JoinColumn(name = "created_by_id")
    // private BaseUser createdBy;
    
    // Temporary fields until migration is applied
    private Long createdById;
    private String createdByName;

    @ManyToMany
    @JoinTable(
            name = "staff_announcements",
            joinColumns = @JoinColumn(name = "announcement_id"),
            inverseJoinColumns = @JoinColumn(name = "staff_id")
    )
    private Set<Staff> publishers = new HashSet<>();

    // Target information
    private String targetType; // CLASSES, ALL_STAFF, ALL_TEACHERS, etc.
    
    @ManyToMany
    @JoinTable(
            name = "announcement_target_classes",
            joinColumns = @JoinColumn(name = "announcement_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    private Set<ClassEntity> targetClasses = new HashSet<>();
} 