package com.example.school_management.feature.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", columnDefinition = "user_role")
    @ColumnTransformer(write = "?::user_role")
    private UserRole role;
    @Id @ManyToOne
    private Permission permission;
}
