package com.example.school_management.feature.operational.controller;

import com.example.school_management.feature.operational.dto.TranscriptDto;
import com.example.school_management.feature.operational.dto.TranscriptSummaryDto;
import com.example.school_management.feature.operational.service.TranscriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/transcripts")
@RequiredArgsConstructor
@Tag(name = "Transcript Management", description = "APIs for generating and exporting academic transcripts")
public class TranscriptController {

    private final TranscriptService transcriptService;

    @GetMapping("/{studentId}")
    @Operation(summary = "Generate complete transcript for a student")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF') or @securityService.isCurrentUser(#studentId)")
    public ResponseEntity<TranscriptDto> generateTranscript(@PathVariable Long studentId) {
        log.debug("Generating transcript for student: {}", studentId);
        
        TranscriptDto transcript = transcriptService.generateTranscript(studentId);
        return ResponseEntity.ok(transcript);
    }

    @GetMapping("/{studentId}/period")
    @Operation(summary = "Generate transcript for a specific period")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF') or @securityService.isCurrentUser(#studentId)")
    public ResponseEntity<TranscriptDto> generateTranscriptForPeriod(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Generating transcript for student {} from {} to {}", studentId, startDate, endDate);
        
        TranscriptDto transcript = transcriptService.generateTranscriptForPeriod(studentId, startDate, endDate);
        return ResponseEntity.ok(transcript);
    }

    @GetMapping("/{studentId}/summary")
    @Operation(summary = "Generate transcript summary (overview)")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF') or @securityService.isCurrentUser(#studentId)")
    public ResponseEntity<TranscriptSummaryDto> generateTranscriptSummary(@PathVariable Long studentId) {
        log.debug("Generating transcript summary for student: {}", studentId);
        
        TranscriptSummaryDto summary = transcriptService.generateTranscriptSummary(studentId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{studentId}/pdf")
    @Operation(summary = "Export transcript as PDF")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF') or @securityService.isCurrentUser(#studentId)")
    public ResponseEntity<byte[]> exportTranscriptAsPdf(@PathVariable Long studentId) {
        log.debug("Exporting transcript as PDF for student: {}", studentId);
        
        byte[] pdfBytes = transcriptService.exportTranscriptAsPdf(studentId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "transcript_" + studentId + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{studentId}/pdf/period")
    @Operation(summary = "Export transcript as PDF for a specific period")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STAFF') or @securityService.isCurrentUser(#studentId)")
    public ResponseEntity<byte[]> exportTranscriptAsPdfForPeriod(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Exporting transcript as PDF for student {} from {} to {}", studentId, startDate, endDate);
        
        byte[] pdfBytes = transcriptService.exportTranscriptAsPdf(studentId, startDate, endDate);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
                "transcript_" + studentId + "_" + startDate + "_to_" + endDate + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
} 