package com.example.school_management.feature.auth.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.Timetable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "teacher")
public class Teacher extends BaseUser {
    @EqualsAndHashCode.Include
    private String qualifications;
    @EqualsAndHashCode.Include
    private String subjectsTaught; // Could be a comma-separated list or managed as a separate relation
    @EqualsAndHashCode.Include
    private Integer weeklyCapacity;
    @EqualsAndHashCode.Include
    private String schedulePreferences;

    @OneToMany(mappedBy = "assignedBy")
    private Set<Grade> grades = new HashSet<>();

    @ManyToMany(mappedBy = "teachers")
    private Set<ClassEntity> classes = new HashSet<>();

    @ManyToMany(mappedBy = "createdBy")
    private Set<LearningResource> learningResources = new HashSet<>();

    @ManyToMany(mappedBy = "teachers")
    private Set<Timetable> timetables = new HashSet<>();
}