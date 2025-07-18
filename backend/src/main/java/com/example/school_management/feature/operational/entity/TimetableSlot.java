package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "timetable_slots")
@EntityListeners(AuditingEntityListener.class)
public class TimetableSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", columnDefinition = "day_of_week")
    @ColumnTransformer(write = "?::day_of_week")
    @EqualsAndHashCode.Include
    private DayOfWeek dayOfWeek;
    
    @EqualsAndHashCode.Include
    private String description;

    @ManyToOne
    @JoinColumn(name = "period_id", nullable = false)
    private Period period;

    @ManyToOne
    @JoinColumn(name = "for_class_id")
    private ClassEntity forClass;

    @ManyToOne
    @JoinColumn(name = "for_course_id")
    private Course forCourse;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "timetable_id")
    private Timetable timetable;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper methods for OptaPlanner
    public boolean isAssigned() {
        return forClass != null && forCourse != null && teacher != null && room != null;
    }

    public boolean conflictsWith(TimetableSlot other) {
        if (this.equals(other)) return false;
        
        // Same day and period
        if (this.dayOfWeek == other.dayOfWeek && this.period.equals(other.period)) {
            // Same teacher
            if (this.teacher != null && other.teacher != null && 
                this.teacher.equals(other.teacher)) {
                return true;
            }
            // Same class
            if (this.forClass != null && other.forClass != null && 
                this.forClass.equals(other.forClass)) {
                return true;
            }
            // Same room
            if (this.room != null && other.room != null && 
                this.room.equals(other.room)) {
                return true;
            }
        }
        return false;
    }

    public String getSlotInfo() {
        return String.format("%s - %s - %s - %s - %s", 
            dayOfWeek, 
            period != null ? period.getIndex() : "N/A",
            forClass != null ? forClass.getName() : "N/A",
            forCourse != null ? forCourse.getName() : "N/A",
            teacher != null ? teacher.getFirstName() + " " + teacher.getLastName() : "N/A"
        );
    }
} 