package com.example.school_management.feature.operational.dto;

import lombok.Value;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

@Value
public class PeriodDto {
    Long id;
    String duration;
    @Min(1) Integer index;
    @NotNull LocalTime startTime;
    @NotNull LocalTime endTime;
} 