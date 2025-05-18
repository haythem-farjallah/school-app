package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.*;
import com.example.school_management.feature.auth.entity.Worker;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;


@Mapper(componentModel = "spring",
        config = BaseUserMapper.class)
public interface WorkerMapper
        extends BaseUserMapper<
        Worker,          // entity subtype
        WorkerCreateDto, // C
        WorkerUpdateDto,  // U
        WorkerDto> {

    /* Extra field mappings that don't match 1-to-1 names */

    @Override
    @Mappings({
            @Mapping(target = "firstName", source = "profile.firstName"),
            @Mapping(target = "lastName",  source = "profile.lastName"),
            @Mapping(target = "email",     source = "profile.email"),
            @Mapping(target = "telephone", source = "profile.telephone"),
            @Mapping(target = "birthday",  source = "profile.birthday"),
            @Mapping(target = "gender",    source = "profile.gender"),
            @Mapping(target = "address",   source = "profile.address"),
            @Mapping(target = "role",      constant = "WORKER")
    })
    Worker toEntity(WorkerCreateDto dto);

    @Override
    WorkerDto toDto(Worker entity);
}