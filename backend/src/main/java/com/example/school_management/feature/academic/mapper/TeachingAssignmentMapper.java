package com.example.school_management.feature.academic.mapper;

import com.example.school_management.feature.academic.dto.CreateTeachingAssignmentDto;
import com.example.school_management.feature.academic.dto.TeachingAssignmentResponseDto;
import com.example.school_management.feature.academic.dto.UpdateTeachingAssignmentDto;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.auth.entity.Teacher;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TeachingAssignmentMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacher", source = "teacherId")
    @Mapping(target = "course", source = "courseId") 
    @Mapping(target = "clazz", source = "classId")
    TeachingAssignment toEntity(CreateTeachingAssignmentDto dto);
    
    @Mapping(target = "teacherId", source = "teacher.id")
    @Mapping(target = "teacherFirstName", source = "teacher.firstName")
    @Mapping(target = "teacherLastName", source = "teacher.lastName")
    @Mapping(target = "teacherEmail", source = "teacher.email")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.name")
    @Mapping(target = "courseCode", source = "course.code")
    @Mapping(target = "classId", source = "clazz.id")
    @Mapping(target = "className", source = "clazz.name")
    @Mapping(target = "createdAt", source = "teacher.createdAt") // Fallback to teacher creation
    @Mapping(target = "updatedAt", source = "teacher.updatedAt") // Fallback to teacher update
    TeachingAssignmentResponseDto toDto(TeachingAssignment entity);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "teacher", source = "teacherId")
    @Mapping(target = "course", source = "courseId")
    @Mapping(target = "clazz", source = "classId")
    void patch(UpdateTeachingAssignmentDto dto, @MappingTarget TeachingAssignment entity);
    
    // Helper methods for mapping IDs to entities
    default Teacher mapTeacherId(Long teacherId) {
        if (teacherId == null) return null;
        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        return teacher;
    }
    
    default Course mapCourseId(Long courseId) {
        if (courseId == null) return null;
        Course course = new Course();
        course.setId(courseId);
        return course;
    }
    
    default ClassEntity mapClassId(Long classId) {
        if (classId == null) return null;
        ClassEntity clazz = new ClassEntity();
        clazz.setId(classId);
        return clazz;
    }
}
