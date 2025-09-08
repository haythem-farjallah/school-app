package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.dto.TimetableExportRequest;
import com.example.school_management.feature.operational.service.TimetableExportService;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.repository.TimetableRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableExportServiceImpl implements TimetableExportService {
    
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    
    @Override
    public byte[] exportTimetable(Long timetableId, TimetableExportRequest request) {
        log.info("Exporting timetable {} in format {}", timetableId, request.getFormat());
        
        switch (request.getFormat().toUpperCase()) {
            case "PDF":
                return exportTimetablePdf(timetableId, request);
            case "EXCEL":
                return exportTimetableExcel(timetableId, request);
            case "CSV":
                return exportTimetableCsv(timetableId, request);
            default:
                throw new IllegalArgumentException("Unsupported export format: " + request.getFormat());
        }
    }
    
    @Override
    public Object previewExport(Long timetableId, String format) {
        log.debug("Generating preview for timetable {} in format {}", timetableId, format);
        
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));
        
        List<TimetableSlot> slots = timetableSlotRepository.findByTimetableId(timetableId);
        
        Map<String, Object> preview = new HashMap<>();
        preview.put("timetableName", timetable.getName());
        preview.put("academicYear", timetable.getAcademicYear());
        preview.put("semester", timetable.getSemester());
        preview.put("totalSlots", slots.size());
        preview.put("assignedSlots", slots.stream().filter(this::isSlotAssigned).count());
        preview.put("emptySlots", slots.stream().filter(slot -> !isSlotAssigned(slot)).count());
        preview.put("format", format);
        preview.put("estimatedPages", calculateEstimatedPages(slots, format));
        preview.put("estimatedSize", calculateEstimatedSize(slots, format));
        
        return preview;
    }
    
    @Override
    public Object getAvailableTemplates() {
        log.debug("Getting available export templates");
        
        List<Map<String, Object>> templates = new ArrayList<>();
        
        // Professional Template
        Map<String, Object> professional = new HashMap<>();
        professional.put("id", "professional");
        professional.put("name", "Professional");
        professional.put("description", "Clean, professional layout suitable for official documents");
        professional.put("features", Arrays.asList("Header/Footer", "Color coding", "Statistics"));
        professional.put("preview", "/templates/professional-preview.png");
        templates.add(professional);
        
        // Minimal Template
        Map<String, Object> minimal = new HashMap<>();
        minimal.put("id", "minimal");
        minimal.put("name", "Minimal");
        minimal.put("description", "Simple, clean layout with minimal styling");
        minimal.put("features", Arrays.asList("Basic grid", "No colors", "Compact"));
        minimal.put("preview", "/templates/minimal-preview.png");
        templates.add(minimal);
        
        // Colorful Template
        Map<String, Object> colorful = new HashMap<>();
        colorful.put("id", "colorful");
        colorful.put("name", "Colorful");
        colorful.put("description", "Vibrant colors with subject-based color coding");
        colorful.put("features", Arrays.asList("Subject colors", "Teacher highlights", "Visual appeal"));
        colorful.put("preview", "/templates/colorful-preview.png");
        templates.add(colorful);
        
        return Map.of("templates", templates, "defaultTemplate", "professional");
    }
    
    @Override
    public byte[] exportTimetablePdf(Long timetableId, TimetableExportRequest request) {
        log.debug("Exporting timetable {} as PDF", timetableId);
        
        try {
            Timetable timetable = getTimetable(timetableId);
            List<TimetableSlot> slots = getFilteredSlots(timetableId, request);
            
            // For now, return a simple PDF content as bytes
            // In a real implementation, you would use a PDF library like iText or Apache PDFBox
            String pdfContent = generatePdfContent(timetable, slots, request);
            return pdfContent.getBytes();
            
        } catch (Exception e) {
            log.error("Failed to export PDF for timetable {}: {}", timetableId, e.getMessage(), e);
            throw new RuntimeException("PDF export failed", e);
        }
    }
    
    @Override
    public byte[] exportTimetableExcel(Long timetableId, TimetableExportRequest request) {
        log.debug("Exporting timetable {} as Excel", timetableId);
        
        try {
            Timetable timetable = getTimetable(timetableId);
            List<TimetableSlot> slots = getFilteredSlots(timetableId, request);
            
            // For now, return a simple Excel-like CSV content
            // In a real implementation, you would use Apache POI
            String excelContent = generateExcelContent(timetable, slots, request);
            return excelContent.getBytes();
            
        } catch (Exception e) {
            log.error("Failed to export Excel for timetable {}: {}", timetableId, e.getMessage(), e);
            throw new RuntimeException("Excel export failed", e);
        }
    }
    
    @Override
    public byte[] exportTimetableCsv(Long timetableId, TimetableExportRequest request) {
        log.debug("Exporting timetable {} as CSV", timetableId);
        
        try {
            Timetable timetable = getTimetable(timetableId);
            List<TimetableSlot> slots = getFilteredSlots(timetableId, request);
            
            return generateCsvContent(timetable, slots, request).getBytes(request.getEncoding());
            
        } catch (Exception e) {
            log.error("Failed to export CSV for timetable {}: {}", timetableId, e.getMessage(), e);
            throw new RuntimeException("CSV export failed", e);
        }
    }
    
    // ===== HELPER METHODS =====
    
    private Timetable getTimetable(Long timetableId) {
        return timetableRepository.findById(timetableId)
                .orElseThrow(() -> new RuntimeException("Timetable not found: " + timetableId));
    }
    
    private List<TimetableSlot> getFilteredSlots(Long timetableId, TimetableExportRequest request) {
        List<TimetableSlot> slots = timetableSlotRepository.findByTimetableId(timetableId);
        
        return slots.stream()
                .filter(slot -> applyFilters(slot, request))
                .collect(Collectors.toList());
    }
    
    private boolean applyFilters(TimetableSlot slot, TimetableExportRequest request) {
        // Apply teacher filter
        if (request.getTeacherIds() != null && !request.getTeacherIds().isEmpty()) {
            if (slot.getTeacher() == null || !request.getTeacherIds().contains(slot.getTeacher().getId())) {
                return false;
            }
        }
        
        // Apply class filter
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            if (slot.getForClass() == null || !request.getClassIds().contains(slot.getForClass().getId())) {
                return false;
            }
        }
        
        // Apply course filter
        if (request.getCourseIds() != null && !request.getCourseIds().isEmpty()) {
            if (slot.getForCourse() == null || !request.getCourseIds().contains(slot.getForCourse().getId())) {
                return false;
            }
        }
        
        // Apply room filter
        if (request.getRoomIds() != null && !request.getRoomIds().isEmpty()) {
            if (slot.getRoom() == null || !request.getRoomIds().contains(slot.getRoom().getId())) {
                return false;
            }
        }
        
        // Apply day filter
        if (request.getDaysOfWeek() != null && !request.getDaysOfWeek().isEmpty()) {
            if (!request.getDaysOfWeek().contains(slot.getDayOfWeek().name())) {
                return false;
            }
        }
        
        // Apply empty slots filter
        if (!request.getIncludeEmptySlots() && !isSlotAssigned(slot)) {
            return false;
        }
        
        return true;
    }
    
    private boolean isSlotAssigned(TimetableSlot slot) {
        return slot.getForClass() != null && slot.getForCourse() != null && slot.getTeacher() != null;
    }
    
    private String generatePdfContent(Timetable timetable, List<TimetableSlot> slots, TimetableExportRequest request) {
        StringBuilder content = new StringBuilder();
        
        // Header
        content.append("TIMETABLE EXPORT - PDF FORMAT\n");
        content.append("=====================================\n\n");
        
        if (request.getCustomTitle() != null) {
            content.append("Title: ").append(request.getCustomTitle()).append("\n");
        }
        
        content.append("Timetable: ").append(timetable.getName()).append("\n");
        content.append("Academic Year: ").append(timetable.getAcademicYear()).append("\n");
        content.append("Semester: ").append(timetable.getSemester()).append("\n");
        content.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        // Statistics
        if (request.getIncludeStatistics()) {
            content.append("STATISTICS:\n");
            content.append("Total Slots: ").append(slots.size()).append("\n");
            content.append("Assigned Slots: ").append(slots.stream().filter(this::isSlotAssigned).count()).append("\n");
            content.append("Empty Slots: ").append(slots.stream().filter(slot -> !isSlotAssigned(slot)).count()).append("\n\n");
        }
        
        // Timetable Grid
        content.append("TIMETABLE GRID:\n");
        content.append("===============\n\n");
        
        // Group slots by day and period for better formatting
        Map<String, Map<Integer, TimetableSlot>> organizedSlots = organizeSlots(slots);
        
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"};
        
        for (String day : days) {
            content.append(day).append(":\n");
            Map<Integer, TimetableSlot> daySlots = organizedSlots.get(day);
            if (daySlots != null) {
                for (Map.Entry<Integer, TimetableSlot> entry : daySlots.entrySet()) {
                    TimetableSlot slot = entry.getValue();
                    content.append("  Period ").append(entry.getKey()).append(": ");
                    
                    if (isSlotAssigned(slot)) {
                        content.append(slot.getForCourse().getName())
                               .append(" - ").append(slot.getForClass().getName());
                        
                        if (request.getIncludeTeacherInfo() && slot.getTeacher() != null) {
                            content.append(" (").append(slot.getTeacher().getFirstName())
                                   .append(" ").append(slot.getTeacher().getLastName()).append(")");
                        }
                        
                        if (request.getIncludeRoomInfo() && slot.getRoom() != null) {
                            content.append(" [").append(slot.getRoom().getName()).append("]");
                        }
                    } else {
                        content.append("Free");
                    }
                    content.append("\n");
                }
            }
            content.append("\n");
        }
        
        // Footer
        if (request.getCustomFooter() != null) {
            content.append("\n").append(request.getCustomFooter()).append("\n");
        }
        
        return content.toString();
    }
    
    private String generateExcelContent(Timetable timetable, List<TimetableSlot> slots, TimetableExportRequest request) {
        StringBuilder content = new StringBuilder();
        
        // Excel-like format with tabs
        content.append("TIMETABLE EXPORT - EXCEL FORMAT\n");
        content.append("Sheet: Main Timetable\n\n");
        
        content.append("Timetable Name\t").append(timetable.getName()).append("\n");
        content.append("Academic Year\t").append(timetable.getAcademicYear()).append("\n");
        content.append("Semester\t").append(timetable.getSemester()).append("\n\n");
        
        // Headers
        content.append("Day\tPeriod\tCourse\tClass\tTeacher\tRoom\n");
        
        // Data rows
        for (TimetableSlot slot : slots) {
            if (isSlotAssigned(slot)) {
                content.append(slot.getDayOfWeek().name()).append("\t");
                content.append("Period " + slot.getPeriod().getIndex()).append("\t");
                content.append(slot.getForCourse().getName()).append("\t");
                content.append(slot.getForClass().getName()).append("\t");
                content.append(slot.getTeacher() != null ? 
                    slot.getTeacher().getFirstName() + " " + slot.getTeacher().getLastName() : "").append("\t");
                content.append(slot.getRoom() != null ? slot.getRoom().getName() : "").append("\n");
            }
        }
        
        return content.toString();
    }
    
    private String generateCsvContent(Timetable timetable, List<TimetableSlot> slots, TimetableExportRequest request) {
        StringBuilder content = new StringBuilder();
        String delimiter = request.getDelimiter();
        
        // Headers
        if (request.getIncludeHeaders()) {
            List<String> headers = new ArrayList<>();
            headers.add("Day");
            headers.add("Period");
            headers.add("Course");
            headers.add("Class");
            if (request.getIncludeTeacherInfo()) {
                headers.add("Teacher");
            }
            if (request.getIncludeRoomInfo()) {
                headers.add("Room");
            }
            headers.add("Description");
            
            content.append(String.join(delimiter, headers)).append("\n");
        }
        
        // Data rows
        for (TimetableSlot slot : slots) {
            if (isSlotAssigned(slot) || request.getIncludeEmptySlots()) {
                List<String> row = new ArrayList<>();
                row.add(slot.getDayOfWeek().name());
                row.add("Period " + slot.getPeriod().getIndex());
                row.add(slot.getForCourse() != null ? slot.getForCourse().getName() : "");
                row.add(slot.getForClass() != null ? slot.getForClass().getName() : "");
                
                if (request.getIncludeTeacherInfo()) {
                    row.add(slot.getTeacher() != null ? 
                        slot.getTeacher().getFirstName() + " " + slot.getTeacher().getLastName() : "");
                }
                
                if (request.getIncludeRoomInfo()) {
                    row.add(slot.getRoom() != null ? slot.getRoom().getName() : "");
                }
                
                row.add(slot.getDescription() != null ? slot.getDescription() : "");
                
                content.append(String.join(delimiter, row)).append("\n");
            }
        }
        
        return content.toString();
    }
    
    private Map<String, Map<Integer, TimetableSlot>> organizeSlots(List<TimetableSlot> slots) {
        Map<String, Map<Integer, TimetableSlot>> organized = new HashMap<>();
        
        for (TimetableSlot slot : slots) {
            String day = slot.getDayOfWeek().name();
            Integer periodIndex = slot.getPeriod().getIndex();
            
            organized.computeIfAbsent(day, k -> new HashMap<>()).put(periodIndex, slot);
        }
        
        return organized;
    }
    
    private int calculateEstimatedPages(List<TimetableSlot> slots, String format) {
        // Simple estimation based on slot count
        int slotsPerPage = format.equals("PDF") ? 50 : 100;
        return Math.max(1, (slots.size() + slotsPerPage - 1) / slotsPerPage);
    }
    
    private String calculateEstimatedSize(List<TimetableSlot> slots, String format) {
        // Simple size estimation
        int baseSize = switch (format.toUpperCase()) {
            case "PDF" -> slots.size() * 200; // ~200 bytes per slot
            case "EXCEL" -> slots.size() * 150; // ~150 bytes per slot
            case "CSV" -> slots.size() * 100; // ~100 bytes per slot
            default -> slots.size() * 100;
        };
        
        if (baseSize < 1024) {
            return baseSize + " bytes";
        } else if (baseSize < 1024 * 1024) {
            return (baseSize / 1024) + " KB";
        } else {
            return (baseSize / (1024 * 1024)) + " MB";
        }
    }
}
