package com.example.school_management.feature.auth.dto;

import com.example.school_management.feature.auth.entity.enums.StaffType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StaffUpdateDto implements BaseUserDtoMarker {
    BaseUserCreateDto profile;
    StaffType staffType;
    String department;
} 