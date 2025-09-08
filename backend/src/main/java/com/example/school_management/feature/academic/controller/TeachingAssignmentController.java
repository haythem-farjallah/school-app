package com.example.school_management.feature.academic.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.feature.academic.dto.CreateTeachingAssignmentDto;
import com.example.school_management.feature.academic.dto.TeachingAssignmentResponseDto;
import com.example.school_management.feature.academic.dto.UpdateTeachingAssignmentDto;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.mapper.TeachingAssignmentMapper;
import com.example.school_management.feature.academic.service.TeachingAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/teaching-assignments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teaching Assignments", description = "Teacher-Course linking management API")
@PreAuthorize("hasRole('ADMIN')")
public class TeachingAssignmentController {
    
    private final TeachingAssignmentService service;
    private final TeachingAssignmentMapper mapper;
    
    /* =================== CRUD OPERATIONS =================== */
    
    @Operation(summary = "Create a new teaching assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Teaching assignment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Assignment already exists")
    })
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<TeachingAssignmentResponseDto>> create(
            @Valid @RequestBody CreateTeachingAssignmentDto body) {
        log.debug("POST /admin/teaching-assignments - Creating teaching assignment");
        TeachingAssignment created = service.create(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", mapper.toDto(created)));
    }
    
    @Operation(summary = "Get teaching assignment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teaching assignment found"),
            @ApiResponse(responseCode = "404", description = "Teaching assignment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<TeachingAssignmentResponseDto>> get(@PathVariable long id) {
        log.debug("GET /admin/teaching-assignments/{}", id);
        TeachingAssignment assignment = service.find(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(assignment)));
    }
    
    @Operation(summary = "Update teaching assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teaching assignment updated successfully"),
            @ApiResponse(responseCode = "404", description = "Teaching assignment not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<TeachingAssignmentResponseDto>> patch(
            @PathVariable long id,
            @Valid @RequestBody UpdateTeachingAssignmentDto body) {
        log.debug("PATCH /admin/teaching-assignments/{}", id);
        TeachingAssignment updated = service.patch(id, body);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", mapper.toDto(updated)));
    }
    
    @Operation(summary = "Delete teaching assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teaching assignment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Teaching assignment not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable long id) {
        log.debug("DELETE /admin/teaching-assignments/{}", id);
        service.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }
    
    /* =================== LISTING & FILTERING =================== */
    
    @Operation(summary = "List teaching assignments with pagination and filters")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<TeachingAssignmentResponseDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search query (optional)") @RequestParam(required = false) String search,
            HttpServletRequest request) {
        
        log.debug("GET /admin/teaching-assignments - page={}, size={}, search={}", page, size, search);
        
        Page<TeachingAssignment> assignments = service.findWithAdvancedFilters(
                PageRequest.of(page, size),
                request.getParameterMap()
        );
        
        var dto = new PageDto<>(assignments.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    @Operation(summary = "Search teaching assignments")
    @GetMapping("/search")
    public ResponseEntity<ApiSuccessResponse<PageDto<TeachingAssignmentResponseDto>>> search(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search query") @RequestParam String q) {
        
        log.debug("GET /admin/teaching-assignments/search - page={}, size={}, q={}", page, size, q);
        
        Page<TeachingAssignment> assignments = service.search(PageRequest.of(page, size), q);
        var dto = new PageDto<>(assignments.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    @Operation(summary = "Advanced filtering for teaching assignments")
    @GetMapping("/filter")
    public ResponseEntity<ApiSuccessResponse<PageDto<TeachingAssignmentResponseDto>>> filter(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        log.debug("GET /admin/teaching-assignments/filter - Advanced filtering");
        
        Page<TeachingAssignment> assignments = service.findWithAdvancedFilters(
                PageRequest.of(page, size),
                request.getParameterMap()
        );
        
        var dto = new PageDto<>(assignments.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    /* =================== TEACHER-SPECIFIC ENDPOINTS =================== */
    
    @Operation(summary = "Get teaching assignments for a specific teacher")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiSuccessResponse<PageDto<TeachingAssignmentResponseDto>>> getByTeacher(
            @PathVariable Long teacherId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        log.debug("GET /admin/teaching-assignments/teacher/{}", teacherId);
        
        Page<TeachingAssignment> assignments = service.findByTeacherId(teacherId, PageRequest.of(page, size));
        var dto = new PageDto<>(assignments.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    @Operation(summary = "Get teaching assignments for a specific course")
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiSuccessResponse<PageDto<TeachingAssignmentResponseDto>>> getByCourse(
            @PathVariable Long courseId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        log.debug("GET /admin/teaching-assignments/course/{}", courseId);
        
        Page<TeachingAssignment> assignments = service.findByCourseId(courseId, PageRequest.of(page, size));
        var dto = new PageDto<>(assignments.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    @Operation(summary = "Get teaching assignments for a specific class")
    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiSuccessResponse<PageDto<TeachingAssignmentResponseDto>>> getByClass(
            @PathVariable Long classId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        log.debug("GET /admin/teaching-assignments/class/{}", classId);
        
        Page<TeachingAssignment> assignments = service.findByClassId(classId, PageRequest.of(page, size));
        var dto = new PageDto<>(assignments.map(mapper::toDto));
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", dto));
    }
    
    /* =================== BULK OPERATIONS =================== */
    
    @Operation(summary = "Bulk delete teaching assignments")
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiSuccessResponse<Void>> bulkDelete(
            @Parameter(description = "List of assignment IDs to delete") @RequestBody List<Long> ids) {
        log.debug("DELETE /admin/teaching-assignments/bulk {}", ids);
        service.bulkDelete(ids);
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", null));
    }
    
    @Operation(summary = "Assign teacher to multiple courses in a class")
    @PostMapping("/assign/teacher-to-courses")
    public ResponseEntity<ApiSuccessResponse<String>> assignTeacherToCourses(
            @RequestBody AssignTeacherToCoursesRequest request) {
        log.debug("POST /admin/teaching-assignments/assign/teacher-to-courses - teacher: {}, courses: {}, class: {}", 
                request.teacherId(), request.courseIds(), request.classId());
        
        service.assignTeacherToCourses(request.teacherId(), request.courseIds(), request.classId());
        
        return ResponseEntity.ok(new ApiSuccessResponse<>(
                "Teacher assigned to " + request.courseIds().size() + " courses successfully", null));
    }
    
    @Operation(summary = "Assign multiple teachers to a course in a class")
    @PostMapping("/assign/teachers-to-course")
    public ResponseEntity<ApiSuccessResponse<String>> assignTeachersToClass(
            @RequestBody AssignTeachersToClassRequest request) {
        log.debug("POST /admin/teaching-assignments/assign/teachers-to-course - teachers: {}, course: {}, class: {}", 
                request.teacherIds(), request.courseId(), request.classId());
        
        service.assignTeachersToClass(request.teacherIds(), request.classId(), request.courseId());
        
        return ResponseEntity.ok(new ApiSuccessResponse<>(
                request.teacherIds().size() + " teachers assigned to course successfully", null));
    }
    
    @Operation(summary = "Bulk create teaching assignments")
    @PostMapping("/bulk/create")
    public ResponseEntity<ApiSuccessResponse<String>> bulkCreate(
            @Parameter(description = "List of teaching assignments to create") 
            @RequestBody List<CreateTeachingAssignmentDto> assignments) {
        log.debug("POST /admin/teaching-assignments/bulk/create - {} assignments", assignments.size());
        
        service.bulkAssignTeachersToCourses(assignments);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>(
                "Bulk teaching assignments created successfully", null));
    }
    
    /* =================== REQUEST DTOs =================== */
    
    public record AssignTeacherToCoursesRequest(
            @Parameter(description = "Teacher ID") Long teacherId,
            @Parameter(description = "List of course IDs") List<Long> courseIds,
            @Parameter(description = "Class ID") Long classId
    ) {}
    
    public record AssignTeachersToClassRequest(
            @Parameter(description = "List of teacher IDs") List<Long> teacherIds,
            @Parameter(description = "Course ID") Long courseId,
            @Parameter(description = "Class ID") Long classId
    ) {}
}

