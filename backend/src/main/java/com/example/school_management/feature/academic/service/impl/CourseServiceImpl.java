package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import com.example.school_management.commons.utils.FilterFields;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.service.CourseService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static com.example.school_management.feature.academic.utils.EnrollmentUtils.fetch;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final FilterFields FILTER_FIELDS = new FilterFields(
            Set.of("name", "credit", "weeklyCapacity", "teacher.id"),
            Set.of("name", "credit", "weeklyCapacity"));

    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    private final CourseRepository  courseRepo;
    private final TeacherRepository teacherRepo;
    private final AcademicMapper    mapper;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;
    private final CurrentSchoolResolver currentSchool;

    /* ─────────────────── CRUD ─────────────────── */

    @Override
    public CourseDto create(CreateCourseRequest r) {
        log.debug("Creating course {}", r);

        School school = currentSchool.resolve();
        if (courseRepo.existsBySchoolIdAndNameIgnoreCase(school.getId(), r.name())) {
            log.warn("Course '{}' already exists", r.name());
            throw new ConflictException("Course name already exists");
        }

        Course entity = new Course();
        entity.setSchool(school);
        fill(entity, r);
        entity.setCode(generateCourseCode());

        Course savedEntity = courseRepo.save(entity);
        CourseDto dto = mapper.toCourseDto(savedEntity);
        log.info("Course created id={}", dto.id());
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "New course created";
            String details = String.format("Course created: %s (ID: %d), Credit: %d, Teacher: %s", 
                savedEntity.getName(), savedEntity.getId(), savedEntity.getCredit(),
                savedEntity.getTeacher() != null ? savedEntity.getTeacher().getEmail() : "None");
            
            auditService.createAuditEvent(
                AuditEventType.COURSE_CREATED,
                "Course",
                savedEntity.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for course creation: {}", e.getClass().getSimpleName());
        }
        
        return dto;
    }

    @Override
    public CourseDto update(Long id, UpdateCourseRequest r) {
        log.debug("Updating course {} with {}", id, r);
        Long schoolId = currentSchool.resolve().getId();
        Course entity = findCourse(id, schoolId);
        if (r.name() != null && courseRepo.existsBySchoolIdAndNameIgnoreCaseAndIdNot(schoolId, r.name(), id)) {
            throw new ConflictException("Course name already exists");
        }
        String oldName = entity.getName();
        Float oldCredit = entity.getCredit();
        String oldTeacher = entity.getTeacher() != null ? entity.getTeacher().getEmail() : "None";
        
        mapper.updateCourseEntity(r, entity);
        
        // Handle teacher mapping manually since we ignored it in the mapper
        if (r.teacherId() != null) {
            entity.setTeacher(fetch(teacherRepo, r.teacherId(), "Teacher"));
        }
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String newTeacher = entity.getTeacher() != null ? entity.getTeacher().getEmail() : "None";
            String summary = "Course updated";
            String details = String.format("Course updated: ID %d, old name: %s -> %s, old credit: %d -> %d, old teacher: %s -> %s", 
                id, oldName, entity.getName(), oldCredit, entity.getCredit(), oldTeacher, newTeacher);
            
            auditService.createAuditEvent(
                AuditEventType.COURSE_UPDATED,
                "Course",
                id,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for course update: {}", e.getClass().getSimpleName());
        }
        
        return mapper.toCourseDto(entity);
    }

    @Override public void delete(Long id) {
        log.info("Deleting course {}", id);
        
        // Get course details before deletion for audit
        Course entity = findCourse(id, currentSchool.resolve().getId());
        String courseName = entity.getName();
        String teacherEmail = entity.getTeacher() != null ? entity.getTeacher().getEmail() : "None";
        
        courseRepo.delete(entity);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Course deleted";
            String details = String.format("Course deleted: %s (ID: %d), Teacher: %s", courseName, id, teacherEmail);
            
            auditService.createAuditEvent(
                AuditEventType.COURSE_DELETED,
                "Course",
                id,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for course deletion: {}", e.getClass().getSimpleName());
        }
    }

    @Override public CourseDto get(Long id) {
        log.debug("Fetching course {}", id);
        return mapper.toCourseDto(findCourse(id, currentSchool.resolve().getId()));
    }

    /* ─────────────────── LIST ─────────────────── */

    @Transactional(readOnly = true)
    @Override
    public Page<CourseDto> list(Pageable p, Long teacherId, String nameLike) {

        log.trace("Listing courses teacherId={} nameLike={} {}", teacherId, nameLike, p);

        Specification<Course> spec = inCurrentSchool();

        if (teacherId != null)
            spec = spec.and((root, q, cb) -> cb.equal(root.get("teacher").get("id"), teacherId));

        if (nameLike != null && !nameLike.isBlank())
            spec = spec.and((root, q, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + nameLike.toLowerCase() + "%"));

        return courseRepo.findAll(spec, p).map(mapper::toCourseDto);
    }

    /* ─────────────────── helper ─────────────────── */

    private Course findCourse(Long id, Long schoolId) {
        return courseRepo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private Specification<Course> inCurrentSchool() {
        Long schoolId = currentSchool.resolve().getId();
        return (root, query, cb) -> cb.equal(root.get("school").get("id"), schoolId);
    }

    private static String generateCourseCode() {
        // 96 random bits fit in 16 URL-safe characters, leaving four for the prefix.
        byte[] random = new byte[12];
        CODE_RANDOM.nextBytes(random);
        return "CRS-" + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private void fill(Course c, CreateCourseRequest r) {
        c.setName(r.name());
        c.setColor(r.color());
        c.setCredit(r.credit());
        c.setWeeklyCapacity(r.weeklyCapacity());

        if (r.teacherId() != null)
            c.setTeacher(fetch(teacherRepo, r.teacherId(), "Teacher"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDto> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FILTER_FIELDS.requireSortable(pageable.getSort());
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap, FILTER_FIELDS);
        Specification<Course> spec = inCurrentSchool()
                .and(DynamicSpecificationBuilder.build(criteria));
        return courseRepo.findAll(spec, pageable).map(mapper::toCourseDto);
    }
    
    /**
     * Get the current authenticated user
     */
    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
    }

}
