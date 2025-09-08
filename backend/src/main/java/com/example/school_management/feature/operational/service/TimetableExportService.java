package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.TimetableExportRequest;

public interface TimetableExportService {
    
    /**
     * Export timetable in the specified format
     */
    byte[] exportTimetable(Long timetableId, TimetableExportRequest request);
    
    /**
     * Generate a preview of the export
     */
    Object previewExport(Long timetableId, String format);
    
    /**
     * Get available export templates
     */
    Object getAvailableTemplates();
    
    /**
     * Export timetable as PDF
     */
    byte[] exportTimetablePdf(Long timetableId, TimetableExportRequest request);
    
    /**
     * Export timetable as Excel
     */
    byte[] exportTimetableExcel(Long timetableId, TimetableExportRequest request);
    
    /**
     * Export timetable as CSV
     */
    byte[] exportTimetableCsv(Long timetableId, TimetableExportRequest request);
}
