package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
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
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.operational.dto.TimetableSlotResponseDto;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;

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
    private final TimetableSlotRepository timetableSlotRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final PeriodRepository periodRepository;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;

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
            .orElseThrow(() -> new RuntimeException("Timetable not found"));
        
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

    @Operation(summary = "Create sample timetable data for a teacher (for testing)")
    @PostMapping("/teacher/{teacherId}/create-sample-data")
    public ResponseEntity<ApiSuccessResponse<String>> createSampleTeacherData(@PathVariable Long teacherId) {
        log.debug("Creating sample timetable data for teacher: {}", teacherId);
        
        try {
            // Get teacher
            var teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
            
            // Get some classes, courses, periods, and rooms
            var classes = classRepository.findAll();
            var courses = courseRepository.findAll();
            var periods = periodRepository.findAll();
            var rooms = roomRepository.findAll();
            
            if (classes.isEmpty() || courses.isEmpty() || periods.isEmpty() || rooms.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiSuccessResponse<>("error", "Missing required data (classes, courses, periods, or rooms)"));
            }
            
            // Create sample slots
            List<TimetableSlot> slots = new ArrayList<>();
            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY};
            
            for (int dayIndex = 0; dayIndex < days.length; dayIndex++) {
                for (int periodIndex = 0; periodIndex < Math.min(3, periods.size()); periodIndex++) {
                    TimetableSlot slot = new TimetableSlot();
                    slot.setDayOfWeek(days[dayIndex]);
                    slot.setPeriod(periods.get(periodIndex));
                    slot.setTeacher(teacher);
                    slot.setForClass(classes.get(dayIndex % classes.size()));
                    slot.setForCourse(courses.get(periodIndex % courses.size()));
                    slot.setRoom(rooms.get((dayIndex + periodIndex) % rooms.size()));
                    slot.setDescription(String.format("Sample: %s - %s", 
                        courses.get(periodIndex % courses.size()).getName(),
                        classes.get(dayIndex % classes.size()).getName()));
                    
                    slots.add(slot);
                }
            }
            
            timetableSlotRepository.saveAll(slots);
            
            return ResponseEntity.ok(new ApiSuccessResponse<>("Sample timetable data created successfully", 
                String.format("Created %d sample slots for teacher %s", slots.size(), teacher.getFirstName())));
                
        } catch (Exception e) {
            log.error("Error creating sample data for teacher {}: {}", teacherId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiSuccessResponse<>("error", "Failed to create sample data: " + e.getMessage()));
        }
    }

    @Operation(summary = "Debug: Check database state for teacher timetables (NO AUTH)")
    @GetMapping("/debug/teacher/{teacherId}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> debugTeacherData(@PathVariable Long teacherId) {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            // Check if teacher exists
            var teacher = teacherRepository.findById(teacherId);
            debug.put("teacherExists", teacher.isPresent());
            if (teacher.isPresent()) {
                debug.put("teacherId", teacher.get().getId());
                debug.put("teacherName", teacher.get().getFirstName() + " " + teacher.get().getLastName());
                debug.put("teacherEmail", teacher.get().getEmail());
                debug.put("subjectsTaught", teacher.get().getSubjectsTaught());
                debug.put("weeklyCapacity", teacher.get().getWeeklyCapacity());
            }
            
            // Check timetable slots for this teacher
            List<TimetableSlot> slots = timetableService.getSlotsByTeacherId(teacherId);
            debug.put("slotsCount", slots.size());
            debug.put("slots", slots.stream().map(slot -> {
                Map<String, Object> slotInfo = new HashMap<>();
                slotInfo.put("id", slot.getId());
                slotInfo.put("day", slot.getDayOfWeek());
                slotInfo.put("period", slot.getPeriod() != null ? slot.getPeriod().getIndex() : "No Period");
                slotInfo.put("course", slot.getForCourse() != null ? slot.getForCourse().getName() : "No Course");
                slotInfo.put("class", slot.getForClass() != null ? slot.getForClass().getName() : "No Class");
                slotInfo.put("room", slot.getRoom() != null ? slot.getRoom().getName() : "No Room");
                slotInfo.put("description", slot.getDescription());
                return slotInfo;
            }).collect(Collectors.toList()));
            
            // Check total slots in database
            debug.put("totalSlotsInDB", timetableSlotRepository.count());
            
            // Check all teachers count
            debug.put("totalTeachers", teacherRepository.count());
            
            // Check all courses
            var courses = courseRepository.findAll();
            debug.put("totalCourses", courses.size());
            debug.put("courseNames", courses.stream().map(c -> c.getName()).collect(Collectors.toList()));
            
            // Check all classes
            var classes = classRepository.findAll();
            debug.put("totalClasses", classes.size());
            debug.put("classNames", classes.stream().map(c -> c.getName()).collect(Collectors.toList()));
            
            // Check if any slots exist for any teacher
            List<TimetableSlot> allSlots = timetableSlotRepository.findAll();
            debug.put("allSlotsCount", allSlots.size());
            debug.put("slotsWithTeachers", allSlots.stream()
                .filter(slot -> slot.getTeacher() != null)
                .map(slot -> "Teacher " + slot.getTeacher().getId() + " (" + 
                    slot.getTeacher().getFirstName() + " " + slot.getTeacher().getLastName() + ") - " +
                    slot.getDayOfWeek() + " " + slot.getPeriod().getIndex())
                .collect(Collectors.toList()));
                
            return ResponseEntity.ok(new ApiSuccessResponse<>("Debug data retrieved successfully", debug));
            
        } catch (Exception e) {
            debug.put("error", e.getMessage());
            debug.put("stackTrace", e.getStackTrace());
            return ResponseEntity.ok(new ApiSuccessResponse<>("Debug data with error", debug));
        }
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

    @Operation(summary = "Debug optimization and return raw data")
    @PostMapping("/debug/class/{classId}/optimize")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> debugOptimization(@PathVariable Long classId) {
        log.debug("Debug optimization for class: {}", classId);
        try {
            // Perform optimization
            timetableService.optimizeTimetableForClass(classId);
            
            // Get the results
            List<TimetableSlot> slots = timetableService.getSlotsByClassId(classId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Optimization completed");
            result.put("slotsCreated", slots.size());
            result.put("slots", slots.stream().map(slot -> {
                Map<String, Object> slotData = new HashMap<>();
                slotData.put("id", slot.getId());
                slotData.put("dayOfWeek", slot.getDayOfWeek());
                slotData.put("description", slot.getDescription());
                slotData.put("periodId", slot.getPeriod() != null ? slot.getPeriod().getId() : null);
                slotData.put("periodIndex", slot.getPeriod() != null ? slot.getPeriod().getIndex() : null);
                slotData.put("classId", slot.getForClass() != null ? slot.getForClass().getId() : null);
                slotData.put("courseId", slot.getForCourse() != null ? slot.getForCourse().getId() : null);
                slotData.put("courseName", slot.getForCourse() != null ? slot.getForCourse().getName() : null);
                slotData.put("teacherId", slot.getTeacher() != null ? slot.getTeacher().getId() : null);
                slotData.put("teacherName", slot.getTeacher() != null ? 
                    slot.getTeacher().getFirstName() + " " + slot.getTeacher().getLastName() : null);
                slotData.put("roomId", slot.getRoom() != null ? slot.getRoom().getId() : null);
                slotData.put("roomName", slot.getRoom() != null ? slot.getRoom().getName() : null);
                slotData.put("timetableId", slot.getTimetable() != null ? slot.getTimetable().getId() : null);
                return slotData;
            }).collect(Collectors.toList()));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during debug optimization: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("stackTrace", e.getStackTrace());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Test optimization and return results")
    @PostMapping("/test/class/{classId}/optimize")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> testOptimization(@PathVariable Long classId) {
        log.debug("Testing optimization for class: {}", classId);
        try {
            // Perform optimization
            timetableService.optimizeTimetableForClass(classId);
            
            // Get the results
            List<TimetableSlot> slots = timetableService.getSlotsByClassId(classId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Optimization completed successfully");
            result.put("slotsCreated", slots.size());
            result.put("classId", classId);
            
            // Add slot details
            List<Map<String, Object>> slotDetails = slots.stream()
                .map(slot -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("day", slot.getDayOfWeek());
                    detail.put("period", slot.getPeriod().getIndex());
                    detail.put("course", slot.getForCourse() != null ? slot.getForCourse().getName() : "N/A");
                    detail.put("teacher", slot.getTeacher() != null ? 
                        slot.getTeacher().getFirstName() + " " + slot.getTeacher().getLastName() : "N/A");
                    detail.put("room", slot.getRoom() != null ? slot.getRoom().getName() : "N/A");
                    return detail;
                })
                .collect(Collectors.toList());
            
            result.put("slots", slotDetails);
            
            return ResponseEntity.ok(new ApiSuccessResponse<>("Optimization test completed", result));
            
        } catch (Exception e) {
            log.error("Error during test optimization for class: {}", classId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("classId", classId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiSuccessResponse<>("Optimization test failed", errorResult));
        }
    }

    @Operation(summary = "Get teachers with their courses for timetable assignment")
    @GetMapping("/teachers-with-courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiSuccessResponse<List<Map<String, Object>>>> getTeachersWithCourses() {
        log.debug("Getting teachers with their courses for timetable assignment");
        try {
            List<Map<String, Object>> teachersWithCourses = teacherRepository.findAll().stream()
                .map(teacher -> {
                    Map<String, Object> teacherData = new HashMap<>();
                    teacherData.put("id", teacher.getId());
                    teacherData.put("firstName", teacher.getFirstName());
                    teacherData.put("lastName", teacher.getLastName());
                    teacherData.put("email", teacher.getEmail());
                    teacherData.put("subjectsTaught", teacher.getSubjectsTaught());
                    teacherData.put("weeklyCapacity", teacher.getWeeklyCapacity());
                    
                    // Get courses taught by this teacher
                    List<Map<String, Object>> courses = teacher.getClasses().stream()
                        .flatMap(clazz -> clazz.getCourses().stream())
                        .filter(course -> course.getTeacher() != null && course.getTeacher().getId().equals(teacher.getId()))
                        .distinct()
                        .map(course -> {
                            Map<String, Object> courseData = new HashMap<>();
                            courseData.put("id", course.getId());
                            courseData.put("name", course.getName());
                            courseData.put("code", course.getCode());
                            courseData.put("color", course.getColor());
                            courseData.put("weeklyCapacity", course.getWeeklyCapacity());
                            return courseData;
                        })
                        .collect(Collectors.toList());
                    
                    teacherData.put("courses", courses);
                    
                    // Calculate current workload (number of assigned slots)
                    long currentSlots = timetableSlotRepository.countByTeacherId(teacher.getId());
                    teacherData.put("currentWorkload", currentSlots);
                    teacherData.put("availability", teacher.getWeeklyCapacity() != null ? 
                        Math.max(0, teacher.getWeeklyCapacity() - currentSlots) : "unlimited");
                    
                    return teacherData;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new ApiSuccessResponse<>("Teachers with courses retrieved successfully", teachersWithCourses));
            
        } catch (Exception e) {
            log.error("Error getting teachers with courses: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiSuccessResponse<>("Failed to get teachers with courses", null));
        }
    }
} 