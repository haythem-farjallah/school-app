package com.example.school_management.feature.academic.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "levels")
public class Level {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // One-to-Many relationship with ClassEntity
    @OneToMany(mappedBy = "level")
    private Set<ClassEntity> classes = new HashSet<>();

    // Many-to-Many relationship with Course (if levels define a set of subjects)
    @ManyToMany
    @JoinTable(
            name = "level_courses",
            joinColumns = @JoinColumn(name = "level_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}