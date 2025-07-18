package com.example.school_management.feature.auth.dto;

import com.example.school_management.feature.auth.entity.enums.StaffType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * DTO used by the admin API to create a {@code Staff}.
 *
 *  • All "person" fields live in the nested {@link BaseUserCreateDto}.
 *  • Staff-specific fields are added beside it.
 *  • Record = immutable → safe to expose in controller layer.
 */
public record StaffCreateDto(

        @NotNull @Valid
        BaseUserCreateDto profile,          // firstName, lastName, email, …

        StaffType staffType,                // e.g. "MAINTENANCE", "SECURITY"
        
        String department                   // e.g. "IT", "FINANCE"

) implements BaseUserDtoMarker { } 