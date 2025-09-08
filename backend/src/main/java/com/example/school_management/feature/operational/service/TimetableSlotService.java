package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.TimetableSlotRequest;
import com.example.school_management.feature.operational.entity.TimetableSlot;

public interface TimetableSlotService {
    
    /**
     * Create a new timetable slot
     */
    TimetableSlot createSlot(TimetableSlotRequest request);
    
    /**
     * Update an existing timetable slot
     */
    TimetableSlot updateSlot(Long slotId, TimetableSlotRequest request);
    
    /**
     * Delete a timetable slot
     */
    void deleteSlot(Long slotId);
    
    /**
     * Get a timetable slot by ID
     */
    TimetableSlot getSlot(Long slotId);
}
