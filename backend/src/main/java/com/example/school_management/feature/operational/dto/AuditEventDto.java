package com.example.school_management.feature.operational.dto;

import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import jakarta.validation.constraints.NotNull;

@Value
public class AuditEventDto {
    // Read-only fields (for responses)
    @JsonIgnore
    Long id;
    @JsonIgnore
    Long actedById;
    @JsonIgnore
    String actedByEmail;
    @JsonIgnore
    String ipAddress;
    @JsonIgnore
    String userAgent;
    // Write-only fields (for requests)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull AuditEventType eventType;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String summary;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String details;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String entityType;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    Long entityId;
} 