package com.example.school_management.dev;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DevTimetableFixtureGuardTest {
    @ParameterizedTest
    @CsvSource({"test,true", "fixtures,true", "dev,false", "dev,missing", "dev;prod,true", "dev;production,true"})
    void cannotLoadWithoutDevAndExplicitOptInOrWithProductionProfile(String profiles, String enabled) {
        var runner = new ApplicationContextRunner().withUserConfiguration(DevTimetableFixtureLoader.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profiles.split(";")));
        if (!enabled.equals("missing")) {
            runner = runner.withPropertyValues("school.dev.timetable-fixtures.enabled=" + enabled);
        }
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DevTimetableFixtureLoader.class);
        });
    }
}
