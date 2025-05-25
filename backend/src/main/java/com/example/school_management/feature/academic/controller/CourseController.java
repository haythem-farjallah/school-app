package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.utils.FieldFilterUtil;
import com.example.school_management.commons.utils.QueryParams;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CourseController {

    private final CourseService service;

    /* CRUD -------------------------------------------------- */

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<CourseDto>> create(
            @RequestBody @Valid CreateCourseRequest req) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<CourseDto>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCourseRequest req) {

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.update(id, req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<CourseDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.get(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }


    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<CourseDto>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String nameLike) {

        var dto = new PageDto<>(service.list(PageRequest.of(page, size),
                teacherId, nameLike));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @GetMapping("/new")
    public ResponseEntity<MappingJacksonValue> list(
            QueryParams qp  // automatically resolved by your ArgumentResolver
    ) {
        var page = service.listCourses(qp);
        var dto  = new PageDto<>(page);

        var resp    = new ApiSuccessResponse<>("success", dto);
        var wrapper = new MappingJacksonValue(resp);
        FieldFilterUtil.apply(wrapper, qp);
        return ResponseEntity.ok(wrapper);
    }
}
