package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.ParentDto;
import com.example.school_management.feature.auth.dto.ParentUpdateDto;
import com.example.school_management.feature.auth.entity.Parent;
import com.example.school_management.feature.auth.entity.enums.ContactMethod;
import org.mapstruct.*;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
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
            @Mapping(target = "role",       constant = "PARENT"),
            @Mapping(target = "preferredContactMethod", source = "preferredContactMethod", qualifiedByName = "stringToContactMethod")
    })
    Parent toEntity(ParentCreateDto dto);

    @Override
    @Mappings({
        @Mapping(target = "children", source = "children")
    })
    ParentDto toDto(Parent entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
        @Mapping(target = "preferredContactMethod", source = "preferredContactMethod", qualifiedByName = "stringToContactMethod"),
        @Mapping(target = "relation", source = "relation", qualifiedByName = "stringToRelation"),
        @Mapping(target = "children", ignore = true) // Handle children assignment in service layer
    })
    void patch(ParentUpdateDto dto, @MappingTarget Parent entity);

    /**
     * Convert String to ContactMethod enum
     */
    @Named("stringToContactMethod")
    default ContactMethod stringToContactMethod(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return ContactMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Convert String to Relation enum
     */
    @Named("stringToRelation")
    default com.example.school_management.feature.auth.entity.enums.Relation stringToRelation(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return com.example.school_management.feature.auth.entity.enums.Relation.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
} 