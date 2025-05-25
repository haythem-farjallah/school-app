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
import com.example.school_management.feature.auth.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
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
    private final LevelRepository   levelRepo;
    private final CourseRepository  courseRepo;
    private final StudentRepository studentRepo;
    private final AcademicMapper    mapper;
    private final TeachingAssignmentRepository  assignmentRepo;

    /* ─────────────────── CRUD ─────────────────── */

    @Override
    public ClassDto create(CreateClassRequest r) {
        log.debug("Creating class: {}", r);
        if (classRepo.existsByNameIgnoreCase(r.name())) {
            log.warn("Class name '{}' already exists", r.name());
            throw new ConflictException("Class name already exists");
        }

        ClassEntity entity = new ClassEntity();
        entity.setName(r.name());
        if (r.levelId() != null)
            entity.setLevel(fetch(levelRepo, r.levelId(), "Level"));

        ClassDto dto = mapper.toClassDto(classRepo.save(entity));
        log.info("Class created id={}", dto.id());
        return dto;
    }

    @Override
    public ClassDto update(Long id, UpdateClassRequest r) {
        log.debug("Updating class {} with {}", id, r);
        ClassEntity entity = fetch(classRepo, id, "Class");
        mapper.updateClassEntity(r, entity);
        return mapper.toClassDto(entity);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting class {}", id);
        classRepo.deleteById(id);
    }

    @Override
    public ClassDto get(Long id) {
        log.debug("Fetching class {}", id);
        return mapper.toClassDto(fetch(classRepo, id, "Class"));
    }

    /* ─────────────────── LIST ─────────────────── */

    @Override
    @Transactional(readOnly = true)
    public Page<ClassDto> list(Pageable page, Long levelId, String nameLike) {

        log.trace("Listing classes levelId={} nameLike={} {}", levelId, nameLike, page);

        Specification<ClassEntity> spec = (root, q, cb) -> cb.conjunction();

        if (levelId != null)
            spec = spec.and((root, q, cb) -> cb.equal(root.get("level").get("id"), levelId));

        if (nameLike != null && !nameLike.isBlank())
            spec = spec.and((root, q, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + nameLike.toLowerCase() + "%"));

        return classRepo.findAll(spec, page).map(mapper::toClassDto);
    }

    /* ─────────────────── ENROLMENT (batch) ────── */

    @Override
    public ClassDto mutateStudents(Long classId, BatchIdsRequest req) {
        log.debug("Batch {} students {} in class {}", req.operation(), req.ids(), classId);
        ClassEntity entity = fetch(classRepo, classId, "Class");
        applyBatch(entity.getStudents(), req,
                id -> fetch(studentRepo, id, "Student"));
        return mapper.toClassDto(entity);
    }

    @Override
    public ClassDto mutateCourses(Long classId, BatchIdsRequest req) {
        log.debug("Batch {} courses {} in class {}", req.operation(), req.ids(), classId);
        ClassEntity entity = fetch(classRepo, classId, "Class");
        applyBatch(entity.getCourses(), req,
                id -> fetch(courseRepo, id, "Course"));
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

        Specification<ClassEntity> combined = fetchSpec.and(filterSpec);

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

    @Transactional(readOnly = true)
    public Page<ClassCardDto> listCards(QueryParams qp) {

        Page<ClassEntity> page = classRepo.findAll(
                new SpecificationBuilder<ClassEntity>(qp).build(),
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

    @Transactional(readOnly = true)
    public ClassViewDto getDetails(Long classId) {
        ClassEntity c = fetch(classRepo, classId, "Class");
        List<AssignmentDto> list = assignmentRepo.findAllByClassId(classId)
                .stream()
                .map(mapper::toAssignmentDto)
                .toList();
        return new ClassViewDto(c.getId(), c.getName(), list);
    }
}
