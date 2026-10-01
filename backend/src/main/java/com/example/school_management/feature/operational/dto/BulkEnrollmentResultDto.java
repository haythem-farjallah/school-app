package com.example.school_management.feature.operational.dto;

import java.util.List;

public record BulkEnrollmentResultDto(
        int requestedStudents,
        int uniqueStudentsProcessed,
        int studentsEnrolled,
        int studentsFailed,
        int duplicatesIgnored,
        List<Long> enrolledStudentIds,
        List<Failure> failures) {
    public record Failure(Long studentId, String code, String message) {}
}
