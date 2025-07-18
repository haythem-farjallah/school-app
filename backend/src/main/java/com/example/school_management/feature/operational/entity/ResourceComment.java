package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.auth.entity.BaseUser;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "resource_comments")
public class ResourceComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne
    @JoinColumn(name = "on_resource_id", nullable = false)
    private LearningResource onResource;

    @ManyToOne
    @JoinColumn(name = "commented_by_id", nullable = false)
    private BaseUser commentedBy;

    @CreatedDate
    private LocalDateTime createdAt;
} 