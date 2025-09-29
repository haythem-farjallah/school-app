package com.example.school_management.feature.unit.auth.mapper;

import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.mapper.AuthMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class AuthMapperTest {

    private AuthMapper authMapper;

    @BeforeEach
    void setUp() {
        authMapper = Mappers.getMapper(AuthMapper.class);
    }

    @Test
    void toStudent_mapsAllFields() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setRole(UserRole.STUDENT);
        request.setEmail("student@test.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        // when
        Student student = authMapper.toStudent(request);

        // then
        assertThat(student).isNotNull();
        assertThat(student.getEmail()).isEqualTo("student@test.com");
        assertThat(student.getPassword()).isEqualTo("password123");
        assertThat(student.getFirstName()).isEqualTo("John");
        assertThat(student.getLastName()).isEqualTo("Doe");
        assertThat(student.getId()).isNull(); // should be ignored
    }

    @Test
    void toTeacher_mapsAllFields() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setRole(UserRole.TEACHER);
        request.setEmail("teacher@test.com");
        request.setPassword("teacherPass");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        // when
        Teacher teacher = authMapper.toTeacher(request);

        // then
        assertThat(teacher).isNotNull();
        assertThat(teacher.getEmail()).isEqualTo("teacher@test.com");
        assertThat(teacher.getPassword()).isEqualTo("teacherPass");
        assertThat(teacher.getFirstName()).isEqualTo("Jane");
        assertThat(teacher.getLastName()).isEqualTo("Smith");
        assertThat(teacher.getId()).isNull();
    }

    @Test
    void toParent_mapsAllFields() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setRole(UserRole.PARENT);
        request.setEmail("parent@test.com");
        request.setPassword("parentPass");
        request.setFirstName("Bob");
        request.setLastName("Johnson");

        // when
        Parent parent = authMapper.toParent(request);

        // then
        assertThat(parent).isNotNull();
        assertThat(parent.getEmail()).isEqualTo("parent@test.com");
        assertThat(parent.getPassword()).isEqualTo("parentPass");
        assertThat(parent.getFirstName()).isEqualTo("Bob");
        assertThat(parent.getLastName()).isEqualTo("Johnson");
        assertThat(parent.getId()).isNull();
    }

    @Test
    void toAdmin_mapsAllFields() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setRole(UserRole.ADMIN);
        request.setEmail("admin@test.com");
        request.setPassword("adminPass");
        request.setFirstName("Alice");
        request.setLastName("Admin");

        // when
        Administration admin = authMapper.toAdmin(request);

        // then
        assertThat(admin).isNotNull();
        assertThat(admin.getEmail()).isEqualTo("admin@test.com");
        assertThat(admin.getPassword()).isEqualTo("adminPass");
        assertThat(admin.getFirstName()).isEqualTo("Alice");
        assertThat(admin.getLastName()).isEqualTo("Admin");
        assertThat(admin.getId()).isNull();
    }

    @Test
    void toStaff_mapsAllFields() {
        // given
        RegisterRequest request = new RegisterRequest();
        request.setRole(UserRole.STAFF);
        request.setEmail("staff@test.com");
        request.setPassword("staffPass");
        request.setFirstName("Charlie");
        request.setLastName("Worker");

        // when
        Staff staff = authMapper.toStaff(request);

        // then
        assertThat(staff).isNotNull();
        assertThat(staff.getEmail()).isEqualTo("staff@test.com");
        assertThat(staff.getPassword()).isEqualTo("staffPass");
        assertThat(staff.getFirstName()).isEqualTo("Charlie");
        assertThat(staff.getLastName()).isEqualTo("Worker");
        assertThat(staff.getId()).isNull();
    }
}
