package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.ParentDto;
import com.example.school_management.feature.auth.dto.ParentUpdateDto;
import com.example.school_management.feature.auth.entity.Parent;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        config = BaseUserMapper.class)
public interface ParentMapper
        extends BaseUserMapper<
        Parent,          // entity subtype
        ParentCreateDto, // C
        ParentUpdateDto, // U
        ParentDto> {     // R

    /* Extra field mappings that don't match 1-to-1 names */

    @Override
    @Mappings({
            @Mapping(target = "firstName",  source = "profile.firstName"),
            @Mapping(target = "lastName",   source = "profile.lastName"),
            @Mapping(target = "email",      source = "profile.email"),
            @Mapping(target = "telephone",  source = "profile.telephone"),
            @Mapping(target = "birthday",   source = "profile.birthday"),
            @Mapping(target = "gender",     source = "profile.gender"),
            @Mapping(target = "address",    source = "profile.address"),
            @Mapping(target = "role",       constant = "PARENT")
    })
    Parent toEntity(ParentCreateDto dto);

    @Override
    ParentDto toDto(Parent entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patch(ParentUpdateDto dto, @MappingTarget Parent entity);
} 