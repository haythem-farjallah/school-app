package com.example.school_management.commons.configs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for caching and rate limiting
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Redis connection factory
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        log.info("Connecting to Redis at {}:{} (database: {})", redisHost, redisPort, redisDatabase);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Redis template for general operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON serializer
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        om.registerModule(new JavaTimeModule());
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(om, Object.class);

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
        // Create ObjectMapper with JavaTimeModule for cache serialization
        ObjectMapper cacheObjectMapper = new ObjectMapper();
        cacheObjectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        cacheObjectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        cacheObjectMapper.registerModule(new JavaTimeModule());
        
        // Create serializer with proper ObjectMapper
        Jackson2JsonRedisSerializer<Object> cacheSerializer = new Jackson2JsonRedisSerializer<>(cacheObjectMapper, Object.class);
        
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
}
