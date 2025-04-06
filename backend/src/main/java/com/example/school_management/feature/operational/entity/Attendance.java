package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.BaseUser;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user (could be a teacher or student)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BaseUser user;

    // The course for which the attendance is recorded
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    private LocalDateTime date;
    private Boolean present;
}