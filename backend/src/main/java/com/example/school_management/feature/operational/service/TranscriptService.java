package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.dto.TranscriptDto;
import com.example.school_management.feature.operational.dto.TranscriptSummaryDto;

import java.time.LocalDate;

public interface TranscriptService {
    
    // Generate complete transcript for a student
    TranscriptDto generateTranscript(Long studentId);
    
    // Generate transcript for a specific period
    TranscriptDto generateTranscriptForPeriod(Long studentId, LocalDate startDate, LocalDate endDate);
    
    // Generate transcript summary (overview)
    TranscriptSummaryDto generateTranscriptSummary(Long studentId);
    
    // Export transcript as PDF
    byte[] exportTranscriptAsPdf(Long studentId);
    
    // Export transcript as PDF for a specific period
    byte[] exportTranscriptAsPdf(Long studentId, LocalDate startDate, LocalDate endDate);
} 