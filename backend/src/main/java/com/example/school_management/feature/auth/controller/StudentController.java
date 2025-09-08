package com.example.school_management.feature.auth.controller;

import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.service.ExportService;
import com.example.school_management.commons.service.EmailService;
import com.example.school_management.feature.auth.dto.StudentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDto;
import com.example.school_management.feature.auth.dto.StudentUpdateDto;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.mapper.StudentMapper;
import com.example.school_management.feature.auth.service.StudentService;
import com.example.school_management.feature.auth.dto.StudentStatsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
@Tag(name = "Students", description = "Endpoints for managing students")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService service;
    private final StudentMapper mapper;
    private final ExportService exportService;
    private final EmailService emailService;

    /* -------- CREATE ---------- */
    @Operation(summary = "Create a new student")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Student created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiSuccessResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Student with this email already exists")
    })
    @PostMapping
    @CacheEvict(value = "students", allEntries = true)
    public ResponseEntity<ApiSuccessResponse<StudentDto>> create(
            @Valid @RequestBody StudentCreateDto body) {
        log.debug("POST /students {}", body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", mapper.toDto(service.create(body))));
    }

    /* ---------- READ (single) ---------- */
    @Operation(summary = "Get student details by ID")
    @Parameter(name = "id", description = "ID of the student to retrieve", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student found successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @GetMapping("/{id}")
    @Cacheable(value = "students", key = "#id")
    public ResponseEntity<ApiSuccessResponse<StudentDto>> get(@PathVariable long id) {
        log.debug("GET /students/{}", id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.find(id))));
    }

    /* ---------- READ (paged list with filters) ---------- */
    @Operation(summary = "List students with pagination and optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Students retrieved successfully")
    })
    @GetMapping
    @Cacheable(value = "students", key = "#page + '_' + #size + '_' + #firstNameLike + '_' + #lastNameLike + '_' + #emailLike + '_' + #gradeLevel + '_' + #enrollmentYear + '_' + #status")
    public ResponseEntity<ApiSuccessResponse<PageDto<StudentDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by first name pattern (optional)") @RequestParam(required = false) String firstNameLike,
            @Parameter(description = "Filter by last name pattern (optional)") @RequestParam(required = false) String lastNameLike,
            @Parameter(description = "Filter by email pattern (optional)") @RequestParam(required = false) String emailLike,
            @Parameter(description = "Filter by grade level (optional)") @RequestParam(required = false) String gradeLevel,
            @Parameter(description = "Filter by enrollment year (optional)") @RequestParam(required = false) Integer enrollmentYear,
            @Parameter(description = "Filter by status (optional)") @RequestParam(required = false) String status) {
        
        log.debug("GET /students page={}, size={}, firstNameLike={}, lastNameLike={}, emailLike={}, gradeLevel={}, enrollmentYear={}, status={}", 
                page, size, firstNameLike, lastNameLike, emailLike, gradeLevel, enrollmentYear, status);
        
        var dto = new PageDto<>(service.findAllWithFilters(
                PageRequest.of(page, size),
                firstNameLike,
                lastNameLike,
                emailLike,
                gradeLevel,
                enrollmentYear,
                status
        ).map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* ---------- UPDATE / PATCH ---------- */
    @Operation(summary = "Update an existing student by ID")
    @Parameter(name = "id", description = "ID of the student to update", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student updated successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<StudentDto>> patch(@PathVariable long id,
                           @Valid @RequestBody StudentUpdateDto body) {
        log.debug("PATCH /students/{} {}", id, body);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(service.patch(id, body))));
    }

    /* ---------- SOFT-DELETE ---------- */
    @Operation(summary = "Delete a student by ID (soft delete)")
    @Parameter(name = "id", description = "ID of the student to delete", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Student not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        log.debug("DELETE /students/{}", id);
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    /* ---------- BULK OPERATIONS ---------- */
    @Operation(summary = "Bulk delete students by IDs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Students deleted successfully")
    })
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiSuccessResponse<Void>> bulkDelete(
            @Parameter(description = "List of student IDs to delete") @RequestBody java.util.List<Long> ids) {
        log.debug("DELETE /students/bulk {}", ids);
        service.bulkDelete(ids);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }

    /* ---------- STATISTICS ---------- */
    @Operation(summary = "Get student statistics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiSuccessResponse<StudentStatsDto>> getStats() {
        log.debug("GET /students/stats");
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", service.getStats()));
    }

    /* ---------- SEARCH ---------- */
    @Operation(summary = "Search students by multiple criteria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponse<PageDto<StudentDto>>> search(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search query (searches in name, email)") @RequestParam String q) {
        
        log.debug("GET /students/search page={}, size={}, q={}", page, size, q);
        
        var dto = new PageDto<>(service.search(PageRequest.of(page, size), q).map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    /* ---------- ADVANCED FILTERING ---------- */
    @Operation(summary = "Advanced filtering for students", 
               description = """
                   Advanced filtering with support for:
                   - Text filters: firstName_like, lastName_like, email_like
                   - Exact matches: gradeLevel_eq, status_eq
                   - Comparisons: enrollmentYear_gte, enrollmentYear_lte
                   - Date ranges: enrolledAt_from, enrolledAt_to
                   - Lists: gradeLevel_in (comma-separated)
                   - Sorting: sort=firstName:asc,lastName:desc
                   - Global search: search=query
                   - Fuzzy search: fuzzy=true
                   """)
    @GetMapping("/filter")
    public ResponseEntity<ApiSuccessResponse<PageDto<StudentDto>>> filterStudents(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.debug("GET /students/filter with advanced filtering");
        
        var dto = new PageDto<>(service.findWithAdvancedFilters(
                PageRequest.of(page, size), 
                request.getParameterMap()
        ).map(mapper::toDto));
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }

    /* ---------- EXPORT AND EMAIL OPERATIONS ---------- */
    
    @Operation(summary = "Bulk update student status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student statuses updated successfully")
    })
    @PatchMapping("/bulk/status")
    public ResponseEntity<ApiSuccessResponse<String>> bulkUpdateStatus(
            @RequestBody BulkStatusUpdateRequest request) {
        log.debug("PATCH /students/bulk/status - ids: {}, status: {}", request.ids(), request.status());
        service.bulkUpdateStatus(request.ids(), request.status());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Student statuses updated successfully", null));
    }

    @Operation(summary = "Export students to CSV format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV export completed successfully")
    })
    @PostMapping("/export/csv")
    public ResponseEntity<String> exportStudentsCSV(
            @RequestBody(required = false) ExportRequest request) throws IOException {
        log.debug("POST /students/export/csv - ids: {}", request != null ? request.ids() : "all");
        
        List<Student> students = (request != null && request.ids() != null && !request.ids().isEmpty()) 
            ? service.findByIds(request.ids())
            : service.findAll();
            
        String csvContent = exportService.exportUsersToCSV(students, "students");
        String filename = exportService.generateExportFilename("students", "csv");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }

    @Operation(summary = "Export students to Excel format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Excel export completed successfully")
    })
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportStudentsExcel(
            @RequestBody(required = false) ExportRequest request) throws IOException {
        log.debug("POST /students/export/excel - ids: {}", request != null ? request.ids() : "all");
        
        List<Student> students = (request != null && request.ids() != null && !request.ids().isEmpty()) 
            ? service.findByIds(request.ids())
            : service.findAll();
            
        byte[] excelContent = exportService.exportUsersToExcel(students, "students");
        String filename = exportService.generateExportFilename("students", "xlsx");
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }

    @Operation(summary = "Send bulk email to students")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk email initiated successfully")
    })
    @PostMapping("/bulk/email")
    public ResponseEntity<ApiSuccessResponse<String>> sendBulkEmail(
            @RequestBody BulkEmailRequest request) {
        log.debug("POST /students/bulk/email - ids: {}, subject: {}", request.ids(), request.subject());
        
        List<Student> students = service.findByIds(request.ids());
        
        Map<String, Object> variables = Map.of(
            "message", request.message(),
            "actionUrl", request.actionUrl() != null ? request.actionUrl() : "",
            "actionText", request.actionText() != null ? request.actionText() : "View Details"
        );
        
        CompletableFuture<EmailService.BulkEmailResult> future = emailService.sendBulkEmails(
            students, request.subject(), "email/bulk-notification", variables);
        
        // Don't wait for completion, return immediately
        future.thenAccept(result -> {
            log.info("Bulk email to students completed: {}/{} successful", 
                    result.getSuccessCount(), result.getTotalCount());
        });
        
        return ResponseEntity.ok(new ApiSuccessResponse<>(
            "Bulk email initiated for " + students.size() + " students", null));
    }


    // Request DTOs
    public record BulkStatusUpdateRequest(
            @Parameter(description = "List of student IDs") List<Long> ids,
            @Parameter(description = "New status") String status,
            @Parameter(description = "Reason for status change") String reason
    ) {}

    public record ExportRequest(
            @Parameter(description = "List of student IDs to export (optional - exports all if empty)") List<Long> ids
    ) {}

    public record BulkEmailRequest(
            @Parameter(description = "List of student IDs") List<Long> ids,
            @Parameter(description = "Email subject") String subject,
            @Parameter(description = "Email message content") String message,
            @Parameter(description = "Action URL (optional)") String actionUrl,
            @Parameter(description = "Action button text (optional)") String actionText
    ) {}
} 