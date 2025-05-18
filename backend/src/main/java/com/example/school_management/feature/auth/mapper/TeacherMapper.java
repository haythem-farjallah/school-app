package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.TeacherCreateDto;
import com.example.school_management.feature.auth.dto.TeacherDto;
import com.example.school_management.feature.auth.dto.TeacherUpdateDto;
import com.example.school_management.feature.auth.entity.Teacher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring",
        config = BaseUserMapper.class)
public interface TeacherMapper
        extends BaseUserMapper<
        Teacher,          // entity subtype
        TeacherCreateDto, // C
        TeacherUpdateDto,  // U
        TeacherDto> {

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
            @Mapping(target = "role",       constant = "TEACHER")
    })
    Teacher toEntity(TeacherCreateDto dto);

    @Override
    TeacherDto toDto(Teacher entity);
}