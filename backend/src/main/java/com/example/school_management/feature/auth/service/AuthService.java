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
    private final WorkerRepository workerRepository;

    /**
     * Authenticate a user and return JWT tokens plus user profile data.
     */
    public LoginResponse login(LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

            BaseUser user = userDetailsService.findBaseUserByEmail(req.getEmail());
            if (user.isPasswordChangeRequired()) {
                throw new ResponseStatusException(HttpStatus.LOCKED,
                        "FIRST_LOGIN_PASSWORD_CHANGE_REQUIRED");
            }

            // token creation
            String access  = jwtTokenProvider.generateAccessToken((UserDetails) auth.getPrincipal());
            String refresh = jwtTokenProvider.generateRefreshToken((UserDetails) auth.getPrincipal());

            return new LoginResponse(access, refresh, userMapper.toDto(user));
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }
    /**
     * Register a new user based on the provided role.
     */
    @Transactional
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
            case WORKER -> {
                Worker user = authMapper.toWorker(request);
                finalizeAndSave(user, workerRepository);
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
