package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.academic.dto.TeacherClassDto;
import com.example.school_management.feature.academic.service.TeacherClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/classes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Classes", description = "Teacher's class management API")
@PreAuthorize("hasRole('TEACHER')")
@SecurityRequirement(name = "bearerAuth")
public class TeacherClassController {
    
    private final TeacherClassService teacherClassService;
    
    @Operation(summary = "Get teacher's assigned classes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classes retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<TeacherClassDto>>> getMyClasses(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search query (optional)") @RequestParam(required = false) String search,
            Authentication authentication) {
        
        log.debug("GET /api/v1/teacher/classes - teacher: {}, page: {}, size: {}, search: {}", 
                authentication.getName(), page, size, search);
        
        var classes = teacherClassService.getTeacherClasses(
                authentication.getName(), 
                PageRequest.of(page, size), 
                search
        );
        
        var dto = new PageDto<>(classes);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    @Operation(summary = "Get all teacher's assigned classes (no pagination)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Classes retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/all")
    public ResponseEntity<ApiSuccessResponse<List<TeacherClassDto>>> getAllMyClasses(
            @Parameter(description = "Search query (optional)") @RequestParam(required = false) String search,
            Authentication authentication) {
        
        log.debug("GET /api/v1/teacher/classes/all - teacher: {}, search: {}", 
                authentication.getName(), search);
        
        var classes = teacherClassService.getAllTeacherClasses(authentication.getName(), search);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", classes));
    }
    
    @Operation(summary = "Get teacher's class statistics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/stats")
    public ResponseEntity<ApiSuccessResponse<TeacherClassStatsDto>> getMyClassStats(
            Authentication authentication) {
        
        log.debug("GET /api/v1/teacher/classes/stats - teacher: {}", authentication.getName());
        
        var stats = teacherClassService.getTeacherClassStats(authentication.getName());
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", stats));
    }
    
    @Operation(summary = "Debug teacher information")
    @GetMapping("/debug")
    public ResponseEntity<ApiSuccessResponse<Object>> debugTeacherInfo(
            Authentication authentication) {
        
        log.info("🔍 GET /api/v1/teacher/classes/debug - teacher: {}", authentication.getName());
        
        try {
            var debugInfo = teacherClassService.getDebugInfo(authentication.getName());
            log.info("🔍 Debug info for teacher {}: {}", authentication.getName(), debugInfo);
            return ResponseEntity.ok(new ApiSuccessResponse<>("success", debugInfo));
        } catch (Exception e) {
            log.error("❌ Debug error for teacher {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.ok(new ApiSuccessResponse<>("error", Map.of(
                "error", e.getMessage(),
                "teacherEmail", authentication.getName(),
                "stackTrace", e.getStackTrace()
            )));
        }
    }
    
    public record TeacherClassStatsDto(
            int totalClasses,
            int totalStudents,
            double averageGrade,
            int totalCapacity,
            double capacityUsed
    ) {}
}
