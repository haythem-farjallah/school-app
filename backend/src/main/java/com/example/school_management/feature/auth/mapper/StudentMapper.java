package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.StudentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDto;
import com.example.school_management.feature.auth.dto.StudentUpdateDto;
import com.example.school_management.feature.auth.entity.Student;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        config = BaseUserMapper.class)
public interface StudentMapper
        extends BaseUserMapper<
        Student,          // entity subtype
        StudentCreateDto, // C
        StudentUpdateDto, // U
        StudentDto> {     // R

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
            @Mapping(target = "role",       constant = "STUDENT")
    })
    Student toEntity(StudentCreateDto dto);

    @Override
    StudentDto toDto(Student entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void patch(StudentUpdateDto dto, @MappingTarget Student entity);

}
