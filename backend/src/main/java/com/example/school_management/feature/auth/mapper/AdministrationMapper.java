package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.AdministrationCreateDto;
import com.example.school_management.feature.auth.dto.AdministrationDto;
import com.example.school_management.feature.auth.dto.AdministrationUpdateDto;
import com.example.school_management.feature.auth.entity.Administration;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface AdministrationMapper extends BaseUserMapper<Administration, AdministrationCreateDto, AdministrationUpdateDto, AdministrationDto> {

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
            @Mapping(target = "role",      source = "profile.role"),
            @Mapping(target = "password",  source = "password"),
            @Mapping(target = "department", source = "department"),
            @Mapping(target = "jobTitle",   source = "jobTitle")
    })
    Administration toEntity(AdministrationCreateDto dto);

    @Override
    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "firstName", source = "firstName"),
            @Mapping(target = "lastName", source = "lastName"),
            @Mapping(target = "email", source = "email"),
            @Mapping(target = "telephone", source = "telephone"),
            @Mapping(target = "department", source = "department"),
            @Mapping(target = "jobTitle", source = "jobTitle")
    })
    AdministrationDto toDto(Administration entity);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "telephone", source = "telephone"),
            @Mapping(target = "address", source = "address"),
            @Mapping(target = "department", source = "department"),
            @Mapping(target = "jobTitle", source = "jobTitle")
    })
    void patch(AdministrationUpdateDto dto, @MappingTarget Administration entity);
}