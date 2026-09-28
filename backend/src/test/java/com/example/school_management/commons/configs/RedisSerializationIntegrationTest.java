package com.example.school_management.commons.configs;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.auth.dto.StudentDto;
import com.example.school_management.feature.auth.dto.UserDto;
import com.example.school_management.feature.auth.entity.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Application values stored through RedisTemplate and the Redis cache survive a round trip
 * through the Testcontainers Redis, and type ids outside the allowlist are refused on read.
 */
@IntegrationTest
class RedisSerializationIntegrationTest {

    private static final String KEY_PREFIX = "redis-serialization-test:";

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    CacheManager cacheManager;

    @AfterEach
    void deleteTestKeys() {
        stringRedisTemplate.delete(stringRedisTemplate.keys(KEY_PREFIX + "*"));
        users().clear();
    }

    @Test
    void userDtoRoundTripsThroughTheUsersCache() {
        UserDto user = userDto(new HashSet<>(Set.of("grades:read", "attendance:write")));

        users().put("user:7", user);

        assertThat(stringRedisTemplate.opsForValue().get("users::user:7"))
                .contains("\"" + UserDto.class.getName() + "\"")
                .contains("\"java.util.HashSet\"");
        assertThat(users().get("user:7", UserDto.class)).isEqualTo(user);
    }

    @Test
    void studentListRoundTripsThroughRedisTemplate() {
        List<StudentDto> students = new ArrayList<>(List.of(
                new StudentDto(1L, "Amira", "Ben Ali", "amira@example.test", "20000001",
                        LocalDate.of(2010, 5, 3), "FEMALE", "Tunis", "GRADE_8", 2023),
                new StudentDto(2L, "Youssef", "Trabelsi", "youssef@example.test", null,
                        null, "MALE", null, "GRADE_9", 2022)));

        redisTemplate.opsForValue().set(KEY_PREFIX + "students", students);

        assertThat(stringRedisTemplate.opsForValue().get(KEY_PREFIX + "students"))
                .startsWith("[\"java.util.ArrayList\"")
                .contains("\"" + StudentDto.class.getName() + "\"");
        assertThat(redisTemplate.opsForValue().get(KEY_PREFIX + "students")).isEqualTo(students);
    }

    @Test
    void cachedValueWithTypeOutsideTheAllowlistIsRefused() {
        users().put("user:8", userDto(new TreeSet<>(Set.of("grades:read"))));

        assertThatThrownBy(() -> users().get("user:8", UserDto.class))
                .isInstanceOf(SerializationException.class)
                .rootCause()
                .hasMessageContaining("java.util.TreeSet")
                .hasMessageContaining("PolymorphicTypeValidator");
    }

    @Test
    void storedTypeIdOutsideTheAllowlistIsRefused() {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + "foreign",
                "[\"java.util.LinkedList\",[\"harmless\"]]");

        assertThatThrownBy(() -> redisTemplate.opsForValue().get(KEY_PREFIX + "foreign"))
                .isInstanceOf(SerializationException.class)
                .rootCause()
                .hasMessageContaining("java.util.LinkedList")
                .hasMessageContaining("PolymorphicTypeValidator");
    }

    private Cache users() {
        return Objects.requireNonNull(cacheManager.getCache("users"));
    }

    private static UserDto userDto(Set<String> permissions) {
        return UserDto.builder()
                .id(7L)
                .firstName("Leila")
                .lastName("Haddad")
                .email("leila@example.test")
                .role(UserRole.TEACHER)
                .profileTheme("dark")
                .profileLanguage("fr")
                .permissions(permissions)
                .build();
    }
}
