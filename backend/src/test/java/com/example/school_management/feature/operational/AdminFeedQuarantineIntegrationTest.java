package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.operational.repository.AdminFeedRepository;
import com.example.school_management.feature.operational.service.AdminFeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The admin feed has no School ownership: every route is unavailable and touches no feed data. */
@IntegrationTest
class AdminFeedQuarantineIntegrationTest {

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoSpyBean AdminFeedService service;
    @MockitoSpyBean AdminFeedRepository repository;

    static Stream<MockHttpServletRequestBuilder> routes() {
        return Stream.of(
                get("/api/v1/admin-feeds"),
                get("/api/v1/admin-feeds/1"),
                delete("/api/v1/admin-feeds/1"),
                patch("/api/v1/admin-feeds/1/read"),
                patch("/api/v1/admin-feeds/read-all"),
                get("/api/v1/admin-feeds/event-type/USER_CREATED"),
                get("/api/v1/admin-feeds/notification-type/INFO"),
                get("/api/v1/admin-feeds/severity/ERROR"),
                get("/api/v1/admin-feeds/entity-type/Class"),
                get("/api/v1/admin-feeds/triggered-by/1"),
                get("/api/v1/admin-feeds/target-user/1"),
                get("/api/v1/admin-feeds/unread"),
                get("/api/v1/admin-feeds/high-priority"),
                get("/api/v1/admin-feeds/date-range"),
                get("/api/v1/admin-feeds/recent"),
                get("/api/v1/admin-feeds/stats"));
    }

    @ParameterizedTest
    @MethodSource("routes")
    void everyRouteIsUnavailableForAdministratorsWithoutTouchingFeedData(MockHttpServletRequestBuilder route) throws Exception {
        route.header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL));
        mockMvc.perform(route)
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("ADMIN_FEED_NOT_TENANT_SCOPED"))
                .andExpect(jsonPath("$.instance").value(org.hamcrest.Matchers.startsWith("/api/v1/admin-feeds")));

        verifyNoInteractions(service, repository);
    }

    @Test
    void instanceMatchesTheRequestPath() throws Exception {
        mockMvc.perform(get("/api/v1/admin-feeds/unread").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.instance").value("/api/v1/admin-feeds/unread"));
    }

    @Test
    void nonAdministratorsStayForbiddenAndAnonymousStaysUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin-feeds").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin-feeds"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service, repository);
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.31." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();
        return "Bearer " + token;
    }
}
