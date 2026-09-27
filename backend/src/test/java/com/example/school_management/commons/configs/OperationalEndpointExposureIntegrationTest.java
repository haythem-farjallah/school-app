package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With the default configuration only a detail-free health endpoint is public; other Actuator
 * endpoints and the API documentation are not served.
 */
@IntegrationTest
class OperationalEndpointExposureIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    CustomUserDetailsService userDetailsService;

    @Test
    void anonymousHealthHasNoDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void otherActuatorEndpointsAreNotPublic() throws Exception {
        for (String uri : new String[] {"/actuator", "/actuator/info", "/actuator/prometheus", "/actuator/env"}) {
            mockMvc.perform(get(uri)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void otherActuatorEndpointsAreNotExposedEvenToAdmins() throws Exception {
        String admin = "Bearer " + accessToken(DevFixtureLoader.ADMIN_EMAIL);

        for (String uri : new String[] {"/actuator/info", "/actuator/env"}) {
            mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isNotFound());
        }
    }

    @Test
    void apiDocumentationIsNotServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }

    private String accessToken(String email) {
        BaseUser user = userDetailsService.findBaseUserByEmail(email);
        return jwtTokenProvider.generateAccessToken(userDetailsService.toUserDetails(user), user.getTokenVersion());
    }
}
