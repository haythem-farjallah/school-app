package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.auth.dto.ParentCreateDto;
import com.example.school_management.feature.auth.dto.ParentDto;
import com.example.school_management.feature.auth.dto.ParentUpdateDto;
import com.example.school_management.feature.auth.mapper.ParentMapper;
import com.example.school_management.feature.auth.service.ParentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/parent-management")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class ParentController {

    private final ParentService service;
    private final ParentMapper mapper;

    /* -------- CREATE ---------- */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ParentDto>> create(
            @Valid @RequestBody ParentCreateDto body) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", mapper.toDto(service.create(body))));
    }

    /* ---------- READ (single) ---------- */
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ParentDto>> get(@PathVariable long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.find(id))));
    }

    /* ---------- READ (paged list with filters) ---------- */
    @Operation(summary = "List parents with pagination and optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parents retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<ParentDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by first name pattern (optional)") @RequestParam(required = false) String firstNameLike,
            @Parameter(description = "Filter by last name pattern (optional)") @RequestParam(required = false) String lastNameLike,
            @Parameter(description = "Filter by email pattern (optional)") @RequestParam(required = false) String emailLike,
            @Parameter(description = "Filter by telephone pattern (optional)") @RequestParam(required = false) String telephoneLike,
            @Parameter(description = "Filter by preferred contact method pattern (optional)") @RequestParam(required = false) String preferredContactMethodLike) {
        
        log.debug("GET /admin/parent-management page={}, size={}, firstNameLike={}, lastNameLike={}, emailLike={}, telephoneLike={}, preferredContactMethodLike={}", 
                page, size, firstNameLike, lastNameLike, emailLike, telephoneLike, preferredContactMethodLike);
        
        var dto = new PageDto<>(service.findAllWithFilters(
                PageRequest.of(page, size),
                firstNameLike,
                lastNameLike,
                emailLike,
                telephoneLike,
                preferredContactMethodLike
        ).map(mapper::toDto));
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* ---------- UPDATE / PATCH ---------- */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ParentDto>> patch(@PathVariable long id,
                          @Valid @RequestBody ParentUpdateDto body) {
        log.info("📥 PATCH /admin/parent-management/{} - Received update request:", id);
        log.info("   📞 Telephone: '{}'", body.getTelephone());
        log.info("   🏠 Address: '{}'", body.getAddress());
        log.info("   💬 Preferred Contact Method: '{}'", body.getPreferredContactMethod());
        log.info("   👥 Relation: '{}'", body.getRelation());
        log.info("   👶 Children emails: {}", body.getChildren());
        
        ParentDto result = mapper.toDto(service.patch(id, body));
        
        log.info("📤 PATCH /admin/parent-management/{} - Returning updated parent:", id);
        log.info("   📞 Telephone: '{}'", result.telephone());
        log.info("   🏠 Address: '{}'", result.address());
        log.info("   💬 Preferred Contact Method: '{}'", result.preferredContactMethod());
        log.info("   👥 Relation: '{}'", result.relation());
        log.info("   👶 Children count: {}", result.children() != null ? result.children().size() : 0);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", result));
    }

    /* ---------- SOFT-DELETE ---------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    /* ---------- SEARCH ---------- */
    @Operation(summary = "Search parents by multiple criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponse<PageDto<ParentDto>>> search(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search query (searches in name, email, telephone)") @RequestParam String q) {
        
        log.debug("GET /admin/parent-management/search page={}, size={}, q={}", page, size, q);
        
        var dto = new PageDto<>(service.search(PageRequest.of(page, size), q).map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    /* ---------- ADVANCED FILTERING ---------- */
    @Operation(summary = "Advanced filtering for parents", 
               description = """
                   Advanced filtering with support for:
                   - Text filters: firstName_like, lastName_like, email_like, telephone_like, preferredContactMethod_like
                   - Exact matches: relation_eq
                   - Sorting: sort=firstName:asc,lastName:desc
                   - Global search: search=query
                   - Fuzzy search: fuzzy=true
                   """)
    @GetMapping("/filter")
    public ResponseEntity<ApiSuccessResponse<PageDto<ParentDto>>> filterParents(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("GET /admin/parent-management/filter with advanced filtering");
        
        var dto = new PageDto<>(service.findWithAdvancedFilters(
                PageRequest.of(page, size), 
                request.getParameterMap()
        ).map(mapper::toDto));
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
} 