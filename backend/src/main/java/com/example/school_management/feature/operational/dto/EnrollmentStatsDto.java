package com.example.school_management.feature.operational.dto;

import lombok.Value;

@Value
public class EnrollmentStatsDto {
    Long totalEnrollments;
    Long activeEnrollments;
    Long completedEnrollments;
    Long transferredEnrollments;
    Long withdrawnEnrollments;
    Double completionRate;
    Double averageFinalGrade;
} 