package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.Staff;
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

    @ManyToMany
    @JoinTable(
            name = "staff_announcements",
            joinColumns = @JoinColumn(name = "announcement_id"),
            inverseJoinColumns = @JoinColumn(name = "staff_id")
    )
    private Set<Staff> publishers = new HashSet<>();
} 