package com.example.school_management.feature.academic.mapper;

import com.example.school_management.feature.academic.dto.LearningResourceDto;
import com.example.school_management.feature.academic.entity.LearningResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface LearningResourceMapper {

    @Mapping(target = "teacherIds", expression = "java(mapTeacherIds(entity))")
    @Mapping(target = "classIds", expression = "java(mapClassIds(entity))")
    @Mapping(target = "courseIds", expression = "java(mapCourseIds(entity))")
    @Mapping(target = "commentCount", expression = "java(entity.getComments() != null ? entity.getComments().size() : 0)")
    @Mapping(target = "viewCount", source = "viewCount")
    @Mapping(target = "downloadCount", source = "downloadCount")
    LearningResourceDto toDto(LearningResource entity);

    default Set<Long> mapTeacherIds(LearningResource entity) {
        if (entity.getCreatedBy() == null) return Set.of();
        return entity.getCreatedBy().stream()
                .map(teacher -> teacher.getId())
                .collect(Collectors.toSet());
    }

    default Set<Long> mapClassIds(LearningResource entity) {
        if (entity.getTargetClasses() == null) return Set.of();
        return entity.getTargetClasses().stream()
                .map(classEntity -> classEntity.getId())
                .collect(Collectors.toSet());
    }

    default Set<Long> mapCourseIds(LearningResource entity) {
        if (entity.getTargetCourses() == null) return Set.of();
        return entity.getTargetCourses().stream()
                .map(course -> course.getId())
                .collect(Collectors.toSet());
    }
} 