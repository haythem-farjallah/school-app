package com.example.school_management.feature.academic.dto;

import com.example.school_management.feature.academic.entity.enums.ResourceType;
import lombok.Value;

import java.util.Set;

@Value
public class UpdateLearningResourceRequest {
    String title;
    String description;
    String url;
    ResourceType type;
    String thumbnailUrl;
    Integer duration;
    Boolean isPublic;
    String status;
    Set<Long> teacherIds;
    Set<Long> classIds;
    Set<Long> courseIds;
} 