package com.example.school_management.feature.operational.service;

import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetablePdfService {

    private final TemplateEngine templateEngine;

    public byte[] generateTimetablePdf(Timetable timetable) {
        try {
            // Prepare data for template
            Context context = new Context();
            context.setVariable("timetable", timetable);
            context.setVariable("slotsByDay", organizeSlotsByDay(timetable.getSlots()));
            context.setVariable("daysOfWeek", DayOfWeek.values());
            context.setVariable("generatedAt", new Date());
            
            // Generate HTML from template
            String htmlContent = templateEngine.process("timetable/timetable-pdf", context);
            
            // Convert HTML to PDF
            return convertHtmlToPdf(htmlContent);
            
        } catch (Exception e) {
            log.error("Error generating timetable PDF", e);
            throw new RuntimeException("Failed to generate timetable PDF", e);
        }
    }

    public byte[] generateClassTimetablePdf(Long classId, List<TimetableSlot> slots) {
        try {
            // Prepare data for template
            Context context = new Context();
            context.setVariable("classId", classId);
            context.setVariable("slotsByDay", organizeSlotsByDay(slots));
            context.setVariable("daysOfWeek", DayOfWeek.values());
            context.setVariable("generatedAt", new Date());
            
            // Generate HTML from template
            String htmlContent = templateEngine.process("timetable/class-timetable-pdf", context);
            
            // Convert HTML to PDF
            return convertHtmlToPdf(htmlContent);
            
        } catch (Exception e) {
            log.error("Error generating class timetable PDF", e);
            throw new RuntimeException("Failed to generate class timetable PDF", e);
        }
    }

    public byte[] generateTeacherTimetablePdf(Long teacherId, List<TimetableSlot> slots) {
        try {
            // Prepare data for template
            Context context = new Context();
            context.setVariable("teacherId", teacherId);
            context.setVariable("slotsByDay", organizeSlotsByDay(slots));
            context.setVariable("daysOfWeek", DayOfWeek.values());
            context.setVariable("generatedAt", new Date());
            
            // Generate HTML from template
            String htmlContent = templateEngine.process("timetable/teacher-timetable-pdf", context);
            
            // Convert HTML to PDF
            return convertHtmlToPdf(htmlContent);
            
        } catch (Exception e) {
            log.error("Error generating teacher timetable PDF", e);
            throw new RuntimeException("Failed to generate teacher timetable PDF", e);
        }
    }

    private Map<DayOfWeek, List<TimetableSlot>> organizeSlotsByDay(Collection<TimetableSlot> slots) {
        return slots.stream()
                .collect(Collectors.groupingBy(
                        TimetableSlot::getDayOfWeek,
                        Collectors.toList()
                ));
    }

    private byte[] convertHtmlToPdf(String htmlContent) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);
            return outputStream.toByteArray();
        }
    }
} 