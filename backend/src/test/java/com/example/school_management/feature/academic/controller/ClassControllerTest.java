package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.exceptions.GlobalExceptionHandler;
import com.example.school_management.feature.academic.service.ClassService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The controller and GlobalExceptionHandler without the security chain: a failing
 * ClassService is reported as an error, not as an empty page.
 */
class ClassControllerTest {

    private final ClassService service = mock(ClassService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ClassController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void currentTeacherClassesFailureIsAnErrorNotAnEmptyPage() throws Exception {
        when(service.getCurrentTeacherClasses(any())).thenThrow(new DataAccessResourceFailureException("connection refused"));

        mockMvc.perform(get("/api/v1/classes/teacher/me"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("UNEXPECTED_ERROR"))
                .andExpect(jsonPath("$.instance").value("/api/v1/classes/teacher/me"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
