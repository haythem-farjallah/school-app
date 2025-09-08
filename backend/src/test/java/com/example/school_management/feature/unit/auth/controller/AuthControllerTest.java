/*package com.example.school_management.feature.unit.auth.controller;

import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.controller.AuthController;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.service.AuthService;
import com.example.school_management.feature.auth.service.OtpService;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
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
    private BaseUserRepository userRepo;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /forgot-password → 200 and OTP sent")
    void forgotPassword_sendsOtp() throws Exception {
        BaseUser u = new Student(); u.setEmail("a@b.com");
        given(uds.findBaseUserByEmail("a@b.com")).willReturn(u);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\"}"))
                .andDo(print())
                .andExpect(status().isOk());

        then(otpService).should().generateAndSendOtp(u);
    }

    @Test
    @DisplayName("POST /reset-password → 200 and password reset")
    void resetPassword_validOtp_resetsPassword() throws Exception {
        BaseUser u = new Student(); u.setEmail("x@y.com");
        given(uds.findBaseUserByEmail("x@y.com")).willReturn(u);
        willDoNothing().given(otpService).validateOtp(u, "123456");
        given(passwordEncoder.encode("newpass")).willReturn("ENC[newpass]");
        // ← **FIX**: save(...) returns the saved entity, so stub it to return `u`
        given(userRepo.save(u)).willReturn(u);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":"x@y.com",
                              "otp":"123456",
                              "newPassword":"newpass"
                            }
                        """))
                .andExpect(status().isOk());

        then(userRepo).should().save(u);
        assert !u.isPasswordChangeRequired();
    }

    @Test
    @DisplayName("POST /change-password → 200 on valid current password")
    void changePassword_validOld_updatesPassword() throws Exception {
        BaseUser u = new Student();
        u.setEmail("u@v.com");
        u.setPassword("ENC[old]");
        u.setPasswordChangeRequired(true);

        given(uds.findBaseUserByEmail("u@v.com")).willReturn(u);
        given(passwordEncoder.matches("oldpass", "ENC[old]")).willReturn(true);
        given(passwordEncoder.encode("newpass")).willReturn("ENC[new]");
        // ← **FIX**: same here
        given(userRepo.save(u)).willReturn(u);

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email":"u@v.com",
                              "oldPassword":"oldpass",
                              "newPassword":"newpass"
                            }
                        """))
                .andExpect(status().isOk());

        then(userRepo).should().save(u);
        assert !u.isPasswordChangeRequired();
        assert u.getPassword().equals("ENC[new]");
    }
}
*/