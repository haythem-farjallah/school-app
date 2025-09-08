package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.operational.service.TimetableExportService;
import com.example.school_management.feature.operational.dto.TimetableExportRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
@Tag(name = "Timetable Export", description = "Export timetables in various formats")
public class TimetableExportController {
    
    private final TimetableExportService exportService;
    
    @PostMapping("/{timetableId}/export")
    @Operation(summary = "Export timetable in specified format")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ByteArrayResource> exportTimetable(
            @PathVariable Long timetableId,
            @Valid @RequestBody TimetableExportRequest request) {
        
        log.debug("Exporting timetable {} in format {}", timetableId, request.getFormat());
        
        try {
            byte[] exportData = exportService.exportTimetable(timetableId, request);
            
            String filename = generateFilename(timetableId, request.getFormat());
            String contentType = getContentType(request.getFormat());
            
            ByteArrayResource resource = new ByteArrayResource(exportData);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(exportData.length)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Failed to export timetable {}: {}", timetableId, e.getMessage(), e);
            throw new RuntimeException("Export failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/{timetableId}/export/preview")
    @Operation(summary = "Preview timetable export")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<Object>> previewExport(
            @PathVariable Long timetableId,
            @RequestParam String format) {
        
        log.debug("Previewing timetable {} export in format {}", timetableId, format);
        
        Object preview = exportService.previewExport(timetableId, format);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Export preview generated", preview));
    }
    
    @GetMapping("/export/templates")
    @Operation(summary = "Get available export templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    public ResponseEntity<ApiSuccessResponse<Object>> getExportTemplates() {
        log.debug("Getting available export templates");
        
        Object templates = exportService.getAvailableTemplates();
        return ResponseEntity.ok(new ApiSuccessResponse<>("Export templates retrieved", templates));
    }
    
    private String generateFilename(Long timetableId, String format) {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );
        return String.format("timetable_%d_%s.%s", timetableId, timestamp, format.toLowerCase());
    }
    
    private String getContentType(String format) {
        switch (format.toUpperCase()) {
            case "PDF":
                return "application/pdf";
            case "EXCEL":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "CSV":
                return "text/csv";
            default:
                return "application/octet-stream";
        }
    }
}
