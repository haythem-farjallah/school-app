package com.example.school_management.feature.operational.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimetableExportRequest {
    
    @NotNull(message = "Export format is required")
    @Pattern(regexp = "PDF|EXCEL|CSV", message = "Format must be PDF, EXCEL, or CSV")
    private String format;
    
    // Content Options
    @Builder.Default
    private Boolean includeTeacherInfo = true;
    @Builder.Default
    private Boolean includeRoomInfo = true;
    @Builder.Default
    private Boolean includeConflicts = false;
    @Builder.Default
    private Boolean includeStatistics = false;
    @Builder.Default
    private Boolean includeEmptySlots = true;
    
    // Filtering Options
    private List<Long> teacherIds;
    private List<Long> classIds;
    private List<Long> courseIds;
    private List<Long> roomIds;
    private List<String> daysOfWeek;
    
    // Date Range (optional)
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Formatting Options
    @Builder.Default
    private String orientation = "LANDSCAPE"; // PORTRAIT, LANDSCAPE
    @Builder.Default
    private String pageSize = "A4"; // A4, A3, LETTER
    @Builder.Default
    private Boolean colorCoded = true;
    @Builder.Default
    private String theme = "PROFESSIONAL"; // PROFESSIONAL, MINIMAL, COLORFUL
    
    // Custom Options
    private String customTitle;
    private String customHeader;
    private String customFooter;
    private Map<String, Object> additionalOptions;
    
    // Template Options
    private String templateId;
    @Builder.Default
    private Boolean useCustomTemplate = false;
    
    // Export Metadata
    @Builder.Default
    private Boolean includeMetadata = true;
    @Builder.Default
    private Boolean includeExportDate = true;
    @Builder.Default
    private Boolean includeWatermark = false;
    private String watermarkText;
    
    // Advanced Options
    @Builder.Default
    private Integer fontSize = 10;
    @Builder.Default
    private Boolean compactView = false;
    @Builder.Default
    private Boolean showGridLines = true;
    @Builder.Default
    private Boolean showTimeLabels = true;
    @Builder.Default
    private Boolean groupByClass = false;
    @Builder.Default
    private Boolean groupByTeacher = false;
    
    // Quality Settings (for PDF)
    @Builder.Default
    private String quality = "HIGH"; // LOW, MEDIUM, HIGH
    @Builder.Default
    private Boolean optimizeForPrint = true;
    
    // Excel Specific Options
    @Builder.Default
    private Boolean createSeparateSheets = false;
    @Builder.Default
    private Boolean includeFormulas = false;
    @Builder.Default
    private Boolean protectWorkbook = false;
    
    // CSV Specific Options
    @Builder.Default
    private String delimiter = ",";
    @Builder.Default
    private String encoding = "UTF-8";
    @Builder.Default
    private Boolean includeHeaders = true;
}
