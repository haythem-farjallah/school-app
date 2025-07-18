package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Value
public class EnrollmentDto {
    // Read-only fields for API responses
    Long id;
    String studentName;
    String studentEmail;
    String className;
    Integer gradeCount;
    LocalDate enrolledAt;
    EnrollmentStatus status;
    Float finalGrad;
    
    // Write-only fields for API requests
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull Long studentId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull Long classId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    LocalDate startDate;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    LocalDate endDate;
} 