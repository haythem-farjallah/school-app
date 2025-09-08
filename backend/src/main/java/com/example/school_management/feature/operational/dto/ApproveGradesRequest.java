package com.example.school_management.feature.operational.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ApproveGradesRequest {
    
    @NotEmpty(message = "Student IDs list cannot be empty")
    private List<Long> studentIds;
    
    @NotNull(message = "Semester is required")
    private CreateEnhancedGradeRequest.Semester semester;
    
    @NotNull(message = "Approved by is required")
    private String approvedBy;
}
