package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.dto.CreateAcademicYearRequest;
import com.example.school_management.feature.academic.dto.CreateClassRequest;
import com.example.school_management.feature.academic.dto.UpdateClassRequest;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.service.AcademicYearService;
import com.example.school_management.feature.academic.service.ClassService;
import com.example.school_management.feature.auth.entity.enums.GradeLevel;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.operational.service.EnrollmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Transactional
class ClassAcademicYearWriteIntegrationTest {
    @Autowired ClassService service;
    @Autowired AcademicYearService calendar;
    @Autowired AcademicYearRepository years;
    @Autowired ClassRepository classes;
    @Autowired EnrollmentService enrollment;
    @Autowired StudentRepository students;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired ObjectMapper json;
    @Autowired MockMvc mvc;

    @Test
    void nameOnlyCreatePersistsCurrentYearAndUpdatePreservesOwnership() {
        long first = year("Configured A", true);
        var created = service.create(new CreateClassRequest("7-A"));
        assertOwnership(created.id(), first, "Configured A");
        long second = year("Configured B", true);
        var later = service.create(new CreateClassRequest("7-B"));
        assertOwnership(later.id(), second, "Configured B");
        service.update(created.id(), new UpdateClassRequest("Renamed 7-A"));
        assertOwnership(created.id(), first, "Configured A");
        assertThat(classes.findById(created.id()).orElseThrow().getName()).isEqualTo("Renamed 7-A");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void creationSupportsFullValidYearName(boolean autoEnrollment) {
        String name = "Y".repeat(255);
        long current = year(name, true);
        long id;
        if (autoEnrollment) {
            Long student = middleStudent();
            var result = enrollment.autoEnrollByGradeLevel("MIDDLE");
            assertThat(result.success()).isTrue();
            id = jdbc.queryForObject("SELECT class_id FROM enrollments WHERE student_id = ? AND status = 'ACTIVE'", Long.class, student);
        } else {
            id = service.create(new CreateClassRequest("Long year name")).id();
        }
        assertOwnership(id, current, name);
    }

    @Test
    void noActiveYearRejectsCreationWithoutCreatingClassOrYear() {
        year("Inactive", false);
        long count = classes.count();
        assertThatThrownBy(() -> service.create(new CreateClassRequest("Cannot create")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no active AcademicYear");
        assertThat(classes.count()).isEqualTo(count);
        assertThat(years.count()).isEqualTo(1);
    }

    @Test
    void namesConflictCaseInsensitivelyWithinYearButMayRecurInAnotherYear() {
        long first = year("Configured", true);
        service.create(new CreateClassRequest("7-A"));
        assertThatThrownBy(() -> service.create(new CreateClassRequest("7-A"))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.create(new CreateClassRequest("7-a"))).isInstanceOf(ConflictException.class);
        long second = year("Next", true);
        var repeated = service.create(new CreateClassRequest("7-A"));
        assertThat(jdbc.queryForObject("SELECT academic_year_id FROM classes WHERE id = ?", Long.class, repeated.id())).isEqualTo(second);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM classes WHERE name = '7-A' AND academic_year_id = ?", Long.class, first)).isEqualTo(1);
    }

    @Test
    void unknownJsonCannotOverrideYearAndSerializationDoesNotLoadOrExposeRelation() throws Exception {
        long current = year("Configured", true);
        long other = year("Other", false);
        String login = mvc.perform(post("/api/auth/login").with(request -> { request.setRemoteAddr("198.18.53.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("email", DevFixtureLoader.ADMIN_EMAIL, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = "Bearer " + json.readTree(login).path("data").path("accessToken").asText();
        String response = mvc.perform(post("/api/v1/classes").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"API class\",\"academicYearId\":" + other + ",\"academicYear\":{\"id\":" + other + "}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("API class"))
                .andExpect(jsonPath("$.data.academicYear").doesNotExist())
                .andExpect(jsonPath("$.data.academicYearId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).path("data").path("id").asLong();
        assertOwnership(id, current, "Configured");
        mvc.perform(put("/api/v1/classes/{id}", id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed API class\",\"academicYearId\":" + other + ",\"academicYear\":{\"id\":" + other + "}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Renamed API class"));
        assertOwnership(id, current, "Configured");
        em.clear();
        var entity = classes.findById(id).orElseThrow();
        assertThat(Hibernate.isInitialized(entity.getAcademicYear())).isFalse();
        em.clear();
        var serialized = json.readTree(json.writeValueAsString(entity));
        assertThat(serialized.has("academicYear")).isFalse();
        assertThat(Hibernate.isInitialized(entity.getAcademicYear())).isFalse();
    }

    @Test
    void autoEnrollmentIgnoresOtherYearClassesAndTheirSections() {
        long previous = year("Previous", false);
        long current = year("Current", true);
        fixtureClass("Old M-A", "A", previous, 30);
        var student = middleStudent();
        var result = enrollment.autoEnrollByGradeLevel("MIDDLE");
        assertThat(result.success()).isTrue();
        assertThat(result.classesCreated()).isEqualTo(1);
        Long selected = jdbc.queryForObject("SELECT class_id FROM enrollments WHERE student_id = ? AND status = 'ACTIVE'", Long.class, student);
        assertOwnership(selected, current, "Current");
        assertThat(classes.findById(selected).orElseThrow().getName()).isEqualTo("M-A");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void autoEnrollmentSkipsNamesReservedByNameOnlyClasses(boolean preview) {
        long previous = year("Previous", false);
        long current = year("Current", true);
        long manual = service.create(new CreateClassRequest("m-a")).id();
        fixtureClass("M-B", "B", current, 0);
        fixtureClass("M-C", "C", previous, 30);
        var student = middleStudent();
        long before = classes.count();
        var result = preview ? enrollment.previewAutoEnrollment() : enrollment.autoEnrollByGradeLevel("MIDDLE");
        assertThat(result.success()).isTrue();
        assertThat(result.createdClasses()).contains("M-C");
        assertThat(classes.findById(manual).orElseThrow().getGradeLevel()).isNull();
        if (preview) {
            assertThat(classes.count()).isEqualTo(before);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM enrollments WHERE student_id = ? AND status = 'ACTIVE'", Long.class, student)).isZero();
        } else {
            assertThat(result.classesCreated()).isEqualTo(1);
            Long selected = jdbc.queryForObject("SELECT class_id FROM enrollments WHERE student_id = ? AND status = 'ACTIVE'", Long.class, student);
            assertOwnership(selected, current, "Current");
            assertThat(classes.findById(selected).orElseThrow().getName()).isEqualTo("M-C");
            assertThat(selected).isNotEqualTo(manual);
        }
    }

    @Test
    void autoEnrollmentReusesCurrentYearClass() {
        long current = year("Current", true);
        long existing = fixtureClass("Current M-A", "A", current, 30);
        var student = middleStudent();
        var result = enrollment.autoEnrollByGradeLevel("MIDDLE");
        assertThat(result.success()).isTrue();
        assertThat(result.classesCreated()).isZero();
        assertThat(jdbc.queryForObject("SELECT class_id FROM enrollments WHERE student_id = ? AND status = 'ACTIVE'", Long.class, student)).isEqualTo(existing);
    }

    @Test
    void newSectionsUseCurrentYearIncludingFullClassesAndPreviewDoesNotPersist() {
        long previous = year("Previous", false);
        long current = year("Current", true);
        fixtureClass("Old M-B", "B", previous, 30);
        fixtureClass("Current M-A", "A", current, 0);
        middleStudent();
        long before = classes.count();
        var preview = enrollment.previewAutoEnrollment();
        assertThat(preview.success()).isTrue();
        assertThat(preview.createdClasses()).contains("M-B");
        assertThat(classes.count()).isEqualTo(before);
        var result = enrollment.autoEnrollByGradeLevel("MIDDLE");
        assertThat(result.success()).isTrue();
        assertThat(result.classesCreated()).isEqualTo(1);
        Long created = jdbc.queryForObject("SELECT id FROM classes WHERE name = 'M-B'", Long.class);
        assertOwnership(created, current, "Current");
    }

    @ParameterizedTest
    @ValueSource(strings = {"all", "grade", "preview"})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void autoEnrollmentWithoutActiveYearPropagatesConfigurationFailureWithoutWrites(String operation) {
        long before = classes.count();
        assertThatThrownBy(() -> {
            switch (operation) {
                case "all" -> enrollment.autoEnrollAllStudents();
                case "grade" -> enrollment.autoEnrollByGradeLevel("MIDDLE");
                case "preview" -> enrollment.previewAutoEnrollment();
                default -> throw new AssertionError("Unknown test operation");
            }
        }).isInstanceOf(IllegalStateException.class).hasMessageContaining("no active AcademicYear");
        assertThat(classes.count()).isEqualTo(before);
        assertThat(years.count()).isZero();
    }

    @Test
    void persistenceRejectsClassWithoutYear() {
        var unowned = new ClassEntity();
        unowned.setName("Unowned");
        assertThatThrownBy(() -> classes.saveAndFlush(unowned))
                .isInstanceOfAny(ConstraintViolationException.class, DataIntegrityViolationException.class);
    }

    @Test
    void deletingClassDoesNotCascadeToItsAcademicYear() {
        long current = year("Current", true);
        long id = service.create(new CreateClassRequest("Disposable class")).id();
        classes.deleteById(id);
        classes.flush();
        assertThat(years.existsById(current)).isTrue();
    }

    private long year(String name, boolean active) {
        return calendar.create(new CreateAcademicYearRequest(name, LocalDate.of(2026, 8, 17), LocalDate.of(2027, 7, 9), active)).id();
    }

    private Long middleStudent() {
        var student = students.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        student.setGradeLevel(GradeLevel.MIDDLE);
        em.flush();
        return student.getId();
    }

    private long fixtureClass(String name, String section, Long year, int capacity) {
        return jdbc.queryForObject("INSERT INTO classes(name, grade_level, section, academic_year_id, capacity) VALUES (?, 'MIDDLE', ?, ?, ?) RETURNING id", Long.class, name, section, year, capacity);
    }

    private void assertOwnership(long id, long year, String yearName) {
        em.flush();
        em.clear();
        assertThat(jdbc.queryForObject("SELECT academic_year_id FROM classes WHERE id = ?", Long.class, id)).isEqualTo(year);
        var reloaded = classes.findById(id).orElseThrow();
        assertThat(reloaded.getAcademicYear().getId()).isEqualTo(year);
        assertThat(reloaded.getAcademicYear().getName()).isEqualTo(yearName);
    }
}
