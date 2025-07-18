package com.example.school_management.feature.operational.dto;

import lombok.Value;

@Value
public class EnrollmentStatsDto {
    Long totalEnrollments;
    Long activeEnrollments;
    Long pendingEnrollments;
    Long completedEnrollments;
    Long droppedEnrollments;
    Double completionRate;
    Double averageFinalGrade;
} 