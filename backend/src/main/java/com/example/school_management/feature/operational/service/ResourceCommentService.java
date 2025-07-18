package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.CreateResourceCommentRequest;
import com.example.school_management.feature.operational.dto.ResourceCommentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResourceCommentService {
    
    ResourceCommentDto create(CreateResourceCommentRequest request);
    
    ResourceCommentDto get(Long id);
    
    void delete(Long id);
    
    Page<ResourceCommentDto> findByResourceId(Long resourceId, Pageable pageable);
    
    Page<ResourceCommentDto> findByUserId(Long userId, Pageable pageable);
    
    Page<ResourceCommentDto> list(Pageable pageable);
} 