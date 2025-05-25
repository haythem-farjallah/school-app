package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.utils.FetchJoinSpecification;
import com.example.school_management.commons.utils.QueryParams;
import com.example.school_management.commons.utils.SpecificationBuilder;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.service.CourseService;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
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

        CourseDto dto = mapper.toCourseDto(courseRepo.save(entity));
        log.info("Course created id={}", dto.id());
        return dto;
    }

    @Override
    public CourseDto update(Long id, UpdateCourseRequest r) {
        log.debug("Updating course {} with {}", id, r);
        Course entity = fetch(courseRepo, id, "Course");
        mapper.updateCourseEntity(r, entity);
        return mapper.toCourseDto(entity);
    }

    @Override public void delete(Long id) {
        log.info("Deleting course {}", id);
        courseRepo.deleteById(id);
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
        c.setCoefficient(r.coefficient());

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

}
