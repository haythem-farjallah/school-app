package com.example.school_management.feature.academic.dto;

import com.example.school_management.feature.academic.entity.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.util.Set;

@Value
public class CreateLearningResourceRequest {
    @NotBlank
    String title;
    
    String description;
    
    @NotBlank
    String url;
    
    @NotNull
    ResourceType type;
    
    String thumbnailUrl;
    
    Integer duration;
    
    Boolean isPublic = true;
    
    Set<Long> teacherIds;
    
    Set<Long> classIds;
    
    Set<Long> courseIds;
} 