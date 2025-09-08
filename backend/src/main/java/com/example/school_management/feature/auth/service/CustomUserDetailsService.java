package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.*;
import com.example.school_management.feature.auth.repository.RolePermissionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService
 {

    private final StudentRepository       studentRepo;
    private final TeacherRepository       teacherRepo;
    private final ParentRepository        parentRepo;
    private final AdministrationRepository adminRepo;
    private final StaffRepository         staffRepo;

    private final RolePermissionRepo      rolePermRepo;   // <-- NEW

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        BaseUser user = findBaseUserByEmail(email);

        /* 1. ROLE_… authority */
        Collection<GrantedAuthority> auth = new ArrayList<>();
        auth.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        /* 2. Default permissions for that role */
        rolePermRepo.findAllCodesByRole(user.getRole())
                .forEach(code -> auth.add(new SimpleGrantedAuthority(code)));

        /* 3. Per-user overrides */
        user.getPermissions()
                .forEach(p -> auth.add(new SimpleGrantedAuthority(p.getCode())));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                auth
        );
    }

    /* ---------- helper ---------- */
    @Cacheable(value = "users", key = "#email")
    public BaseUser findBaseUserByEmail(String email) {

        return Stream.of(
                        studentRepo.findByEmail(email),
                        teacherRepo.findByEmail(email),
                        parentRepo.findByEmail(email),
                        adminRepo.findByEmail(email),
                        staffRepo.findByEmail(email)
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + email));
    }
}