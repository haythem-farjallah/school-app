package com.example.school_management.feature.unit.auth.mapper;

import com.example.school_management.feature.auth.dto.UserDto;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void toDto_mapsAllFields() {
        // given
        Student student = new Student();
        student.setId(1L);
        student.setEmail("student@test.com");
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setRole(UserRole.STUDENT);
        
        ProfileSettings settings = new ProfileSettings();
        settings.setTheme("dark");
        settings.setLanguage("en");
        student.setProfileSettings(settings);

        Permission permission = new Permission();
        permission.setCode("GRADE_READ");
        student.setPermissions(Set.of(permission));

        // when
        UserDto dto = userMapper.toDto(student);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getEmail()).isEqualTo("student@test.com");
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(dto.getProfileTheme()).isEqualTo("dark");
        assertThat(dto.getProfileLanguage()).isEqualTo("en");
        assertThat(dto.getPermissions()).contains("GRADE_READ");
    }

    @Test
    void toDto_withNullPermissions_returnsNull() {
        // given
        Teacher teacher = new Teacher();
        teacher.setId(2L);
        teacher.setEmail("teacher@test.com");
        teacher.setRole(UserRole.TEACHER);
        teacher.setPermissions(null);

        // when
        UserDto dto = userMapper.toDto(teacher);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getPermissions()).isNull();
    }

    @Test
    void toDto_withEmptyPermissions_returnsEmptySet() {
        // given
        Staff staff = new Staff();
        staff.setId(3L);
        staff.setEmail("staff@test.com");
        staff.setRole(UserRole.STAFF);
        staff.setPermissions(Set.of());

        // when
        UserDto dto = userMapper.toDto(staff);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getPermissions()).isEmpty();
    }

    @Test
    void toDto_withMultiplePermissions_mapsAllCodes() {
        // given
        Administration admin = new Administration();
        admin.setId(4L);
        admin.setEmail("admin@test.com");
        admin.setRole(UserRole.ADMIN);

        Permission perm1 = new Permission();
        perm1.setCode("USER_CREATE");
        Permission perm2 = new Permission();
        perm2.setCode("USER_DELETE");
        Permission perm3 = new Permission();
        perm3.setCode("USER_UPDATE");
        
        admin.setPermissions(Set.of(perm1, perm2, perm3));

        // when
        UserDto dto = userMapper.toDto(admin);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getPermissions()).hasSize(3);
        assertThat(dto.getPermissions()).containsExactlyInAnyOrder(
                "USER_CREATE", "USER_DELETE", "USER_UPDATE"
        );
    }

    @Test
    void toDto_withNullProfileSettings_handlesGracefully() {
        // given
        Parent parent = new Parent();
        parent.setId(5L);
        parent.setEmail("parent@test.com");
        parent.setRole(UserRole.PARENT);
        parent.setProfileSettings(null);

        // when
        UserDto dto = userMapper.toDto(parent);

        // then
        assertThat(dto).isNotNull();
        assertThat(dto.getProfileTheme()).isNull();
        assertThat(dto.getProfileLanguage()).isNull();
    }
}
