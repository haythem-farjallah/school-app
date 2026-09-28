package com.example.school_management.feature.communication;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StaffRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends to caller-chosen recipients are administrative, and the OTP and device
 * registration routes no longer exist, through the real filter chain.
 */
@IntegrationTest
class CommunicationQuarantineIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StaffRepository staffRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Staff staff;

    @BeforeEach
    void createStaff() {
        Staff st = new Staff();
        st.setRole(UserRole.STAFF);
        st.setEmail("staff-" + UUID.randomUUID() + "@fixtures.school.test");
        st.setFirstName("Sara");
        st.setLastName("Staff");
        st.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        st.setStatus(Status.ACTIVE);
        st.setIsEmailVerified(true);
        staff = staffRepository.save(st);
    }

    @AfterEach
    void removeStaff() {
        staffRepository.deleteById(staff.getId());
    }

    @Test
    void aTeacherCannotSendToArbitraryRecipients() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);
        String email = json(Map.of("recipientEmail", "someone@example.test", "subject", "Hello", "content", "Hello"));
        String sms = json(Map.of("recipientPhone", "+15550100", "message", "Hello"));
        String push = json(Map.of("recipientId", "1", "title", "Hello", "body", "Hello"));
        String variables = json(Map.of("name", "Someone"));

        for (MockHttpServletRequestBuilder send : List.of(
                post("/api/notifications/email/send").content(email),
                post("/api/notifications/email/send-templated").param("templateName", "welcome")
                        .param("recipientEmail", "someone@example.test").content(variables),
                post("/api/notifications/sms/send").content(sms),
                post("/api/notifications/sms/send-templated").param("templateName", "welcome")
                        .param("recipientPhone", "+15550100").content(variables),
                post("/api/notifications/push/send").content(push),
                post("/api/notifications/realtime/send").param("userId", "1").content(variables))) {
            mockMvc.perform(send.header(HttpHeaders.AUTHORIZATION, teacher).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("ACCESS_DENIED"));
        }
    }

    @Test
    void administratorsAndStaffKeepRealTimeSends() throws Exception {
        for (String caller : List.of(DevFixtureLoader.ADMIN_EMAIL, staff.getEmail())) {
            mockMvc.perform(post("/api/notifications/realtime/send").param("userId", "1")
                            .header(HttpHeaders.AUTHORIZATION, bearer(caller))
                            .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("message", "Hello"))))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void theOtpAndDeviceRegistrationRoutesAreGone() throws Exception {
        for (String caller : List.of(DevFixtureLoader.STUDENT_EMAIL, DevFixtureLoader.ADMIN_EMAIL)) {
            String bearer = bearer(caller);
            mockMvc.perform(post("/api/notifications/sms/send-otp").header(HttpHeaders.AUTHORIZATION, bearer)
                            .param("recipientPhone", "+15550100").param("otp", "123456"))
                    .andExpect(status().isNotFound());
            mockMvc.perform(post("/api/notifications/push/register-device").header(HttpHeaders.AUTHORIZATION, bearer)
                            .param("userId", "1").param("deviceToken", "token").param("platform", "web"))
                    .andExpect(status().isNotFound());
        }
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.31." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
