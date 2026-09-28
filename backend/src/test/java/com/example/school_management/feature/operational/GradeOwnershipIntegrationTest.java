package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A grade can be updated only by the teacher who assigned it, and deleted only by
 * that teacher within 24 hours of grading. The fixture teacher is teacher A.
 */
@IntegrationTest
class GradeOwnershipIntegrationTest {

    private static final String DELETE_BODY = "{\"reason\":\"Entered for the wrong student\"}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    ClassRepository classRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    GradeRepository gradeRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Teacher teacherB;
    private ClassEntity schoolClass;
    private Enrollment enrollment;
    private Grade recentGrade;
    private Grade oldGrade;

    @BeforeEach
    void gradeByTeacherA() {
        Teacher teacherA = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        teacherB = teacherRepository.save(otherTeacher());

        ClassEntity c = new ClassEntity();
        c.setName("Grade ownership " + UUID.randomUUID());
        schoolClass = classRepository.save(c);

        Enrollment e = new Enrollment();
        e.setStudent(studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow());
        e.setClassEntity(schoolClass);
        e.setStatus(EnrollmentStatus.ACTIVE);
        enrollment = enrollmentRepository.save(e);

        recentGrade = gradeRepository.save(grade(teacherA, LocalDateTime.now().minusHours(1)));
        oldGrade = gradeRepository.save(grade(teacherA, LocalDateTime.now().minusHours(25)));
    }

    @AfterEach
    void removeGrades() {
        gradeRepository.deleteAll(gradeRepository.findAllById(List.of(recentGrade.getId(), oldGrade.getId())));
        enrollmentRepository.deleteById(enrollment.getId());
        classRepository.deleteById(schoolClass.getId());
        teacherRepository.deleteById(teacherB.getId());
    }

    @Test
    void anotherTeacherCannotUpdateTheGrade() throws Exception {
        expectForbidden(mockMvc.perform(patch("/api/v1/grades/{id}", recentGrade.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherB.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":2}")),
                "You can only update grades you assigned");

        assertThat(gradeRepository.findById(recentGrade.getId()).orElseThrow().getScore()).isEqualTo(14f);
    }

    @Test
    void theAssigningTeacherCanUpdateTheGrade() throws Exception {
        mockMvc.perform(patch("/api/v1/grades/{id}", recentGrade.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":16}"))
                .andExpect(status().isOk());

        assertThat(gradeRepository.findById(recentGrade.getId()).orElseThrow().getScore()).isEqualTo(16f);
    }

    @Test
    void anotherTeacherCannotDeleteTheGrade() throws Exception {
        expectForbidden(mockMvc.perform(delete("/api/v1/grades/{id}", recentGrade.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teacherB.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DELETE_BODY)),
                "You can only delete grades you assigned in the last 24 hours");

        assertThat(gradeRepository.existsById(recentGrade.getId())).isTrue();
    }

    @Test
    void theAssigningTeacherCannotDeleteAfter24Hours() throws Exception {
        expectForbidden(mockMvc.perform(delete("/api/v1/grades/{id}", oldGrade.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DELETE_BODY)),
                "You can only delete grades you assigned in the last 24 hours");

        assertThat(gradeRepository.existsById(oldGrade.getId())).isTrue();
    }

    @Test
    void theAssigningTeacherCanDeleteWithin24Hours() throws Exception {
        mockMvc.perform(delete("/api/v1/grades/{id}", recentGrade.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DELETE_BODY))
                .andExpect(status().isOk());

        assertThat(gradeRepository.existsById(recentGrade.getId())).isFalse();
    }

    private Grade grade(Teacher assignedBy, LocalDateTime gradedAt) {
        Grade grade = new Grade();
        grade.setEnrollment(enrollment);
        grade.setAssignedBy(assignedBy);
        grade.setContent("Quiz");
        grade.setScore(14f);
        grade.setGradedAt(gradedAt);
        return grade;
    }

    private Teacher otherTeacher() {
        Teacher teacher = new Teacher();
        teacher.setRole(UserRole.TEACHER);
        teacher.setEmail("teacher-b-" + UUID.randomUUID() + "@fixtures.school.test");
        teacher.setFirstName("Tina");
        teacher.setLastName("Other");
        teacher.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        teacher.setStatus(Status.ACTIVE);
        teacher.setIsEmailVerified(true);
        return teacher;
    }

    private void expectForbidden(ResultActions result, String detail) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.24." + clientAddress.incrementAndGet());
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
