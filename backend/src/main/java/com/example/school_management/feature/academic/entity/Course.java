package com.example.school_management.feature.academic.entity;

import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String name;

    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String code; // Course code like MATH101

    @Column(columnDefinition = "TEXT")
    private String description;

    @EqualsAndHashCode.Include
    private String color = "#9660EB";

    @EqualsAndHashCode.Include
    private Float credit;  // Weight for final grade computation

    @Column(nullable = false)
    private Integer credits = 3; // Credit hours

    @EqualsAndHashCode.Include
    private Integer weeklyCapacity = 3; // Default weekly hours for the course

    @Column(name = "duration_periods")
    private Integer durationPeriods = 1; // How many consecutive periods (1, 2, 3, etc.)

    @Column(name = "weekly_frequency")
    private Integer weeklyFrequency = 3; // How many times per week (1, 2, 3, etc.)

    @Column(name = "can_split")
    private Boolean canSplit = false; // Can this course be split across different days?

    @Column(name = "preferred_periods")
    private String preferredPeriods; // JSON: ["1,2", "3,4"] - preferred period combinations

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @OneToMany(mappedBy = "forCourse")
    @JsonIgnore
    private Set<TimetableSlot> timetableSlots = new HashSet<>();

    @ManyToMany(mappedBy = "courses")
    @JsonIgnore
    private Set<ClassEntity> classes = new HashSet<>();

    @ManyToMany(mappedBy = "targetCourses")
    @JsonIgnore
    private Set<LearningResource> learningResources = new HashSet<>();

    // Getters and setters for scheduling fields
    public Integer getDurationPeriods() {
        return durationPeriods;
    }

    public void setDurationPeriods(Integer durationPeriods) {
        this.durationPeriods = durationPeriods;
    }

    public Integer getWeeklyFrequency() {
        return weeklyFrequency;
    }

    public void setWeeklyFrequency(Integer weeklyFrequency) {
        this.weeklyFrequency = weeklyFrequency;
    }

    public Boolean getCanSplit() {
        return canSplit;
    }

    public void setCanSplit(Boolean canSplit) {
        this.canSplit = canSplit;
    }

    public String getPreferredPeriods() {
        return preferredPeriods;
    }

    public void setPreferredPeriods(String preferredPeriods) {
        this.preferredPeriods = preferredPeriods;
    }
}