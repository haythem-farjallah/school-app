package com.example.school_management.feature.unit.auth.controller;

import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.controller.AuthController;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.service.AuthService;
import com.example.school_management.feature.auth.service.OtpService;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import com.example.school_management.feature.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.example.school_management.commons.configs.JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private com.example.school_management.commons.configs.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private CustomUserDetailsService uds;

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepo;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /forgot-password → 200 and OTP sent")
    void forgotPassword_sendsOtp() throws Exception {
        // given
        BaseUser user = new Student(); 
        user.setEmail("test@example.com");
        given(uds.findBaseUserByEmail("test@example.com")).willReturn(user);

        // when/then
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\"}"))
                .andDo(print())
                .andExpect(status().isOk());

        then(otpService).should().generateAndSendOtp(user);
    }

    @Test
    @DisplayName("POST /reset-password → 200 and password reset")
    void resetPassword_validOtp_resetsPassword() throws Exception {
        // given
        BaseUser user = new Student(); 
        user.setEmail("user@example.com");
        given(uds.findBaseUserByEmail("user@example.com")).willReturn(user);
        willDoNothing().given(otpService).validateOtp(user, "123456");
        given(passwordEncoder.encode("newPassword123")).willReturn("ENC[newPassword123]");
        given(userRepo.save(user)).willReturn(user);

        // when/then
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":"user@example.com",
                              "otp":"123456",
                              "newPassword":"newPassword123"
                            }
                        """))
                .andExpect(status().isOk());

        then(userRepo).should().save(user);
        assert !user.isPasswordChangeRequired();
    }

    @Test
    @DisplayName("POST /change-password → 200 on valid current password")
    void changePassword_validOld_updatesPassword() throws Exception {
        // given
        BaseUser user = new Student();
        user.setEmail("student@example.com");
        user.setPassword("ENC[oldPassword]");
        user.setPasswordChangeRequired(true);

        given(uds.findBaseUserByEmail("student@example.com")).willReturn(user);
        given(passwordEncoder.matches("oldPassword", "ENC[oldPassword]")).willReturn(true);
        given(passwordEncoder.encode("newPassword456")).willReturn("ENC[newPassword456]");
        given(userRepo.save(user)).willReturn(user);

        // when/then
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":"student@example.com",
                              "oldPassword":"oldPassword",
                              "newPassword":"newPassword456"
                            }
                        """))
                .andExpect(status().isOk());

        then(userRepo).should().save(user);
        assert !user.isPasswordChangeRequired();
        assert user.getPassword().equals("ENC[newPassword456]");
    }

    @Test
    @DisplayName("POST /change-password → 400 on invalid old password")
    void changePassword_invalidOld_returnsBadRequest() throws Exception {
        // given
        BaseUser user = new Student();
        user.setEmail("student@example.com");
        user.setPassword("ENC[oldPassword]");

        given(uds.findBaseUserByEmail("student@example.com")).willReturn(user);
        given(passwordEncoder.matches("wrongPassword", "ENC[oldPassword]")).willReturn(false);

        // when/then
        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":"student@example.com",
                              "oldPassword":"wrongPassword",
                              "newPassword":"newPassword456"
                            }
                        """))
                .andExpect(status().isBadRequest());
    }
}