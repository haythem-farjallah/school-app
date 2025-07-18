package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.Teacher;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "grades")
public class Grade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;
    private Float score;
    private Float weight = 1.0f;
    private LocalDateTime gradedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private Teacher assignedBy;
} 