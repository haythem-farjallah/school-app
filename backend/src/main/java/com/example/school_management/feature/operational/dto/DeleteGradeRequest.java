package com.example.school_management.feature.operational.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeleteGradeRequest {
    
    @NotBlank(message = "Deletion reason is required")
    @Size(min = 10, max = 500, message = "Deletion reason must be between 10 and 500 characters")
    private String reason;
    
    @Size(max = 1000, message = "Additional notes cannot exceed 1000 characters")
    private String additionalNotes;
    
    private boolean notifyStudent = false;
    private boolean notifyParent = false;
} 