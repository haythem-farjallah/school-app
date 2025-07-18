package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.utils.FetchJoinSpecification;
import com.example.school_management.commons.utils.QueryParams;
import com.example.school_management.commons.utils.SpecificationBuilder;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.service.CourseService;
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

import static com.example.school_management.feature.academic.utils.EnrollmentUtils.fetch;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository  courseRepo;
    private final TeacherRepository teacherRepo;
    private final AcademicMapper    mapper;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;

    /* ─────────────────── CRUD ─────────────────── */

    @Override
    public CourseDto create(CreateCourseRequest r) {
        log.debug("Creating course {}", r);

        /* Optional: tighten duplicate-name rule across teacher scope */
        if (courseRepo.existsByNameIgnoreCase(r.name())) {
            log.warn("Course '{}' already exists", r.name());
            throw new ConflictException("Course name already exists");
        }

        Course entity = new Course();
        fill(entity, r);

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
            log.warn("Failed to create audit event for course creation: {}", e.getMessage());
        }
        
        return dto;
    }

    @Override
    public CourseDto update(Long id, UpdateCourseRequest r) {
        log.debug("Updating course {} with {}", id, r);
        Course entity = fetch(courseRepo, id, "Course");
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
            log.warn("Failed to create audit event for course update: {}", e.getMessage());
        }
        
        return mapper.toCourseDto(entity);
    }

    @Override public void delete(Long id) {
        log.info("Deleting course {}", id);
        
        // Get course details before deletion for audit
        Course entity = fetch(courseRepo, id, "Course");
        String courseName = entity.getName();
        String teacherEmail = entity.getTeacher() != null ? entity.getTeacher().getEmail() : "None";
        
        courseRepo.deleteById(id);
        
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
            log.warn("Failed to create audit event for course deletion: {}", e.getMessage());
        }
    }

    @Override public CourseDto get(Long id) {
        log.debug("Fetching course {}", id);
        return mapper.toCourseDto(fetch(courseRepo, id, "Course"));
    }

    /* ─────────────────── LIST ─────────────────── */

    @Transactional(readOnly = true)
    @Override
    public Page<CourseDto> list(Pageable p, Long teacherId, String nameLike) {

        log.trace("Listing courses teacherId={} nameLike={} {}", teacherId, nameLike, p);

        Specification<Course> spec = (root, q, cb) -> cb.conjunction();

        if (teacherId != null)
            spec = spec.and((root, q, cb) -> cb.equal(root.get("teacher").get("id"), teacherId));

        if (nameLike != null && !nameLike.isBlank())
            spec = spec.and((root, q, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + nameLike.toLowerCase() + "%"));

        return courseRepo.findAll(spec, p).map(mapper::toCourseDto);
    }

    /* ─────────────────── helper ─────────────────── */

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
    public Page<CourseDto> listCourses(QueryParams qp) {
        // 1) turn include=… into LEFT-JOINs for exactly the requested relations
        Specification<Course> joinSpec =
                FetchJoinSpecification.fetchRelations(qp.getInclude());

        // 2) build a filter spec from all filter[...] params
        Specification<Course> filterSpec =
                new SpecificationBuilder<Course>(qp).build();

        // 3) combine them
        Specification<Course> combinedSpec =
                Specification.where(joinSpec)
                        .and(filterSpec);

        // 4) page + sort from qp
        Sort sort = qp.getSort().isEmpty()
                ? Sort.unsorted()
                : Sort.by(qp.getSort());
        Pageable pageReq = PageRequest.of(qp.getPage(), qp.getSize(), sort);

        // 5) query + map to DTO
        return courseRepo
                .findAll(combinedSpec, pageReq)
                .map(mapper::toCourseDto);
    }
    
    /**
     * Get the current authenticated user
     */
    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + email));
    }

}
