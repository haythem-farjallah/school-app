package com.example.school_management.feature.auth.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.Timetable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

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
    @JsonIgnore
    private Set<Grade> grades = new HashSet<>();

    @ManyToMany(mappedBy = "teachers")
    @JsonIgnore
    private Set<ClassEntity> classes = new HashSet<>();

    @ManyToMany(mappedBy = "createdBy")
    @JsonIgnore
    private Set<LearningResource> learningResources = new HashSet<>();

    @ManyToMany(mappedBy = "teachers")
    @JsonIgnore
    private Set<Timetable> timetables = new HashSet<>();

    // Getters and Setters
    public String getQualifications() { return qualifications; }
    public void setQualifications(String qualifications) { this.qualifications = qualifications; }
    
    public String getSubjectsTaught() { return subjectsTaught; }
    public void setSubjectsTaught(String subjectsTaught) { this.subjectsTaught = subjectsTaught; }
    
    public Integer getWeeklyCapacity() { return weeklyCapacity; }
    public void setWeeklyCapacity(Integer weeklyCapacity) { this.weeklyCapacity = weeklyCapacity; }
    
    public String getSchedulePreferences() { return schedulePreferences; }
    public void setSchedulePreferences(String schedulePreferences) { this.schedulePreferences = schedulePreferences; }
    
    public Set<Grade> getGrades() { return grades; }
    public void setGrades(Set<Grade> grades) { this.grades = grades; }
    
    public Set<ClassEntity> getClasses() { return classes; }
    public void setClasses(Set<ClassEntity> classes) { this.classes = classes; }
    
    public Set<LearningResource> getLearningResources() { return learningResources; }
    public void setLearningResources(Set<LearningResource> learningResources) { this.learningResources = learningResources; }
    
    public Set<Timetable> getTimetables() { return timetables; }
    public void setTimetables(Set<Timetable> timetables) { this.timetables = timetables; }
}