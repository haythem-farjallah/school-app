package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.operational.dto.CreateEnhancedGradeRequest;
import com.example.school_management.feature.operational.entity.EnhancedGrade;
import com.example.school_management.feature.operational.repository.EnhancedGradeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The recorded approver of a grade is the authenticated account, whatever the
 * request body claims.
 */
@IntegrationTest
class GradeApprovalIdentityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    EnhancedGradeRepository enhancedGradeRepository;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private EnhancedGrade grade;

    @BeforeEach
    void createUnapprovedGrade() {
        EnhancedGrade g = new EnhancedGrade();
        g.setStudentId(studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow().getId());
        g.setClassId(1L);
        g.setCourseId(1L);
        g.setTeacherId(1L);
        g.setExamType(CreateEnhancedGradeRequest.ExamType.FIRST_EXAM);
        g.setSemester(CreateEnhancedGradeRequest.Semester.THIRD);
        g.setScore(15.0);
        g.setMaxScore(20.0);
        grade = enhancedGradeRepository.save(g);
    }

    @AfterEach
    void deleteGrade() {
        enhancedGradeRepository.deleteById(grade.getId());
    }

    @Test
    void approverIsTheAuthenticatedAccountNotTheRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/grades/approve")
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studentIds", List.of(grade.getStudentId()),
                                "semester", "THIRD",
                                "approvedBy", "Principal Someone Else"))))
                .andExpect(status().isOk());

        EnhancedGrade approved = enhancedGradeRepository.findById(grade.getId()).orElseThrow();
        assertThat(approved.getIsApproved()).isTrue();
        assertThat(approved.getApprovedBy()).isEqualTo(DevFixtureLoader.ADMIN_EMAIL);
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.23." + clientAddress.incrementAndGet());
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
