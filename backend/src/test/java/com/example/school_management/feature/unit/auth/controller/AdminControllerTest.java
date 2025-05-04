package com.example.school_management.feature.unit.auth.controller;

import com.example.school_management.commons.configs.JwtAuthenticationFilter;
import com.example.school_management.commons.configs.JwtTokenProvider;
import com.example.school_management.feature.auth.controller.AdminController;
import com.example.school_management.feature.auth.dto.CreateStudentWithParentsRequest;
import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDtoCreate;
import com.example.school_management.feature.auth.service.AdminService;
import com.example.school_management.commons.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminController.class
)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockitoBean AdminService   adminService;
    @MockitoBean EmailService   emailService;
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    private StudentDtoCreate baseStudent() {
        return new StudentDtoCreate(
                "Alice","Anderson","alice@example.com","555-1234",
                LocalDateTime.of(2010,1,1,0,0),"F","123 Maple St","5th",2020
        );
    }
    private ParentCreateDto baseParent() {
        return new ParentCreateDto(
                "Bob","Brown","bob@example.com","555-5678","email"
        );
    }

    @Test @DisplayName("POST /api/admin/students → 201 and service called")
    @WithMockUser(username="sysadmin", roles={"ADMIN"})
    void create_validPayload_201() throws Exception {
        var req = new CreateStudentWithParentsRequest(
                baseStudent(),
                List.of(baseParent())
        );

        mockMvc.perform(post("/api/admin/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        then(adminService).should().createStudentWithParents(req);
    }

    @Test @DisplayName("POST with no parents → 201 (controller doesn’t itself block)")
    @WithMockUser(username="sysadmin", roles={"ADMIN"})
    void create_zeroParents_201() throws Exception {
        var req = new CreateStudentWithParentsRequest(baseStudent(), List.of());

        mockMvc.perform(post("/api/admin/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        then(adminService).should().createStudentWithParents(req);
    }

    @Test @DisplayName("POST with invalid JSON → 400")
    @WithMockUser(username="sysadmin", roles={"ADMIN"})
    void create_malformedJson_400() throws Exception {
        mockMvc.perform(post("/api/admin/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"just a string\""))
                .andExpect(status().isBadRequest());

        then(adminService).should(never()).createStudentWithParents(Mockito.any());
    }
}
