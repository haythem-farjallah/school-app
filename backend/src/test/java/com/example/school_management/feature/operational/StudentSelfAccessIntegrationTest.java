package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A student reaches student-specific data only for their own account, through the
 * real filter chain. The fixture student is student A; student B is another student
 * enrolled in the same class.
 */
@IntegrationTest
class StudentSelfAccessIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    ClassRepository classRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Student studentA;
    private Student studentB;
    private ClassEntity schoolClass;
    private Enrollment enrollmentA;
    private Enrollment enrollmentB;

    @BeforeEach
    void enrollTwoStudents() {
        studentA = studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();

        Student other = new Student();
        other.setRole(UserRole.STUDENT);
        other.setEmail("student-b-" + UUID.randomUUID() + "@fixtures.school.test");
        other.setFirstName("Bea");
        other.setLastName("Other");
        other.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        other.setStatus(Status.ACTIVE);
        other.setIsEmailVerified(true);
        studentB = studentRepository.save(other);

        ClassEntity c = new ClassEntity();
        c.setName("Self access " + UUID.randomUUID());
        schoolClass = classRepository.save(c);

        enrollmentA = enrollmentRepository.save(activeEnrollment(studentA));
        enrollmentB = enrollmentRepository.save(activeEnrollment(studentB));
    }

    @AfterEach
    void removeEnrollments() {
        enrollmentRepository.deleteAll(enrollmentRepository.findAllById(List.of(enrollmentA.getId(), enrollmentB.getId())));
        classRepository.deleteById(schoolClass.getId());
        studentRepository.deleteById(studentB.getId());
    }

    @Test
    void gradeSheetAndExportAreSelfOnly() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get("/api/v1/grades/student/{id}/sheet", studentA.getId()).param("semester", "FIRST")
                        .header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(studentA.getId()));
        mockMvc.perform(get("/api/v1/grades/student/{id}/export", studentA.getId()).param("semester", "FIRST")
                        .header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        expectForbidden(mockMvc.perform(get("/api/v1/grades/student/{id}/sheet", studentB.getId()).param("semester", "FIRST")
                .header(HttpHeaders.AUTHORIZATION, student)));
        expectForbidden(mockMvc.perform(get("/api/v1/grades/student/{id}/export", studentB.getId()).param("semester", "FIRST")
                .header(HttpHeaders.AUTHORIZATION, student)));

        // Administrators keep their school-wide access.
        mockMvc.perform(get("/api/v1/grades/student/{id}/sheet", studentB.getId()).param("semester", "FIRST")
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(studentB.getId()));
    }

    @Test
    void attendanceIsSelfOnly() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);
        long teacherId = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow().getId();

        for (String uri : new String[]{"/api/v1/attendance/user/{id}", "/api/v1/attendance/statistics/user/{id}"}) {
            mockMvc.perform(get(uri, studentA.getId()).params(dateRange()).header(HttpHeaders.AUTHORIZATION, student))
                    .andExpect(status().isOk());
            expectForbidden(mockMvc.perform(get(uri, studentB.getId()).params(dateRange())
                    .header(HttpHeaders.AUTHORIZATION, student)));
            expectForbidden(mockMvc.perform(get(uri, teacherId).params(dateRange())
                    .header(HttpHeaders.AUTHORIZATION, student)));
        }
    }

    @Test
    void enrollmentListAndStatsAreSelfOnly() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get("/api/v1/enrollments/student/{id}", studentA.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(enrollmentA.getId()));
        mockMvc.perform(get("/api/v1/enrollments/stats/student/{id}", studentA.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk());

        expectForbidden(mockMvc.perform(get("/api/v1/enrollments/student/{id}", studentB.getId())
                .header(HttpHeaders.AUTHORIZATION, student)));
        expectForbidden(mockMvc.perform(get("/api/v1/enrollments/stats/student/{id}", studentB.getId())
                .header(HttpHeaders.AUTHORIZATION, student)));
    }

    @Test
    void directEnrollmentLookupIsSelfOnly() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get("/api/v1/enrollments/{id}", enrollmentA.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(enrollmentA.getId()));

        expectForbidden(mockMvc.perform(get("/api/v1/enrollments/{id}", enrollmentB.getId())
                .header(HttpHeaders.AUTHORIZATION, student)));

        mockMvc.perform(get("/api/v1/enrollments/{id}", enrollmentB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    void classesByStudentAreSelfOnly() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get("/api/v1/classes/student/{id}", studentA.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(schoolClass.getId()));

        expectForbidden(mockMvc.perform(get("/api/v1/classes/student/{id}", studentB.getId())
                .header(HttpHeaders.AUTHORIZATION, student)));
    }

    @Test
    void transcriptIsSelfOnlyForStudents() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get("/api/v1/transcripts/{id}", studentA.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentA.getId()));
        mockMvc.perform(get("/api/v1/transcripts/{id}/summary", studentA.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk());

        for (String uri : new String[]{"/api/v1/transcripts/{id}", "/api/v1/transcripts/{id}/summary",
                "/api/v1/transcripts/{id}/pdf"}) {
            expectForbidden(mockMvc.perform(get(uri, studentB.getId()).header(HttpHeaders.AUTHORIZATION, student)));
        }
        for (String uri : new String[]{"/api/v1/transcripts/{id}/period", "/api/v1/transcripts/{id}/pdf/period"}) {
            expectForbidden(mockMvc.perform(get(uri, studentB.getId()).params(dateRange())
                    .header(HttpHeaders.AUTHORIZATION, student)));
        }
    }

    @Test
    void transcriptStaysClosedToParents() throws Exception {
        // No parent access is introduced; the denial is a 403, not an expression failure.
        expectForbidden(mockMvc.perform(get("/api/v1/transcripts/{id}", studentA.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.PARENT_EMAIL))));
    }

    private Enrollment activeEnrollment(Student student) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassEntity(schoolClass);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
    }

    private static MultiValueMap<String, String> dateRange() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("startDate", "2024-01-01");
        params.add("endDate", "2030-12-31");
        return params;
    }

    private void expectForbidden(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("ACCESS_DENIED"));
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.21." + clientAddress.incrementAndGet());
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
