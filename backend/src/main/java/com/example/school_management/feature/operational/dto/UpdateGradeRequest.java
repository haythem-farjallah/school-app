package com.example.school_management.feature.operational.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateGradeRequest {
    
    @DecimalMin(value = "0.0", message = "Score must be between 0 and 20")
    @DecimalMax(value = "20.0", message = "Score must be between 0 and 20")
    private Float score;
    
    @DecimalMin(value = "0.1", message = "Weight must be greater than 0")
    @DecimalMax(value = "10.0", message = "Weight cannot exceed 10")
    private Float weight;
    
    @Size(max = 500, message = "Comment cannot exceed 500 characters")
    private String comment;
    
    @Size(max = 200, message = "Reason cannot exceed 200 characters")
    private String updateReason;
} 