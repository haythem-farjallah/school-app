package com.example.school_management.feature.academic.entity;

import com.example.school_management.feature.auth.entity.Teacher;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "teaching_assignments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"class_id","course_id"}))
@Data
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)        // Class 3-way link
    @JoinColumn(name = "class_id")
    private ClassEntity clazz;                // ← “class” is a keyword

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    private Integer weeklyHours = 0;          // for future timetable
}
