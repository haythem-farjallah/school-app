package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.operational.dto.CreateEnhancedGradeRequest;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "enhanced_grades")
@EntityListeners(AuditingEntityListener.class)
public class EnhancedGrade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    
    @Column(name = "student_first_name")
    private String studentFirstName;
    
    @Column(name = "student_last_name")
    private String studentLastName;
    
    @Column(name = "student_email")
    private String studentEmail;
    
    @Column(name = "class_id", nullable = false)
    private Long classId;
    
    @Column(name = "class_name")
    private String className;
    
    @Column(name = "course_id", nullable = false)
    private Long courseId;
    
    @Column(name = "course_name")
    private String courseName;
    
    @Column(name = "course_code")
    private String courseCode;
    
    @Column(name = "course_coefficient")
    private Double courseCoefficient;
    
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    
    @Column(name = "teacher_first_name")
    private String teacherFirstName;
    
    @Column(name = "teacher_last_name")
    private String teacherLastName;
    
    @Column(name = "teacher_email")
    private String teacherEmail;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false)
    private CreateEnhancedGradeRequest.ExamType examType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "semester", nullable = false)
    private CreateEnhancedGradeRequest.Semester semester;
    
    @Column(name = "score", nullable = false)
    private Double score;
    
    @Column(name = "max_score", nullable = false)
    private Double maxScore;
    
    @Column(name = "percentage")
    private Double percentage;
    
    @Column(name = "teacher_remarks", columnDefinition = "TEXT")
    private String teacherRemarks;
    
    @Column(name = "teacher_signature")
    private String teacherSignature;
    
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
    
    @Column(name = "is_approved")
    private Boolean isApproved = false;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    private void calculatePercentage() {
        if (score != null && maxScore != null && maxScore > 0) {
            this.percentage = (score / maxScore) * 100;
        }
    }
}
