package com.example.school_management.feature.academic.mapper;

import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AcademicMapper {

    /* ─────────────────────── ENTITY ➜ DTO ─────────────────────── */

    /* ---------- Class ---------- */
    @Mapping(target = "studentIds", source = "students", qualifiedByName = "studentIdSet")
    @Mapping(target = "courseIds",  source = "courses",  qualifiedByName = "courseIdSet")
    @Mapping(target = "teacherIds", source = "teachers", qualifiedByName = "teacherIdSet")
    @Mapping(target = "assignedRoomId", source = "assignedRoom.id")
    ClassDto toClassDto(ClassEntity entity);

    /* ---------- Course ---------- */
    @Mapping(target = "teacherId", source = "teacher.id")
    CourseDto toCourseDto(Course entity);



    /* ─────────────────────── UPDATE PATCHERS ───────────────────── */

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateClassEntity(UpdateClassRequest src, @MappingTarget ClassEntity target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "timetableSlots", ignore = true)
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "learningResources", ignore = true)
    void updateCourseEntity(UpdateCourseRequest src, @MappingTarget Course target);

    /* ─────────────────────── HELPERS ───────────────────────────── */

    @Named("studentIdSet")
    static Set<Long> mapStudents(Set<Student> students) {
        return students.stream().map(Student::getId).collect(Collectors.toSet());
    }

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
