package com.example.school_management.feature.operational.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.time.LocalDateTime;

@Value
public class GradeDto {
    // Read-only fields
    @JsonIgnore
    Long id;
    @JsonIgnore
    String studentName;
    @JsonIgnore
    String className;
    @JsonIgnore
    String assignedByName;
    @JsonIgnore
    LocalDateTime gradedAt;
    // Write-only fields
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull Long enrollmentId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String content;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull @DecimalMin("0.0") @DecimalMax("20.0") Float score;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    Float weight;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull Long assignedById;
} 