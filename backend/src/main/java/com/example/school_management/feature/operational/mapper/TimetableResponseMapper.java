package com.example.school_management.feature.operational.mapper;

import com.example.school_management.feature.operational.dto.TimetableResponseDto;
import com.example.school_management.feature.operational.dto.TimetableSlotResponseDto;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TimetableResponseMapper {
    
    public TimetableResponseDto toDto(Timetable timetable) {
        if (timetable == null) {
            return null;
        }
        
        TimetableResponseDto dto = new TimetableResponseDto();
        dto.setId(timetable.getId());
        dto.setName(timetable.getName());
        dto.setDescription(timetable.getDescription());
        dto.setAcademicYear(timetable.getAcademicYear());
        dto.setSemester(timetable.getSemester());
        dto.setCreatedAt(timetable.getCreatedAt());
        dto.setUpdatedAt(timetable.getUpdatedAt());
        
        // Convert slots to DTOs
        if (timetable.getSlots() != null) {
            List<TimetableSlotResponseDto> slotDtos = timetable.getSlots().stream()
                    .map(this::toSlotDto)
                    .collect(Collectors.toList());
            dto.setSlots(slotDtos);
        }
        
        return dto;
    }
    
    public TimetableSlotResponseDto toSlotDto(TimetableSlot slot) {
        if (slot == null) {
            return null;
        }
        
        TimetableSlotResponseDto dto = new TimetableSlotResponseDto();
        dto.setId(slot.getId());
        dto.setDayOfWeek(slot.getDayOfWeek());
        dto.setDescription(slot.getDescription());
        dto.setCreatedAt(slot.getCreatedAt());
        dto.setUpdatedAt(slot.getUpdatedAt());
        
        // Period info (flattened)
        if (slot.getPeriod() != null) {
            dto.setPeriodId(slot.getPeriod().getId());
            dto.setPeriodIndex(slot.getPeriod().getIndex());
            dto.setPeriodStartTime(slot.getPeriod().getStartTime());
            dto.setPeriodEndTime(slot.getPeriod().getEndTime());
        }
        
        // Class info (flattened)
        if (slot.getForClass() != null) {
            dto.setForClassId(slot.getForClass().getId());
            dto.setForClassName(slot.getForClass().getName());
        }
        
        // Course info (flattened)
        if (slot.getForCourse() != null) {
            dto.setForCourseId(slot.getForCourse().getId());
            dto.setForCourseName(slot.getForCourse().getName());
            dto.setForCourseCode(slot.getForCourse().getCode());
            dto.setForCourseColor(slot.getForCourse().getColor());
        }
        
        // Teacher info (flattened)
        if (slot.getTeacher() != null) {
            dto.setTeacherId(slot.getTeacher().getId());
            dto.setTeacherFirstName(slot.getTeacher().getFirstName());
            dto.setTeacherLastName(slot.getTeacher().getLastName());
            dto.setTeacherEmail(slot.getTeacher().getEmail());
        }
        
        // Room info (flattened)
        if (slot.getRoom() != null) {
            dto.setRoomId(slot.getRoom().getId());
            dto.setRoomName(slot.getRoom().getName());
            dto.setRoomCapacity(slot.getRoom().getCapacity());
        }
        
        return dto;
    }
}

