package com.example.school_management.feature.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {
    @Id
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Id @ManyToOne
    private Permission permission;
}
