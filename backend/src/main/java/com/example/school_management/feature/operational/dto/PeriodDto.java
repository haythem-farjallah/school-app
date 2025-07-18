package com.example.school_management.feature.operational.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

@Value
public class PeriodDto {
    // Read-only fields
    @JsonIgnore
    Long id;
    @JsonIgnore
    String duration;
    // Write-only fields
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Min(1) Integer index;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull LocalTime startTime;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull LocalTime endTime;
} 