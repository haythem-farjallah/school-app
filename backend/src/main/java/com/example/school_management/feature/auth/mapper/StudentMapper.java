package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.StudentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDto;
import com.example.school_management.feature.auth.dto.StudentUpdateDto;
import com.example.school_management.feature.auth.entity.Student;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface StudentMapper {
    StudentDto toDto(Student s);

    @Mapping(target="id", ignore=true)
    @Mapping(target="password", ignore=true)     // we handle that in service
    @Mapping(target="permissions", ignore=true)
    @Mapping(target="profileSettings", ignore=true)
    Student toEntity(StudentCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(StudentUpdateDto dto, @MappingTarget Student entity);
}
