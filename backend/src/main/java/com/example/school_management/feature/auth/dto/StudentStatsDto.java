package com.example.school_management.feature.auth.dto;

import java.util.Map;

public record StudentStatsDto(
    long totalStudents,
    long activeStudents,
    long suspendedStudents,
    Map<String, Long> studentsByGradeLevel,
    Map<Integer, Long> studentsByEnrollmentYear
) {} 