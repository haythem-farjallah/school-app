package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.service.TimetableService;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
@Tag(name = "Debug", description = "Debug endpoints (NO AUTH)")
public class TimetableDebugController {

    private final TimetableService timetableService;
    private final TimetableSlotRepository timetableSlotRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;

    @Operation(summary = "Debug: Check database state for teacher timetables (NO AUTH)")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiSuccessResponse<Map<String, Object>>> debugTeacherData(@PathVariable Long teacherId) {
        Map<String, Object> debug = new HashMap<>();
        
        try {
            log.info("DEBUG: Checking data for teacher ID: {}", teacherId);
            
            // Check if teacher exists
            var teacher = teacherRepository.findById(teacherId);
            debug.put("teacherExists", teacher.isPresent());
            if (teacher.isPresent()) {
                debug.put("teacherId", teacher.get().getId());
                debug.put("teacherName", teacher.get().getFirstName() + " " + teacher.get().getLastName());
                debug.put("teacherEmail", teacher.get().getEmail());
                debug.put("subjectsTaught", teacher.get().getSubjectsTaught());
                debug.put("weeklyCapacity", teacher.get().getWeeklyCapacity());
                log.info("DEBUG: Teacher found - {} {}, subjects: {}", 
                    teacher.get().getFirstName(), teacher.get().getLastName(), teacher.get().getSubjectsTaught());
            } else {
                log.warn("DEBUG: Teacher with ID {} not found", teacherId);
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
            log.info("DEBUG: Found {} slots for teacher {}", slots.size(), teacherId);
            
            // Check total slots in database
            long totalSlots = timetableSlotRepository.count();
            debug.put("totalSlotsInDB", totalSlots);
            log.info("DEBUG: Total slots in database: {}", totalSlots);
            
            // Check all teachers count
            long totalTeachers = teacherRepository.count();
            debug.put("totalTeachers", totalTeachers);
            
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
                    slot.getDayOfWeek() + " " + (slot.getPeriod() != null ? slot.getPeriod().getIndex() : "No Period"))
                .collect(Collectors.toList()));
                
            log.info("DEBUG: Found {} total slots, {} with teachers assigned", 
                allSlots.size(), allSlots.stream().filter(slot -> slot.getTeacher() != null).count());
                
            return ResponseEntity.ok(new ApiSuccessResponse<>("Debug data retrieved successfully", debug));
            
        } catch (Exception e) {
            log.error("DEBUG: Error occurred", e);
            debug.put("error", e.getMessage());
            debug.put("errorClass", e.getClass().getSimpleName());
            return ResponseEntity.ok(new ApiSuccessResponse<>("Debug data with error", debug));
        }
    }
}
