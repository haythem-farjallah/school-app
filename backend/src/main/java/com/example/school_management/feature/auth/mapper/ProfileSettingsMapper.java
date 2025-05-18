package com.example.school_management.feature.auth.mapper;


import com.example.school_management.feature.auth.dto.ProfileSettingsDto;
import com.example.school_management.feature.auth.entity.ProfileSettings;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProfileSettingsMapper {
    ProfileSettingsMapper MAPPER = Mappers.getMapper(ProfileSettingsMapper.class);

    ProfileSettingsDto toDto(ProfileSettings settings);

    /** patch non-null values from DTO into existing entity */
    void patch(ProfileSettingsDto dto, @MappingTarget ProfileSettings settings);
}