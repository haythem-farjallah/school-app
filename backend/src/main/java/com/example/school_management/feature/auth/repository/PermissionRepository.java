package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> { }
