package com.example.school_management.feature.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "worker")
public class Worker extends BaseUser {
    private String jobRole;
    private String department;
}