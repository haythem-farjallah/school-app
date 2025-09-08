package com.example.school_management.commons.controller;

import com.example.school_management.feature.auth.entity.BaseUser;

/**
 * Abstract mapper interface defining common mapping operations for user entities.
 * 
 * @param <T> Entity type (Teacher, Student, Staff, etc.)
 * @param <CreateDto> DTO for creation
 * @param <UpdateDto> DTO for updates  
 * @param <ResponseDto> DTO for responses
 */
public interface AbstractUserMapper<T extends BaseUser, CreateDto, UpdateDto, ResponseDto> {

    /**
     * Convert entity to response DTO
     */
    ResponseDto toDto(T entity);

    /**
     * Convert create DTO to entity
     */
    T toEntity(CreateDto createDto);

    /**
     * Update entity from update DTO
     */
    void updateEntityFromDto(UpdateDto updateDto, T entity);
}
