package com.example.school_management.feature.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO used by the admin API to create a {@code Parent}.
 *
 *  • All "person" fields live in the nested {@link BaseUserCreateDto}.
 *  • Parent-specific fields are added beside it.
 *  • Record = immutable → safe to expose in controller layer.
 */
public record ParentCreateDto(

        @NotNull @Valid
        BaseUserCreateDto profile,          // firstName, lastName, email, …

        String preferredContactMethod,      // e.g. "email", "phone", "sms"
        
        String relation,                    // e.g. "FATHER", "MOTHER", "GUARDIAN"
        
        List<String> childrenEmails        // List of student emails to assign as children

) implements BaseUserDtoMarker { }
