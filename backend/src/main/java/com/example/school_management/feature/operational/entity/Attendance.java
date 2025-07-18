package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "attendance")
@EntityListeners(AuditingEntityListener.class)
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
    @JoinColumn(name = "course_id")
    private Course course;

    // The class for which the attendance is recorded
    @ManyToOne
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    // The timetable slot for which the attendance is recorded
    @ManyToOne
    @JoinColumn(name = "timetable_slot_id")
    private TimetableSlot timetableSlot;

    // Date of attendance
    @Column(name = "date", nullable = false)
    private LocalDate date;

    // Attendance status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "attendance_status")
    @ColumnTransformer(write = "?::attendance_status")
    private AttendanceStatus status;

    // User type (STUDENT, TEACHER, etc.)
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", columnDefinition = "user_type")
    @ColumnTransformer(write = "?::user_type")
    private UserType userType;

    // Additional remarks
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // Excuse for absence
    @Column(name = "excuse", columnDefinition = "TEXT")
    private String excuse;

    // Medical note
    @Column(name = "medical_note", columnDefinition = "TEXT")
    private String medicalNote;

    // When the attendance was recorded
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    // Who recorded the attendance
    @ManyToOne
    @JoinColumn(name = "recorded_by_id")
    private BaseUser recordedBy;

    // Legacy field for backward compatibility
    @Column(name = "present")
    private Boolean present;

    // Audit fields
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper methods for status checking
    public boolean isPresent() {
        return status == AttendanceStatus.PRESENT;
    }

    public boolean isAbsent() {
        return status == AttendanceStatus.ABSENT;
    }

    public boolean isLate() {
        return status == AttendanceStatus.LATE;
    }

    public boolean isExcused() {
        return status == AttendanceStatus.EXCUSED;
    }
}