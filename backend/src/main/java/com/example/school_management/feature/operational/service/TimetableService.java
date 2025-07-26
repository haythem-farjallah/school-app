package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.CreateTimetableRequest;
import com.example.school_management.feature.operational.dto.TimetableDto;
import com.example.school_management.feature.operational.dto.UpdateTimetableRequest;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TimetableService {
    
    // Timetable CRUD operations
    TimetableDto create(CreateTimetableRequest request);
    TimetableDto update(Long id, UpdateTimetableRequest request);
    void delete(Long id);
    TimetableDto get(Long id);
    Page<TimetableDto> list(Pageable pageable, String academicYear, String semester);
    List<TimetableDto> findByAcademicYear(String academicYear);
    List<TimetableDto> findByAcademicYearAndSemester(String academicYear, String semester);
    
    // TimetableSlot operations
    List<TimetableSlot> getSlotsByClassId(Long classId);
    List<TimetableSlot> getSlotsByTeacherId(Long teacherId);
    TimetableSlot updateSlot(Long slotId, TimetableSlot updatedSlot);
    TimetableSlot createSlot(TimetableSlot newSlot);
    void deleteSlot(Long slotId);
    
    // Class-specific operations
    void saveSlotsForClass(Long classId, List<TimetableSlot> slots);
    void optimizeTimetableForClass(Long classId);
} 