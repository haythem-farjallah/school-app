package com.example.school_management.feature.auth.mapper;

import com.example.school_management.commons.dtos.RegisterRequest;
import com.example.school_management.feature.auth.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    @Mapping(target="id", ignore = true)
    @Mapping(target="permissions", ignore = true)
    @Mapping(target="profileSettings", ignore = true)
    Student toStudent(RegisterRequest rq);

    @Mapping(target="id", ignore = true)
    @Mapping(target="permissions", ignore = true)
    @Mapping(target="profileSettings", ignore = true)
    Teacher toTeacher(RegisterRequest rq);

    @Mapping(target="id", ignore = true)
    @Mapping(target="permissions", ignore = true)
    @Mapping(target="profileSettings", ignore = true)
    Parent toParent(RegisterRequest rq);

    @Mapping(target="id", ignore = true)
    @Mapping(target="permissions", ignore = true)
    @Mapping(target="profileSettings", ignore = true)
    Administration toAdmin(RegisterRequest rq);

    @Mapping(target="id", ignore = true)
    @Mapping(target="permissions", ignore = true)
    @Mapping(target="profileSettings", ignore = true)
    Staff toStaff(RegisterRequest rq);

}