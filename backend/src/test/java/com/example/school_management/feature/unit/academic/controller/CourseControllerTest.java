package com.example.school_management.feature.unit.academic.controller;


import com.example.school_management.feature.academic.controller.CourseController;
import com.example.school_management.feature.academic.dto.CourseDto;
import com.example.school_management.feature.academic.dto.CreateCourseRequest;
import com.example.school_management.feature.academic.dto.UpdateCourseRequest;
import com.example.school_management.feature.academic.service.CourseService;
import com.example.school_management.commons.configs.JwtAuthenticationFilter;
import com.example.school_management.commons.configs.JwtTokenProvider;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockitoBean CourseService service;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoSpyBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCourse_returnsSuccess() throws Exception {
        var req = new CreateCourseRequest("Biology","green" ,2.0,5L);
        var dto = new CourseDto(10L, "Biology","green" ,1.5,5L);
        when(service.create(any(CreateCourseRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Biology"));

        verify(service).create(any(CreateCourseRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCourse_returnsUpdatedDto() throws Exception {
        var req = new UpdateCourseRequest("Chemistry", "red",2.0,1L);
        var dto = new CourseDto(11L, "Chemistry", "blue",1.5,7L);
        when(service.update(eq(11L), any(UpdateCourseRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/courses/11")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.name").value("Chemistry"));

        verify(service).update(eq(11L), any(UpdateCourseRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCourse_returnsDto() throws Exception {
        var dto = new CourseDto(12L, "Physics", "purple",1.5,3L);
        when(service.get(12L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/courses/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.name").value("Physics"));

        verify(service).get(12L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCourse_success() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/13").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(service).delete(13L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listCourses_returnsPage() throws Exception {
        var dto = new CourseDto(20L, "History", "red",2.0,2L);
        var page = new PageImpl<>(List.of(dto));
        when(service.list(PageRequest.of(0, 10), null, "His")).thenReturn(page);

        mockMvc.perform(get("/api/v1/courses")
                        .param("page", "0")
                        .param("size", "10")
                        .param("nameLike", "His"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(20))
                .andExpect(jsonPath("$.data.content[0].name").value("History"));

        verify(service).list(PageRequest.of(0, 10), null, "His");
    }
}
