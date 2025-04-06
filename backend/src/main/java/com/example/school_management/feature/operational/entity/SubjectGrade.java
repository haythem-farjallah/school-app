package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "subject_grades")
public class SubjectGrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to the course (from the academic module)
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Reference to the student (from the userauth module)
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // Reference to the teacher who entered the grade (from the userauth module)
    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    // The semester during which this grade was recorded
    @ManyToOne
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    private Double note1;
    private Double note2;
    private Double note3;
    private Double coefficient;
    private Double finalNote;

    // Optionally link to a Bulletin if this grade is part of a report card
    @ManyToOne
    @JoinColumn(name = "bulletin_id")
    private Bulletin bulletin;
}
