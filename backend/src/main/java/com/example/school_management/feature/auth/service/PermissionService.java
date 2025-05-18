package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Permission;
import com.example.school_management.feature.auth.entity.RolePermission;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.PermissionRepository;
import com.example.school_management.feature.auth.repository.RolePermissionRepo;
import com.example.school_management.feature.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional          // every write method runs in one TX
public class PermissionService {

    private final PermissionRepository permRepo;
    private final RolePermissionRepo rolePermRepo;
    private final UserRepository userRepo;

    /* ---------- catalogue ---------- */
    public Set<String> listAllCodes() {
        return permRepo.findAll().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    /* ---------- role defaults ---------- */
    public Set<String> getRoleDefaults(UserRole role) {
        return rolePermRepo.findAllCodesByRole(role);
    }

    public void replaceRoleDefaults(UserRole role, Set<String> codes) {
        // validate codes exist
        Set<Permission> perms = permRepo.findByCodeIn(codes);
        if (perms.size() != codes.size()) {
            throw new IllegalArgumentException("Unknown permission code(s)");
        }
        rolePermRepo.deleteByRole(role);
        perms.forEach(p -> rolePermRepo.save(new RolePermission(role, p)));
    }

    /* ---------- per-user overrides ---------- */
    public Set<String> getUserPerms(long userId) {
        BaseUser u = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("user"));
        return u.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    public void replaceUserPerms(long userId, Set<String> codes) {
        BaseUser u = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("user"));
        Set<Permission> perms = permRepo.findByCodeIn(codes);
        if (perms.size() != codes.size()) {
            throw new IllegalArgumentException("Unknown permission code(s)");
        }
        u.setPermissions(perms);
        userRepo.save(u);
    }
}
