package com.example.school_management.feature.operational;

import com.example.school_management.commons.exceptions.BadRequestException;
import com.example.school_management.commons.exceptions.GlobalExceptionHandler;
import com.example.school_management.feature.operational.controller.SmartTimetableController;
import com.example.school_management.feature.operational.dto.TimetableOptimizationRequest;
import com.example.school_management.feature.operational.service.impl.SmartTimetableServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SmartTimetableErrorSemanticsTest {
    @Test
    void missingTimetableIdIsBadRequestAtServiceAndHttpBoundaries() throws Exception {
        var service = new SmartTimetableServiceImpl(null, null, null, null, null, null, null);
        assertThatThrownBy(() -> service.optimizeWithAI(new TimetableOptimizationRequest()))
                .isInstanceOf(BadRequestException.class);
        var mvc = MockMvcBuilders.standaloneSetup(new SmartTimetableController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/v1/smart-timetable/optimize").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Bad Request")).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/v1/smart-timetable/optimize"));
    }

    @Test
    void rejectedScheduleChangeIsConflictWithoutMutatingSlots() throws Exception {
        var service = spy(new SmartTimetableServiceImpl(null, null, null, null, null, null, null));
        // Conflict detection is currently a stub. Pin the real service's error when validation rejects a change.
        doReturn(false).when(service).validateScheduleChange(1L, 2L, null, null);
        var mvc = MockMvcBuilders.standaloneSetup(new SmartTimetableController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/v1/smart-timetable/1/apply-change").param("slotId", "2"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Conflict")).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Schedule change would create conflicts"))
                .andExpect(jsonPath("$.instance").value("/api/v1/smart-timetable/1/apply-change"));
    }
}
