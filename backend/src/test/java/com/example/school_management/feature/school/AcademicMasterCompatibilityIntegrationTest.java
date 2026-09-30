package com.example.school_management.feature.school;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.dto.CreateCourseRequest;
import com.example.school_management.feature.academic.dto.UpdateCourseRequest;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.service.CourseService;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional
class AcademicMasterCompatibilityIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    CourseRepository courses;

    @Autowired
    CourseService courseService;

    @Autowired
    RoomRepository rooms;

    @Autowired
    SchoolRepository schools;

    @Autowired
    CurrentSchoolResolver currentSchool;

    private static final AtomicInteger clients = new AtomicInteger();

    @Test
    void legacyCourseCreateWorksWithoutCodeOrSchoolInputAndCodeSurvivesUpdate() throws Exception {
        String token = bearer();
        var response = mvc.perform(post("/api/v1/courses").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "name", "Compatibility " + UUID.randomUUID(), "color", "#3366ff", "credit", 3, "weeklyCapacity", 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").doesNotExist())
                .andExpect(jsonPath("$.data.school").doesNotExist())
                .andExpect(jsonPath("$.data.schoolId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).path("data").path("id").asLong();
        Course created = courses.findById(id).orElseThrow();
        String code = created.getCode();
        int maxLength = 20;
        assertThat(code).isNotBlank().startsWith("CRS-").hasSizeLessThanOrEqualTo(maxLength);
        assertThat(created.getSchool().getId()).isEqualTo(currentSchool.resolve().getId());

        courseService.update(id, new UpdateCourseRequest("Renamed compatibility course", null, null, null, null));
        courses.flush();
        assertThat(courses.findById(id).orElseThrow().getCode()).isEqualTo(code);
    }

    @Test
    void separateCreationsGetIndependentInternalCodes() {
        var first = courseService.create(new CreateCourseRequest("First " + UUID.randomUUID(), "#3366ff", 3.0f, 3, null));
        var second = courseService.create(new CreateCourseRequest("Second " + UUID.randomUUID(), "#3366ff", 3.0f, 3, null));
        Course a = courses.findById(first.id()).orElseThrow();
        Course b = courses.findById(second.id()).orElseThrow();
        assertThat(a.getCode()).isNotEqualTo(b.getCode());
        assertThat(a.getSchool().getId()).isEqualTo(b.getSchool().getId());
    }

    @Test
    void legacyRoomCreateAndReadKeepInternalSchoolOutOfJson() throws Exception {
        String token = bearer();
        var response = mvc.perform(post("/api/v1/rooms").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Compatibility Room\",\"capacity\":30,\"roomType\":\"CLASSROOM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Compatibility Room"))
                .andExpect(jsonPath("$.data.school").doesNotExist())
                .andExpect(jsonPath("$.data.schoolId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(response).path("data").path("id").asLong();
        assertThat(rooms.findById(id).orElseThrow().getSchool().getId()).isEqualTo(currentSchool.resolve().getId());
        mvc.perform(get("/api/v1/rooms/{id}", id).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.school").doesNotExist());
    }

    @Test
    void legacyPeriodReadKeepsDtoContract() throws Exception {
        mvc.perform(get("/api/v1/periods").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].index").value(1))
                .andExpect(jsonPath("$[0].school").doesNotExist())
                .andExpect(jsonPath("$[0].schoolId").doesNotExist());
    }

    @Test
    void resolverReturnsTheOnlyMigratedSchool() {
        assertThat(currentSchool.resolve()).isEqualTo(schools.findAll().get(0));
    }

    @Test
    void resolverAndCourseCreationFailOnAmbiguousSchoolContext() {
        School extra = new School();
        extra.setName("Another Test School");
        schools.saveAndFlush(extra);
        long before = courses.count();
        assertThatThrownBy(currentSchool::resolve).isInstanceOf(IllegalStateException.class).hasMessageContaining("multiple Schools");
        assertThatThrownBy(() -> courseService.create(new CreateCourseRequest("Ambiguous", "#3366ff", 3.0f, 3, null)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("multiple Schools");
        assertThat(courses.count()).isEqualTo(before);
    }

    private String bearer() throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr("10.0.42." + clients.incrementAndGet()); return request; })
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "email", DevFixtureLoader.ADMIN_EMAIL, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).path("data").path("accessToken").asText();
    }
}
