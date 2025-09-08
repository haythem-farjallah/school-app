package com.example.school_management.feature.operational.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO containing the results of an auto-enrollment process
 */
public record AutoEnrollmentResultDto(
    boolean success,
    String message,
    int totalStudentsProcessed,
    int studentsEnrolled,
    int studentsAlreadyEnrolled,
    int classesCreated,
    int classesUsed,
    LocalDateTime processedAt,
    Map<String, Integer> enrollmentsByGradeLevel,
    List<String> createdClasses,
    List<String> errors,
    boolean isPreview
) {}
