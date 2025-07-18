package com.example.school_management.feature.unit.auth.services;

import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.repository.*;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    /* ---- all ctor deps ---- */
    @Mock
    StudentRepository studentRepo;
    @Mock
    TeacherRepository teacherRepo;
    @Mock
    ParentRepository parentRepo;
    @Mock
    AdministrationRepository adminRepo;
    @Mock StaffRepository         staffRepo;
    @Mock RolePermissionRepo       rolePermRepo;

    @InjectMocks CustomUserDetailsService uds;

    @Test
    void authorities_mergeRoleDefaultsAndOverrides() {
        // given
        Staff u = new Staff();
        u.setRole(UserRole.STAFF);
        u.setEmail("worker@test");
        u.setPassword("dummy");
        Permission extra = new Permission();
        extra.setCode("STUDENT_CREATE");
        u.setPermissions(Set.of(extra));

        when(rolePermRepo.findAllCodesByRole(UserRole.STAFF))
                .thenReturn(Set.of("STUDENT_READ"));

        // this repo will return the user; the others stay Optional.empty()
        when(staffRepo.findByEmail("worker@test"))
                .thenReturn(Optional.of(u));

        // when
        var auths = uds.loadUserByUsername("worker@test")
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // then
        assertThat(auths).contains("STUDENT_READ", "STUDENT_CREATE");
    }
}
