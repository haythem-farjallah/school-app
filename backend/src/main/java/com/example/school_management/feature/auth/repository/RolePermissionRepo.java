package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.RolePermission;
import com.example.school_management.feature.auth.entity.RolePermissionId;
import com.example.school_management.feature.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface RolePermissionRepo
        extends JpaRepository<RolePermission, RolePermissionId> {

    @Query("select rp.permission.code from RolePermission rp where rp.role = :role")
    Set<String> findAllCodesByRole(@Param("role") UserRole role);

    void deleteByRole(UserRole role);
}