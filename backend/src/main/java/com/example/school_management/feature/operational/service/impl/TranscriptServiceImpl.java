package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.AttendanceService;
import com.example.school_management.feature.operational.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TranscriptServiceImpl implements TranscriptService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceService attendanceService;
    private final TemplateEngine templateEngine;

    @Override
    public TranscriptDto generateTranscript(Long studentId) {
        log.debug("Generating transcript for student: {}", studentId);
        
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        // Get all enrollments for the student
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(studentId, 
                com.example.school_management.feature.operational.entity.enums.EnrollmentStatus.ACTIVE);
        
        if (enrollments.isEmpty()) {
            throw new ResourceNotFoundException("No active enrollments found for student");
        }
        
        // Generate transcript data
        List<TranscriptCourseDto> allCourses = generateAllCoursesData(enrollments);
        TranscriptSummaryDto summary = generateSummary(student, enrollments);
        
        // Calculate overall GPA
        double overallGPA = calculateOverallGPA(allCourses);
        int totalCredits = allCourses.stream().mapToInt(course -> course.getCredits().intValue()).sum();
        
        return new TranscriptDto(
                studentId,
                student.getFirstName() + " " + student.getLastName(),
                student.getEmail(),
                student.getEnrolledAt() != null ? student.getEnrolledAt().toLocalDate() : LocalDate.now(),
                LocalDate.now(),
                overallGPA,
                totalCredits,
                determineAcademicStanding(overallGPA),
                allCourses,
                summary
        );
    }

    @Override
    public TranscriptDto generateTranscriptForPeriod(Long studentId, LocalDate startDate, LocalDate endDate) {
        log.debug("Generating transcript for student {} from {} to {}", studentId, startDate, endDate);
        
        // For now, return the full transcript - can be enhanced later to filter by date
        return generateTranscript(studentId);
    }

    @Override
    public TranscriptSummaryDto generateTranscriptSummary(Long studentId) {
        log.debug("Generating transcript summary for student: {}", studentId);
        
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(studentId, 
                com.example.school_management.feature.operational.entity.enums.EnrollmentStatus.ACTIVE);
        
        return generateSummary(student, enrollments);
    }

    @Override
    public byte[] exportTranscriptAsPdf(Long studentId) {
        log.debug("Exporting transcript as PDF for student: {}", studentId);
        
        TranscriptDto transcript = generateTranscript(studentId);
        return generatePdfFromTranscript(transcript);
    }

    @Override
    public byte[] exportTranscriptAsPdf(Long studentId, LocalDate startDate, LocalDate endDate) {
        log.debug("Exporting transcript as PDF for student {} from {} to {}", studentId, startDate, endDate);
        
        TranscriptDto transcript = generateTranscriptForPeriod(studentId, startDate, endDate);
        return generatePdfFromTranscript(transcript);
    }

    private List<TranscriptCourseDto> generateAllCoursesData(List<Enrollment> enrollments) {
        return enrollments.stream()
                .flatMap(enrollment -> enrollment.getGrades().stream())
                .map(this::convertGradeToCourseDto)
                .collect(Collectors.toList());
    }

    private TranscriptCourseDto convertGradeToCourseDto(Grade grade) {
        return new TranscriptCourseDto(
                grade.getEnrollment().getClassEntity().getName(), // Using class name as course name
                "COURSE-" + grade.getEnrollment().getClassEntity().getId(), // Simplified course code
                grade.getScore().doubleValue(),
                3.0, // Default credits
                convertScoreToLetterGrade(grade.getScore()),
                grade.getAssignedBy().getFirstName() + " " + grade.getAssignedBy().getLastName(),
                grade.getGradedAt() != null ? grade.getGradedAt().toLocalDate() : LocalDate.now()
        );
    }

    private TranscriptSummaryDto generateSummary(Student student, List<Enrollment> enrollments) {
        // Get attendance statistics for the last year
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        
        AttendanceStatisticsDto attendanceStats = attendanceService.getUserAttendanceStatistics(
                student.getId(), startDate, endDate);
        
        // Get current class
        String currentClass = enrollments.stream()
                .map(enrollment -> enrollment.getClassEntity().getName())
                .findFirst()
                .orElse("Not Enrolled");
        
        // Calculate overall GPA and credits
        List<TranscriptCourseDto> allCourses = generateAllCoursesData(enrollments);
        double overallGPA = calculateOverallGPA(allCourses);
        int totalCredits = allCourses.stream().mapToInt(course -> course.getCredits().intValue()).sum();
        
        return new TranscriptSummaryDto(
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getEnrolledAt() != null ? student.getEnrolledAt().toLocalDate() : LocalDate.now(),
                LocalDate.now(),
                overallGPA,
                totalCredits,
                allCourses.size(),
                determineAcademicStanding(overallGPA),
                attendanceStats.getAttendancePercentage(),
                attendanceStats.getAbsentDays().intValue(),
                attendanceStats.getExcusedDays().intValue(),
                student.getGradeLevel() != null ? student.getGradeLevel().name() : "Unknown",
                currentClass
        );
    }

    private double calculateOverallGPA(List<TranscriptCourseDto> courses) {
        if (courses.isEmpty()) return 0.0;
        
        double totalGradePoints = courses.stream()
                .mapToDouble(course -> course.getGrade() * course.getCredits())
                .sum();
        
        double totalCredits = courses.stream()
                .mapToDouble(TranscriptCourseDto::getCredits)
                .sum();
        
        return totalCredits > 0 ? totalGradePoints / totalCredits : 0.0;
    }

    private String convertScoreToLetterGrade(Float score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    private String determineAcademicStanding(double gpa) {
        if (gpa >= 3.5) return "Dean's List";
        if (gpa >= 3.0) return "Good Standing";
        if (gpa >= 2.0) return "Academic Warning";
        return "Academic Probation";
    }

    private byte[] generatePdfFromTranscript(TranscriptDto transcript) {
        try {
            Context context = new Context();
            context.setVariable("transcript", transcript);
            context.setVariable("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("gpaFormatted", String.format("%.2f", transcript.getOverallGPA()));
            context.setVariable("totalCredits", transcript.getTotalCredits());
            
            // Add course data
            context.setVariable("courses", transcript.getAllCourses());
            
            // Add summary data
            context.setVariable("summary", transcript.getSummary());
            
            String html = templateEngine.process("transcript/transcript", context);
            
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            renderer.createPDF(baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating PDF transcript", e);
            throw new RuntimeException("Failed to generate PDF transcript", e);
        }
    }
} 