package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enrollment_status")
    @ColumnTransformer(write = "?::enrollment_status")
    private EnrollmentStatus status = EnrollmentStatus.PENDING;
    
    private LocalDateTime enrolledAt = LocalDateTime.now();
    private Float finalGrad;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @OneToMany(mappedBy = "enrollment")
    private Set<Grade> grades = new HashSet<>();
} 