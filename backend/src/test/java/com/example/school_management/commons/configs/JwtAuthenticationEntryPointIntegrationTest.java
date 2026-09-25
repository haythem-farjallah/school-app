package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 401 responses written by the security filter chain for protected endpoints.
 */
@IntegrationTest
class JwtAuthenticationEntryPointIntegrationTest {

    private static final String PROTECTED_URI = "/api/me/profile";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void requestWithoutTokenGetsProblemDetail() throws Exception {
        expectUnauthorizedProblem(mockMvc.perform(get(PROTECTED_URI)));
    }

    @Test
    void malformedTokenGetsProblemDetail() throws Exception {
        String body = expectUnauthorizedProblem(mockMvc.perform(get(PROTECTED_URI)
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")));

        assertThat(body).doesNotContain("not-a-jwt", "Bearer");
    }

    @Test
    void tokenSignedWithAnotherKeyGetsProblemDetail() throws Exception {
        String forged = Jwts.builder()
                .setSubject(DevFixtureLoader.ADMIN_EMAIL)
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor("some-other-signing-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        String body = expectUnauthorizedProblem(mockMvc.perform(get(PROTECTED_URI)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged)));

        assertThat(body).doesNotContain(forged, DevFixtureLoader.ADMIN_EMAIL, "signature", "JWT");
    }

    /** Asserts the 401 problem-detail contract and returns the raw body. */
    private String expectUnauthorizedProblem(ResultActions result) throws Exception {
        String body = result
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.instance").value(PROTECTED_URI))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.statusCode").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(body).size()).isEqualTo(5);
        return body;
    }
}
