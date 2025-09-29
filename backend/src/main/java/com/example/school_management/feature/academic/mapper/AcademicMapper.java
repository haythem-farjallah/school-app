package com.example.school_management.feature.academic.mapper;

import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface AcademicMapper {

    @Mapping(target = "studentIds", source = "enrollments", qualifiedByName = "enrollmentStudentIdSet")
    @Mapping(target = "courseIds",  source = "courses",  qualifiedByName = "courseIdSet")
    @Mapping(target = "teacherIds", source = "teachers", qualifiedByName = "teacherIdSet")
    @Mapping(target = "assignedRoomId", source = "assignedRoom.id")
    ClassDto toClassDto(ClassEntity entity);

    @Mapping(target = "teacherId", source = "teacher.id")
    CourseDto toCourseDto(Course entity);




    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateClassEntity(UpdateClassRequest src, @MappingTarget ClassEntity target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "teacher", ignore = true)
    @Mapping(target = "timetableSlots", ignore = true)
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "learningResources", ignore = true)
    void updateCourseEntity(UpdateCourseRequest src, @MappingTarget Course target);


    @Named("studentIdSet")
    static Set<Long> mapStudents(Set<Student> students) {
        return students.stream().map(Student::getId).collect(Collectors.toSet());
    }

    @Named("enrollmentStudentIdSet")
    static Set<Long> mapEnrollmentStudents(Set<Enrollment> enrollments) {
        return enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .map(e -> e.getStudent().getId())
                .collect(Collectors.toSet());
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

    @Mapping(target = "studentCount",  expression = "java((int) stCnt)")
    @Mapping(target = "courseCount",   expression = "java((int) crsCnt)")
    @Mapping(target = "teacherCount",  expression = "java((int) tchCnt)")
    ClassCardDto toCardDto(ClassEntity e,
                           long stCnt,
                           long crsCnt,
                           long tchCnt);

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
