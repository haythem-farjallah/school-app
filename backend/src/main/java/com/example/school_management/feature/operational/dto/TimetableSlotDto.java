package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import jakarta.validation.constraints.NotNull;

@Value
public class TimetableSlotDto {
    // Read-only fields
    @JsonIgnore
    Long id;
    @JsonIgnore
    Integer periodIndex;
    @JsonIgnore
    String periodTime;
    @JsonIgnore
    Long classId;
    @JsonIgnore
    String className;
    @JsonIgnore
    Long courseId;
    @JsonIgnore
    String courseName;
    // Write-only fields
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull DayOfWeek dayOfWeek;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String description;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull Long periodId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    Long forClassId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    Long forCourseId;
} 