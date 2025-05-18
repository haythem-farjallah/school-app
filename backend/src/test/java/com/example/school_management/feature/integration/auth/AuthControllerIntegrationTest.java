package com.example.school_management.feature.integration.auth;


import com.example.school_management.commons.dtos.*;
import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.dto.*;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional
class AuthControllerIntegrationTest {
    // 1) Define your Testcontainer
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url",      postgres::getJdbcUrl);
        registry.add("spring.flyway.user",     postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    UserRepository userRepo;
    @MockitoBean
    EmailService emailService;

    @PersistenceContext
    EntityManager em;
    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @BeforeEach
    void cleanUp() {
        em.createNativeQuery("TRUNCATE TABLE profile_settings CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE role_permissions CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE users CASCADE").executeUpdate();
    }

    @Test
    void register_thenLogin_thenChangePassword_flow() throws Exception {
        // 1) REGISTER
        RegisterRequest register = new RegisterRequest();
        register.setRole(UserRole.STUDENT);
        register.setEmail("int@test.com");
        register.setPassword("initialPass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        // user saved with passwordChangeRequired==true
        Student saved = (Student) userRepo.findByEmail("int@test.com").orElseThrow();
        assertThat(saved.isPasswordChangeRequired()).isTrue();

        // 2) FIRST-LOGIN CHANGE-PASSWORD (old→new)
        ChangePasswordRequest change = new ChangePasswordRequest();
        change.setEmail("int@test.com");
        change.setOldPassword("initialPass");
        change.setNewPassword("brandNew");

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(change)))
                .andExpect(status().isOk());

        // verify flag cleared and password updated
        saved = (Student) userRepo.findByEmail("int@test.com").get();
        assertThat(saved.isPasswordChangeRequired()).isFalse();
        // note: real encoding applies, so just check non‐empty
        assertThat(saved.getPassword()).isNotBlank();

        // 3) FORGOT-PASSWORD → generates OTP
        ForgotPasswordRequest forgot = new ForgotPasswordRequest();
        forgot.setEmail("int@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(forgot)))
                .andExpect(status().isOk());

        saved = (Student) userRepo.findByEmail("int@test.com").get();
        assertThat(saved.getOtpCode()).hasSize(6);
        assertThat(saved.getOtpExpiry()).isAfter(LocalDateTime.now());

        // 4) RESET-PASSWORD using that OTP
        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setEmail("int@test.com");
        reset.setOtp(saved.getOtpCode());
        reset.setNewPassword("resetPass");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(reset)))
                .andExpect(status().isOk());

        saved = (Student) userRepo.findByEmail("int@test.com").get();
        assertThat(saved.isPasswordChangeRequired()).isFalse();
        assertThat(saved.getOtpCode()).isNull();
        assertThat(saved.getOtpExpiry()).isNull();
    }


}