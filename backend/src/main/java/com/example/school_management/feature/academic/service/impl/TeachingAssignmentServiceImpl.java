package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.commons.service.GenericFilterService;
import com.example.school_management.feature.academic.dto.CreateTeachingAssignmentDto;
import com.example.school_management.feature.academic.dto.UpdateTeachingAssignmentDto;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.mapper.TeachingAssignmentMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.service.TeachingAssignmentService;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TeachingAssignmentServiceImpl implements TeachingAssignmentService {
    
    private final TeachingAssignmentRepository assignmentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final TeachingAssignmentMapper mapper;
    private final GenericFilterService genericFilterService;
    
    @Override
    public TeachingAssignment create(CreateTeachingAssignmentDto dto) {
        log.debug("Creating teaching assignment: {}", dto);
        
        // Validate entities exist
        Teacher teacher = teacherRepository.findById(dto.teacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + dto.teacherId()));
        Course course = courseRepository.findById(dto.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + dto.courseId()));
        ClassEntity clazz = classRepository.findById(dto.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + dto.classId()));
        
        // Check for existing assignment
        if (assignmentRepository.existsByClazzIdAndCourseId(dto.classId(), dto.courseId())) {
            throw new IllegalArgumentException("Assignment already exists for course " + course.getName() + " in class " + clazz.getName());
        }
        
        TeachingAssignment assignment = mapper.toEntity(dto);
        assignment.setTeacher(teacher);
        assignment.setCourse(course);
        assignment.setClazz(clazz);
        
        TeachingAssignment saved = assignmentRepository.save(assignment);
        log.info("Created teaching assignment: {} teaches {} to {}", 
                teacher.getFirstName() + " " + teacher.getLastName(),
                course.getName(), 
                clazz.getName());
        
        return saved;
    }
    
    @Override
    @Transactional(readOnly = true)
    public TeachingAssignment find(long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teaching assignment not found with id: " + id));
    }
    
    @Override
    public TeachingAssignment patch(long id, UpdateTeachingAssignmentDto dto) {
        log.debug("Updating teaching assignment {}: {}", id, dto);
        
        TeachingAssignment assignment = find(id);
        
        // Update teacher if provided
        if (dto.teacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.teacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + dto.teacherId()));
            assignment.setTeacher(teacher);
        }
        
        // Update course if provided
        if (dto.courseId() != null) {
            Course course = courseRepository.findById(dto.courseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + dto.courseId()));
            assignment.setCourse(course);
        }
        
        // Update class if provided
        if (dto.classId() != null) {
            ClassEntity clazz = classRepository.findById(dto.classId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + dto.classId()));
            assignment.setClazz(clazz);
        }
        
        // Update weekly hours if provided
        if (dto.weeklyHours() != null) {
            assignment.setWeeklyHours(dto.weeklyHours());
        }
        
        TeachingAssignment saved = assignmentRepository.save(assignment);
        log.info("Updated teaching assignment {}", id);
        
        return saved;
    }
    
    @Override
    public void delete(long id) {
        log.debug("Deleting teaching assignment {}", id);
        TeachingAssignment assignment = find(id);
        assignmentRepository.delete(assignment);
        log.info("Deleted teaching assignment {}", id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findAll(Pageable pageable) {
        return assignmentRepository.findAll(pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> search(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return findAll(pageable);
        }
        
        // Basic search implementation - could be enhanced with more sophisticated search
        // Use simple text search for teaching assignments
        Specification<TeachingAssignment> spec = genericFilterService.buildTextSearchSpecification(
                query, "teacher.firstName", "teacher.lastName", "teacher.email", 
                "course.name", "course.code", "clazz.name");
        
        if (spec != null) {
            return assignmentRepository.findAll(spec, pageable);
        } else {
            return findAll(pageable);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        Specification<TeachingAssignment> spec = genericFilterService.buildSpecificationFromParams(parameterMap);
        return assignmentRepository.findAll(spec, pageable);
    }
    
    @Override
    public void bulkDelete(List<Long> ids) {
        log.debug("Bulk deleting {} teaching assignments", ids.size());
        assignmentRepository.deleteAllById(ids);
        log.info("Bulk deleted {} teaching assignments", ids.size());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findByIds(List<Long> ids) {
        return assignmentRepository.findAllById(ids);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findAll() {
        return assignmentRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findByTeacherId(Long teacherId) {
        return assignmentRepository.findByTeacherId(teacherId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findByTeacherId(Long teacherId, Pageable pageable) {
        Specification<TeachingAssignment> spec = (root, query, cb) -> 
                cb.equal(root.get("teacher").get("id"), teacherId);
        return assignmentRepository.findAll(spec, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findByCourseId(Long courseId) {
        return assignmentRepository.findByCourseId(courseId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findByCourseId(Long courseId, Pageable pageable) {
        Specification<TeachingAssignment> spec = (root, query, cb) -> 
                cb.equal(root.get("course").get("id"), courseId);
        return assignmentRepository.findAll(spec, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignment> findByClassId(Long classId) {
        return assignmentRepository.findByClazzId(classId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignment> findByClassId(Long classId, Pageable pageable) {
        Specification<TeachingAssignment> spec = (root, query, cb) -> 
                cb.equal(root.get("clazz").get("id"), classId);
        return assignmentRepository.findAll(spec, pageable);
    }
    
    @Override
    public void assignTeacherToCourses(Long teacherId, List<Long> courseIds, Long classId) {
        log.debug("Assigning teacher {} to courses {} in class {}", teacherId, courseIds, classId);
        
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
        ClassEntity clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        
        for (Long courseId : courseIds) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
            
            // Skip if assignment already exists
            if (!assignmentRepository.existsByClazzIdAndCourseId(classId, courseId)) {
                TeachingAssignment assignment = new TeachingAssignment();
                assignment.setTeacher(teacher);
                assignment.setCourse(course);
                assignment.setClazz(clazz);
                assignment.setWeeklyHours(0); // Default value
                assignmentRepository.save(assignment);
            }
        }
        
        log.info("Assigned teacher {} to {} courses in class {}", teacherId, courseIds.size(), classId);
    }
    
    @Override
    public void assignTeachersToClass(List<Long> teacherIds, Long classId, Long courseId) {
        log.debug("Assigning teachers {} to course {} in class {}", teacherIds, courseId, classId);
        
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        ClassEntity clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        
        for (Long teacherId : teacherIds) {
            Teacher teacher = teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
            
            // Check if assignment already exists (one teacher per course per class)
            if (!assignmentRepository.existsByClazzIdAndCourseId(classId, courseId)) {
                TeachingAssignment assignment = new TeachingAssignment();
                assignment.setTeacher(teacher);
                assignment.setCourse(course);
                assignment.setClazz(clazz);
                assignment.setWeeklyHours(0); // Default value
                assignmentRepository.save(assignment);
            }
        }
        
        log.info("Assigned {} teachers to course {} in class {}", teacherIds.size(), courseId, classId);
    }
    
    @Override
    public void bulkAssignTeachersToCourses(List<CreateTeachingAssignmentDto> assignments) {
        log.debug("Bulk creating {} teaching assignments", assignments.size());
        
        for (CreateTeachingAssignmentDto dto : assignments) {
            try {
                create(dto);
            } catch (Exception e) {
                log.warn("Failed to create assignment: {}, error: {}", dto, e.getMessage());
                // Continue with other assignments instead of failing completely
            }
        }
        
        log.info("Bulk created teaching assignments");
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByTeacherAndCourseAndClass(Long teacherId, Long courseId, Long classId) {
        Specification<TeachingAssignment> spec = (root, query, cb) -> 
                cb.and(
                        cb.equal(root.get("teacher").get("id"), teacherId),
                        cb.equal(root.get("course").get("id"), courseId),
                        cb.equal(root.get("clazz").get("id"), classId)
                );
        return assignmentRepository.findAll(spec).size() > 0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasConflictingAssignment(Long teacherId, Long courseId, Long classId) {
        // Check if this course is already assigned to another teacher in the same class
        return assignmentRepository.existsByClazzIdAndCourseId(classId, courseId);
    }
}
