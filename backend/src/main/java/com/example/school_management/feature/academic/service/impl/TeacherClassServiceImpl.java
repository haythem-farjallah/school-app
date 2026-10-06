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
import java.util.Locale;
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
        Long schoolId = currentSchool.resolve().getId();
        Teacher teacher = findTeacher(teacherEmail, schoolId);
        Page<ClassEntity> page = classRepository.findTeacherClassPage(teacher.getId(), schoolId,
                searchPattern(search), pageable);
        return new PageImpl<>(buildTeacherClassDtos(teacher, schoolId, page.getContent()), pageable, page.getTotalElements());
    }

    @Override
    public List<TeacherClassDto> getAllTeacherClasses(String teacherEmail, String search) {
        Long schoolId = currentSchool.resolve().getId();
        Teacher teacher = findTeacher(teacherEmail, schoolId);
        return buildTeacherClassDtos(teacher, schoolId,
                classRepository.findTeacherClasses(teacher.getId(), schoolId, searchPattern(search)));
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

    private Teacher findTeacher(String email, Long schoolId) {
        return teacherRepository.findByEmailAndSchoolId(email, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    private String searchPattern(String search) {
        if (search == null || search.isBlank()) return null;
        // SQL LIKE must preserve the existing literal substring search, including %, _ and backslash.
        return "%" + search.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private List<TeacherClassDto> buildTeacherClassDtos(Teacher teacher, Long schoolId, List<ClassEntity> classes) {
        if (classes.isEmpty()) return List.of();
        List<Long> classIds = classes.stream().map(ClassEntity::getId).toList();
        classRepository.findWithCoursesByIdsAndSchoolId(classIds, schoolId);
        Map<Long, List<TeachingAssignment>> assignmentsByClass = teachingAssignmentRepository
                .findByTeacherIdAndSchoolId(teacher.getId(), schoolId).stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getClazz().getId()));
        Map<Long, Long> studentCounts = enrollmentRepository.countActiveRosters(classIds).stream()
                .collect(Collectors.toMap(EnrollmentRepository.RosterCountRow::getClassId,
                        EnrollmentRepository.RosterCountRow::getStudentCount));
        Map<Long, GradeRepository.ClassGradeSummary> gradeSummaries = gradeRepository
                .summarizeByClassIdsAndSchoolId(classIds, schoolId).stream()
                .collect(Collectors.toMap(GradeRepository.ClassGradeSummary::getClassId, summary -> summary));
        return classes.stream().map(clazz -> toDto(clazz,
                assignmentsByClass.getOrDefault(clazz.getId(), List.of()), schoolId,
                studentCounts.getOrDefault(clazz.getId(), 0L), gradeSummaries.get(clazz.getId()))).toList();
    }

    private TeacherClassDto toDto(ClassEntity clazz, List<TeachingAssignment> assignments, Long schoolId,
            Long studentCount, GradeRepository.ClassGradeSummary summary) {
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
                studentCount.intValue(),
                clazz.getAssignedRoom() != null ? clazz.getAssignedRoom().getName() : "TBD", "",
                summary == null ? null : (summary.getAverageGrade() == null ? 0.0 : summary.getAverageGrade()), "active", courses, null, null);
    }

}
