package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.service.ExportService;
import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.dto.StaffCreateDto;
import com.example.school_management.feature.auth.dto.StaffDto;
import com.example.school_management.feature.auth.dto.StaffUpdateDto;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.mapper.StaffMapper;
import com.example.school_management.feature.auth.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Staff Management", description = "Endpoints for managing staff members")
@Slf4j
public class StaffController {

    private final StaffService service;
    private final StaffMapper mapper;
    private final ExportService exportService;
    private final EmailService emailService;

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

    /* ---------- BULK OPERATIONS ---------- */
    
    @Operation(summary = "Bulk delete staff members by IDs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff members deleted successfully")
    })
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiSuccessResponse<Void>> bulkDelete(
            @Parameter(description = "List of staff IDs to delete") @RequestBody List<Long> ids) {
        log.debug("DELETE /staff/bulk {}", ids);
        service.bulkDelete(ids);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Bulk update staff status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff statuses updated successfully")
    })
    @PatchMapping("/bulk/status")
    public ResponseEntity<ApiSuccessResponse<String>> bulkUpdateStatus(
            @RequestBody BulkStatusUpdateRequest request) {
        log.debug("PATCH /staff/bulk/status - ids: {}, status: {}", request.ids(), request.status());
        service.bulkUpdateStatus(request.ids(), request.status());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Staff statuses updated successfully", null));
    }

    @Operation(summary = "Export staff to CSV format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV export completed successfully")
    })
    @PostMapping("/export/csv")
    public ResponseEntity<String> exportStaffCSV(
            @RequestBody(required = false) ExportRequest request) throws IOException {
        log.debug("POST /staff/export/csv - ids: {}", request != null ? request.ids() : "all");
        
        List<Staff> staff = (request != null && request.ids() != null && !request.ids().isEmpty()) 
            ? service.findByIds(request.ids())
            : service.findAll();
            
        String csvContent = exportService.exportUsersToCSV(staff, "staff");
        String filename = exportService.generateExportFilename("staff", "csv");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    @Operation(summary = "Export staff to Excel format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Excel export completed successfully")
    })
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportStaffExcel(
            @RequestBody(required = false) ExportRequest request) throws IOException {
        log.debug("POST /staff/export/excel - ids: {}", request != null ? request.ids() : "all");
        
        List<Staff> staff = (request != null && request.ids() != null && !request.ids().isEmpty()) 
            ? service.findByIds(request.ids())
            : service.findAll();
            
        byte[] excelContent = exportService.exportUsersToExcel(staff, "staff");
        String filename = exportService.generateExportFilename("staff", "xlsx");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }

    @Operation(summary = "Send bulk email to staff")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk email initiated successfully")
    })
    @PostMapping("/bulk/email")
    public ResponseEntity<ApiSuccessResponse<String>> sendBulkEmail(
            @RequestBody BulkEmailRequest request) {
        log.debug("POST /staff/bulk/email - ids: {}, subject: {}", request.ids(), request.subject());
        
        List<Staff> staff = service.findByIds(request.ids());
        
        Map<String, Object> variables = Map.of(
            "message", request.message(),
            "actionUrl", request.actionUrl() != null ? request.actionUrl() : "",
            "actionText", request.actionText() != null ? request.actionText() : "View Details"
        );
        
        CompletableFuture<EmailService.BulkEmailResult> future = emailService.sendBulkEmails(
            staff, request.subject(), "email/bulk-notification", variables);
        
        // Don't wait for completion, return immediately
        future.thenAccept(result -> {
            log.info("Bulk email to staff completed: {}/{} successful", 
                    result.getSuccessCount(), result.getTotalCount());
        });
        
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Bulk email initiated for " + staff.size() + " staff members", null));
    }

    // Request DTOs
    public record BulkStatusUpdateRequest(
            @Parameter(description = "List of staff IDs") List<Long> ids,
            @Parameter(description = "New status") String status,
            @Parameter(description = "Reason for status change") String reason
    ) {}

    public record ExportRequest(
            @Parameter(description = "List of staff IDs to export (optional - exports all if empty)") List<Long> ids
    ) {}

    public record BulkEmailRequest(
            @Parameter(description = "List of staff IDs") List<Long> ids,
            @Parameter(description = "Email subject") String subject,
            @Parameter(description = "Email message content") String message,
            @Parameter(description = "Action URL (optional)") String actionUrl,
            @Parameter(description = "Action button text (optional)") String actionText
    ) {}
} 