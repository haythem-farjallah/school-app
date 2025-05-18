package com.example.school_management.feature.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor                       // JPA needs a no-args ctor
@AllArgsConstructor
@EqualsAndHashCode
public class RolePermissionId implements Serializable {

    private UserRole role;
    private Long permission;
}