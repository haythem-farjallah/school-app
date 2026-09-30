package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Transactional
class AcademicCalendarApiIntegrationTest {
    private static final String YEARS = "/api/v1/academic-years";
    private static final String YEAR_BODY = "{\"name\":\"Configured\",\"startDate\":\"2026-09-01\",\"endDate\":\"2027-06-30\",\"active\":false}";
    private static final String TERM_BODY = "{\"name\":\"Autumn\",\"sequenceNumber\":1,\"startDate\":\"2026-09-01\",\"endDate\":\"2027-01-31\"}";
    private static final AtomicInteger clients = new AtomicInteger();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AcademicYearRepository years;
    @Autowired CurrentSchoolResolver currentSchool;

    @Test
    void adminConfiguresCalendarThroughDtosWithoutCallerOwnership() throws Exception {
        String token = bearer(DevFixtureLoader.ADMIN_EMAIL);
        String suppliedOwnership = YEAR_BODY.substring(0, YEAR_BODY.length() - 1) + ",\"schoolId\":999999,\"school\":{\"id\":999999}}";
        var response = mvc.perform(post(YEARS).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(suppliedOwnership))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Configured"))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.school").doesNotExist())
                .andExpect(jsonPath("$.data.schoolId").doesNotExist())
                .andExpect(jsonPath("$.data.createdAt").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long yearId = json.readTree(response).path("data").path("id").asLong();
        assertThat(years.findById(yearId).orElseThrow().getSchool().getId()).isEqualTo(currentSchool.resolve().getId());
        mvc.perform(post(YEARS + "/{id}/activate", yearId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
        mvc.perform(put(YEARS + "/{id}", yearId).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(YEAR_BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
        mvc.perform(get(YEARS + "/{id}", yearId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.school").doesNotExist());
        mvc.perform(get(YEARS).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(yearId));

        String terms = YEARS + "/" + yearId + "/terms";
        String suppliedYear = TERM_BODY.substring(0, TERM_BODY.length() - 1) + ",\"academicYearId\":999999}";
        response = mvc.perform(post(terms).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(suppliedYear))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.academicYearId").value(yearId))
                .andExpect(jsonPath("$.data.academicYear").doesNotExist())
                .andExpect(jsonPath("$.data.school").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long termId = json.readTree(response).path("data").path("id").asLong();
        mvc.perform(get(terms + "/{id}", termId).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Autumn"));
        mvc.perform(get(terms).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].sequenceNumber").value(1));
        mvc.perform(put(terms + "/{id}", termId).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(TERM_BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.academicYearId").value(yearId));
    }

    @ParameterizedTest
    @ValueSource(strings = {DevFixtureLoader.STUDENT_EMAIL, DevFixtureLoader.TEACHER_EMAIL, DevFixtureLoader.PARENT_EMAIL})
    void nonAdminCannotMutateAnyCalendarEndpoint(String email) throws Exception {
        String token = bearer(email);
        mvc.perform(post(YEARS).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(YEAR_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(put(YEARS + "/1").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(YEAR_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post(YEARS + "/1/activate").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post(YEARS + "/1/terms").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(TERM_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(put(YEARS + "/1/terms/1").header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(TERM_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotMutateCalendarConfiguration() throws Exception {
        mvc.perform(post(YEARS).with(user("staff").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(YEAR_BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post(YEARS + "/1/activate").with(user("staff").roles("STAFF")))
                .andExpect(status().isForbidden());
        mvc.perform(post(YEARS + "/1/terms").with(user("staff").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(TERM_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousAccessIsDenied() throws Exception {
        mvc.perform(get(YEARS)).andExpect(status().isUnauthorized());
        mvc.perform(get(YEARS + "/1/terms")).andExpect(status().isUnauthorized());
        mvc.perform(post(YEARS).contentType(MediaType.APPLICATION_JSON).content(YEAR_BODY)).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedSchoolDataReadIsAllowed() throws Exception {
        String token = bearer(DevFixtureLoader.STUDENT_EMAIL);
        mvc.perform(get(YEARS).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(get(YEARS + "/999999").header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(get(YEARS + "/999999/terms").header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test
    void invalidCalendarInputAndDuplicatesUseProblemDetails() throws Exception {
        String token = bearer(DevFixtureLoader.ADMIN_EMAIL);
        mvc.perform(post(YEARS).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(YEAR_BODY.replace("2027-06-30", "2026-09-01")))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post(YEARS).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        String response = mvc.perform(post(YEARS).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(YEAR_BODY))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long yearId = json.readTree(response).path("data").path("id").asLong();
        mvc.perform(post(YEARS).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(YEAR_BODY))
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post(YEARS + "/{id}/terms", yearId).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(TERM_BODY.replace("2026-09-01", "2026-08-31")))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(post(YEARS + "/{id}/terms", yearId).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(TERM_BODY.replace("\"sequenceNumber\":1", "\"sequenceNumber\":0")))
                .andExpect(status().isBadRequest());
    }

    private String bearer(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr("10.0.83." + clients.incrementAndGet()); return request; })
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).path("data").path("accessToken").asText();
    }
}
