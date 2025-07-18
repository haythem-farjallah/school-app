package com.example.school_management.feature.operational.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Set;

@Value
public class TimetableDto {
    // Read-only fields
    @JsonIgnore
    Long id;
    @JsonIgnore
    LocalDateTime createdAt;
    @JsonIgnore
    LocalDateTime updatedAt;
    @JsonIgnore
    Integer totalSlots;
    @JsonIgnore
    Set<Long> classIds;
    @JsonIgnore
    Set<Long> teacherIds;
    @JsonIgnore
    Set<Long> roomIds;
    
    // Write-only fields
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Timetable name is required")
    String name;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String description;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Academic year is required")
    String academicYear;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Semester is required")
    String semester;
} 