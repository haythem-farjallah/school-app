package com.example.school_management.feature.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "teacher")
public class Teacher extends BaseUser {
    private String qualifications;
    private String subjectsTaught; // Could be a comma-separated list or managed as a separate relation
    private Integer availableHours;
    private String schedulePreferences;
}