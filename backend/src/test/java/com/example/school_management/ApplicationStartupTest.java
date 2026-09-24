package com.example.school_management;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Arrays;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class ApplicationStartupTest {

    @Autowired
    Flyway flyway;

    @Autowired
    RedisConnectionFactory redisConnectionFactory;

    @Autowired
    CacheManager cacheManager;

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
    void redisIsReachableAndBacksTheCache() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }
}
