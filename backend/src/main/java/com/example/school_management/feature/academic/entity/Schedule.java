package com.example.school_management.feature.academic.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "schedules")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String description;
    
    @EqualsAndHashCode.Include
    private LocalDateTime startDate;
    
    @EqualsAndHashCode.Include
    private LocalDateTime endDate;
    
    @EqualsAndHashCode.Include
    private Boolean isActive = true;

    // Many-to-Many relationship with Course
    @ManyToMany
    @JoinTable(
            name = "schedule_courses",
            joinColumns = @JoinColumn(name = "schedule_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}