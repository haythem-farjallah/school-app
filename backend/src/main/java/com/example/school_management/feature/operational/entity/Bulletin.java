package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.Student;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "bulletins")
public class Bulletin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The student receiving this bulletin
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    // The semester for which this bulletin is generated
    @ManyToOne
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    // The list of subject grades that comprise this bulletin
    @OneToMany(mappedBy = "bulletin", cascade = CascadeType.ALL)
    private Set<SubjectGrade> subjectGrades = new HashSet<>();

    private Double overallAverage;
}