package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.StaffCreateDto;
import com.example.school_management.feature.auth.dto.StaffDto;
import com.example.school_management.feature.auth.dto.StaffUpdateDto;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.Permission;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface StaffMapper
        extends BaseUserMapper<
        Staff,          // entity subtype
        StaffCreateDto, // C
        StaffUpdateDto, // U
        StaffDto> {     // R

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "firstName", source = "profile.firstName"),
            @Mapping(target = "lastName",  source = "profile.lastName"),
            @Mapping(target = "email",     source = "profile.email"),
            @Mapping(target = "telephone", source = "profile.telephone"),
            @Mapping(target = "birthday",  source = "profile.birthday"),
            @Mapping(target = "gender",    source = "profile.gender"),
            @Mapping(target = "address",   source = "profile.address"),
            @Mapping(target = "role",      constant = "STAFF"),
            @Mapping(target = "staffType", source = "staffType"),
            @Mapping(target = "department", source = "department")
    })
    Staff toEntity(StaffCreateDto dto);

    @Override
    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "firstName", source = "firstName"),
            @Mapping(target = "lastName", source = "lastName"),
            @Mapping(target = "email", source = "email"),
            @Mapping(target = "telephone", source = "telephone"),
            @Mapping(target = "birthday", source = "birthday"),
            @Mapping(target = "gender", source = "gender"),
            @Mapping(target = "address", source = "address"),
            @Mapping(target = "image", source = "image"),
            @Mapping(target = "staffType", source = "staffType"),
            @Mapping(target = "department", source = "department"),
            @Mapping(target = "permissions", source = "permissions", qualifiedByName = "permissionsToStrings"),
            @Mapping(target = "role", source = "role"),
            @Mapping(target = "isEmailVerified", source = "isEmailVerified"),
            @Mapping(target = "createdAt", source = "createdAt"),
            @Mapping(target = "updatedAt", source = "updatedAt")
    })
    StaffDto toDto(Staff entity);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "firstName", source = "profile.firstName"),
            @Mapping(target = "lastName", source = "profile.lastName"),
            @Mapping(target = "email", source = "profile.email"),
            @Mapping(target = "telephone", source = "profile.telephone"),
            @Mapping(target = "birthday", source = "profile.birthday"),
            @Mapping(target = "gender", source = "profile.gender"),
            @Mapping(target = "address", source = "profile.address"),
            @Mapping(target = "staffType", source = "staffType"),
            @Mapping(target = "department", source = "department")
    })
    void patch(StaffUpdateDto dto, @MappingTarget Staff entity);

    @Named("permissionsToStrings")
    default Set<String> permissionsToStrings(Set<Permission> permissions) {
        if (permissions == null) {
            return null;
        }
        return permissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }
} 