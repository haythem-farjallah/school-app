package com.example.school_management.feature.academic.dto;

import com.example.school_management.commons.json.JsonResource;
import com.fasterxml.jackson.annotation.JsonFilter;

@JsonFilter("fieldFilter")
@JsonResource("assignment")
public record AssignmentDto(
        Long   courseId,
        String courseName,
        Long   teacherId,
        String teacherName,
        Integer weeklyHours) { }
