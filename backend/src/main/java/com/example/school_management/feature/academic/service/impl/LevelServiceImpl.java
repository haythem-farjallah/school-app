package com.example.school_management.feature.academic.service.impl;

import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.Level;
import com.example.school_management.feature.academic.mapper.AcademicMapper;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.LevelRepository;
import com.example.school_management.feature.academic.service.LevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static com.example.school_management.feature.academic.dto.BatchIdsRequest.Operation.*;
import static com.example.school_management.feature.academic.utils.EnrollmentUtils.applyBatch;
import static com.example.school_management.feature.academic.utils.EnrollmentUtils.fetch;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LevelServiceImpl implements LevelService {

    private final LevelRepository  levelRepo;
    private final CourseRepository courseRepo;
    private final AcademicMapper   mapper;

    /* ─────────────────── CRUD ─────────────────── */

    @Override
    public LevelDto create(CreateLevelRequest r) {
        log.debug("Creating level {}", r);
        Level entity = new Level();
        entity.setName(r.name());

        LevelDto dto = mapper.toLevelDto(levelRepo.save(entity));
        log.info("Level created id={}", dto.id());
        return dto;
    }

    @Override
    public LevelDto update(Long id, UpdateLevelRequest r) {
        log.debug("Updating level {} with {}", id, r);
        Level entity = fetch(levelRepo, id, "Level");
        mapper.updateLevelEntity(r, entity);
        return mapper.toLevelDto(entity);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting level {}", id);
        levelRepo.deleteById(id);
    }

    @Override
    public LevelDto get(Long id) {
        log.debug("Fetching level {}", id);
        return mapper.toLevelDto(fetch(levelRepo, id, "Level"));
    }

    /* ─────────────────── LIST ─────────────────── */

    @Transactional(readOnly = true)
    @Override
    public Page<LevelDto> list(Pageable page, String nameLike) {

        log.trace("Listing levels nameLike={} {}", nameLike, page);

        Specification<Level> spec = (root, q, cb) -> cb.conjunction();

        if (nameLike != null && !nameLike.isBlank()) {
            spec = spec.and((root, q, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + nameLike.toLowerCase() + "%"));
        }

        return levelRepo.findAll(spec, page).map(mapper::toLevelDto);
    }

    /* ─────────────────── Batch courses ─────────── */

    @Override
    public LevelDto mutateCourses(Long levelId, BatchIdsRequest req) {
        log.debug("Batch {} courses {} in level {}", req.operation(), req.ids(), levelId);
        Level entity = fetch(levelRepo, levelId, "Level");
        applyBatch(entity.getCourses(), req,
                id -> fetch(courseRepo, id, "Course"));
        return mapper.toLevelDto(entity);
    }

    /* ── single-item helpers (checkbox UX) ─────── */

    @Override
    public LevelDto addCourse(Long levelId, Long courseId) {
        log.debug("Add course {} to level {}", courseId, levelId);
        return mutateCourses(levelId, new BatchIdsRequest(ADD, Set.of(courseId)));
    }

    @Override
    public LevelDto removeCourse(Long levelId, Long courseId) {
        log.debug("Remove course {} from level {}", courseId, levelId);
        return mutateCourses(levelId, new BatchIdsRequest(REMOVE, Set.of(courseId)));
    }
}
