package com.example.school_management.commons.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.service.AbstractUserService;
import com.example.school_management.commons.service.EmailService;
import com.example.school_management.commons.service.ExportService;
import com.example.school_management.feature.auth.entity.BaseUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract base controller providing common CRUD and bulk operations for user entities.
 * 
 * @param <T> Entity type (Teacher, Student, Staff, etc.)
 * @param <CreateDto> DTO for creation
 * @param <UpdateDto> DTO for updates  
 * @param <ResponseDto> DTO for responses
 * @param <S> Service type
 * @param <M> Mapper type
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractUserController<
    T extends BaseUser,
    CreateDto,
    UpdateDto, 
    ResponseDto,
    S extends AbstractUserService<T>,
    M extends AbstractUserMapper<T, CreateDto, UpdateDto, ResponseDto>
> {

    protected final S service;
    protected final M mapper;
    protected final ExportService exportService;
    protected final EmailService emailService;

    /**
     * Get the entity type name for logging and export operations
     */
    protected abstract String getEntityTypeName();

    /**
     * Get the entity type plural name for URLs and logging
     */
    protected abstract String getEntityTypePluralName();

    /* =================== STANDARD CRUD OPERATIONS =================== */

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ResponseDto>> create(@Valid @RequestBody CreateDto body) {
        log.debug("POST /{} - Creating new {}", getEntityTypePluralName(), getEntityTypeName());
        T entity = service.create(body);
        ResponseDto dto = mapper.toDto(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ResponseDto>> get(@PathVariable long id) {
        log.debug("GET /{}/{} - Fetching {} by ID", getEntityTypePluralName(), id, getEntityTypeName());
        T entity = service.find(id);
        ResponseDto dto = mapper.toDto(entity);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<ResponseDto>> patch(
            @PathVariable long id,
            @Valid @RequestBody UpdateDto body) {
        log.debug("PATCH /{}/{} - Updating {}", getEntityTypePluralName(), id, getEntityTypeName());
        T entity = service.patch(id, body);
        ResponseDto dto = mapper.toDto(entity);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        log.debug("DELETE /{}/{} - Deleting {}", getEntityTypePluralName(), id, getEntityTypeName());
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    /* =================== LISTING AND FILTERING =================== */

    @Operation(summary = "List entities with pagination and basic filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entities retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<ResponseDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search query (optional)") @RequestParam(required = false) String search,
            HttpServletRequest request) {
        
        log.debug("GET /{} - page={}, size={}, search={}", getEntityTypePluralName(), page, size, search);
        
        // Use the service's filtering method with all request parameters
        Page<T> entities = service.findWithAdvancedFilters(
                PageRequest.of(page, size), 
                request.getParameterMap()
        );
        
        var dto = new PageDto<>(entities.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "Search entities by query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponse<PageDto<ResponseDto>>> search(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search query") @RequestParam String q) {
        
        log.debug("GET /{}/search - page={}, size={}, q={}", getEntityTypePluralName(), page, size, q);
        
        Page<T> entities = service.search(PageRequest.of(page, size), q);
        var dto = new PageDto<>(entities.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    @Operation(summary = "Advanced filtering with complex criteria")
    @GetMapping("/filter")
    public ResponseEntity<ApiSuccessResponse<PageDto<ResponseDto>>> filter(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        log.debug("GET /{}/filter - Advanced filtering", getEntityTypePluralName());
        
        Page<T> entities = service.findWithAdvancedFilters(
                PageRequest.of(page, size), 
                request.getParameterMap()
        );
        
        var dto = new PageDto<>(entities.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* =================== BULK OPERATIONS =================== */

    @Operation(summary = "Bulk delete entities by IDs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entities deleted successfully")
    })
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiSuccessResponse<Void>> bulkDelete(
            @Parameter(description = "List of entity IDs to delete") @RequestBody List<Long> ids) {
        log.debug("DELETE /{}/bulk - Bulk deleting {} entities", getEntityTypePluralName(), ids.size());
        service.bulkDelete(ids);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    @Operation(summary = "Bulk update entity status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Entity statuses updated successfully")
    })
    @PatchMapping("/bulk/status")
    public ResponseEntity<ApiSuccessResponse<String>> bulkUpdateStatus(
            @RequestBody BulkStatusUpdateRequest request) {
        log.debug("PATCH /{}/bulk/status - ids: {}, status: {}", 
                getEntityTypePluralName(), request.ids(), request.status());
        service.bulkUpdateStatus(request.ids(), request.status());
        return ResponseEntity.ok(new ApiSuccessResponse<>(
                getEntityTypeName() + " statuses updated successfully", null));
    }

    /* =================== EXPORT OPERATIONS =================== */

    @Operation(summary = "Export entities to CSV format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV export completed successfully")
    })
    @PostMapping("/export/csv")
    public ResponseEntity<String> exportCSV(
            @RequestBody(required = false) ExportRequest request) throws IOException {
        log.debug("POST /{}/export/csv - ids: {}", 
                getEntityTypePluralName(), request != null ? request.ids() : "all");
        
        List<T> entities = getEntitiesForExport(request);
        String csvContent = exportService.exportUsersToCSV(entities, getEntityTypePluralName());
        String filename = exportService.generateExportFilename(getEntityTypePluralName(), "csv");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    @Operation(summary = "Export entities to Excel format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Excel export completed successfully")
    })
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestBody(required = false) ExportRequest request) throws IOException {
        log.debug("POST /{}/export/excel - ids: {}", 
                getEntityTypePluralName(), request != null ? request.ids() : "all");
        
        List<T> entities = getEntitiesForExport(request);
        byte[] excelContent = exportService.exportUsersToExcel(entities, getEntityTypePluralName());
        String filename = exportService.generateExportFilename(getEntityTypePluralName(), "xlsx");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }

    @Operation(summary = "Send bulk email to entities")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk email initiated successfully")
    })
    @PostMapping("/bulk/email")
    public ResponseEntity<ApiSuccessResponse<String>> sendBulkEmail(
            @RequestBody BulkEmailRequest request) {
        log.debug("POST /{}/bulk/email - ids: {}, subject: {}", 
                getEntityTypePluralName(), request.ids(), request.subject());
        
        List<T> entities = service.findByIds(request.ids());
        
        Map<String, Object> variables = Map.of(
            "message", request.message(),
            "actionUrl", request.actionUrl() != null ? request.actionUrl() : "",
            "actionText", request.actionText() != null ? request.actionText() : "View Details"
        );
        
        CompletableFuture<EmailService.BulkEmailResult> future = emailService.sendBulkEmails(
            entities, request.subject(), "email/bulk-notification", variables);
        
        // Don't wait for completion, return immediately
        future.thenAccept(result -> {
            log.info("Bulk email to {} completed: {}/{} successful", 
                    getEntityTypePluralName(), result.getSuccessCount(), result.getTotalCount());
        });
        
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Bulk email initiated for " + entities.size() + " " + getEntityTypePluralName(), null));
    }

    /* =================== HELPER METHODS =================== */

    private List<T> getEntitiesForExport(ExportRequest request) {
        return (request != null && request.ids() != null && !request.ids().isEmpty()) 
            ? service.findByIds(request.ids())
            : service.findAll();
    }

    /* =================== REQUEST DTOS =================== */

    public record BulkStatusUpdateRequest(
            @Parameter(description = "List of entity IDs") List<Long> ids,
            @Parameter(description = "New status") String status,
            @Parameter(description = "Reason for status change") String reason
    ) {}

    public record ExportRequest(
            @Parameter(description = "List of entity IDs to export (optional - exports all if empty)") List<Long> ids
    ) {}

    public record BulkEmailRequest(
            @Parameter(description = "List of entity IDs") List<Long> ids,
            @Parameter(description = "Email subject") String subject,
            @Parameter(description = "Email message content") String message,
            @Parameter(description = "Action URL (optional)") String actionUrl,
            @Parameter(description = "Action button text (optional)") String actionText
    ) {}
}
