package com.example.school_management.feature.auth.entity;

import com.example.school_management.feature.auth.entity.enums.GradeLevel;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Transfer;
import org.hibernate.annotations.ColumnTransformer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "student")
public class Student extends BaseUser {
    @Enumerated(EnumType.STRING)
    @Column(name = "grade_level", columnDefinition = "grade_level")
    @ColumnTransformer(write = "?::grade_level")
    @EqualsAndHashCode.Include
    private GradeLevel gradeLevel;
    @EqualsAndHashCode.Include
    private LocalDateTime enrolledAt;

    @OneToMany(mappedBy = "student")
    private Set<Enrollment> enrollments = new HashSet<>();

    @OneToMany(mappedBy = "student")
    private Set<Transfer> transfers = new HashSet<>();

    // Getters and Setters
    public GradeLevel getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(GradeLevel gradeLevel) { this.gradeLevel = gradeLevel; }
    
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    
    public Set<Enrollment> getEnrollments() { return enrollments; }
    public void setEnrollments(Set<Enrollment> enrollments) { this.enrollments = enrollments; }
    
    public Set<Transfer> getTransfers() { return transfers; }
    public void setTransfers(Set<Transfer> transfers) { this.transfers = transfers; }
}