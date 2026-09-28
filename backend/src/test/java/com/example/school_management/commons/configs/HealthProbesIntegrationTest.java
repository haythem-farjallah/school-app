package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Health, liveness and readiness through the real filter chain. Liveness depends on the process
 * only; readiness also on PostgreSQL ("db") and Redis ("redis"). Outages are simulated by swapping
 * a contributor for one reporting DOWN, never by stopping the shared containers.
 */
@IntegrationTest
class HealthProbesIntegrationTest {

    private static final String HEALTH = "/actuator/health";
    private static final String LIVENESS = "/actuator/health/liveness";
    private static final String READINESS = "/actuator/health/readiness";

    private static final String UP = "{\"status\":\"UP\"}";
    private static final String DOWN = "{\"status\":\"DOWN\"}";
    private static final String HEALTH_UP = "{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}";
    private static final String HEALTH_DOWN = "{\"status\":\"DOWN\",\"groups\":[\"liveness\",\"readiness\"]}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    HealthEndpointGroups groups;

    @Autowired
    HealthContributorRegistry contributors;

    @Autowired
    ApplicationContext context;

    @Test
    void healthEndpointsArePublicAndReportOnlyAStatus() throws Exception {
        // The root response also names the probe groups (Spring Boot always lists them there).
        mockMvc.perform(get(HEALTH))
                .andExpect(status().isOk())
                .andExpect(content().json(HEALTH_UP, true));
        for (String uri : new String[] {LIVENESS, READINESS}) {
            mockMvc.perform(get(uri))
                    .andExpect(status().isOk())
                    .andExpect(content().json(UP, true));
        }
    }

    @Test
    void componentsCannotBeQueriedIndividually() throws Exception {
        for (String component : new String[] {"db", "redis", "diskSpace", "livenessState"}) {
            mockMvc.perform(get(HEALTH + "/" + component)).andExpect(status().isNotFound());
        }
    }

    @Test
    void livenessDependsOnTheProcessOnly() {
        HealthEndpointGroup liveness = groups.get("liveness");

        assertThat(liveness.isMember("livenessState")).isTrue();
        for (String dependency : new String[] {"db", "redis", "mail", "diskSpace", "readinessState"}) {
            assertThat(liveness.isMember(dependency)).as(dependency).isFalse();
        }
    }

    @Test
    void readinessDependsOnPostgresqlAndRedisButNotMail() {
        HealthEndpointGroup readiness = groups.get("readiness");

        // The names the groups refer to are the contributors this application actually registers.
        for (String member : new String[] {"readinessState", "db", "redis"}) {
            assertThat(contributors.getContributor(member)).as(member).isNotNull();
            assertThat(readiness.isMember(member)).as(member).isTrue();
        }
        assertThat(contributors.getContributor("mail")).isNull();
        assertThat(readiness.isMember("mail")).isFalse();
        assertThat(readiness.isMember("livenessState")).isFalse();
    }

    @Test
    void databaseOutageMakesTheInstanceUnreadyButNotDead() throws Exception {
        whileDown("db", this::expectUnreadyButAlive);
    }

    @Test
    void redisOutageMakesTheInstanceUnreadyButNotDead() throws Exception {
        whileDown("redis", this::expectUnreadyButAlive);
    }

    @Test
    void brokenLivenessStateFailsLiveness() throws Exception {
        AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
        try {
            mockMvc.perform(get(LIVENESS))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().json(DOWN, true));
        } finally {
            AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
        }
        mockMvc.perform(get(LIVENESS)).andExpect(status().isOk());
    }

    @Test
    void livenessIsNotRateLimitedButReadinessIs() throws Exception {
        mockMvc.perform(get(LIVENESS))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Rate-Limit-Remaining"));
        mockMvc.perform(get(READINESS))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Rate-Limit-Remaining"));
    }

    private void expectUnreadyButAlive() throws Exception {
        mockMvc.perform(get(READINESS))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json(DOWN, true));
        mockMvc.perform(get(HEALTH))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json(HEALTH_DOWN, true));
        mockMvc.perform(get(LIVENESS))
                .andExpect(status().isOk())
                .andExpect(content().json(UP, true));
    }

    private void whileDown(String name, ThrowingRunnable assertions) throws Exception {
        HealthContributor original = contributors.unregisterContributor(name);
        assertThat(original).as(name).isNotNull();
        contributors.registerContributor(name, (HealthIndicator) () -> Health.down().build());
        try {
            assertions.run();
        } finally {
            contributors.unregisterContributor(name);
            contributors.registerContributor(name, original);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
