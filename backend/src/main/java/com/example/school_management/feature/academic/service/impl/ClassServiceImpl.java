package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import com.example.school_management.commons.utils.FilterFields;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.*;
import com.example.school_management.feature.academic.service.ClassService;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.school_management.feature.academic.dto.BatchIdsRequest.Operation.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private static final FilterFields FILTER_FIELDS = new FilterFields(
            Set.of("name", "yearOfStudy", "maxStudents"),
            Set.of("name", "yearOfStudy", "maxStudents"));

    private final ClassRepository   classRepo;
    private final CourseRepository  courseRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final TeacherRepository teacherRepo;
    private final AcademicMapper    mapper;
    private final TeachingAssignmentRepository  assignmentRepo;
    private final AuditService auditService;
    private final CurrentAcademicYearResolver currentAcademicYear;
    private final CurrentSchoolResolver currentSchool;
    private final BaseUserRepository<BaseUser> userRepo;

    /* ─────────────────── CRUD ─────────────────── */

    @Override
    public ClassDto create(CreateClassRequest r) {
        log.debug("Creating class: {}", r);
        AcademicYear academicYear = currentAcademicYear.resolve();
        if (classRepo.existsByAcademicYearIdAndNameIgnoreCase(academicYear.getId(), r.name())) {
            log.warn("Class name '{}' already exists", r.name());
            throw new ConflictException("Class name already exists");
        }

        ClassEntity entity = new ClassEntity();
        entity.setName(r.name());
        entity.setAcademicYear(academicYear);

        ClassEntity savedEntity = classRepo.save(entity);
        ClassDto dto = mapper.toClassDto(savedEntity, Set.of());
        log.info("Class created id={}", dto.id());
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "New class created";
            String details = String.format("Class created: %s (ID: %d)", savedEntity.getName(), savedEntity.getId());
            
            auditService.createAuditEvent(
                AuditEventType.CLASS_CREATED,
                "Class",
                savedEntity.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for class creation: {}", e.getClass().getSimpleName());
        }
        
        return dto;
    }

    @Override
    public ClassDto update(Long id, UpdateClassRequest r) {
        log.debug("Updating class {} with {}", id, r);
        ClassEntity entity = findClass(id);
        if (r.name() != null && classRepo.existsByAcademicYearIdAndNameIgnoreCaseAndIdNot(
                entity.getAcademicYear().getId(), r.name(), id)) {
            throw new ConflictException("Class name already exists");
        }
        String oldName = entity.getName();
        
        mapper.updateClassEntity(r, entity);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Class updated";
            String details = String.format("Class updated: ID %d, old name: %s, new name: %s", 
                id, oldName, entity.getName());
            
            auditService.createAuditEvent(
                AuditEventType.CLASS_UPDATED,
                "Class",
                id,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for class update: {}", e.getClass().getSimpleName());
        }
        
        return toDto(entity);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting class {}", id);
        
        // Get class details before deletion for audit
        ClassEntity entity = findClass(id);
        String className = entity.getName();
        
        classRepo.delete(entity);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Class deleted";
            String details = String.format("Class deleted: %s (ID: %d)", className, id);
            
            auditService.createAuditEvent(
                AuditEventType.CLASS_DELETED,
                "Class",
                id,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for class deletion: {}", e.getClass().getSimpleName());
        }
    }

    @Override
    public ClassDto get(Long id) {
        log.debug("Fetching class {}", id);
        return toDto(findClass(id));
    }

    /* ─────────────────── LIST ─────────────────── */

    @Override
    @Transactional(readOnly = true)
    public Page<ClassDto> list(Pageable page, String nameLike) {

        log.trace("Listing classes nameLike={} {}", nameLike, page);

        Specification<ClassEntity> spec = inCurrentSchool();

        if (nameLike != null && !nameLike.isBlank())
            spec = spec.and((root, q, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + nameLike.toLowerCase() + "%"));

        return toDtoPage(classRepo.findAll(spec, page));
    }

    /* ─────────────────── COURSES (batch) ──────── */

    @Override
    public ClassDto mutateCourses(Long classId, BatchIdsRequest req) {
        log.debug("Batch {} courses {} in class {}", req.operation(), req.ids(), classId);
        ClassEntity entity = findClass(classId);
        Long schoolId = entity.getAcademicYear().getSchool().getId();
        // Validate every ID before changing links, including IDs requested for removal.
        List<Course> requested = req.ids().stream()
                .map(id -> courseRepo.findByIdAndSchoolId(id, schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found")))
                .toList();
        if (req.operation() == ADD) {
            entity.getCourses().addAll(requested);
        } else {
            entity.getCourses().removeIf(course -> req.ids().contains(course.getId()));
        }
        return toDto(entity);
    }

    /* ── single-item wrappers (checkbox UX) ───── */

    @Override public ClassDto addCourse    (Long c, Long d){
        log.debug("Add course {} to class {}", d, c);
        return mutateCourses(c, new BatchIdsRequest(ADD, Set.of(d)));
    }
    @Override public ClassDto removeCourse (Long c, Long d){
        log.debug("Remove course {} from class {}", d, c);
        return mutateCourses(c, new BatchIdsRequest(REMOVE, Set.of(d)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassDto> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FILTER_FIELDS.requireSortable(pageable.getSort());
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap, FILTER_FIELDS);
        Specification<ClassEntity> spec = inCurrentSchool()
                .and(DynamicSpecificationBuilder.build(criteria));
        return toDtoPage(classRepo.findAll(spec, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassCardDto> findCardsWithFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FILTER_FIELDS.requireSortable(pageable.getSort());
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap, FILTER_FIELDS);
        if ((criteria.getSortCriteria() == null || criteria.getSortCriteria().isEmpty()) && pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("name"));
        }
        Specification<ClassEntity> spec = inCurrentSchool().and(DynamicSpecificationBuilder.build(criteria));
        Page<ClassEntity> page = classRepo.findAll(spec, pageable);

        /* -------- aggregate counts -------- */
        List<Long> classIds = page.getContent().stream().map(ClassEntity::getId).toList();
        Map<Long, ClassCountRow> counts =
                assignmentRepo.aggregateForClasses(classIds)
                        .stream()
                        .collect(Collectors
                                .toMap(ClassCountRow::getClassId,
                                        Function.identity()));
        // The student count is the number of ACTIVE Enrollments, the same roster ClassDto.studentIds exposes.
        Map<Long, Long> students = enrollmentRepo.countActiveRosters(classIds).stream()
                .collect(Collectors.toMap(EnrollmentRepository.RosterCountRow::getClassId,
                        EnrollmentRepository.RosterCountRow::getStudentCount));

        /* -------- map to DTOs; fall back to 0 -------- */
        List<ClassCardDto> cards = page.getContent().stream()
                .map(c -> {
                    ClassCountRow row = counts.get(c.getId());
                    int teachers = row != null ? row.getTeacherCnt().intValue() : 0;
                    int courses  = row != null ? row.getCourseCnt().intValue()  : 0;
                    return mapper.toCardDto(c, students.getOrDefault(c.getId(), 0L), courses, teachers);
                })
                .toList();

        return new PageImpl<>(cards, page.getPageable(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ClassViewDto getDetails(Long classId) {
        ClassEntity c = findClass(classId);
        List<AssignmentDto> list = assignmentRepo.findAllByClassId(classId)
                .stream()
                .map(mapper::toAssignmentDto)
                .toList();
        return new ClassViewDto(c.getId(), c.getName(), list);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassDto> getClassesByTeacherId(Long teacherId, Pageable pageable) {
        log.debug("Getting classes for teacher: {}", teacherId);
        List<ClassEntity> classes = classRepo.findByTeacherIdAndSchoolId(teacherId, currentSchool.resolve().getId());
        log.debug("Found {} classes for teacher {}", classes.size(), teacherId);
        
        if (classes.isEmpty()) {
            log.info("No classes found for teacher {}", teacherId);
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Convert to Page manually since repository returns List
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), classes.size());
        List<ClassEntity> pageContent = classes.subList(start, end);
        
        List<ClassDto> classDtos = toDtos(pageContent);
        
        log.debug("Returning {} classes for teacher {}", classDtos.size(), teacherId);
        return new PageImpl<>(classDtos, pageable, classes.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassDto> getClassesByStudentId(Long studentId, Pageable pageable) {
        log.debug("Getting classes for student: {}", studentId);
        
        // A student's current classes are the ones with an ACTIVE enrollment; history is not membership.
        Specification<ClassEntity> spec = (root, query, cb) -> {
            Subquery<Long> active = query.subquery(Long.class);
            Root<Enrollment> enrollment = active.from(Enrollment.class);
            active.select(enrollment.get("id")).where(
                    cb.equal(enrollment.get("classEntity"), root),
                    cb.equal(enrollment.get("student").get("id"), studentId),
                    cb.equal(enrollment.get("status"), EnrollmentStatus.ACTIVE));
            return cb.exists(active);
        };

        return toDtoPage(classRepo.findAll(inCurrentSchool().and(spec), pageable));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ClassDto> getCurrentTeacherClasses(Pageable pageable) {
        log.debug("Getting classes for current teacher");

        // Get current teacher from security context
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = userDetails.getUsername();

        Teacher teacher = teacherRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Current user is not a teacher"));

        log.debug("Found teacher with ID: {}", teacher.getId());
        return getClassesByTeacherId(teacher.getId(), pageable);
    }

    /** A Class with its current roster: the students holding an ACTIVE Enrollment in it. */
    private ClassDto toDto(ClassEntity entity) {
        return mapper.toClassDto(entity, new HashSet<>(enrollmentRepo.findActiveStudentIdsByClassId(entity.getId())));
    }

    /** Resolves the rosters of many classes in one query. */
    private List<ClassDto> toDtos(List<ClassEntity> entities) {
        Map<Long, Set<Long>> rosters = new HashMap<>();
        enrollmentRepo.findActiveRosterRows(entities.stream().map(ClassEntity::getId).toList())
                .forEach(row -> rosters.computeIfAbsent(row.getClassId(), id -> new HashSet<>()).add(row.getStudentId()));
        return entities.stream()
                .map(entity -> mapper.toClassDto(entity, rosters.getOrDefault(entity.getId(), Set.of())))
                .toList();
    }

    private Page<ClassDto> toDtoPage(Page<ClassEntity> page) {
        return new PageImpl<>(toDtos(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    private ClassEntity findClass(Long id) {
        return classRepo.findByIdAndAcademicYearSchoolId(id, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private Specification<ClassEntity> inCurrentSchool() {
        Long schoolId = currentSchool.resolve().getId();
        return (root, query, cb) -> cb.equal(root.get("academicYear").get("school").get("id"), schoolId);
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
