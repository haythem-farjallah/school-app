package com.example.school_management.feature.unit.auth.services;

import com.example.school_management.commons.configs.JwtTokenProvider;
import com.example.school_management.commons.dtos.LoginRequest;
import com.example.school_management.commons.dtos.LoginResponse;
import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.mapper.AuthMapper;
import com.example.school_management.feature.auth.mapper.UserMapper;
import com.example.school_management.feature.auth.repository.*;
import com.example.school_management.feature.auth.service.AuthService;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock AuthMapper authMapper;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock StudentRepository studentRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock ParentRepository parentRepository;
    @Mock AdministrationRepository administrationRepository;
    @Mock WorkerRepository workerRepository;

    @InjectMocks AuthService authService;

    @Test
    void login_withValidCredentials_returnsTokens() {
        // given
        LoginRequest req = new LoginRequest("alice@example.com", "secret");
        UserDetails userDetails = mock(UserDetails.class);
        Authentication fakeAuth = mock(Authentication.class);
        given(fakeAuth.getPrincipal()).willReturn(userDetails);
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(fakeAuth);

        Student user = new Student();
        user.setEmail("alice@example.com");
        user.setPasswordChangeRequired(false);
        given(userDetailsService.findBaseUserByEmail("alice@example.com"))
                .willReturn(user);


        given(jwtTokenProvider.generateAccessToken(userDetails))
                .willReturn("ACCESS_TOKEN");
        given(jwtTokenProvider.generateRefreshToken(userDetails))
                .willReturn("REFRESH_TOKEN");

        // when
        LoginResponse resp = authService.login(req);

        // then
        assertThat(resp.getAccessToken()).isEqualTo("ACCESS_TOKEN");
        assertThat(resp.getRefreshToken()).isEqualTo("REFRESH_TOKEN");
    }

    @Test
    void login_whenPasswordChangeRequired_throwsLocked() {
        given(authenticationManager.authenticate(any()))
                .willReturn(mock(Authentication.class));

        Student user = new Student();
        user.setEmail("chuck@example.com");
        user.setPasswordChangeRequired(true);
        given(userDetailsService.findBaseUserByEmail("chuck@example.com"))
                .willReturn(user);

        LoginRequest req = new LoginRequest("chuck@example.com", "any");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
                    assertThat(rse.getReason()).isEqualTo("FIRST_LOGIN_PASSWORD_CHANGE_REQUIRED");
                });
    }

    @Test
    void login_withBadCredentials_throwsUnauthorized() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        LoginRequest req = new LoginRequest("a@b", "x");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Invalid credentials");
                });
    }

    @Test
    void register_student_encodesPasswordAndSaves() {
        // prepare request
        RegisterRequest req = new RegisterRequest();
        req.setRole(UserRole.STUDENT);
        req.setEmail("s@x.com");
        req.setPassword("plain");

        // stub mapper + encoder
        Student stubStudent = new Student();
        stubStudent.setEmail("s@x.com");
        stubStudent.setPassword("plain");
        given(authMapper.toStudent(any(RegisterRequest.class)))
                .willReturn(stubStudent);
        given(passwordEncoder.encode("plain")).willReturn("ENC");

        // when
        authService.register(req);

        // then
        verify(studentRepository).save(argThat(u ->
                u.getEmail().equals("s@x.com")
                        && u.getPassword().equals("ENC")
                        && u.isPasswordChangeRequired()
        ));
    }
}
