package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.controller.TeacherClassController.TeacherClassStatsDto;
import com.example.school_management.feature.academic.dto.TeacherClassDto;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.service.TeacherClassService;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherClassServiceImpl implements TeacherClassService {
    private final TeacherRepository teacherRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final ClassRepository classRepository;
    private final GradeRepository gradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CurrentSchoolResolver currentSchool;

    @Override
    public Page<TeacherClassDto> getTeacherClasses(String teacherEmail, Pageable pageable, String search) {
        List<TeacherClassDto> allClasses = getAllTeacherClasses(teacherEmail, search);
        int start = (int) Math.min(pageable.getOffset(), allClasses.size());
        int end = Math.min(start + pageable.getPageSize(), allClasses.size());
        return new PageImpl<>(allClasses.subList(start, end), pageable, allClasses.size());
    }

    @Override
    public List<TeacherClassDto> getAllTeacherClasses(String teacherEmail, String search) {
        Long schoolId = currentSchool.resolve().getId();
        Teacher teacher = teacherRepository.findByEmailAndSchoolId(teacherEmail, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        return buildTeacherClassDtos(teacher, schoolId, search);
    }

    @Override
    public TeacherClassStatsDto getTeacherClassStats(String teacherEmail) {
        List<TeacherClassDto> classes = getAllTeacherClasses(teacherEmail, null);
        int totalStudents = classes.stream().mapToInt(c -> c.enrolled() != null ? c.enrolled() : 0).sum();
        int totalCapacity = classes.stream().mapToInt(c -> c.capacity() != null ? c.capacity() : 0).sum();
        double averageGrade = classes.stream().filter(c -> c.averageGrade() != null)
                .mapToDouble(TeacherClassDto::averageGrade).average().orElse(0.0);
        double capacityUsed = totalCapacity > 0 ? totalStudents * 100.0 / totalCapacity : 0.0;
        return new TeacherClassStatsDto(classes.size(), totalStudents, averageGrade, totalCapacity, capacityUsed);
    }

    private List<TeacherClassDto> buildTeacherClassDtos(Teacher teacher, Long schoolId, String search) {
        Map<Long, List<TeachingAssignment>> assignmentsByClass = teachingAssignmentRepository
                .findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getClazz().getId()));
        // This scoped query includes assignments, timetable links and the legacy class_teachers relationship.
        List<TeacherClassDto> classes = classRepository.findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .map(clazz -> toDto(clazz, assignmentsByClass.getOrDefault(clazz.getId(), List.of()), schoolId))
                .toList();
        if (search == null || search.isBlank()) return classes;
        String query = search.trim().toLowerCase();
        return classes.stream().filter(dto ->
                contains(dto.name(), query) || contains(dto.grade(), query) || contains(dto.room(), query)
                        || dto.courses().stream().anyMatch(course -> contains(course.name(), query) || contains(course.code(), query)))
                .toList();
    }

    private TeacherClassDto toDto(ClassEntity clazz, List<TeachingAssignment> assignments, Long schoolId) {
        for (Course course : clazz.getCourses()) {
            if (!schoolId.equals(course.getSchool().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has a Course from a different School");
            }
        }
        if (clazz.getAssignedRoom() != null && !schoolId.equals(clazz.getAssignedRoom().getSchool().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has a Room from a different School");
        }
        List<TeacherClassDto.CourseInfo> courses = assignments.isEmpty()
                ? clazz.getCourses().stream().map(course -> new TeacherClassDto.CourseInfo(
                        course.getId(), course.getName(), course.getCode(), 0)).toList()
                : assignments.stream().map(assignment -> new TeacherClassDto.CourseInfo(
                        assignment.getCourse().getId(), assignment.getCourse().getName(),
                        assignment.getCourse().getCode(), assignment.getWeeklyHours())).toList();
        return new TeacherClassDto(clazz.getId(), clazz.getName(), clazz.getGradeLevel(), clazz.getCapacity(),
                (int) enrollmentRepository.countActiveByClassId(clazz.getId()),
                clazz.getAssignedRoom() != null ? clazz.getAssignedRoom().getName() : "TBD", "",
                calculateClassAverageGrade(clazz.getId(), schoolId), "active", courses, null, null);
    }

    private Double calculateClassAverageGrade(Long classId, Long schoolId) {
        List<Grade> grades = gradeRepository.findByClassIdAndSchoolId(classId, schoolId);
        if (grades.isEmpty()) return null;
        return grades.stream().filter(grade -> grade.getScore() != null)
                .mapToDouble(grade -> grade.getScore().doubleValue()).average().orElse(0.0);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}
