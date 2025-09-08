package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.UserDto;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role")
    @Mapping(target = "profileTheme", source = "profileSettings.theme")
    @Mapping(target = "profileLanguage", source = "profileSettings.language")
    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "permCodes")
    UserDto toDto(BaseUser user);

    @Named("permCodes")
    default Set<String> mapPermissions(Set<Permission> permissions) {
        if (permissions == null) return null;
        return permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
    }

}
