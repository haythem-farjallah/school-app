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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    StudentRepository studentRepo;
    
    @Mock
    TeacherRepository teacherRepo;
    
    @Mock
    ParentRepository parentRepo;
    
    @Mock
    AdministrationRepository adminRepo;
    
    @Mock 
    StaffRepository staffRepo;
    
    @Mock 
    RolePermissionRepo rolePermRepo;

    @InjectMocks 
    CustomUserDetailsService uds;

    @Test
    void loadUserByUsername_withValidStudent_returnsUserDetails() {
        // given
        Student student = new Student();
        student.setEmail("student@test.com");
        student.setPassword("encoded");
        student.setRole(UserRole.STUDENT);

        when(studentRepo.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));
        when(rolePermRepo.findAllCodesByRole(UserRole.STUDENT))
                .thenReturn(Set.of("GRADE_READ"));

        // when
        var userDetails = uds.loadUserByUsername("student@test.com");

        // then
        assertThat(userDetails.getUsername()).isEqualTo("student@test.com");
        assertThat(userDetails.getPassword()).isEqualTo("encoded");
        assertThat(userDetails.getAuthorities()).isNotEmpty();
    }

    @Test
    void loadUserByUsername_withInvalidEmail_throwsException() {
        // given
        when(studentRepo.findByEmail("notfound@test.com"))
                .thenReturn(Optional.empty());
        when(teacherRepo.findByEmail("notfound@test.com"))
                .thenReturn(Optional.empty());
        when(parentRepo.findByEmail("notfound@test.com"))
                .thenReturn(Optional.empty());
        when(adminRepo.findByEmail("notfound@test.com"))
                .thenReturn(Optional.empty());
        when(staffRepo.findByEmail("notfound@test.com"))
                .thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> uds.loadUserByUsername("notfound@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void authorities_mergeRoleDefaultsAndCustomPermissions() {
        // given
        Staff staff = new Staff();
        staff.setRole(UserRole.STAFF);
        staff.setEmail("worker@test.com");
        staff.setPassword("encoded");
        
        Permission customPermission = new Permission();
        customPermission.setCode("STUDENT_CREATE");
        staff.setPermissions(Set.of(customPermission));

        when(rolePermRepo.findAllCodesByRole(UserRole.STAFF))
                .thenReturn(Set.of("STUDENT_READ"));
        when(staffRepo.findByEmail("worker@test.com"))
                .thenReturn(Optional.of(staff));

        // when
        var authorities = uds.loadUserByUsername("worker@test.com")
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // then
        assertThat(authorities).contains("STUDENT_READ", "STUDENT_CREATE");
    }

    @Test
    void findBaseUserByEmail_withValidEmail_returnsUser() {
        // given
        Teacher teacher = new Teacher();
        teacher.setEmail("teacher@test.com");
        
        when(teacherRepo.findByEmail("teacher@test.com"))
                .thenReturn(Optional.of(teacher));

        // when
        BaseUser user = uds.findBaseUserByEmail("teacher@test.com");

        // then
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("teacher@test.com");
    }
}