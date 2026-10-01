package com.example.school_management.feature.academic.mapper;

import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.auth.entity.Teacher;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AcademicMapper {

    /* ─────────────────────── ENTITY ➜ DTO ─────────────────────── */

    /* ---------- Class ---------- */
    // The roster is not part of ClassEntity: it is the Class's ACTIVE Enrollments, resolved by the service.
    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "name", source = "entity.name")
    @Mapping(target = "yearOfStudy", source = "entity.yearOfStudy")
    @Mapping(target = "maxStudents", source = "entity.maxStudents")
    @Mapping(target = "studentIds", source = "studentIds")
    @Mapping(target = "courseIds",  source = "entity.courses",  qualifiedByName = "courseIdSet")
    @Mapping(target = "teacherIds", source = "entity.teachers", qualifiedByName = "teacherIdSet")
    @Mapping(target = "assignedRoomId", source = "entity.assignedRoom.id")
    ClassDto toClassDto(ClassEntity entity, Set<Long> studentIds);

    /* ---------- Course ---------- */
    @Mapping(target = "teacherId", source = "teacher.id")
    CourseDto toCourseDto(Course entity);



    /* ─────────────────────── UPDATE PATCHERS ───────────────────── */

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "academicYear", ignore = true)
    void updateClassEntity(UpdateClassRequest src, @MappingTarget ClassEntity target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "school", ignore = true)
    @Mapping(target = "timetableSlots", ignore = true)
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "learningResources", ignore = true)
    void updateCourseEntity(UpdateCourseRequest src, @MappingTarget Course target);

    /* ─────────────────────── HELPERS ───────────────────────────── */

    @Named("courseIdSet")
    static Set<Long> mapCourses(Set<Course> courses) {
        return courses.stream().map(Course::getId).collect(Collectors.toSet());
    }

    @Named("teacherIdSet")
    static Set<Long> mapTeachers(Set<Teacher> teachers) {
        return teachers.stream().map(Teacher::getId).collect(Collectors.toSet());
    }

    @Named("classEntityIdSet")
    static Set<Long> mapClasses(Set<ClassEntity> classes) {
        return classes.stream().map(ClassEntity::getId).collect(Collectors.toSet());
    }

    /* CARD ------------------------------------------------- */
    @Mapping(target = "studentCount",  expression = "java((int) stCnt)")  // cast required
    @Mapping(target = "courseCount",   expression = "java((int) crsCnt)")
    @Mapping(target = "teacherCount",  expression = "java((int) tchCnt)")
    ClassCardDto toCardDto(ClassEntity e,
                           long stCnt,
                           long crsCnt,
                           long tchCnt);

    /* DETAIL assignment row ------------------------------------- */
    default AssignmentDto toAssignmentDto(TeachingAssignment ta) {
        return new AssignmentDto(
                ta.getCourse().getId(),
                ta.getCourse().getName(),
                ta.getTeacher().getId(),
                ta.getTeacher().getFirstName() + " " + ta.getTeacher().getLastName(),
                ta.getWeeklyHours()
        );
    }


}
