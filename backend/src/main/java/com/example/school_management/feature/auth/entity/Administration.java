package com.example.school_management.feature.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "administration")
public class Administration extends BaseUser {
    private String department;
    private String jobTitle;
}