package com.example.school_management.feature.academic.entity;

import com.example.school_management.feature.auth.entity.Teacher;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String color = "#9660EB";

    private Double coefficient;  // Weight for final grade computation

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;
}