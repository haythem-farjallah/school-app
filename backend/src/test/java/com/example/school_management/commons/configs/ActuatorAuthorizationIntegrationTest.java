package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An environment that exposes more Actuator endpoints does not make them public: everything but
 * health requires an ADMIN token. Metrics export, off by default in tests, is enabled so that the
 * Prometheus endpoint exists.
 */
@IntegrationTest
@AutoConfigureObservability(tracing = false)
@TestPropertySource(properties = "management.endpoints.web.exposure.include=health,info,prometheus")
class ActuatorAuthorizationIntegrationTest {

    private static final String[] ENDPOINTS = {"/actuator/info", "/actuator/prometheus"};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    CustomUserDetailsService userDetailsService;

    @Test
    void healthStaysPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void anonymousRequestsAreUnauthorized() throws Exception {
        for (String uri : ENDPOINTS) {
            mockMvc.perform(get(uri)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void nonAdminRequestsAreForbidden() throws Exception {
        String student = "Bearer " + accessToken(DevFixtureLoader.STUDENT_EMAIL);

        for (String uri : ENDPOINTS) {
            mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, student)).andExpect(status().isForbidden());
        }
    }

    @Test
    void adminRequestsAreServed() throws Exception {
        String admin = "Bearer " + accessToken(DevFixtureLoader.ADMIN_EMAIL);

        for (String uri : ENDPOINTS) {
            mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isOk());
        }
    }

    private String accessToken(String email) {
        BaseUser user = userDetailsService.findBaseUserByEmail(email);
        return jwtTokenProvider.generateAccessToken(userDetailsService.toUserDetails(user), user.getTokenVersion());
    }
}
