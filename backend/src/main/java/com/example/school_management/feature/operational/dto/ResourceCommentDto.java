package com.example.school_management.feature.operational.dto;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class ResourceCommentDto {
    Long id;
    String content;
    Long resourceId;
    String resourceTitle;
    Long commentedById;
    String commentedByName;
    LocalDateTime createdAt;
} 