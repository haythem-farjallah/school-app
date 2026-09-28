package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Prometheus endpoint with the default exposure: ADMIN only, standard Micrometer metrics, and
 * low-cardinality labels. Metrics export, off by default in tests, is enabled so that it exists.
 */
@IntegrationTest
@AutoConfigureObservability(tracing = false)
class PrometheusMetricsIntegrationTest {

    private static final String PROMETHEUS = "/actuator/prometheus";
    private static final long MISSING_TIMETABLE_ID = 987_654_321;
    private static final String REQUEST_ID = "metrics-scrape-request-id";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    CustomUserDetailsService userDetailsService;

    @Test
    void anonymousScrapeIsUnauthorized() throws Exception {
        mockMvc.perform(get(PROMETHEUS)).andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminScrapeIsForbidden() throws Exception {
        for (String email : new String[] {
                DevFixtureLoader.STUDENT_EMAIL, DevFixtureLoader.TEACHER_EMAIL, DevFixtureLoader.PARENT_EMAIL}) {
            mockMvc.perform(get(PROMETHEUS).header(HttpHeaders.AUTHORIZATION, bearer(email)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void adminScrapeUsesThePrometheusTextFormat() throws Exception {
        MockHttpServletResponse response = scrape();

        MediaType contentType = MediaType.parseMediaType(response.getContentType());
        assertThat(contentType.isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
        assertThat(contentType.getParameter("version")).isEqualTo("0.0.4");
        assertThat(response.getContentAsString()).contains("# HELP ", "# TYPE ");
    }

    @Test
    void jvmAndProcessMetricsArePresent() throws Exception {
        String body = scrape().getContentAsString();

        assertThat(body).contains(
                "# TYPE jvm_memory_used_bytes gauge",
                "# TYPE jvm_threads_live_threads gauge",
                "# TYPE jvm_gc_memory_allocated_bytes_total counter",
                "# TYPE process_uptime_seconds gauge",
                "# TYPE system_cpu_count gauge");
    }

    @Test
    void httpRequestsAreRecordedByRouteTemplate() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);
        mockMvc.perform(get("/api/v1/timetables/" + MISSING_TIMETABLE_ID)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .header("X-Request-ID", REQUEST_ID))
                .andExpect(status().isNotFound());

        String body = scrape().getContentAsString();
        List<String> httpLines = body.lines().filter(line -> line.startsWith("http_server_requests_seconds")).toList();

        assertThat(body).contains("# TYPE http_server_requests_seconds histogram");
        assertThat(httpLines).anyMatch(line -> line.startsWith("http_server_requests_seconds_count{")
                && line.contains("method=\"GET\"")
                && line.contains("status=\"404\"")
                && line.contains("outcome=\"CLIENT_ERROR\"")
                && line.contains("uri=\"/api/v1/timetables/{id}\""));
        // The latency histogram stays enabled (percentiles-histogram.http.server.requests).
        assertThat(httpLines).anyMatch(line -> line.startsWith("http_server_requests_seconds_bucket{"));
        assertThat(httpLines).noneMatch(line -> line.contains(String.valueOf(MISSING_TIMETABLE_ID)));
    }

    @Test
    void connectionPoolMetricsArePresent() throws Exception {
        String body = scrape().getContentAsString();

        assertThat(body).contains(
                "hikaricp_connections_active{pool=\"db-pool\"}",
                "hikaricp_connections_idle{pool=\"db-pool\"}",
                "hikaricp_connections_max{pool=\"db-pool\"}",
                "hikaricp_connections_pending{pool=\"db-pool\"}");
    }

    @Test
    void redisCommandLatencyIsRecorded() throws Exception {
        // The rate limiter runs a Redis command before the scrape itself is served.
        assertThat(scrape().getContentAsString()).contains("# TYPE lettuce_command_completion_seconds summary");
    }

    @Test
    void scrapeCarriesNoCallerIdentityOrRequestIds() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);
        mockMvc.perform(get("/api/v1/timetables/" + MISSING_TIMETABLE_ID)
                .header(HttpHeaders.AUTHORIZATION, student)
                .header("X-Request-ID", REQUEST_ID));

        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);
        String body = scrape(admin).getContentAsString();

        assertThat(body).doesNotContain(
                REQUEST_ID,
                DevFixtureLoader.ADMIN_EMAIL,
                DevFixtureLoader.STUDENT_EMAIL,
                student.substring("Bearer ".length()),
                admin.substring("Bearer ".length()),
                "Bearer");
    }

    private MockHttpServletResponse scrape() throws Exception {
        return scrape(bearer(DevFixtureLoader.ADMIN_EMAIL));
    }

    private MockHttpServletResponse scrape(String admin) throws Exception {
        return mockMvc.perform(get(PROMETHEUS).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    private String bearer(String email) {
        BaseUser user = userDetailsService.findBaseUserByEmail(email);
        return "Bearer " + jwtTokenProvider.generateAccessToken(userDetailsService.toUserDetails(user), user.getTokenVersion());
    }
}
