package com.example.school_management.feature.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "student")
public class Student extends BaseUser {
    private String gradeLevel;
    private Integer enrollmentYear;
}