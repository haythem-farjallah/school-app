package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.TransferStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Value
public class TransferDto {
    // Read-only fields
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Long id;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String studentName;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    String studentEmail;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    LocalDateTime requestedAt;
    // Read-write field
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    TransferStatus status;
    // Write-only fields
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull Long studentId;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String reason;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    LocalDateTime effectiveAt;
} 