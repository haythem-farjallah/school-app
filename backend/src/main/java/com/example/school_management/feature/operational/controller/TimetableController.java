package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.operational.dto.CreateTimetableRequest;
import com.example.school_management.feature.operational.dto.TimetableDto;
// import com.example.school_management.feature.operational.dto.UpdateTimetableRequest;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.repository.TimetableRepository;
import com.example.school_management.feature.operational.service.TimetablePdfService;
import com.example.school_management.feature.operational.service.TimetableService;
import com.example.school_management.feature.operational.service.TimetableSlotService;
import com.example.school_management.feature.operational.dto.TimetableSlotRequest;
import com.example.school_management.feature.operational.dto.TimetableResponseDto;
import com.example.school_management.feature.operational.mapper.TimetableResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;
import com.example.school_management.feature.operational.dto.TimetableSlotResponseDto;

@Slf4j
@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF')")
@Tag(name = "Timetables", description = "Endpoints for managing timetables and PDF exports")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class TimetableController {

    private final TimetablePdfService pdfService;
    private final TimetableService timetableService;
    private final TimetableSlotService timetableSlotService;
    private final TimetableResponseMapper timetableResponseMapper;
    private final TimetableRepository timetableRepository;

    // Timetable CRUD operations
    @Operation(summary = "Create a new timetable")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableDto>> create(@Valid @RequestBody CreateTimetableRequest request) {
        log.debug("Creating timetable: {}", request);
        TimetableDto timetable = timetableService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("Timetable created successfully", timetable));
    }

    // Temporarily commented out due to class loading issue
    /*
    @Operation(summary = "Update an existing timetable")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTimetableRequest request) {
        log.debug("Updating timetable {}: {}", id, request);
        TimetableDto timetable = timetableService.update(id, request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable updated successfully", timetable));
    }
    */

    @Operation(summary = "Delete a timetable")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        log.debug("Deleting timetable: {}", id);
        timetableService.delete(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable deleted successfully", null));
    }

    @Operation(summary = "Get timetable by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<TimetableDto>> get(@PathVariable Long id) {
        log.debug("Getting timetable: {}", id);
        TimetableDto timetable = timetableService.get(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable retrieved successfully", timetable));
    }

    @Operation(summary = "List all timetables with pagination and filtering")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<TimetableDto>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by academic year") @RequestParam(required = false) String academicYear,
            @Parameter(description = "Filter by semester") @RequestParam(required = false) String semester) {
        log.debug("Listing timetables - page: {}, size: {}, academicYear: {}, semester: {}", page, size, academicYear, semester);
        
        Pageable pageable = PageRequest.of(page, size);
        var timetables = timetableService.list(pageable, academicYear, semester);
        var dto = new PageDto<>(timetables);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetables retrieved successfully", dto));
    }

    @Operation(summary = "Get timetables by academic year")
    @GetMapping("/academic-year/{academicYear}")
    public ResponseEntity<ApiSuccessResponse<List<TimetableDto>>> getByAcademicYear(@PathVariable String academicYear) {
        log.debug("Getting timetables for academic year: {}", academicYear);
        List<TimetableDto> timetables = timetableService.findByAcademicYear(academicYear);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetables retrieved successfully", timetables));
    }

    @Operation(summary = "Get timetables by academic year and semester")
    @GetMapping("/academic-year/{academicYear}/semester/{semester}")
    public ResponseEntity<ApiSuccessResponse<List<TimetableDto>>> getByAcademicYearAndSemester(
            @PathVariable String academicYear,
            @PathVariable String semester) {
        log.debug("Getting timetables for academic year: {} and semester: {}", academicYear, semester);
        List<TimetableDto> timetables = timetableService.findByAcademicYearAndSemester(academicYear, semester);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetables retrieved successfully", timetables));
    }

    @Operation(summary = "Trigger timetable optimization")
    @PostMapping("/{timetableId}/optimize")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<String>> optimizeTimetable(@PathVariable Long timetableId) {
        log.debug("Optimizing timetable: {}", timetableId);
        
        // Get the timetable and find the associated classes
        Timetable timetable = timetableRepository.findById(timetableId)
            .orElseThrow(() -> new ResourceNotFoundException("Timetable not found"));
        
        if (timetable.getClasses() != null && !timetable.getClasses().isEmpty()) {
            // Optimize for the first class (in practice, you might want to optimize all classes)
            Long classId = timetable.getClasses().iterator().next().getId();
            timetableService.optimizeTimetableForClass(classId);
        } else {
            throw new RuntimeException("No class associated with this timetable");
        }
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("success", "Timetable optimization completed successfully."));
    }

    @Operation(summary = "Get class timetable as JSON")
    @GetMapping("/class/{classId}")
    public ResponseEntity<ApiSuccessResponse<TimetableResponseDto>> getClassTimetable(@PathVariable Long classId) {
        log.debug("Getting class timetable: {}", classId);
        
        // Get slots directly by class ID instead of through Timetable entity
        List<TimetableSlot> slots = timetableService.getSlotsByClassId(classId);
        log.info("Found {} slots for class {}", slots.size(), classId);
        
        // Log slot details for debugging
        for (TimetableSlot slot : slots) {
            log.debug("Slot: {} - {} - {} - {} - {}", 
                slot.getDayOfWeek(),
                slot.getPeriod() != null ? slot.getPeriod().getIndex() : "N/A",
                slot.getForCourse() != null ? slot.getForCourse().getName() : "N/A",
                slot.getTeacher() != null ? slot.getTeacher().getFirstName() + " " + slot.getTeacher().getLastName() : "N/A",
                slot.getRoom() != null ? slot.getRoom().getName() : "N/A"
            );
        }
        
        // Create a response DTO with the slots
        TimetableResponseDto dto = new TimetableResponseDto();
        dto.setId(0L); // Virtual timetable ID
        dto.setName("Timetable for Class " + classId);
        dto.setDescription("Class timetable with individual slots");
        dto.setAcademicYear("2024-2025");
        dto.setSemester("Fall");
        
        // Convert slots to DTOs using the mapper
        List<TimetableSlotResponseDto> slotDtos = slots.stream()
                .map(timetableResponseMapper::toSlotDto)
                .collect(Collectors.toList());
        dto.setSlots(slotDtos);
        
        log.info("Returning timetable DTO with {} slots", dto.getSlots() != null ? dto.getSlots().size() : 0);
        
        return ResponseEntity.ok(new ApiSuccessResponse<>("Class timetable retrieved successfully", dto));
    }

    @Operation(summary = "Get teacher timetable as JSON")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiSuccessResponse<List<TimetableSlot>>> getTeacherTimetable(@PathVariable Long teacherId) {
        log.debug("Getting teacher timetable: {}", teacherId);
        List<TimetableSlot> slots = timetableService.getSlotsByTeacherId(teacherId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Teacher timetable retrieved successfully", slots));
    }

    @Operation(summary = "Export timetable as PDF")
    @GetMapping("/{timetableId}/export/pdf")
    public ResponseEntity<byte[]> exportTimetablePdf(@PathVariable Long timetableId) {
        log.debug("Exporting timetable as PDF: {}", timetableId);
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new IllegalArgumentException("Timetable not found"));
        byte[] pdfBytes = pdfService.generateTimetablePdf(timetable);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"timetable-" + timetableId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @Operation(summary = "Export class timetable as PDF")
    @GetMapping("/class/{classId}/export/pdf")
    public ResponseEntity<byte[]> exportClassTimetablePdf(@PathVariable Long classId) {
        log.debug("Exporting class timetable as PDF: {}", classId);
        List<TimetableSlot> slots = timetableService.getSlotsByClassId(classId);
        byte[] pdfBytes = pdfService.generateClassTimetablePdf(classId, slots);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"class-timetable-" + classId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @Operation(summary = "Export teacher timetable as PDF")
    @GetMapping("/teacher/{teacherId}/export/pdf")
    public ResponseEntity<byte[]> exportTeacherTimetablePdf(@PathVariable Long teacherId) {
        log.debug("Exporting teacher timetable as PDF: {}", teacherId);
        List<TimetableSlot> slots = timetableService.getSlotsByTeacherId(teacherId);
        byte[] pdfBytes = pdfService.generateTeacherTimetablePdf(teacherId, slots);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"teacher-timetable-" + teacherId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @Operation(summary = "Update a timetable slot (manual edit)")
    @PutMapping("/slots/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableSlot>> updateSlot(
            @PathVariable Long slotId, 
            @Valid @RequestBody TimetableSlotRequest request) {
        log.debug("Updating timetable slot: {}", slotId);
        TimetableSlot slot = timetableSlotService.updateSlot(slotId, request);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable slot updated successfully", slot));
    }

    @Operation(summary = "Create a new timetable slot (manual edit)")
    @PostMapping("/slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<TimetableSlot>> createSlot(@Valid @RequestBody TimetableSlotRequest request) {
        log.debug("Creating timetable slot: {}", request);
        TimetableSlot slot = timetableSlotService.createSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("Timetable slot created successfully", slot));
    }

    @Operation(summary = "Delete a timetable slot (manual edit)")
    @DeleteMapping("/slots/{slotId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteSlot(@PathVariable Long slotId) {
        log.debug("Deleting timetable slot: {}", slotId);
        timetableSlotService.deleteSlot(slotId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable slot deleted successfully", null));
    }

    @Operation(summary = "Save timetable slots for a class")
    @PutMapping("/class/{classId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<String>> saveClassSlots(
            @PathVariable Long classId,
            @RequestBody List<TimetableSlot> slots) {
        log.debug("Saving {} slots for class: {}", slots.size(), classId);
        timetableService.saveSlotsForClass(classId, slots);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable slots saved successfully", "success"));
    }

    @Operation(summary = "Optimize timetable for a class")
    @PostMapping("/class/{classId}/optimize")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<String>> optimizeClassTimetable(@PathVariable Long classId) {
        log.debug("Optimizing timetable for class: {}", classId);
        timetableService.optimizeTimetableForClass(classId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Timetable optimization started", "success"));
    }
} 