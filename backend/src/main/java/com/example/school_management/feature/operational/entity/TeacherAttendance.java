package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.operational.dto.TeacherAttendanceRequest;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "teacher_attendance")
@EntityListeners(AuditingEntityListener.class)
public class TeacherAttendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    
    @Column(name = "teacher_first_name")
    private String teacherFirstName;
    
    @Column(name = "teacher_last_name")
    private String teacherLastName;
    
    @Column(name = "teacher_email")
    private String teacherEmail;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TeacherAttendanceRequest.TeacherAttendanceStatus status;
    
    @Column(name = "course_id")
    private Long courseId;
    
    @Column(name = "course_name")
    private String courseName;
    
    @Column(name = "class_id")
    private Long classId;
    
    @Column(name = "class_name")
    private String className;
    
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
    
    @Column(name = "excuse", columnDefinition = "TEXT")
    private String excuse;
    
    @Column(name = "substitute_teacher_id")
    private Long substituteTeacherId;
    
    @Column(name = "substitute_teacher_name")
    private String substituteTeacherName;
    
    @Column(name = "recorded_by_id", nullable = false)
    private Long recordedById;
    
    @Column(name = "recorded_by_name", nullable = false)
    private String recordedByName;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
