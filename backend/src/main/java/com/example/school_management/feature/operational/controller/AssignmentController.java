package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Tag(name = "Assignments", description = "Endpoints for managing assignments")
@SecurityRequirement(name = "bearerAuth")
public class AssignmentController {

    // Temporary DTOs - these should be moved to proper DTO classes later
    public static class AssignmentDto {
        public Long id;
        public String title;
        public String description;
        public LocalDateTime dueDate;
        public String status;
        public Long teacherId;
        public String teacherName;
        public Long classId;
        public String className;
        public Long courseId;
        public String courseName;
        public String priority;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;

        public AssignmentDto(Long id, String title, String description, LocalDateTime dueDate, 
                           String status, Long teacherId, String teacherName, Long classId, 
                           String className, Long courseId, String courseName, String priority) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.status = status;
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.classId = classId;
            this.className = className;
            this.courseId = courseId;
            this.courseName = courseName;
            this.priority = priority;
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
        }
    }

    public static class CreateAssignmentRequest {
        public String title;
        public String description;
        public LocalDateTime dueDate;
        public Long classId;
        public Long courseId;
        public String priority;
    }

    public static class UpdateAssignmentRequest {
        public String title;
        public String description;
        public LocalDateTime dueDate;
        public String status;
        public String priority;
    }

    @Operation(summary = "Get all assignments with pagination and filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignments retrieved successfully")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<PageDto<AssignmentDto>>> getAllAssignments(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId) {
        
        log.debug("GET /assignments - page: {}, search: {}, status: {}, classId: {}, courseId: {}", 
                  pageable.getPageNumber(), search, status, classId, courseId);
        
        // Mock data - replace with actual service call
        List<AssignmentDto> assignments = getMockAssignments();
        
        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            assignments = assignments.stream()
                    .filter(a -> a.title.toLowerCase().contains(search.toLowerCase()) ||
                               a.description.toLowerCase().contains(search.toLowerCase()))
                    .toList();
        }
        
        if (status != null) {
            assignments = assignments.stream()
                    .filter(a -> a.status.equals(status))
                    .toList();
        }
        
        if (classId != null) {
            assignments = assignments.stream()
                    .filter(a -> a.classId.equals(classId))
                    .toList();
        }
        
        if (courseId != null) {
            assignments = assignments.stream()
                    .filter(a -> a.courseId.equals(courseId))
                    .toList();
        }
        
        Page<AssignmentDto> page = new PageImpl<>(assignments, pageable, assignments.size());
        var dto = new PageDto<>(page);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Assignments retrieved successfully", dto));
    }

    @Operation(summary = "Get assignment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment found"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<AssignmentDto>> getAssignmentById(@PathVariable Long id) {
        log.debug("GET /assignments/{}", id);
        
        // Mock data - replace with actual service call
        AssignmentDto assignment = getMockAssignments().stream()
                .filter(a -> a.id.equals(id))
                .findFirst()
                .orElse(null);
        
        if (assignment == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Assignment retrieved successfully", assignment));
    }

    @Operation(summary = "Get assignments by teacher")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Teacher assignments retrieved successfully")
    })
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<PageDto<AssignmentDto>>> getAssignmentsByTeacher(
            @PathVariable Long teacherId,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        log.debug("GET /assignments/teacher/{} - page: {}, search: {}, status: {}", 
                  teacherId, pageable.getPageNumber(), search, status);
        
        // Mock data - replace with actual service call
        List<AssignmentDto> assignments = getMockAssignments().stream()
                .filter(a -> a.teacherId.equals(teacherId))
                .toList();
        
        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            assignments = assignments.stream()
                    .filter(a -> a.title.toLowerCase().contains(search.toLowerCase()) ||
                               a.description.toLowerCase().contains(search.toLowerCase()))
                    .toList();
        }
        
        if (status != null) {
            assignments = assignments.stream()
                    .filter(a -> a.status.equals(status))
                    .toList();
        }
        
        Page<AssignmentDto> page = new PageImpl<>(assignments, pageable, assignments.size());
        var dto = new PageDto<>(page);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher assignments retrieved successfully", dto));
    }

    @Operation(summary = "Get assignments by class")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Class assignments retrieved successfully")
    })
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<PageDto<AssignmentDto>>> getAssignmentsByClass(
            @PathVariable Long classId,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        log.debug("GET /assignments/class/{} - page: {}, search: {}, status: {}", 
                  classId, pageable.getPageNumber(), search, status);
        
        // Mock data - replace with actual service call
        List<AssignmentDto> assignments = getMockAssignments().stream()
                .filter(a -> a.classId.equals(classId))
                .toList();
        
        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            assignments = assignments.stream()
                    .filter(a -> a.title.toLowerCase().contains(search.toLowerCase()) ||
                               a.description.toLowerCase().contains(search.toLowerCase()))
                    .toList();
        }
        
        if (status != null) {
            assignments = assignments.stream()
                    .filter(a -> a.status.equals(status))
                    .toList();
        }
        
        Page<AssignmentDto> page = new PageImpl<>(assignments, pageable, assignments.size());
        var dto = new PageDto<>(page);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class assignments retrieved successfully", dto));
    }

    @Operation(summary = "Get assignments by course")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course assignments retrieved successfully")
    })
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
    public ResponseEntity<ApiSuccessResponse<PageDto<AssignmentDto>>> getAssignmentsByCourse(
            @PathVariable Long courseId,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        log.debug("GET /assignments/course/{} - page: {}, search: {}, status: {}", 
                  courseId, pageable.getPageNumber(), search, status);
        
        // Mock data - replace with actual service call
        List<AssignmentDto> assignments = getMockAssignments().stream()
                .filter(a -> a.courseId.equals(courseId))
                .toList();
        
        // Apply filters
        if (search != null && !search.trim().isEmpty()) {
            assignments = assignments.stream()
                    .filter(a -> a.title.toLowerCase().contains(search.toLowerCase()) ||
                               a.description.toLowerCase().contains(search.toLowerCase()))
                    .toList();
        }
        
        if (status != null) {
            assignments = assignments.stream()
                    .filter(a -> a.status.equals(status))
                    .toList();
        }
        
        Page<AssignmentDto> page = new PageImpl<>(assignments, pageable, assignments.size());
        var dto = new PageDto<>(page);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Course assignments retrieved successfully", dto));
    }

    @Operation(summary = "Create a new assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assignment created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<AssignmentDto>> createAssignment(
            @Valid @RequestBody CreateAssignmentRequest request) {
        
        log.debug("POST /assignments - Creating assignment: {}", request.title);
        
        // Mock creation - replace with actual service call
        AssignmentDto assignment = new AssignmentDto(
                System.currentTimeMillis(), // Mock ID
                request.title,
                request.description,
                request.dueDate,
                "draft",
                322L, // Mock teacher ID
                "Test Teacher",
                request.classId,
                "Mock Class",
                request.courseId,
                "Mock Course",
                request.priority != null ? request.priority : "medium"
        );
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Assignment created successfully", assignment));
    }

    @Operation(summary = "Update an assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment updated successfully"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<AssignmentDto>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        
        log.debug("PUT /assignments/{} - Updating assignment", id);
        
        // Mock update - replace with actual service call
        AssignmentDto assignment = getMockAssignments().stream()
                .filter(a -> a.id.equals(id))
                .findFirst()
                .orElse(null);
        
        if (assignment == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Update fields
        if (request.title != null) assignment.title = request.title;
        if (request.description != null) assignment.description = request.description;
        if (request.dueDate != null) assignment.dueDate = request.dueDate;
        if (request.status != null) assignment.status = request.status;
        if (request.priority != null) assignment.priority = request.priority;
        assignment.updatedAt = LocalDateTime.now();
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Assignment updated successfully", assignment));
    }

    @Operation(summary = "Delete an assignment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteAssignment(@PathVariable Long id) {
        log.debug("DELETE /assignments/{}", id);
        
        // Mock deletion - replace with actual service call
        return ResponseEntity.ok(new ApiSuccessResponse<>("Assignment deleted successfully", null));
    }

    // Mock data method - replace with actual service integration
    private List<AssignmentDto> getMockAssignments() {
        List<AssignmentDto> assignments = new ArrayList<>();
        
        assignments.add(new AssignmentDto(
                1L, "Math Homework Chapter 5", "Complete exercises 1-20 from Chapter 5",
                LocalDateTime.now().plusDays(3), "published", 322L, "Test Teacher",
                1L, "5A", 1L, "Mathematics", "high"
        ));
        
        assignments.add(new AssignmentDto(
                2L, "Physics Lab Report", "Write a lab report on the pendulum experiment",
                LocalDateTime.now().plusDays(7), "published", 322L, "Test Teacher",
                2L, "3B", 2L, "Physics", "medium"
        ));
        
        assignments.add(new AssignmentDto(
                3L, "History Essay", "Write a 500-word essay on World War II",
                LocalDateTime.now().plusDays(10), "draft", 322L, "Test Teacher",
                1L, "5A", 3L, "History", "medium"
        ));
        
        return assignments;
    }
}
