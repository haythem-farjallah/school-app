package com.example.school_management.feature.academic.mapper;

import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.auth.entity.Student;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AcademicMapper {

    /* ─────────────────────── ENTITY ➜ DTO ─────────────────────── */

    /* ---------- Class ---------- */
    @Mapping(target = "levelId",    source = "level.id")
    @Mapping(target = "studentIds", source = "students", qualifiedByName = "studentIdSet")
    @Mapping(target = "courseIds",  source = "courses",  qualifiedByName = "courseIdSet")
    @Mapping(target = "scheduleId", source = "schedule.id")
    ClassDto toClassDto(ClassEntity entity);

    /* ---------- Course ---------- */
    @Mapping(target = "teacherId", source = "teacher.id")
    CourseDto toCourseDto(Course entity);

    /* ---------- Level ---------- */
    @Mapping(target = "courseIds", source = "courses", qualifiedByName = "courseIdSet")
    LevelDto toLevelDto(Level entity);

    /* ─────────────────────── UPDATE PATCHERS ───────────────────── */

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateClassEntity(UpdateClassRequest src, @MappingTarget ClassEntity target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCourseEntity(UpdateCourseRequest src, @MappingTarget Course target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateLevelEntity(UpdateLevelRequest  src, @MappingTarget Level target);

    /* ─────────────────────── HELPERS ───────────────────────────── */

    @Named("studentIdSet")
    static Set<Long> mapStudents(Set<Student> students) {
        return students.stream().map(Student::getId).collect(Collectors.toSet());
    }

    @Named("courseIdSet")
    static Set<Long> mapCourses(Set<Course> courses) {
        return courses.stream().map(Course::getId).collect(Collectors.toSet());
    }

    @Named("classEntityIdSet")
    static Set<Long> mapClasses(Set<ClassEntity> classes) {
        return classes.stream().map(ClassEntity::getId).collect(Collectors.toSet());
    }
}
