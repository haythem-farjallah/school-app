package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.AcademicYearTestFixtures;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
 * Transcripts and student dashboards carry grades: they are administrative, or the
 * student's own, and teachers have no role-only access to them. The fixture student is
 * student A; student B is another student, enrolled in a class of their own.
 */
@IntegrationTest
class StudentRecordQuarantineIntegrationTest {

    @Autowired
    CurrentSchoolResolver currentSchool;

    @Autowired
    AcademicYearRepository academicYears;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StudentRepository studentRepository;

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
    private Enrollment enrollmentB;

    @BeforeEach
    void enrollStudentB() {
        studentA = studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        Student s = new Student();
        s.setRole(UserRole.STUDENT);
        s.setEmail("student-b-" + UUID.randomUUID() + "@fixtures.school.test");
        s.setFirstName("Bea");
        s.setLastName("Other");
        s.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        s.setStatus(Status.ACTIVE);
        studentB = studentRepository.save(s);

        ClassEntity c = new ClassEntity();
        c.setAcademicYear(AcademicYearTestFixtures.create(academicYears, currentSchool));
        c.setName("Student records " + UUID.randomUUID());
        schoolClass = classRepository.save(c);
        Enrollment e = new Enrollment();
        e.setStudent(studentB);
        e.setClassEntity(schoolClass);
        e.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentB = enrollmentRepository.save(e);
    }

    @AfterEach
    void removeStudentB() {
        enrollmentRepository.deleteById(enrollmentB.getId());
        classRepository.deleteById(schoolClass.getId());
        academicYears.deleteById(schoolClass.getAcademicYear().getId());
        studentRepository.deleteById(studentB.getId());
    }

    @Test
    void aTeacherHasNoRoleOnlyAccessToTranscriptsOrStudentDashboards() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        for (MockHttpServletRequestBuilder route : records(studentA.getId())) {
            mockMvc.perform(route.header(HttpHeaders.AUTHORIZATION, teacher))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("ACCESS_DENIED"));
        }
    }

    @Test
    void aStudentReadsOnlyTheirOwnDashboard() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        for (String uri : List.of("/api/v1/dashboard/student/{id}", "/api/v1/dashboard/student/summary/{id}")) {
            mockMvc.perform(get(uri, studentA.getId()).header(HttpHeaders.AUTHORIZATION, student))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("STUDENT"));
            mockMvc.perform(get(uri, studentB.getId()).header(HttpHeaders.AUTHORIZATION, student))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void administratorsKeepTranscriptsAndStudentDashboards() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        mockMvc.perform(get("/api/v1/transcripts/{id}", studentB.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(studentB.getId()));
        mockMvc.perform(get("/api/v1/dashboard/student/{id}", studentA.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("STUDENT"));
    }

    private static List<MockHttpServletRequestBuilder> records(long studentId) {
        return List.of(
                get("/api/v1/transcripts/{id}", studentId),
                get("/api/v1/transcripts/{id}/summary", studentId),
                get("/api/v1/transcripts/{id}/pdf", studentId),
                get("/api/v1/transcripts/{id}/period", studentId).param("startDate", "2024-01-01").param("endDate", "2030-12-31"),
                get("/api/v1/transcripts/{id}/pdf/period", studentId).param("startDate", "2024-01-01").param("endDate", "2030-12-31"),
                get("/api/v1/dashboard/student/{id}", studentId),
                get("/api/v1/dashboard/student/summary/{id}", studentId));
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.32." + clientAddress.incrementAndGet());
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
