package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.UserDto;
import com.example.school_management.feature.auth.entity.BaseUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role")
    @Mapping(target = "profileTheme", source = "profileSettings.theme")
    @Mapping(target = "profileLanguage", source = "profileSettings.language")
    UserDto toDto(BaseUser user);

}
