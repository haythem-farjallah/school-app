package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.example.school_management.feature.operational.service.AttendanceService;
import com.example.school_management.feature.operational.service.TranscriptService;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TranscriptServiceImpl implements TranscriptService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final SchoolMembershipRepository memberships;
    private final CurrentSchoolResolver currentSchool;
    private final AttendanceService attendanceService;
    private final TemplateEngine templateEngine;

    @Override
    public TranscriptDto generateTranscript(Long studentId) {
        Long schoolId = currentSchool.resolve().getId();
        Student student = requireSchoolStudent(studentId, schoolId);
        List<Enrollment> enrollments = requireSchoolEnrollmentHistory(studentId, schoolId);
        List<Grade> grades = gradeRepository.findByStudentIdAndSchoolId(studentId, schoolId);
        LocalDate enrollmentDate = earliestEnrollmentDate(enrollments);
        return buildTranscript(student, enrollments, grades, enrollmentDate, LocalDate.now());
    }

    @Override
    public TranscriptDto generateTranscriptForPeriod(Long studentId, LocalDate startDate, LocalDate endDate) {
        Long schoolId = currentSchool.resolve().getId();
        Student student = requireSchoolStudent(studentId, schoolId);
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must not be after end date");
        }
        List<Enrollment> enrollments = requireSchoolEnrollmentHistory(studentId, schoolId);
        List<Grade> grades = gradeRepository.findByStudentIdAndPeriodAndSchoolId(
                studentId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), schoolId);
        return buildTranscript(student, enrollments, grades, startDate, endDate);
    }

    @Override
    public TranscriptSummaryDto generateTranscriptSummary(Long studentId) {
        return generateTranscript(studentId).getSummary();
    }

    @Override
    public byte[] exportTranscriptAsPdf(Long studentId) {
        TranscriptDto transcript = generateTranscript(studentId);
        return generatePdfFromTranscript(transcript);
    }

    @Override
    public byte[] exportTranscriptAsPdf(Long studentId, LocalDate startDate, LocalDate endDate) {
        TranscriptDto transcript = generateTranscriptForPeriod(studentId, startDate, endDate);
        return generatePdfFromTranscript(transcript);
    }

    private Student requireSchoolStudent(Long studentId, Long schoolId) {
        // Membership status does not erase access to a Student's academic history.
        memberships.findByUserIdAndSchoolId(studentId, schoolId)
                .filter(membership -> membership.getRoles().contains(MembershipRole.STUDENT))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private List<Enrollment> requireSchoolEnrollmentHistory(Long studentId, Long schoolId) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByStudentIdAndSchoolId(studentId, schoolId);
        if (enrollments.isEmpty()) {
            throw new ResourceNotFoundException("No enrollment history found for student");
        }
        return enrollments;
    }

    private LocalDate earliestEnrollmentDate(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(Enrollment::getEnrolledAt)
                .filter(Objects::nonNull)
                .map(enrolledAt -> enrolledAt.toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    private TranscriptDto buildTranscript(Student student, List<Enrollment> enrollments, List<Grade> grades,
                                          LocalDate attendanceStart, LocalDate attendanceEnd) {
        List<TranscriptCourseDto> courses = grades.stream()
                .map(this::convertGradeToCourseDto)
                .collect(Collectors.toList());
        double overallGPA = calculateOverallGPA(courses);
        int totalCredits = courses.stream().mapToInt(course -> course.getCredits().intValue()).sum();
        LocalDate enrollmentDate = earliestEnrollmentDate(enrollments);
        TranscriptSummaryDto summary = generateSummary(student, enrollments, courses, enrollmentDate,
                overallGPA, totalCredits, attendanceStart, attendanceEnd);
        return new TranscriptDto(
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                student.getEmail(),
                enrollmentDate,
                LocalDate.now(),
                overallGPA,
                totalCredits,
                determineAcademicStanding(overallGPA),
                courses,
                summary
        );
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

    private TranscriptSummaryDto generateSummary(Student student, List<Enrollment> enrollments,
                                                  List<TranscriptCourseDto> courses, LocalDate enrollmentDate,
                                                  double overallGPA, int totalCredits,
                                                  LocalDate attendanceStart, LocalDate attendanceEnd) {
        AttendanceStatisticsDto attendanceStats = attendanceService.getUserAttendanceStatistics(
                student.getId(), attendanceStart, attendanceEnd);
        String currentClass = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .map(enrollment -> enrollment.getClassEntity().getName())
                .findFirst()
                .orElse("Not Enrolled");

        return new TranscriptSummaryDto(
                student.getId(),
                student.getFirstName() + " " + student.getLastName(),
                enrollmentDate,
                LocalDate.now(),
                overallGPA,
                totalCredits,
                courses.size(),
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
