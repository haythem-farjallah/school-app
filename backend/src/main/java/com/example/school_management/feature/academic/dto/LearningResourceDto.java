package com.example.school_management.feature.academic.dto;

import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Set;

@Value
public class LearningResourceDto {
    Long id;
    String title;
    String description;
    String url;
    ResourceType type;
    String thumbnailUrl;
    Integer duration;
    Boolean isPublic;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @JsonProperty("teacherIds")
    Set<Long> teacherIds;
    
    @JsonProperty("classIds")
    Set<Long> classIds;
    
    @JsonProperty("courseIds")
    Set<Long> courseIds;
    
    @JsonProperty("commentCount")
    Integer commentCount;
} 