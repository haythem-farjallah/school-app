package com.example.school_management;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.containers.GenericContainer;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ApplicationStartupTest {

    @Autowired
    Flyway flyway;

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    @Qualifier("redisContainer")
    GenericContainer<?> redisContainer;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    @Autowired
    ConfigurableApplicationContext context;

    @Autowired
    SpringTemplateEngine templateEngine;

    @Test
    void flywayAppliesEveryMigrationToAnEmptyDatabase() {
        MigrationInfoService info = flyway.info();

        assertThat(info.all()).isNotEmpty();
        assertThat(info.pending()).isEmpty();
        assertThat(info.all())
                .extracting(MigrationInfo::getState)
                .containsOnly(MigrationState.SUCCESS);

        MigrationInfo latest = Arrays.stream(info.all())
                .max(Comparator.comparing(MigrationInfo::getVersion))
                .orElseThrow();
        assertThat(info.current().getVersion()).isEqualTo(latest.getVersion());
    }

    @Test
    void redisConnectionUsesTheTestcontainer() {
        assertThat(redisConnectionFactory).isInstanceOf(LettuceConnectionFactory.class);
        LettuceConnectionFactory lettuce = (LettuceConnectionFactory) redisConnectionFactory;

        assertThat(lettuce.getHostName()).isEqualTo(redisContainer.getHost());
        assertThat(lettuce.getPort()).isEqualTo(redisContainer.getMappedPort(6379));
    }

    @Test
    void redisIsReachableAndBacksTheCache() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    void noDebugEndpointsAreMapped() {
        List<String> patterns = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .toList();

        assertThat(patterns).isNotEmpty().noneMatch(pattern -> pattern.contains("/debug"));
    }

    @Test
    void testTeachingAssignmentEndpointIsNotMapped() {
        List<String> patterns = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .toList();

        assertThat(patterns)
                .contains("/api/v1/announcements/teacher-classes")
                .doesNotContain("/api/v1/announcements/create-test-assignments");
    }

    @Test
    void duplicateBeanDefinitionsFailStartup() {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();

        assertThat(beanFactory.isAllowBeanDefinitionOverriding()).isFalse();
    }

    @Test
    void emailTemplatesAreCachedAndRender() {
        assertThat(templateEngine.getTemplateResolvers())
                .isNotEmpty()
                .allSatisfy(resolver -> assertThat(resolver)
                        .isInstanceOfSatisfying(AbstractConfigurableTemplateResolver.class,
                                configurable -> assertThat(configurable.isCacheable()).isTrue()));

        Context variables = new Context();
        variables.setVariable("subject", "Your code");
        variables.setVariable("name", "Ada");
        variables.setVariable("code", "482913");

        assertThat(templateEngine.process("otp", variables)).contains("Hello, Ada!", "482913");
    }
}
