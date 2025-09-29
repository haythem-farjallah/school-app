package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Staff;

import java.util.Optional;

public interface StaffRepository extends BaseUserRepository<Staff> {
    Optional<Staff> findByEmailIgnoreCase(String email);
} 