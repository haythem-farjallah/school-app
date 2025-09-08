package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.TeacherCreateDto;
import com.example.school_management.feature.auth.dto.TeacherDto;
import com.example.school_management.feature.auth.dto.TeacherUpdateDto;
import com.example.school_management.feature.auth.entity.Teacher;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TeacherMapper
        extends BaseUserMapper<
        Teacher,          // entity subtype
        TeacherCreateDto, // C
        TeacherUpdateDto,  // U
        TeacherDto> {     // R

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
            @Mapping(target = "role",      constant = "TEACHER"),
            @Mapping(target = "qualifications", source = "qualifications"),
            @Mapping(target = "subjectsTaught", source = "subjectsTaught"),
            @Mapping(target = "weeklyCapacity", source = "availableHours"),
            @Mapping(target = "schedulePreferences", source = "schedulePreferences")
    })
    Teacher toEntity(TeacherCreateDto dto);

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
            @Mapping(target = "qualifications", source = "qualifications"),
            @Mapping(target = "subjectsTaught", source = "subjectsTaught"),
            @Mapping(target = "availableHours", source = "weeklyCapacity"),
            @Mapping(target = "schedulePreferences", source = "schedulePreferences")
    })
    TeacherDto toDto(Teacher entity);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mappings({
            @Mapping(target = "telephone", source = "telephone"),
            @Mapping(target = "address", source = "address"),
            @Mapping(target = "qualifications", source = "qualifications"),
            @Mapping(target = "subjectsTaught", source = "subjectsTaught"),
            @Mapping(target = "weeklyCapacity", source = "availableHours"),
            @Mapping(target = "schedulePreferences", source = "schedulePreferences")
    })
    void patch(TeacherUpdateDto dto, @MappingTarget Teacher entity);
}