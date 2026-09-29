package com.example.school_management.commons.configs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.unit.DataSize;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolves application.properties and its profile files the way Spring Boot does, and checks the
 * intended differences between the production defaults and the dev profile.
 */
class ConfigurationProfilesTest {

    @Test
    void defaultsAreProductionOriented() {
        Binder config = resolve(Map.of());

        assertThat(flag(config, "spring.flyway.out-of-order", false)).isFalse();
        assertThat(flag(config, "spring.flyway.baseline-on-migrate", false)).isFalse();
        assertThat(flag(config, "spring.main.allow-bean-definition-overriding", false)).isFalse();
        assertThat(flag(config, "spring.thymeleaf.cache", true)).isTrue();
        assertThat(samplingProbability(config)).isEqualTo(0.1);
    }

    @Test
    void devTracesEveryRequestAndReloadsTemplates() {
        Binder config = resolve(Map.of(), "dev");

        assertThat(flag(config, "spring.thymeleaf.cache", true)).isFalse();
        assertThat(samplingProbability(config)).isEqualTo(1.0);
        assertThat(flag(config, "spring.flyway.out-of-order", false)).isFalse();
        assertThat(flag(config, "spring.flyway.baseline-on-migrate", false)).isFalse();
        assertThat(flag(config, "spring.main.allow-bean-definition-overriding", false)).isFalse();
    }

    @Test
    void samplingProbabilityIsOverriddenByTheEnvironment() {
        Binder config = resolve(Map.of("MANAGEMENT_TRACING_SAMPLING_PROBABILITY", "0.25"));

        assertThat(samplingProbability(config)).isEqualTo(0.25);
    }

    @Test
    void oneSettingControlsTheUploadLimit() {
        Binder defaults = resolve(Map.of());
        assertThat(uploadLimits(defaults)).containsOnly(DataSize.ofMegabytes(100));

        Binder overridden = resolve(Map.of("SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE", "20MB"));
        assertThat(uploadLimits(overridden)).containsOnly(DataSize.ofMegabytes(20));
    }

    /** Only the given environment variables and the application's own configuration files. */
    private static Binder resolve(Map<String, Object> environmentVariables, String... profiles) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        environmentVariables));
        ConfigDataEnvironmentPostProcessor.applyTo(environment, new DefaultResourceLoader(),
                new DefaultBootstrapContext(), profiles);
        return Binder.get(environment);
    }

    private static boolean flag(Binder config, String name, boolean springDefault) {
        return config.bind(name, Boolean.class).orElse(springDefault);
    }

    private static double samplingProbability(Binder config) {
        return config.bind("management.tracing.sampling.probability", Double.class).get();
    }

    private static DataSize[] uploadLimits(Binder config) {
        return new DataSize[] {
                config.bind("spring.servlet.multipart.max-file-size", DataSize.class).get(),
                config.bind("spring.servlet.multipart.max-request-size", DataSize.class).get()
        };
    }
}
