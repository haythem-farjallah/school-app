package com.example.school_management.commons.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


/**
 * Mock configuration for OptaPlanner beans when OptaPlanner is disabled
 */
@Configuration
public class OptaPlannerMockConfig {

    /**
     * Mock SolverManager bean to prevent dependency injection errors
     * Since OptaPlanner is disabled, we provide a null implementation
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public Object mockSolverManager() {
        return new Object(); // Simple mock object
    }
}
