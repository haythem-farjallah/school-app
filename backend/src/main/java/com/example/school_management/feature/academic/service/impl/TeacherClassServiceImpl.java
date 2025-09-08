package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.feature.academic.controller.TeacherClassController.TeacherClassStatsDto;
import com.example.school_management.feature.academic.dto.TeacherClassDto;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.TeachingAssignment;

import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.service.TeacherClassService;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.repository.GradeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherClassServiceImpl implements TeacherClassService {
    
    private final TeacherRepository teacherRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final ClassRepository classRepository;
    private final GradeRepository gradeRepository;
    
    @Override
    public Page<TeacherClassDto> getTeacherClasses(String teacherEmail, Pageable pageable, String search) {
        log.debug("Getting classes for teacher: {}, search: {}", teacherEmail, search);
        
        Teacher teacher = findTeacherByEmail(teacherEmail);
        List<TeacherClassDto> allClasses = buildTeacherClassDtos(teacher, search);
        
        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allClasses.size());
        List<TeacherClassDto> pageContent = allClasses.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, allClasses.size());
    }
    
    @Override
    public List<TeacherClassDto> getAllTeacherClasses(String teacherEmail, String search) {
        log.info("🔍 Getting all classes for teacher: {}, search: {}", teacherEmail, search);
        
        Teacher teacher = findTeacherByEmail(teacherEmail);
        log.info("🔍 Found teacher: {} (ID: {})", teacher.getEmail(), teacher.getId());
        
        List<TeacherClassDto> classes = buildTeacherClassDtos(teacher, search);
        log.info("🔍 Built {} class DTOs for teacher {}", classes.size(), teacherEmail);
        
        return classes;
    }
    
    @Override
    public TeacherClassStatsDto getTeacherClassStats(String teacherEmail) {
        log.debug("Getting class stats for teacher: {}", teacherEmail);
        
        Teacher teacher = findTeacherByEmail(teacherEmail);
        List<TeacherClassDto> classes = buildTeacherClassDtos(teacher, null);
        
        int totalClasses = classes.size();
        int totalStudents = classes.stream()
                .mapToInt(c -> c.enrolled() != null ? c.enrolled() : 0)
                .sum();
        int totalCapacity = classes.stream()
                .mapToInt(c -> c.capacity() != null ? c.capacity() : 0)
                .sum();
        
        double averageGrade = classes.stream()
                .filter(c -> c.averageGrade() != null)
                .mapToDouble(TeacherClassDto::averageGrade)
                .average()
                .orElse(0.0);
        
        double capacityUsed = totalCapacity > 0 ? (totalStudents * 100.0 / totalCapacity) : 0.0;
        
        return new TeacherClassStatsDto(
                totalClasses,
                totalStudents,
                averageGrade,
                totalCapacity,
                capacityUsed
        );
    }
    
    @Override
    public Object getDebugInfo(String teacherEmail) {
        log.info("🔍 Getting debug info for teacher: {}", teacherEmail);
        
        try {
            Teacher teacher = findTeacherByEmail(teacherEmail);
            List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherId(teacher.getId());
            Set<ClassEntity> directClasses = teacher.getClasses();
            List<ClassEntity> classesFromRepository = classRepository.findByTeacherId(teacher.getId());
            
            Map<String, Object> debugInfo = Map.of(
                "teacherId", teacher.getId(),
                "teacherEmail", teacher.getEmail(),
                "teacherName", teacher.getFirstName() + " " + teacher.getLastName(),
                "teachingAssignmentsCount", assignments.size(),
                "directClassesCount", directClasses != null ? directClasses.size() : 0,
                "repositoryClassesCount", classesFromRepository.size(),
                "teachingAssignments", assignments.stream()
                    .map(ta -> Map.of(
                        "id", ta.getId(),
                        "classId", ta.getClazz().getId(),
                        "className", ta.getClazz().getName(),
                        "courseId", ta.getCourse().getId(),
                        "courseName", ta.getCourse().getName(),
                        "weeklyHours", ta.getWeeklyHours()
                    ))
                    .collect(Collectors.toList()),
                "directClasses", directClasses != null ? 
                    directClasses.stream()
                        .map(c -> Map.of(
                            "id", c.getId(),
                            "name", c.getName(),
                            "gradeLevel", c.getGradeLevel(),
                            "capacity", c.getCapacity(),
                            "studentsCount", c.getStudents() != null ? c.getStudents().size() : 0
                        ))
                        .collect(Collectors.toList()) : 
                    List.of(),
                "repositoryClasses", classesFromRepository.stream()
                    .map(c -> Map.of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "gradeLevel", c.getGradeLevel(),
                        "capacity", c.getCapacity(),
                        "studentsCount", c.getStudents() != null ? c.getStudents().size() : 0
                    ))
                    .collect(Collectors.toList())
            );
            
            log.info("🔍 Debug info compiled: {} teaching assignments, {} direct classes, {} repository classes", 
                assignments.size(), 
                directClasses != null ? directClasses.size() : 0,
                classesFromRepository.size());
            
            return debugInfo;
        } catch (Exception e) {
            log.error("❌ Error getting debug info for teacher {}: {}", teacherEmail, e.getMessage(), e);
            return Map.of(
                "error", e.getMessage(),
                "teacherEmail", teacherEmail,
                "stackTrace", java.util.Arrays.toString(e.getStackTrace())
            );
        }
    }
    
    private Teacher findTeacherByEmail(String email) {
        return teacherRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found with email: " + email));
    }
    
    private List<TeacherClassDto> buildTeacherClassDtos(Teacher teacher, String search) {
        log.info("🔍 Building class DTOs for teacher: {} (ID: {})", teacher.getEmail(), teacher.getId());
        
        // Get all teaching assignments for this teacher
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherId(teacher.getId());
        log.info("🔍 Found {} teaching assignments for teacher {}", assignments.size(), teacher.getId());
        
        // Also get classes directly assigned to teacher (many-to-many relationship)
        // We need to fetch this fresh from the database to avoid lazy loading issues
        Teacher freshTeacher = teacherRepository.findById(teacher.getId()).orElse(teacher);
        Set<ClassEntity> directClasses = freshTeacher.getClasses();
        log.info("🔍 Found {} direct class assignments for teacher {}", 
                directClasses != null ? directClasses.size() : 0, teacher.getId());
        
        // Also check using a direct query to the class_teachers junction table
        List<ClassEntity> classesFromJunction = classRepository.findByTeacherId(teacher.getId());
        log.info("🔍 Found {} classes from junction table query for teacher {}", 
                classesFromJunction.size(), teacher.getId());
        
        // Combine all approaches
        Set<Long> allClassIds = new HashSet<>();
        
        // Add classes from teaching assignments
        assignments.forEach(ta -> {
            allClassIds.add(ta.getClazz().getId());
            log.info("🔍 Added class {} from teaching assignment", ta.getClazz().getId());
        });
        
        // Add direct class assignments
        if (directClasses != null) {
            directClasses.forEach(clazz -> {
                allClassIds.add(clazz.getId());
                log.info("🔍 Added class {} from direct assignment", clazz.getId());
            });
        }
        
        // Add classes from junction table query
        classesFromJunction.forEach(clazz -> {
            allClassIds.add(clazz.getId());
            log.info("🔍 Added class {} from junction table query", clazz.getId());
        });
        
        log.info("🔍 Total unique classes for teacher {}: {}", teacher.getId(), allClassIds.size());
        
        if (allClassIds.isEmpty()) {
            log.warn("❌ No classes found for teacher {} ({})", teacher.getEmail(), teacher.getId());
            return new ArrayList<>();
        }
        
        // Group teaching assignments by class
        Map<Long, List<TeachingAssignment>> assignmentsByClass = assignments.stream()
                .collect(Collectors.groupingBy(ta -> ta.getClazz().getId()));
        
        // Build DTOs for each class
        List<TeacherClassDto> classDtos = allClassIds.stream()
                .map(classId -> {
                    // Get class entity
                    ClassEntity classEntity = null;
                    List<TeacherClassDto.CourseInfo> courses = new ArrayList<>();
                    
                    // Try to get from teaching assignments first
                    List<TeachingAssignment> classAssignments = assignmentsByClass.get(classId);
                    if (classAssignments != null && !classAssignments.isEmpty()) {
                        classEntity = classAssignments.get(0).getClazz();
                        courses = classAssignments.stream()
                                .map(ta -> new TeacherClassDto.CourseInfo(
                                        ta.getCourse().getId(),
                                        ta.getCourse().getName(),
                                        ta.getCourse().getCode(),
                                        ta.getWeeklyHours()
                                ))
                                .collect(Collectors.toList());
                    } else {
                        // Get from direct class assignment
                        if (directClasses != null) {
                            classEntity = directClasses.stream()
                                    .filter(c -> c.getId().equals(classId))
                                    .findFirst()
                                    .orElse(null);
                        }
                        
                        // If still null, try to fetch from database
                        if (classEntity == null) {
                            classEntity = classRepository.findById(classId).orElse(null);
                            if (classEntity == null) {
                                log.warn("Could not find class entity for ID: {}", classId);
                                return null;
                            }
                        }
                        
                        // For direct assignments without teaching assignments, 
                        // we'll show the class courses but without specific teacher assignment info
                        if (classEntity.getCourses() != null) {
                            courses = classEntity.getCourses().stream()
                                    .map(course -> new TeacherClassDto.CourseInfo(
                                            course.getId(),
                                            course.getName(),
                                            course.getCode(),
                                            0 // No specific weekly hours assigned
                                    ))
                                    .collect(Collectors.toList());
                        }
                    }
                    
                    if (classEntity == null) {
                        return null;
                    }
                    
                    // Calculate average grade for this class
                    Double averageGrade = calculateClassAverageGrade(classId);
                    
                    // Build schedule string (simplified for now)
                    String schedule = buildScheduleString(classEntity);
                    
                    return new TeacherClassDto(
                            classEntity.getId(),
                            classEntity.getName(),
                            classEntity.getGradeLevel(),
                            classEntity.getCapacity(),
                            classEntity.getStudents() != null ? classEntity.getStudents().size() : 0,
                            classEntity.getAssignedRoom() != null ? classEntity.getAssignedRoom().getName() : "TBD",
                            schedule,
                            averageGrade,
                            "active", // Assuming all classes are active
                            courses,
                            null, // createdAt - not available in ClassEntity
                            null  // updatedAt - not available in ClassEntity
                    );
                })
                .filter(dto -> dto != null) // Remove null entries
                .collect(Collectors.toList());
        
        // Apply search filter if provided
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            classDtos = classDtos.stream()
                    .filter(dto -> 
                            dto.name().toLowerCase().contains(searchLower) ||
                            dto.grade().toLowerCase().contains(searchLower) ||
                            (dto.room() != null && dto.room().toLowerCase().contains(searchLower)) ||
                            dto.courses().stream().anyMatch(course -> 
                                    course.name().toLowerCase().contains(searchLower) ||
                                    course.code().toLowerCase().contains(searchLower)
                            )
                    )
                    .collect(Collectors.toList());
        }
        
        return classDtos;
    }
    
    private Double calculateClassAverageGrade(Long classId) {
        try {
            List<Grade> grades = gradeRepository.findByClassId(classId);
            if (grades.isEmpty()) {
                return null;
            }
            
            return grades.stream()
                    .filter(grade -> grade.getScore() != null)
                    .mapToDouble(grade -> grade.getScore().doubleValue())
                    .average()
                    .orElse(0.0);
        } catch (Exception e) {
            log.warn("Error calculating average grade for class {}: {}", classId, e.getMessage());
            return null;
        }
    }
    
    private String buildScheduleString(ClassEntity classEntity) {
        // This is a simplified implementation
        // In a real system, you'd query the timetable for this class
        return "Mon, Wed, Fri - 8:00 AM"; // Placeholder
    }
}
