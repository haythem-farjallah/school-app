package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.auth.dto.StaffCreateDto;
import com.example.school_management.feature.auth.dto.StaffDto;
import com.example.school_management.feature.auth.dto.StaffUpdateDto;
import com.example.school_management.feature.auth.mapper.StaffMapper;
import com.example.school_management.feature.auth.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Staff Management", description = "Endpoints for managing staff members")
@Slf4j
public class StaffController {

    private final StaffService service;
    private final StaffMapper mapper;

    @Operation(summary = "Create a new staff member")
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<StaffDto>> create(
            @Valid @RequestBody StaffCreateDto body) {
        log.info("Creating staff member with data: {}", body);
        try {
            var result = service.create(body);
            log.info("Staff member created successfully with ID: {}", result.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiSuccessResponse<>("Staff member created successfully", mapper.toDto(result)));
        } catch (Exception e) {
            log.error("Failed to create staff member: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Operation(summary = "Get staff member by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<StaffDto>> get(@PathVariable long id) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("Staff member retrieved successfully", mapper.toDto(service.find(id))));
    }

    @Operation(summary = "List all staff members with pagination and filters")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<StaffDto>>> list(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String firstNameLike,
            @RequestParam(required = false) String lastNameLike,
            @RequestParam(required = false) String emailLike,
            @RequestParam(required = false) String staffType,
            @RequestParam(required = false) String department) {
        
        var result = service.findAllWithFilters(
                pageable, 
                search, 
                firstNameLike, 
                lastNameLike, 
                emailLike, 
                staffType, 
                department
        );
        var dto = new PageDto<>(result.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("Staff members retrieved successfully", dto));
    }

    @Operation(summary = "Update staff member")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<StaffDto>> patch(@PathVariable long id,
                            @Valid @RequestBody StaffUpdateDto body) {
        return ResponseEntity.ok(new ApiSuccessResponse<>("Staff member updated successfully", mapper.toDto(service.patch(id, body))));
    }

    @Operation(summary = "Delete staff member")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Staff member deleted successfully", null));
    }
} 