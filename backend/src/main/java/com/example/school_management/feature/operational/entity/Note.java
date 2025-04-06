package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "notes")
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The course for which the note is given
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // The class in which the note is given
    @ManyToOne
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    // The teacher who provides the note
    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // The student who receives the note
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private Double note1;
    private Double note2;
}