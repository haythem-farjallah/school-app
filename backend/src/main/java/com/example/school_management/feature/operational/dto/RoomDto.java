package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.RoomType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Value
public class RoomDto {
    // Read-only fields
    @JsonIgnore
    Long id;
    @JsonIgnore
    Boolean isAssigned;
    // Write-only fields
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank String name;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Min(1) Integer capacity;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull RoomType roomType;
} 