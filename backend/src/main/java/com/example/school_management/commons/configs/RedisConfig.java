package com.example.school_management.commons.configs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Redis serialization and cache configuration. The connection factory is
 * Spring Boot's auto-configured one (spring.data.redis.* or a service connection).
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /** Package whose classes may be named by a stored type id. */
    private static final String APPLICATION_PACKAGE = "com.example.school_management.";

    /** JDK collections the application values stored so far are built from. */
    private static final Set<Class<?>> ALLOWED_JDK_TYPES = Set.of(ArrayList.class, HashSet.class);

    /**
     * Stored values name their own classes, so reading them back is restricted to an allowlist:
     * a type id outside it fails deserialization instead of instantiating an arbitrary class.
     */
    private static final PolymorphicTypeValidator TYPE_VALIDATOR = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType(APPLICATION_PACKAGE)
            .allowIfSubType(new BasicPolymorphicTypeValidator.TypeMatcher() {
                @Override
                public boolean match(MapperConfig<?> config, Class<?> clazz) {
                    return ALLOWED_JDK_TYPES.contains(clazz);
                }
            })
            .build();

    /**
     * Redis template for general operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON serializer
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(redisObjectMapper(), Object.class);

        // String serializer
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Key serializer
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Value serializer
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache manager with TTL configurations
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<Object> cacheSerializer = new Jackson2JsonRedisSerializer<>(redisObjectMapper(), Object.class);
        
        // Default cache configuration
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL: 30 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(cacheSerializer))
                .disableCachingNullValues();

        // Specific cache configurations
        RedisCacheConfiguration authCacheConfig = defaultCacheConfig.entryTtl(Duration.ofMinutes(15)); // Auth cache: 15 minutes
        RedisCacheConfiguration userCacheConfig = defaultCacheConfig.entryTtl(Duration.ofMinutes(60)); // User data: 1 hour
        RedisCacheConfiguration listingCacheConfig = defaultCacheConfig.entryTtl(Duration.ofMinutes(10)); // Listings: 10 minutes
        RedisCacheConfiguration timetableCacheConfig = defaultCacheConfig.entryTtl(Duration.ofMinutes(5)); // Timetables: 5 minutes

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withCacheConfiguration("auth", authCacheConfig)
                .withCacheConfiguration("users", userCacheConfig)
                .withCacheConfiguration("students", userCacheConfig)
                .withCacheConfiguration("teachers", userCacheConfig)
                .withCacheConfiguration("staff", userCacheConfig)
                .withCacheConfiguration("admins", userCacheConfig)
                .withCacheConfiguration("classes", listingCacheConfig)
                .withCacheConfiguration("courses", listingCacheConfig)
                .withCacheConfiguration("timetables", timetableCacheConfig)
                .withCacheConfiguration("announcements", listingCacheConfig)
                .withCacheConfiguration("resources", listingCacheConfig)
                .withCacheConfiguration("grades", listingCacheConfig)
                .build();
    }

    /** Shared by RedisTemplate and the cache so both apply the same type allowlist. */
    private static ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(TYPE_VALIDATOR, ObjectMapper.DefaultTyping.NON_FINAL);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
