package com.example.school_management.feature.unit.academic.controller;

import com.example.school_management.commons.configs.JwtAuthenticationFilter;
import com.example.school_management.commons.configs.JwtTokenProvider;
import com.example.school_management.feature.academic.controller.ClassController;
import com.example.school_management.feature.academic.dto.ClassDto;
import com.example.school_management.feature.academic.dto.CreateClassRequest;
import com.example.school_management.feature.academic.service.ClassService;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller‐level tests: request→JSON→controller→service, security gate enforced.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(ClassController.class)
class ClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private ClassService classService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoSpyBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    /* ───────────────────────── create ───────────────────────── */

    @Test
    @WithMockUser(roles = "ADMIN")
    void createClass_returnsSuccessJson() throws Exception {
        CreateClassRequest req = new CreateClassRequest("3-A");
        ClassDto dto = new ClassDto(1L, "3-A", null, null, Set.of(), Set.of(), Set.of(), null);

        when(classService.create(any(CreateClassRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/classes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.id", is(1)));

        verify(classService).create(any(CreateClassRequest.class));
    }

    /* ───────────────────────── list ─────────────────────────── */

    @Test
    @WithMockUser(roles = "ADMIN")
    void listClasses_withFilter() throws Exception {
        ClassDto dto = new ClassDto(3L, "Science-A", null, null, Set.of(), Set.of(), Set.of(), null);
        when(classService.list(any(PageRequest.class), anyString()))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/classes")
                        .param("page", "0")
                        .param("size", "10")
                        .param("nameLike", "Sci"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id", is(3)));

        verify(classService)
                .list(PageRequest.of(0, 10), "Sci");
    }

    /* ───────────────────────── add single student ───────────── */

    @Test
    @WithMockUser(roles = "ADMIN")
    void addStudent_callsServiceAndReturnsDto() throws Exception {
        ClassDto dto = new ClassDto(1L, "3-A", null, null, Set.of(99L), Set.of(), Set.of(), null);

        when(classService.addStudent(1L, 99L))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/classes/1/students/99").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentIds[0]", is(99)));

        verify(classService).addStudent(1L, 99L);
    }
}
