package com.example.school_management.feature.academic.entity;

import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String name;

    @EqualsAndHashCode.Include
    private String color = "#9660EB";

    @EqualsAndHashCode.Include
    private Float credit;  // Weight for final grade computation

    @EqualsAndHashCode.Include
    private Integer weeklyCapacity = 3; // Default weekly hours for the course

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @OneToMany(mappedBy = "forCourse")
    private Set<TimetableSlot> timetableSlots = new HashSet<>();

    @ManyToMany(mappedBy = "courses")
    private Set<ClassEntity> classes = new HashSet<>();

    @ManyToMany(mappedBy = "targetCourses")
    private Set<LearningResource> learningResources = new HashSet<>();
}