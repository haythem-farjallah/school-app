package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.example.school_management.feature.auth.service.CustomUserDetailsService.PASSWORD_CHANGE_REQUIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtAuthenticationFilter hands every request to the rest of the chain exactly once, whatever it
 * decided about the token, and never treats an exception thrown further down the chain as its own.
 */
@IntegrationTest
class JwtAuthenticationFilterIntegrationTest {

    @Autowired
    JwtAuthenticationFilter filter;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    CustomUserDetailsService userDetailsService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken() {
        String token = accessToken(student(user -> { }));

        Downstream downstream = filter("GET", "/api/me/profile", "Bearer " + token);

        downstream.assertCalledOnce();
        assertThat(downstream.authentication.get()).isNotNull();
    }

    @Test
    void missingToken() {
        Downstream downstream = filter("GET", "/api/me/profile", null);

        downstream.assertCalledOnce();
        assertThat(downstream.authentication.get()).isNull();
    }

    @Test
    void malformedToken() {
        Downstream downstream = filter("GET", "/api/me/profile", "Bearer not-a-jwt");

        downstream.assertCalledOnce();
        assertThat(downstream.authentication.get()).isNull();
    }

    @Test
    void staleTokenVersion() {
        BaseUser user = student(u -> { });
        String token = accessToken(user);
        update(user, u -> u.setTokenVersion(u.getTokenVersion() + 1));

        assertAnonymousOnce("GET", "/api/me/profile", token);
    }

    @Test
    void suspendedAccount() {
        BaseUser user = student(u -> { });
        String token = accessToken(user);
        update(user, u -> u.setStatus(Status.SUSPENDED));

        assertAnonymousOnce("GET", "/api/me/profile", token);
    }

    @Test
    void deletedAccount() {
        BaseUser user = student(u -> { });
        String token = accessToken(user);
        update(user, u -> u.setStatus(Status.DELETED));

        assertAnonymousOnce("GET", "/api/me/profile", token);
    }

    @Test
    void passwordChangeRequiredOnOtherEndpoint() {
        String token = accessToken(student(u -> u.setPasswordChangeRequired(true)));

        Downstream downstream = assertAnonymousOnce("GET", "/api/me/profile", token);

        assertThat(downstream.request.getAttribute(PASSWORD_CHANGE_REQUIRED)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void passwordChangeRequiredOnChangePassword() {
        String token = accessToken(student(u -> u.setPasswordChangeRequired(true)));

        Downstream downstream = filter("POST", "/api/auth/change-password", "Bearer " + token);

        downstream.assertCalledOnce();
        assertThat(downstream.authentication.get()).isNotNull();
        assertThat(downstream.request.getAttribute(PASSWORD_CHANGE_REQUIRED)).isNull();
    }

    private Downstream assertAnonymousOnce(String method, String uri, String token) {
        Downstream downstream = filter(method, uri, "Bearer " + token);

        downstream.assertCalledOnce();
        assertThat(downstream.authentication.get()).isNull();
        return downstream;
    }

    /**
     * Runs the filter with a downstream chain that throws; the chain must have run only once and its own
     * exception must reach the caller unchanged.
     */
    private Downstream filter(String method, String uri, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServletPath(uri);
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        }
        Downstream downstream = new Downstream(request);

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), downstream))
                .isSameAs(downstream.failure);
        return downstream;
    }

    /** A downstream chain that records how often it runs and what it saw, then fails like a controller. */
    private static class Downstream implements FilterChain {
        final MockHttpServletRequest request;
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<Authentication> authentication = new AtomicReference<>();
        final IllegalStateException failure = new IllegalStateException("downstream failure");

        Downstream(MockHttpServletRequest request) {
            this.request = request;
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse res) {
            calls.incrementAndGet();
            authentication.set(SecurityContextHolder.getContext().getAuthentication());
            throw failure;
        }

        void assertCalledOnce() {
            assertThat(calls.get()).as("downstream chain invocations").isEqualTo(1);
        }
    }

    private BaseUser student(Consumer<Student> customize) {
        Student student = new Student();
        student.setRole(UserRole.STUDENT);
        student.setEmail("jwt-filter-" + UUID.randomUUID() + "@accounts.school.test");
        student.setFirstName("Jwt");
        student.setLastName("Filter");
        student.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        student.setStatus(Status.ACTIVE);
        student.setPasswordChangeRequired(false);
        student.setIsEmailVerified(true);
        customize.accept(student);
        return studentRepository.saveAndFlush(student);
    }

    private void update(BaseUser user, Consumer<BaseUser> change) {
        BaseUser current = userRepository.findById(user.getId()).orElseThrow();
        change.accept(current);
        userRepository.saveAndFlush(current);
    }

    private String accessToken(BaseUser user) {
        return jwtTokenProvider.generateAccessToken(userDetailsService.toUserDetails(user), user.getTokenVersion());
    }
}
