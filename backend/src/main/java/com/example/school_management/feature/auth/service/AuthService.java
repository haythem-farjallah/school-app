package com.example.school_management.feature.auth.service;

import com.example.school_management.commons.configs.JwtTokenProvider;
import com.example.school_management.commons.dtos.LoginRequest;
import com.example.school_management.commons.dtos.LoginResponse;
import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.mapper.AuthMapper;
import com.example.school_management.feature.auth.mapper.UserMapper;
import com.example.school_management.feature.auth.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AuthMapper authMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final AdministrationRepository administrationRepository;
    private final StaffRepository staffRepository;
    private final PermissionService permissionService;

    /**
     * Authenticate a user and return JWT tokens plus user profile data.
     */
    @CacheEvict(value = "auth", key = "#req.email")
    public LoginResponse login(LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

                    BaseUser user = userDetailsService.findBaseUserByEmail(req.getEmail());
        
        // token creation
        String access  = jwtTokenProvider.generateAccessToken((UserDetails) auth.getPrincipal());
        String refresh = jwtTokenProvider.generateRefreshToken((UserDetails) auth.getPrincipal());

        var userDto = userMapper.toDto(user);
        // Compute effective permissions: role defaults ∪ per-user overrides
        var defaults = permissionService.getRoleDefaults(user.getRole());
        var overrides = user.getPermissions().stream().map(p -> p.getCode()).collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> effective = new java.util.HashSet<>(defaults);
        effective.addAll(overrides);
        userDto.setPermissions(effective);

        // Return login response with password change requirement flag
        return new LoginResponse(access, refresh, userDto, user.isPasswordChangeRequired());
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }
    /**
     * Register a new user based on the provided role.
     */
    @Transactional
    @CacheEvict(value = {"users", "students", "teachers", "staff", "admins"}, allEntries = true)
    public void register(RegisterRequest request) {
        switch (request.getRole()) {
            case STUDENT -> {
                Student user = authMapper.toStudent(request);
                finalizeAndSave(user, studentRepository);
            }
            case TEACHER -> {
                Teacher user = authMapper.toTeacher(request);
                finalizeAndSave(user, teacherRepository);
            }
            case PARENT -> {
                Parent user = authMapper.toParent(request);
                finalizeAndSave(user, parentRepository);
            }
            case ADMIN -> {
                Administration user = authMapper.toAdmin(request);
                finalizeAndSave(user, administrationRepository);
            }
            case STAFF -> {
                Staff user = authMapper.toStaff(request);
                finalizeAndSave(user, staffRepository);
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported role: " + request.getRole()
            );
        }
    }

    private <U extends BaseUser> void finalizeAndSave(
            U user, JpaRepository<U, Long> repo
    ) {
        if (user.getStatus() == null) {
            user.setStatus(Status.ACTIVE);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setPasswordChangeRequired(true);
        repo.save(user);
        log.info("Registered {} with forced password change: {}", user.getRole(), user.getEmail());
    }


}
