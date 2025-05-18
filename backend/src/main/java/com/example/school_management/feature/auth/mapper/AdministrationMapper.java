package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.AdministrationCreateDto;
import com.example.school_management.feature.auth.dto.AdministrationDto;
import com.example.school_management.feature.auth.dto.AdministrationUpdateDto;
import com.example.school_management.feature.auth.entity.Administration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring",
        config = BaseUserMapper.class)   // inherits toEntity / patch signature
public interface AdministrationMapper
        extends BaseUserMapper<Administration,
        AdministrationCreateDto,
        AdministrationUpdateDto,
        AdministrationDto> {

    @Override
    @Mappings({
            @Mapping(target = "firstName", source = "profile.firstName"),
            @Mapping(target = "lastName",  source = "profile.lastName"),
            @Mapping(target = "email",     source = "profile.email"),
            @Mapping(target = "telephone", source = "profile.telephone"),
            @Mapping(target = "birthday",  source = "profile.birthday"),
            @Mapping(target = "gender",    source = "profile.gender"),
            @Mapping(target = "address",   source = "profile.address"),
            @Mapping(target = "role",      constant = "ADMIN")      // hard-set role
    })
    Administration toEntity(AdministrationCreateDto dto);
}