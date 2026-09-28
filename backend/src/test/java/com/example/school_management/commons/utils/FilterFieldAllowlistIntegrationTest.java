package com.example.school_management.commons.utils;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dynamic filter endpoints accept only the field and sort paths listed for them.
 * Before the allowlist, a request parameter could name any entity path, including
 * account columns such as the password hash, and use it as a filter or sort oracle.
 */
@IntegrationTest
class FilterFieldAllowlistIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StudentRepository studentRepository;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Test
    void approvedFieldsFilter() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        mockMvc.perform(get("/api/v1/students/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("firstName_like", "Sam")
                        .param("sort", "lastName:asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].email").value(hasItem(DevFixtureLoader.STUDENT_EMAIL)));

        mockMvc.perform(get("/api/admin/teachers/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("email_like", "teacher@fixtures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].email").value(hasItem(DevFixtureLoader.TEACHER_EMAIL)));

        mockMvc.perform(get("/api/admin/parent-management/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("lastName_like", "Parent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].email").value(hasItem(DevFixtureLoader.PARENT_EMAIL)));

        mockMvc.perform(get("/admin/teaching-assignments").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("teacher.firstName_like", "Theo")
                        .param("sort", "course.name:asc"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/attendance/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("date_from", "2024-01-01")
                        .param("sort", "date,desc"))
                .andExpect(status().isOk());
    }

    @Test
    void approvedNestedIdPathFilters() throws Exception {
        long studentId = studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/grades/filter").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .param("enrollment.student.id_eq", String.valueOf(studentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void sensitiveFieldsAreRejected() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        expectBadRequest(mockMvc.perform(get("/api/v1/students/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("password_like", "$2")),
                "Unsupported filter field 'password'", "/api/v1/students/filter");

        expectBadRequest(mockMvc.perform(get("/api/admin/teachers/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("otpCode_notnull", "true")),
                "Unsupported filter field 'otpCode'", "/api/admin/teachers/filter");

        expectBadRequest(mockMvc.perform(get("/api/admin/parent-management/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("tokenVersion_gte", "0")),
                "Unsupported filter field 'tokenVersion'", "/api/admin/parent-management/filter");
    }

    @Test
    void sensitiveNestedPathsAreRejected() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        // Attendance.user is the account itself: this used to filter on the password hash.
        expectBadRequest(mockMvc.perform(get("/api/v1/attendance/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("user.password_like", "$2")),
                "Unsupported filter field 'user.password'", "/api/v1/attendance/filter");

        expectBadRequest(mockMvc.perform(get("/admin/teaching-assignments/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("teacher.password_like", "$2")),
                "Unsupported filter field 'teacher.password'", "/admin/teaching-assignments/filter");

        // "enrollment.student.id" is allowed; nothing else under it is.
        expectBadRequest(mockMvc.perform(get("/api/v1/grades/filter")
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .param("enrollment.student.otpCode_notnull", "true")),
                "Unsupported filter field 'enrollment.student.otpCode'", "/api/v1/grades/filter");
    }

    @Test
    void unapprovedFieldsAndOperationsAreRejected() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        // A real, non-secret column that the student filter does not offer.
        expectBadRequest(mockMvc.perform(get("/api/v1/students/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("telephone_like", "5")),
                "Unsupported filter field 'telephone'", "/api/v1/students/filter");

        expectBadRequest(mockMvc.perform(get("/api/v1/students/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("firstName_regex", "S.*")),
                "Unsupported filter parameter 'firstName_regex'", "/api/v1/students/filter");
    }

    @Test
    void unapprovedSortPathsAreRejected() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        // The filter's own sort parameter
        expectBadRequest(mockMvc.perform(get("/api/v1/students/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("sort", "password:asc")),
                "Unsupported sort field 'password'", "/api/v1/students/filter");

        expectBadRequest(mockMvc.perform(get("/admin/teaching-assignments").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("sort", "teacher.password:desc")),
                "Unsupported sort field 'teacher.password'", "/admin/teaching-assignments");

        // Spring Data Pageable sort on endpoints that bind a Pageable
        expectBadRequest(mockMvc.perform(get("/api/v1/attendance/filter").header(HttpHeaders.AUTHORIZATION, admin)
                        .param("sort", "user.password,asc")),
                "Unsupported sort field 'user.password'", "/api/v1/attendance/filter");

        expectBadRequest(mockMvc.perform(get("/api/v1/grades/filter")
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .param("sort", "assignedBy.email,desc")),
                "Unsupported sort field 'assignedBy.email'", "/api/v1/grades/filter");
    }

    private void expectBadRequest(ResultActions result, String detail, String instance) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(instance));
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.20." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
