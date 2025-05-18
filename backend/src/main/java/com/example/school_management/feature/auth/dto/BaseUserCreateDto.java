package com.example.school_management.feature.auth.dto;

import com.example.school_management.feature.auth.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Shared “person” payload used when an admin creates ANY user-type.
 * (Students need some extra fields, which you wrap in their own DTO.)
 *
 * You can call it either BaseUserCreateDto or keep your original
 * name UserProfileDTO — the code works the same; just be consistent.
 */
public record BaseUserCreateDto(

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank @Email
        String email,

        String telephone,

        LocalDate birthday,

        /**  e.g. "M", "F", "Non-binary" — keep it String or use an enum */
        String gender,

        @Size(max = 255)
        String address,

        /** For most roles you could hard-code the role in the service layer.
         But keeping it here lets you reuse the same DTO for custom roles
         without another wrapper. */
        UserRole role
) implements BaseUserDtoMarker { }