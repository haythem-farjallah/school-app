package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.operational.entity.Announcement;
import com.example.school_management.feature.operational.repository.AnnouncementRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A teacher can update or delete only announcements they created; administrators
 * keep managing every announcement. The fixture teacher is teacher A.
 */
@IntegrationTest
class AnnouncementOwnershipIntegrationTest {

    private static final String RENAME = "{\"title\":\"Renamed\"}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AnnouncementRepository announcementRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Teacher teacherB;
    private Announcement byTeacherA;
    private Announcement byAdmin;

    @BeforeEach
    void createAnnouncements() {
        Teacher t = new Teacher();
        t.setRole(UserRole.TEACHER);
        t.setEmail("teacher-b-" + UUID.randomUUID() + "@fixtures.school.test");
        t.setFirstName("Tina");
        t.setLastName("Other");
        t.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        t.setStatus(Status.ACTIVE);
        t.setIsEmailVerified(true);
        teacherB = teacherRepository.save(t);

        byTeacherA = announcementRepository.save(announcement(DevFixtureLoader.TEACHER_EMAIL));
        byAdmin = announcementRepository.save(announcement(DevFixtureLoader.ADMIN_EMAIL));
    }

    @AfterEach
    void deleteAnnouncements() {
        announcementRepository.deleteAll(announcementRepository.findAllById(List.of(byTeacherA.getId(), byAdmin.getId())));
        teacherRepository.deleteById(teacherB.getId());
    }

    @Test
    void anotherTeacherCannotUpdateOrDeleteTheAnnouncement() throws Exception {
        String teacher = bearer(teacherB.getEmail());

        mockMvc.perform(put("/api/v1/announcements/{id}", byTeacherA.getId()).header(HttpHeaders.AUTHORIZATION, teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(RENAME))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only update announcements you created"));
        mockMvc.perform(delete("/api/v1/announcements/{id}", byTeacherA.getId()).header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only delete announcements you created"));

        Announcement unchanged = announcementRepository.findById(byTeacherA.getId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("Ownership test announcement");
    }

    @Test
    void aTeacherCannotChangeAnAdministratorsAnnouncement() throws Exception {
        mockMvc.perform(put("/api/v1/announcements/{id}", byAdmin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON).content(RENAME))
                .andExpect(status().isForbidden());
    }

    @Test
    void theCreatorCanUpdateAndDeleteTheAnnouncement() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        mockMvc.perform(put("/api/v1/announcements/{id}", byTeacherA.getId()).header(HttpHeaders.AUTHORIZATION, teacher)
                        .contentType(MediaType.APPLICATION_JSON).content(RENAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Renamed"));
        mockMvc.perform(delete("/api/v1/announcements/{id}", byTeacherA.getId()).header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isOk());

        assertThat(announcementRepository.existsById(byTeacherA.getId())).isFalse();
    }

    @Test
    void anAdministratorCanUpdateAndDeleteAnyAnnouncement() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        mockMvc.perform(put("/api/v1/announcements/{id}", byTeacherA.getId()).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(RENAME))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/announcements/{id}", byTeacherA.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());

        assertThat(announcementRepository.existsById(byTeacherA.getId())).isFalse();
    }

    private Announcement announcement(String creatorEmail) {
        Announcement a = new Announcement();
        a.setTitle("Ownership test announcement");
        a.setBody("Body");
        a.setCreatedAt(LocalDateTime.now());
        a.setCreatedById(userRepository.findByEmail(creatorEmail).orElseThrow().getId());
        return a;
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.26." + clientAddress.incrementAndGet());
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
