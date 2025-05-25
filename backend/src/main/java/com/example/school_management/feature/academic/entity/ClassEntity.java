package com.example.school_management.feature.academic.entity;

import com.example.school_management.feature.auth.entity.Student;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "classes")
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Many-to-Many relationship with Student (from userauth module)
    @ManyToMany
    @JoinTable(
            name = "class_students",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<Student> students = new HashSet<>();

    // Many-to-Many relationship with Course
    @ManyToMany
    @JoinTable(
            name = "class_courses",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<Course> courses = new HashSet<>();

    // One-to-One relationship with Schedule
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    // Optionally link to a Level
    @ManyToOne
    @JoinColumn(name = "level_id")
    private Level level;
}