package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatusConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "enrollments")
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = EnrollmentStatusConverter.class)
    @Column(name = "status", nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;
    
    private LocalDateTime enrolledAt = LocalDateTime.now();
    private Float finalGrad;

    // Enrollment is history: its Student and Class never change after creation.
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, updatable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false, updatable = false)
    private ClassEntity classEntity;

    @OneToMany(mappedBy = "enrollment")
    private Set<Grade> grades = new HashSet<>();
} 