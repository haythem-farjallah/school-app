package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.operational.dto.TimetableResponseDto;
import com.example.school_management.feature.operational.service.StudentTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Timetables")
@SecurityRequirement(name = "bearerAuth")
public class StudentTimetableController {
    private final StudentTimetableService studentTimetableService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated student's current class timetable")
    public ResponseEntity<ApiSuccessResponse<TimetableResponseDto>> getMyTimetable() {
        return ResponseEntity.ok(new ApiSuccessResponse<>(
                "Student timetable retrieved successfully", studentTimetableService.getMyTimetable()));
    }
}
