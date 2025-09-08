package com.example.school_management.feature.operational.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TimetableResponseDto {
    private Long id;
    private String name;
    private String description;
    private String academicYear;
    private String semester;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TimetableSlotResponseDto> slots;
}

