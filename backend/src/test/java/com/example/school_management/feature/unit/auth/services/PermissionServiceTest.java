package com.example.school_management.feature.unit.auth.services;

import com.example.school_management.feature.auth.entity.Permission;
import com.example.school_management.feature.auth.entity.RolePermission;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.PermissionRepository;
import com.example.school_management.feature.auth.repository.RolePermissionRepo;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.service.PermissionService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    PermissionRepository permRepo;
    @Mock
    RolePermissionRepo rolePermRepo;
    @Mock
    UserRepository userRepo;
    @InjectMocks
    PermissionService service;

    @Test
    void replaceRoleDefaults_insertsAfterWipe() {
        var teacherRole = UserRole.TEACHER;
        var perm = new Permission(); perm.setCode("X");
        when(permRepo.findByCodeIn(Set.of("X"))).thenReturn(Set.of(perm));

        service.replaceRoleDefaults(teacherRole, Set.of("X"));

        verify(rolePermRepo).deleteByRole(teacherRole);
        verify(rolePermRepo).save(new RolePermission(teacherRole, perm));
    }

    @Test
    void getUserPerms_throwsWhenMissing() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> service.getUserPerms(99L));
    }
}
