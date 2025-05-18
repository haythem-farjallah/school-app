package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.auth.dto.PermissionUpdateDto;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/permissions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PermissionAdminController {

    private final PermissionService permissionService;

    /* ---------- full catalogue ---------- */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<Set<String>>> listAll() {
        var payload = new ApiSuccessResponse<>("success",
                permissionService.listAllCodes());
        return ResponseEntity.ok(payload);
    }

    /* ---------- role defaults ---------- */
    @GetMapping("/roles/{role}")
    public ResponseEntity<ApiSuccessResponse<Set<String>>> showRole(
            @PathVariable UserRole role) {

        var payload = new ApiSuccessResponse<>("success",
                permissionService.getRoleDefaults(role));
        return ResponseEntity.ok(payload);
    }

    @PutMapping("/roles/{role}")
    public ResponseEntity<ApiSuccessResponse<Void>> updateRole(
            @PathVariable UserRole role,
            @RequestBody PermissionUpdateDto body) {

        permissionService.replaceRoleDefaults(role, body.codes());
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    /* ---------- per-user overrides ---------- */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiSuccessResponse<Set<String>>> showUser(
            @PathVariable long id) {

        var payload = new ApiSuccessResponse<>("success",
                permissionService.getUserPerms(id));
        return ResponseEntity.ok(payload);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> updateUser(
            @PathVariable long id,
            @RequestBody PermissionUpdateDto body) {

        permissionService.replaceUserPerms(id, body.codes());
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }
}
