package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.auth.dto.TeacherCreateDto;
import com.example.school_management.feature.auth.dto.TeacherDto;
import com.example.school_management.feature.auth.dto.TeacherUpdateDto;
import com.example.school_management.feature.auth.mapper.TeacherMapper;
import com.example.school_management.feature.auth.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TeacherController {

    private final TeacherService service;
    private final TeacherMapper mapper;

    /* -------- CREATE ---------- */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<TeacherDto>> create(
            @Valid @RequestBody TeacherCreateDto body) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", mapper.toDto(service.create(body))));
    }

    /* ---------- READ (single) ---------- */
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<TeacherDto>> get(@PathVariable long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.find(id))));
    }

    /* ---------- READ (paged list with filters) ---------- */
    @Operation(summary = "List teachers with pagination and optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teachers retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<TeacherDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by first name pattern (optional)") @RequestParam(required = false) String firstNameLike,
            @Parameter(description = "Filter by last name pattern (optional)") @RequestParam(required = false) String lastNameLike,
            @Parameter(description = "Filter by email pattern (optional)") @RequestParam(required = false) String emailLike,
            @Parameter(description = "Filter by qualifications pattern (optional)") @RequestParam(required = false) String qualificationsLike,
            @Parameter(description = "Filter by subjects taught pattern (optional)") @RequestParam(required = false) String subjectsTaughtLike,
            @Parameter(description = "Filter by available hours (optional)") @RequestParam(required = false) Integer availableHours,
            @Parameter(description = "Filter by schedule preferences pattern (optional)") @RequestParam(required = false) String schedulePreferencesLike) {
        
        log.debug("GET /teachers page={}, size={}, firstNameLike={}, lastNameLike={}, emailLike={}, qualificationsLike={}, subjectsTaughtLike={}, availableHours={}, schedulePreferencesLike={}", 
                page, size, firstNameLike, lastNameLike, emailLike, qualificationsLike, subjectsTaughtLike, availableHours, schedulePreferencesLike);
        
        var dto = new PageDto<>(service.findAllWithFilters(
                PageRequest.of(page, size),
                firstNameLike,
                lastNameLike,
                emailLike,
                qualificationsLike,
                subjectsTaughtLike,
                availableHours,
                schedulePreferencesLike
        ).map(mapper::toDto));
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* ---------- UPDATE / PATCH ---------- */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<TeacherDto>> patch(@PathVariable long id,
                            @Valid @RequestBody TeacherUpdateDto body) {
        log.info("📥 PATCH /admin/teachers/{} - Received update request:", id);
        log.info("   📞 Telephone: '{}'", body.telephone());
        log.info("   🏠 Address: '{}'", body.address());
        log.info("   🎓 Qualifications: '{}'", body.qualifications());
        log.info("   📚 Subjects Taught: '{}'", body.subjectsTaught());
        log.info("   ⏰ Available Hours: {}", body.availableHours());
        log.info("   📅 Schedule Preferences: '{}'", body.schedulePreferences());
        
        TeacherDto result = mapper.toDto(service.patch(id, body));
        
        log.info("📤 PATCH /admin/teachers/{} - Returning updated teacher:", id);
        log.info("   📞 Telephone: '{}'", result.telephone());
        log.info("   🏠 Address: '{}'", result.address());
        log.info("   🎓 Qualifications: '{}'", result.qualifications());
        log.info("   📚 Subjects Taught: '{}'", result.subjectsTaught());
        log.info("   ⏰ Available Hours: {}", result.availableHours());
        log.info("   📅 Schedule Preferences: '{}'", result.schedulePreferences());
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", result));
    }

    /* ---------- SOFT-DELETE ---------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    /* ---------- Search method ---------- */
    @Operation(summary = "Search teachers by multiple criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponse<PageDto<TeacherDto>>> search(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search query (searches in name, email, qualifications, subjects)") @RequestParam String q) {
        
        log.debug("GET /teachers/search page={}, size={}, q={}", page, size, q);
        
        var dto = new PageDto<>(service.search(PageRequest.of(page, size), q).map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    /* ---------- ADVANCED FILTERING ---------- */
    @Operation(summary = "Advanced filtering for teachers", 
               description = """
                   Advanced filtering with support for:
                   - Text filters: firstName_like, lastName_like, email_like, qualifications_like, subjectsTaught_like, schedulePreferences_like
                   - Exact matches: availableHours_eq
                   - Comparisons: availableHours_gte, availableHours_lte
                   - Lists: qualifications_in (comma-separated)
                   - Sorting: sort=firstName:asc,lastName:desc
                   - Global search: search=query
                   - Fuzzy search: fuzzy=true
                   """)
    @GetMapping("/filter")
    public ResponseEntity<ApiSuccessResponse<PageDto<TeacherDto>>> filterTeachers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("GET /teachers/filter with advanced filtering");
        
        var dto = new PageDto<>(service.findWithAdvancedFilters(
                PageRequest.of(page, size), 
                request.getParameterMap()
        ).map(mapper::toDto));
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
}
