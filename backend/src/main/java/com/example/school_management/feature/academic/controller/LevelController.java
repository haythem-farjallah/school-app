package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.utils.FieldFilterUtil;
import com.example.school_management.commons.utils.QueryParams;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.service.LevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/levels")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LevelController {

    private final LevelService service;

    /* CRUD -------------------------------------------------- */

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<LevelDto>> create(
            @RequestBody @Valid CreateLevelRequest req) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<LevelDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateLevelRequest req) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(id, req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<LevelDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    /* LIST -------------------------------------------------- */

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<LevelDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nameLike) {

        var dto = new PageDto<>(service.list(PageRequest.of(page, size), nameLike));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* Batch & single course links -------------------------- */

    @PatchMapping("/{levelId}/courses")
    public ResponseEntity<ApiSuccessResponse<LevelDto>> batchCourses(
            @PathVariable Long levelId,
            @RequestBody @Valid BatchIdsRequest body) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success",
                service.mutateCourses(levelId, body)));
    }

    @PostMapping("/{levelId}/courses/{courseId}")
    public ResponseEntity<ApiSuccessResponse<LevelDto>> addCourse(
            @PathVariable Long levelId, @PathVariable Long courseId) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success",
                service.addCourse(levelId, courseId)));
    }

    @DeleteMapping("/{levelId}/courses/{courseId}")
    public ResponseEntity<ApiSuccessResponse<LevelDto>> removeCourse(
            @PathVariable Long levelId, @PathVariable Long courseId) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success",
                service.removeCourse(levelId, courseId)));
    }

    @GetMapping("/new")
    public ResponseEntity<MappingJacksonValue> list(
            QueryParams qp  // automatically resolved by your ArgumentResolver
    ) {
        var page = service.listLevels(qp);
        var dto  = new PageDto<>(page);

        var resp    = new ApiSuccessResponse<>("success", dto);
        var wrapper = new MappingJacksonValue(resp);
        FieldFilterUtil.apply(wrapper, qp);
        return ResponseEntity.ok(wrapper);
    }
}
