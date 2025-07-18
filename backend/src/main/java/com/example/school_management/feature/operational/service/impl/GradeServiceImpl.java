package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.AuditEvent;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.example.school_management.feature.operational.service.GradeService;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.mapper.OperationalMapper;
import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeServiceImpl implements GradeService {
    private final GradeRepository gradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;

    private final AuditService auditService;
    private final OperationalMapper mapper;

    @Override
    @Transactional
    public void enterBulkGrades(BulkGradeEntryRequest request) {
        Set<Long> seen = new HashSet<>();
        // Get current teacher from security context
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Teacher teacher = teacherRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        for (BulkGradeEntryRequest.StudentGradeEntry entry : request.getGrades()) {
            if (entry.getStudentId() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("Student ID and grade value are required");
            }
            if (entry.getValue() < 0 || entry.getValue() > 20) {
                throw new IllegalArgumentException("Grade value must be between 0 and 20");
            }
            if (!seen.add(entry.getStudentId())) {
                throw new IllegalArgumentException("Duplicate grade for student ID: " + entry.getStudentId());
            }
            Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassId(entry.getStudentId(), request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found for student " + entry.getStudentId()));
            Grade grade = new Grade();
            grade.setEnrollment(enrollment);
            grade.setScore(entry.getValue().floatValue());
            grade.setContent(request.getAssessmentType() + (request.getTerm() != null ? (" - " + request.getTerm()) : "") + (entry.getComment() != null ? (" - " + entry.getComment()) : ""));
            grade.setGradedAt(java.time.LocalDateTime.now());
            grade.setWeight(entry.getWeight() != null ? entry.getWeight() : 1.0f);
            grade.setAssignedBy(teacher);
            gradeRepository.save(grade);
        }
    }

    @Override
    @Transactional
    public void updateGrade(Long gradeId, UpdateGradeRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Grade not found with id: " + gradeId));
        
        String oldValues = String.format("Score: %.2f, Weight: %.2f, Content: %s", 
            grade.getScore(), grade.getWeight(), grade.getContent());
        
        if (request.getScore() != null) grade.setScore(request.getScore());
        if (request.getWeight() != null) grade.setWeight(request.getWeight());
        if (request.getComment() != null) grade.setContent(request.getComment());
        grade.setGradedAt(LocalDateTime.now());
        
        String newValues = String.format("Score: %.2f, Weight: %.2f, Content: %s", 
            grade.getScore(), grade.getWeight(), grade.getContent());
        
        gradeRepository.save(grade);
        
        // Create audit event
        BaseUser currentUser = getCurrentUser();
        String auditDetails = String.format("Grade updated. Old values: %s. New values: %s. Reason: %s", 
            oldValues, newValues, request.getUpdateReason());
        auditService.createGradeAuditEvent(AuditEventType.GRADE_UPDATED, gradeId, 
            "Grade updated", auditDetails, currentUser);
        
        log.info("Grade {} updated by user {}", gradeId, currentUser.getEmail());
    }

    @Override
    @Transactional
    public void deleteGrade(Long gradeId, DeleteGradeRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Grade not found with id: " + gradeId));
        
        String gradeDetails = String.format("Student: %s %s, Score: %.2f, Content: %s", 
            grade.getEnrollment().getStudent().getFirstName(),
            grade.getEnrollment().getStudent().getLastName(),
            grade.getScore(), grade.getContent());
        
        BaseUser currentUser = getCurrentUser();
        String auditDetails = String.format("Grade deleted. Details: %s. Reason: %s. Notes: %s", 
            gradeDetails, request.getReason(), request.getAdditionalNotes());
        auditService.createGradeAuditEvent(AuditEventType.GRADE_DELETED, gradeId, 
            "Grade deleted", auditDetails, currentUser);
        
        gradeRepository.delete(grade);
        
        log.info("Grade {} deleted by user {} for reason: {}", gradeId, currentUser.getEmail(), request.getReason());
    }

    @Override
    public GradeResponse getGradeById(Long gradeId) {
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Grade not found with id: " + gradeId));
        
        GradeResponse response = mapper.toGradeResponse(grade);
        BaseUser currentUser = getCurrentUser();
        response.setCanEdit(canEditGrade(gradeId, currentUser.getId()));
        response.setCanDelete(canDeleteGrade(gradeId, currentUser.getId()));
        
        return response;
    }

    @Override
    public List<GradeResponse> getGradesByStudentId(Long studentId) {
        List<Grade> grades = gradeRepository.findByStudentId(studentId);
        return grades.stream()
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<GradeResponse> getGradesByClassId(Long classId) {
        List<Grade> grades = gradeRepository.findByClassId(classId);
        return grades.stream()
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<GradeResponse> getGradesByTeacherId(Long teacherId) {
        List<Grade> grades = gradeRepository.findByTeacherId(teacherId);
        return grades.stream()
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<GradeResponse> getGradesByEnrollmentId(Long enrollmentId) {
        List<Grade> grades = gradeRepository.findByEnrollmentId(enrollmentId);
        return grades.stream()
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public Page<GradeResponse> getGradesByStudentId(Long studentId, Pageable pageable) {
        Page<Grade> grades = gradeRepository.findByStudentIdOrderByGradedAtDesc(studentId, pageable);
        return grades.map(this::mapGradeToResponse);
    }

    @Override
    public Page<GradeResponse> getGradesByClassId(Long classId, Pageable pageable) {
        Page<Grade> grades = gradeRepository.findByClassIdOrderByGradedAtDesc(classId, pageable);
        return grades.map(this::mapGradeToResponse);
    }

    @Override
    public Page<GradeResponse> getGradesByEnrollmentId(Long enrollmentId, Pageable pageable) {
        Page<Grade> grades = gradeRepository.findByEnrollmentIdOrderByGradedAtDesc(enrollmentId, pageable);
        return grades.map(this::mapGradeToResponse);
    }

    @Override
    public GradeStatistics getStudentGradeStatistics(Long studentId) {
        List<Grade> grades = gradeRepository.findByStudentId(studentId);
        return calculateGradeStatistics(grades, studentId, null, null);
    }

    @Override
    public GradeStatistics getStudentGradeStatisticsForClass(Long studentId, Long classId) {
        List<Grade> grades = gradeRepository.findByStudentIdAndClassId(studentId, classId);
        return calculateGradeStatistics(grades, studentId, classId, null);
    }

    @Override
    public GradeStatistics getClassGradeStatistics(Long classId) {
        List<Grade> grades = gradeRepository.findByClassId(classId);
        return calculateGradeStatistics(grades, null, classId, null);
    }

    @Override
    public GradeStatistics getGradeStatisticsForDateRange(Long studentId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Grade> grades = gradeRepository.findByGradedAtBetween(startDate, endDate)
            .stream()
            .filter(grade -> grade.getEnrollment().getStudent().getId().equals(studentId))
            .collect(Collectors.toList());
        return calculateGradeStatistics(grades, studentId, null, null);
    }

    @Override
    public List<AuditEvent> getGradeAuditHistory(Long gradeId) {
        return auditService.getGradeAuditHistory(gradeId);
    }

    @Override
    public boolean canEditGrade(Long gradeId, Long userId) {
        Grade grade = gradeRepository.findById(gradeId).orElse(null);
        if (grade == null) return false;
        
        // Teachers can edit their own grades
        return grade.getAssignedBy().getId().equals(userId);
    }

    @Override
    public boolean canDeleteGrade(Long gradeId, Long userId) {
        Grade grade = gradeRepository.findById(gradeId).orElse(null);
        if (grade == null) return false;
        
        // Only the teacher who assigned it can delete within 24 hours
        if (grade.getAssignedBy().getId().equals(userId)) {
            LocalDateTime cutoff = grade.getGradedAt().plusHours(24);
            return LocalDateTime.now().isBefore(cutoff);
        }
        
        return false;
    }

    @Override
    public boolean gradeExistsForEnrollmentAndContent(Long enrollmentId, String content) {
        return gradeRepository.findByEnrollmentIdAndContent(enrollmentId, content).isPresent();
    }

    @Override
    public Page<GradeResponse> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap);
        Specification<Grade> spec = DynamicSpecificationBuilder.build(criteria);
        Page<Grade> grades = gradeRepository.findAll(spec, pageable);
        return grades.map(this::mapGradeToResponse);
    }

    @Override
    public Page<GradeResponse> getAllGrades(Pageable pageable, String search, Long courseId) {
        Page<Grade> grades;
        
        if (search != null && !search.trim().isEmpty() && courseId != null) {
            // Both search and courseId filters
            grades = gradeRepository.findBySearchAndCourseId(search.trim(), courseId, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            // Only search filter
            grades = gradeRepository.findBySearch(search.trim(), pageable);
        } else if (courseId != null) {
            // Only courseId filter
            grades = gradeRepository.findByCourseId(courseId, pageable);
        } else {
            // No filters - get all
            grades = gradeRepository.findAll(pageable);
        }
        
        return grades.map(this::mapGradeToResponse);
    }
    
    // Helper methods
    private GradeResponse mapGradeToResponse(Grade grade) {
        GradeResponse response = mapper.toGradeResponse(grade);
        BaseUser currentUser = getCurrentUser();
        response.setCanEdit(canEditGrade(grade.getId(), currentUser.getId()));
        response.setCanDelete(canDeleteGrade(grade.getId(), currentUser.getId()));
        return response;
    }
    
    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Teacher teacher = teacherRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Current teacher not found"));
        return teacher; // Teacher extends BaseUser
    }
    
    private GradeStatistics calculateGradeStatistics(List<Grade> grades, Long studentId, Long classId, Long courseId) {
        if (grades.isEmpty()) {
            return GradeStatistics.builder()
                .averageGrade(0.0)
                .minimumGrade(0.0f)
                .maximumGrade(0.0f)
                .totalGrades(0L)
                .excellentCount(0L)
                .goodCount(0L)
                .satisfactoryCount(0L)
                .needsImprovementCount(0L)
                .studentId(studentId)
                .classId(classId)
                .courseId(courseId)
                .weightedAverage(0.0)
                .letterGrade("N/A")
                .passStatus("PENDING")
                .trend("STABLE")
                .build();
        }
        
        // Calculate basic statistics
        double sum = grades.stream().mapToDouble(Grade::getScore).sum();
        double weightedSum = grades.stream().mapToDouble(grade -> grade.getScore() * grade.getWeight()).sum();
        double totalWeight = grades.stream().mapToDouble(Grade::getWeight).sum();
        
        float min = grades.stream().map(Grade::getScore).min(Float::compareTo).orElse(0.0f);
        float max = grades.stream().map(Grade::getScore).max(Float::compareTo).orElse(0.0f);
        
        double average = sum / grades.size();
        double weightedAverage = totalWeight > 0 ? weightedSum / totalWeight : average;
        
        // Calculate distribution
        long excellent = grades.stream().mapToLong(grade -> grade.getScore() >= 18 ? 1 : 0).sum();
        long good = grades.stream().mapToLong(grade -> grade.getScore() >= 14 && grade.getScore() < 18 ? 1 : 0).sum();
        long satisfactory = grades.stream().mapToLong(grade -> grade.getScore() >= 10 && grade.getScore() < 14 ? 1 : 0).sum();
        long needsImprovement = grades.stream().mapToLong(grade -> grade.getScore() < 10 ? 1 : 0).sum();
        
        // Get recent grades for context (last 5)
        List<GradeResponse> recentGrades = grades.stream()
            .sorted((g1, g2) -> g2.getGradedAt().compareTo(g1.getGradedAt()))
            .limit(5)
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
        
        return GradeStatistics.builder()
            .averageGrade(average)
            .minimumGrade(min)
            .maximumGrade(max)
            .totalGrades((long) grades.size())
            .excellentCount(excellent)
            .goodCount(good)
            .satisfactoryCount(satisfactory)
            .needsImprovementCount(needsImprovement)
            .studentId(studentId)
            .classId(classId)
            .courseId(courseId)
            .weightedAverage(weightedAverage)
            .letterGrade(calculateLetterGrade(weightedAverage))
            .passStatus(weightedAverage >= 10 ? "PASS" : "FAIL")
            .trend("STABLE") // TODO: Implement trend calculation
            .recentGrades(recentGrades)
            .build();
    }
    
    private String calculateLetterGrade(double average) {
        if (average >= 18) return "A";
        if (average >= 14) return "B";
        if (average >= 10) return "C";
        if (average >= 8) return "D";
        return "F";
    }
} 