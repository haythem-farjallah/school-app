package com.example.school_management.feature.unit.academic.controller;

import com.example.school_management.commons.configs.JwtAuthenticationFilter;
import com.example.school_management.commons.configs.JwtTokenProvider;
import com.example.school_management.feature.academic.controller.LevelController;
import com.example.school_management.feature.academic.dto.BatchIdsRequest;
import com.example.school_management.feature.academic.dto.CreateLevelRequest;
import com.example.school_management.feature.academic.dto.LevelDto;
import com.example.school_management.feature.academic.service.LevelService;
import com.example.school_management.feature.auth.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LevelController.class)
class LevelControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockitoBean LevelService service;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoSpyBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createLevel_returnsDto() throws Exception {
        var req = new CreateLevelRequest("Grade 1");
        var dto = new LevelDto(1L, "Grade 1", Set.of());
        when(service.create(any(CreateLevelRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/levels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Grade 1"));

        verify(service).create(any(CreateLevelRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listLevels_returnsPage() throws Exception {
        var dto = new LevelDto(2L, "Grade 2", Set.of());
        var page = new PageImpl<>(List.of(dto));
        when(service.list(PageRequest.of(0, 10), "Grad")).thenReturn(page);

        mockMvc.perform(get("/api/v1/levels")
                        .param("page", "0")
                        .param("size", "10")
                        .param("nameLike", "Grad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("Grade 2"));

        verify(service).list(PageRequest.of(0, 10), "Grad");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void batchCourses_updatesAndReturnsLevel() throws Exception {
        // stub to return courseIds = [5,6]
        var body = new BatchIdsRequest(BatchIdsRequest.Operation.ADD, Set.of(5L, 6L));
        var dto = new LevelDto(3L, "Grade 3", Set.of(5L, 6L));
        when(service.mutateCourses(eq(3L), any(BatchIdsRequest.class))).thenReturn(dto);

        mockMvc.perform(patch("/api/v1/levels/3/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseIds", containsInAnyOrder(5, 6)));

        verify(service).mutateCourses(eq(3L), any(BatchIdsRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addAndRemoveCourse_single() throws Exception {
        // ADD case: stub to return one courseId = 7
        var addDto = new LevelDto(4L, "Grade 4", Set.of(7L));
        when(service.addCourse(4L, 7L)).thenReturn(addDto);

        mockMvc.perform(post("/api/v1/levels/4/courses/7").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseIds[0]").value(7));

        verify(service).addCourse(4L, 7L);

        // REMOVE case: stub to return empty set
        var removeDto = new LevelDto(4L, "Grade 4", Set.of());
        when(service.removeCourse(4L, 7L)).thenReturn(removeDto);

        mockMvc.perform(delete("/api/v1/levels/4/courses/7").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseIds").isEmpty());

        verify(service).removeCourse(4L, 7L);
    }
}
