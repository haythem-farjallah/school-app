package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.utils.FetchJoinSpecification;
import com.example.school_management.commons.utils.QueryParams;
import com.example.school_management.commons.utils.SpecificationBuilder;
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
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.school_management.feature.academic.utils.EnrollmentUtils.applyBatch;
import static com.example.school_management.feature.academic.utils.EnrollmentUtils.fetch;
import static com.example.school_management.feature.academic.dto.BatchIdsRequest.Operation.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassRepository   classRepo;
    private final CourseRepository  courseRepo;
    private final StudentRepository studentRepo;
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
        ClassDto dto = mapper.toClassDto(savedEntity);
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
        
        return mapper.toClassDto(entity);
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
        return mapper.toClassDto(findClass(id));
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

        return classRepo.findAll(spec, page).map(mapper::toClassDto);
    }

    /* ─────────────────── ENROLMENT (batch) ────── */

    @Override
    public ClassDto mutateStudents(Long classId, BatchIdsRequest req) {
        log.debug("Batch {} students {} in class {}", req.operation(), req.ids(), classId);
        ClassEntity entity = findClass(classId);
        applyBatch(entity.getStudents(), req,
                id -> fetch(studentRepo, id, "Student"));
        return mapper.toClassDto(entity);
    }

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
        return mapper.toClassDto(entity);
    }

    /* ── single-item wrappers (checkbox UX) ───── */

    @Override public ClassDto addStudent   (Long c, Long s){
        log.debug("Add student {} to class {}", s, c);
        return mutateStudents(c, new BatchIdsRequest(ADD, Set.of(s)));
    }
    @Override public ClassDto removeStudent(Long c, Long s){
        log.debug("Remove student {} from class {}", s, c);
        return mutateStudents(c, new BatchIdsRequest(REMOVE, Set.of(s)));
    }
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
    public Page<ClassDto> listClasses(QueryParams qp) {
        // 1) Build a spec that fetches any includes AND applies your filters
        Specification<ClassEntity> fetchSpec  =
                FetchJoinSpecification.joinRelations(qp.getInclude());
        Specification<ClassEntity> filterSpec =
                new SpecificationBuilder<ClassEntity>(qp).build();

        Specification<ClassEntity> combined = inCurrentSchool().and(fetchSpec).and(filterSpec);

        // 2) Build a PageRequest with sort & pagination
        PageRequest pageReq = PageRequest.of(
                qp.getPage(),
                qp.getSize(),
                qp.getSort().isEmpty()
                        ? Sort.unsorted()
                        : Sort.by(qp.getSort())
        );

        // 3) Execute and map
        return classRepo
                .findAll(combined, pageReq)
                .map(mapper::toClassDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassCardDto> listCards(QueryParams qp) {

        Page<ClassEntity> page = classRepo.findAll(
                inCurrentSchool().and(new SpecificationBuilder<ClassEntity>(qp).build()),
                PageRequest.of(qp.getPage(), qp.getSize(),
                        qp.getSort().isEmpty()
                                ? Sort.by("name")
                                : Sort.by(qp.getSort())));

        /* -------- aggregate counts -------- */
        Map<Long, ClassCountRow> counts =
                assignmentRepo.aggregateForClasses(
                                page.getContent()
                                        .stream()
                                        .map(ClassEntity::getId)
                                        .toList())
                        .stream()
                        .collect(Collectors
                                .toMap(ClassCountRow::getClassId,
                                        Function.identity()));

        /* -------- map to DTOs; fall back to 0 -------- */
        List<ClassCardDto> cards = page.getContent().stream()
                .map(c -> {
                    ClassCountRow row = counts.get(c.getId());
                    int teachers = row != null ? row.getTeacherCnt().intValue() : 0;
                    int courses  = row != null ? row.getCourseCnt().intValue()  : 0;
                    int students = c.getStudents().size();     // already loaded
                    return mapper.toCardDto(c, students, courses, teachers);
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
        
        List<ClassDto> classDtos = pageContent.stream()
                .map(mapper::toClassDto)
                .toList();
        
        log.debug("Returning {} classes for teacher {}", classDtos.size(), teacherId);
        return new PageImpl<>(classDtos, pageable, classes.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassDto> getClassesByStudentId(Long studentId, Pageable pageable) {
        log.debug("Getting classes for student: {}", studentId);
        
        // Get classes through enrollments
        Specification<ClassEntity> spec = (root, query, cb) -> {
            var enrollmentJoin = root.join("enrollments");
            var studentJoin = enrollmentJoin.join("student");
            return cb.equal(studentJoin.get("id"), studentId);
        };
        
        Page<ClassEntity> classPage = classRepo.findAll(inCurrentSchool().and(spec), pageable);
        return classPage.map(mapper::toClassDto);
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
