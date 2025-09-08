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
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.EnhancedGrade;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.EnhancedGradeRepository;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeServiceImpl implements GradeService {
    private final GradeRepository gradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnhancedGradeRepository enhancedGradeRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final CourseRepository courseRepository;

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
    
    // ===== ENHANCED GRADE MANAGEMENT IMPLEMENTATIONS =====
    
    @Override
    @Transactional(readOnly = true)
    public List<TeacherGradeClassView> getTeacherGradeClasses(Long teacherId) {
        log.debug("Getting grade classes for teacher: {}", teacherId);
        
        // Get all teaching assignments for this teacher
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherId(teacherId);
        
        // Group by class and course to create class views
        Map<String, TeacherGradeClassView> classViewMap = new HashMap<>();
        
        for (TeachingAssignment assignment : assignments) {
            String key = assignment.getClazz().getId() + "-" + assignment.getCourse().getId();
            
            if (!classViewMap.containsKey(key)) {
                TeacherGradeClassView classView = TeacherGradeClassView.builder()
                        .classId(assignment.getClazz().getId())
                        .className(assignment.getClazz().getName())
                        .courseId(assignment.getCourse().getId())
                        .courseName(assignment.getCourse().getName())
                        .courseCode(assignment.getCourse().getCode())
                        .coefficient(assignment.getCourse().getCredit() != null ? assignment.getCourse().getCredit().doubleValue() : 1.0)
                        .semester(CreateEnhancedGradeRequest.Semester.FIRST) // Default semester
                        .examTypes(Arrays.asList(CreateEnhancedGradeRequest.ExamType.values()))
                        .students(new ArrayList<>())
                        .build();
                
                classViewMap.put(key, classView);
            }
        }
        
        return new ArrayList<>(classViewMap.values());
    }
    
    @Override
    @Transactional(readOnly = true)
    public TeacherGradeClassView getTeacherGradeClass(Long teacherId, Long classId, Long courseId) {
        log.debug("Getting grade class view for teacher: {}, class: {}, course: {}", teacherId, classId, courseId);
        
        // Verify teaching assignment exists
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherId(teacherId);
        TeachingAssignment assignment = assignments.stream()
                .filter(ta -> ta.getClazz().getId().equals(classId) && ta.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Teaching assignment not found for teacher " + teacherId + ", class " + classId + ", course " + courseId));
        
        // Get all students enrolled in this class
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        
        List<TeacherGradeClassView.TeacherGradeStudent> students = new ArrayList<>();
        
        for (Enrollment enrollment : enrollments) {
            // Get existing grades for this student in this course
            List<EnhancedGrade> existingGrades = enhancedGradeRepository.findByStudentIdAndCourseId(enrollment.getStudent().getId(), courseId);
            
            // Calculate attendance rate (simplified - you might want to integrate with actual attendance system)
            Double attendanceRate = calculateStudentAttendanceRate(enrollment.getStudent().getId());
            
            // Build current grades
            TeacherGradeClassView.TeacherGradeStudent.CurrentGrades currentGrades = buildCurrentGrades(existingGrades);
            
            // Calculate average
            Double average = calculateStudentCourseAverage(existingGrades);
            
            TeacherGradeClassView.TeacherGradeStudent student = TeacherGradeClassView.TeacherGradeStudent.builder()
                    .studentId(enrollment.getStudent().getId())
                    .firstName(enrollment.getStudent().getFirstName())
                    .lastName(enrollment.getStudent().getLastName())
                    .email(enrollment.getStudent().getEmail())
                    .enrollmentId(enrollment.getId())
                    .currentGrades(currentGrades)
                    .average(average)
                    .attendanceRate(attendanceRate)
                    .build();
            
            students.add(student);
        }
        
        return TeacherGradeClassView.builder()
                .classId(classId)
                .className(assignment.getClazz().getName())
                .courseId(courseId)
                .courseName(assignment.getCourse().getName())
                .courseCode(assignment.getCourse().getCode())
                .coefficient(assignment.getCourse().getCredit() != null ? assignment.getCourse().getCredit().doubleValue() : 1.0)
                .semester(CreateEnhancedGradeRequest.Semester.FIRST) // Default semester
                .examTypes(Arrays.asList(CreateEnhancedGradeRequest.ExamType.values()))
                .students(students)
                .build();
    }
    
    @Override
    @Transactional
    public List<EnhancedGradeResponse> createBulkEnhancedGrades(BulkEnhancedGradeEntryRequest request) {
        log.debug("Creating bulk enhanced grades for class: {}, course: {}", request.getClassId(), request.getCourseId());
        
        List<EnhancedGradeResponse> responses = new ArrayList<>();
        
        for (BulkEnhancedGradeEntryRequest.StudentGradeEntry gradeEntry : request.getGrades()) {
            // Get student and course information
            Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassId(gradeEntry.getStudentId(), request.getClassId())
                    .orElseThrow(() -> new IllegalArgumentException("Student " + gradeEntry.getStudentId() + " not enrolled in class " + request.getClassId()));
            
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + request.getCourseId()));
            
            // Check if grade already exists
            Optional<EnhancedGrade> existingGrade = enhancedGradeRepository.findByStudentIdAndCourseIdAndExamTypeAndSemester(
                    gradeEntry.getStudentId(), request.getCourseId(), request.getExamType(), request.getSemester());
            
            EnhancedGrade grade;
            if (existingGrade.isPresent()) {
                // Update existing grade
                grade = existingGrade.get();
                grade.setScore(gradeEntry.getScore());
                grade.setMaxScore(request.getMaxScore());
                grade.setTeacherRemarks(gradeEntry.getTeacherRemarks());
                grade.setTeacherSignature(request.getTeacherSignature());
                grade.setGradedAt(LocalDateTime.now());
            } else {
                // Create new grade
                grade = new EnhancedGrade();
                grade.setStudentId(gradeEntry.getStudentId());
                grade.setStudentFirstName(enrollment.getStudent().getFirstName());
                grade.setStudentLastName(enrollment.getStudent().getLastName());
                grade.setStudentEmail(enrollment.getStudent().getEmail());
                grade.setClassId(request.getClassId());
                grade.setClassName(enrollment.getClassEntity().getName());
                grade.setCourseId(request.getCourseId());
                grade.setCourseName(course.getName());
                grade.setCourseCode(course.getCode());
                grade.setCourseCoefficient(course.getCredit() != null ? course.getCredit().doubleValue() : 1.0);
                grade.setExamType(request.getExamType());
                grade.setSemester(request.getSemester());
                grade.setScore(gradeEntry.getScore());
                grade.setMaxScore(request.getMaxScore());
                grade.setTeacherRemarks(gradeEntry.getTeacherRemarks());
                grade.setTeacherSignature(request.getTeacherSignature());
                grade.setGradedAt(LocalDateTime.now());
                
                // Set teacher information (you might want to get this from security context)
                BaseUser currentUser = getCurrentUser();
                if (currentUser instanceof Teacher) {
                    Teacher teacher = (Teacher) currentUser;
                    grade.setTeacherId(teacher.getId());
                    grade.setTeacherFirstName(teacher.getFirstName());
                    grade.setTeacherLastName(teacher.getLastName());
                    grade.setTeacherEmail(teacher.getEmail());
                }
            }
            
            EnhancedGrade savedGrade = enhancedGradeRepository.save(grade);
            responses.add(mapToEnhancedGradeResponse(savedGrade));
        }
        
        log.info("Created/updated {} enhanced grades", responses.size());
        return responses;
    }
    
    @Override
    @Transactional
    public EnhancedGradeResponse createEnhancedGrade(CreateEnhancedGradeRequest request) {
        log.debug("Creating enhanced grade for student: {}, course: {}", request.getStudentId(), request.getCourseId());
        
        // Get student and course information
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassId(request.getStudentId(), request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Student " + request.getStudentId() + " not enrolled in class " + request.getClassId()));
        
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + request.getCourseId()));
        
        // Check if grade already exists
        Optional<EnhancedGrade> existingGrade = enhancedGradeRepository.findByStudentIdAndCourseIdAndExamTypeAndSemester(
                request.getStudentId(), request.getCourseId(), request.getExamType(), request.getSemester());
        
        if (existingGrade.isPresent()) {
            throw new IllegalArgumentException("Grade already exists for this student, course, exam type, and semester");
        }
        
        // Create new grade
        EnhancedGrade grade = new EnhancedGrade();
        grade.setStudentId(request.getStudentId());
        grade.setStudentFirstName(enrollment.getStudent().getFirstName());
        grade.setStudentLastName(enrollment.getStudent().getLastName());
        grade.setStudentEmail(enrollment.getStudent().getEmail());
        grade.setClassId(request.getClassId());
        grade.setClassName(enrollment.getClassEntity().getName());
        grade.setCourseId(request.getCourseId());
        grade.setCourseName(course.getName());
        grade.setCourseCode(course.getCode());
        grade.setCourseCoefficient(course.getCredit() != null ? course.getCredit().doubleValue() : 1.0);
        grade.setExamType(request.getExamType());
        grade.setSemester(request.getSemester());
        grade.setScore(request.getScore());
        grade.setMaxScore(request.getMaxScore());
        grade.setTeacherRemarks(request.getTeacherRemarks());
        grade.setTeacherSignature(request.getTeacherSignature());
        grade.setGradedAt(LocalDateTime.now());
        
        // Set teacher information
        BaseUser currentUser = getCurrentUser();
        if (currentUser instanceof Teacher) {
            Teacher teacher = (Teacher) currentUser;
            grade.setTeacherId(teacher.getId());
            grade.setTeacherFirstName(teacher.getFirstName());
            grade.setTeacherLastName(teacher.getLastName());
            grade.setTeacherEmail(teacher.getEmail());
        }
        
        EnhancedGrade savedGrade = enhancedGradeRepository.save(grade);
        log.info("Created enhanced grade with ID: {}", savedGrade.getId());
        
        return mapToEnhancedGradeResponse(savedGrade);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<StaffGradeReview> getStaffGradeReviews(Long classId, CreateEnhancedGradeRequest.Semester semester) {
        log.debug("Getting staff grade reviews for class: {}, semester: {}", classId, semester);
        
        // Get all students in the class
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        List<StaffGradeReview> reviews = new ArrayList<>();
        
        for (Enrollment enrollment : enrollments) {
            // Get all grades for this student in this semester
            List<EnhancedGrade> studentGrades = enhancedGradeRepository.findByStudentIdAndSemester(enrollment.getStudent().getId(), semester);
            
            // Group grades by course
            Map<Long, List<EnhancedGrade>> gradesByCourse = studentGrades.stream()
                    .collect(Collectors.groupingBy(EnhancedGrade::getCourseId));
            
            List<StaffGradeReview.StaffSubjectReview> subjectReviews = new ArrayList<>();
            double totalWeightedScore = 0.0;
            double totalCoefficients = 0.0;
            
            for (Map.Entry<Long, List<EnhancedGrade>> entry : gradesByCourse.entrySet()) {
                List<EnhancedGrade> courseGrades = entry.getValue();
                EnhancedGrade firstGrade = courseGrades.get(0); // Get course info from first grade
                
                // Build subject grades
                StaffGradeReview.StaffSubjectReview.SubjectGrades subjectGrades = StaffGradeReview.StaffSubjectReview.SubjectGrades.builder()
                        .firstExam(getGradeByExamType(courseGrades, CreateEnhancedGradeRequest.ExamType.FIRST_EXAM))
                        .secondExam(getGradeByExamType(courseGrades, CreateEnhancedGradeRequest.ExamType.SECOND_EXAM))
                        .finalExam(getGradeByExamType(courseGrades, CreateEnhancedGradeRequest.ExamType.FINAL_EXAM))
                        .build();
                
                // Calculate course average
                double courseAverage = calculateCourseAverage(courseGrades);
                
                // Check if needs review (e.g., if any grade is below 50%)
                boolean needsReview = courseGrades.stream().anyMatch(g -> g.getPercentage() < 50.0);
                
                StaffGradeReview.StaffSubjectReview subjectReview = StaffGradeReview.StaffSubjectReview.builder()
                        .courseId(firstGrade.getCourseId())
                        .courseName(firstGrade.getCourseName())
                        .courseCode(firstGrade.getCourseCode())
                        .coefficient(firstGrade.getCourseCoefficient())
                        .teacherName(firstGrade.getTeacherFirstName() + " " + firstGrade.getTeacherLastName())
                        .grades(subjectGrades)
                        .average(courseAverage)
                        .teacherRemarks(getLatestTeacherRemarks(courseGrades))
                        .needsReview(needsReview)
                        .build();
                
                subjectReviews.add(subjectReview);
                
                // Add to weighted calculation
                totalWeightedScore += courseAverage * firstGrade.getCourseCoefficient();
                totalCoefficients += firstGrade.getCourseCoefficient();
            }
            
            // Calculate overall average
            double overallAverage = totalCoefficients > 0 ? totalWeightedScore / totalCoefficients : 0.0;
            
            // Calculate class rank (simplified - you might want a more sophisticated ranking)
            Integer classRank = calculateClassRank(enrollment.getStudent().getId(), classId, semester);
            
            // Calculate attendance rate
            Double attendanceRate = calculateStudentAttendanceRate(enrollment.getStudent().getId());
            
            // Check if grades are approved
            boolean isApproved = studentGrades.stream().allMatch(EnhancedGrade::getIsApproved);
            
            StaffGradeReview review = StaffGradeReview.builder()
                    .studentId(enrollment.getStudent().getId())
                    .studentFirstName(enrollment.getStudent().getFirstName())
                    .studentLastName(enrollment.getStudent().getLastName())
                    .classId(classId)
                    .className(enrollment.getClassEntity().getName())
                    .semester(semester)
                    .subjects(subjectReviews)
                    .overallAverage(overallAverage)
                    .classRank(classRank)
                    .attendanceRate(attendanceRate)
                    .isApproved(isApproved)
                    .approvedAt(getApprovalDate(studentGrades))
                    .approvedBy(getApprovalBy(studentGrades))
                    .build();
            
            reviews.add(review);
        }
        
        return reviews;
    }
    
    @Override
    @Transactional
    public void approveGrades(ApproveGradesRequest request) {
        log.debug("Approving grades for {} students in semester {}", request.getStudentIds().size(), request.getSemester());
        
        for (Long studentId : request.getStudentIds()) {
            List<EnhancedGrade> studentGrades = enhancedGradeRepository.findByStudentIdAndSemester(studentId, request.getSemester());
            
            for (EnhancedGrade grade : studentGrades) {
                grade.setIsApproved(true);
                grade.setApprovedAt(LocalDateTime.now());
                grade.setApprovedBy(request.getApprovedBy());
            }
            
            enhancedGradeRepository.saveAll(studentGrades);
        }
        
        log.info("Approved grades for {} students", request.getStudentIds().size());
    }
    
    @Override
    @Transactional(readOnly = true)
    public StudentGradeSheet getStudentGradeSheet(Long studentId, CreateEnhancedGradeRequest.Semester semester) {
        log.debug("Generating grade sheet for student: {}, semester: {}", studentId, semester);
        
        // Get student information
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        
        // Get student's active enrollment (assuming one active enrollment)
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE);
        if (enrollments.isEmpty()) {
            throw new IllegalArgumentException("No active enrollment found for student: " + studentId);
        }
        
        Enrollment enrollment = enrollments.get(0); // Take the first active enrollment
        
        // Get all grades for this student in this semester
        List<EnhancedGrade> studentGrades = enhancedGradeRepository.findByStudentIdAndSemester(studentId, semester);
        
        // Group grades by course
        Map<Long, List<EnhancedGrade>> gradesByCourse = studentGrades.stream()
                .collect(Collectors.groupingBy(EnhancedGrade::getCourseId));
        
        List<StudentGradeSheet.SubjectGrade> subjects = new ArrayList<>();
        double totalScore = 0.0;
        double totalMaxScore = 0.0;
        double totalWeightedScore = 0.0;
        double totalCoefficients = 0.0;
        
        for (Map.Entry<Long, List<EnhancedGrade>> entry : gradesByCourse.entrySet()) {
            List<EnhancedGrade> courseGrades = entry.getValue();
            EnhancedGrade firstGrade = courseGrades.get(0);
            
            // Build subject grades
            StudentGradeSheet.SubjectGrade.SubjectGrades subjectGrades = StudentGradeSheet.SubjectGrade.SubjectGrades.builder()
                    .firstExam(getGradeByExamType(courseGrades, CreateEnhancedGradeRequest.ExamType.FIRST_EXAM))
                    .secondExam(getGradeByExamType(courseGrades, CreateEnhancedGradeRequest.ExamType.SECOND_EXAM))
                    .finalExam(getGradeByExamType(courseGrades, CreateEnhancedGradeRequest.ExamType.FINAL_EXAM))
                    .quizzes(getGradesByExamType(courseGrades, CreateEnhancedGradeRequest.ExamType.QUIZ))
                    .assignments(getGradesByExamType(courseGrades, CreateEnhancedGradeRequest.ExamType.ASSIGNMENT))
                    .build();
            
            // Calculate course average and weighted score
            double courseAverage = calculateCourseAverage(courseGrades);
            double weightedScore = courseAverage * firstGrade.getCourseCoefficient();
            
            StudentGradeSheet.SubjectGrade subjectGrade = StudentGradeSheet.SubjectGrade.builder()
                    .courseId(firstGrade.getCourseId())
                    .courseName(firstGrade.getCourseName())
                    .courseCode(firstGrade.getCourseCode())
                    .coefficient(firstGrade.getCourseCoefficient())
                    .teacherId(firstGrade.getTeacherId())
                    .teacherFirstName(firstGrade.getTeacherFirstName())
                    .teacherLastName(firstGrade.getTeacherLastName())
                    .grades(subjectGrades)
                    .average(courseAverage)
                    .weightedScore(weightedScore)
                    .teacherRemarks(getLatestTeacherRemarks(courseGrades))
                    .teacherSignature(getLatestTeacherSignature(courseGrades))
                    .letterGrade(calculateLetterGrade(courseAverage))
                    .build();
            
            subjects.add(subjectGrade);
            
            // Add to totals
            double courseTotal = courseGrades.stream().mapToDouble(EnhancedGrade::getScore).sum();
            double courseMaxTotal = courseGrades.stream().mapToDouble(EnhancedGrade::getMaxScore).sum();
            totalScore += courseTotal;
            totalMaxScore += courseMaxTotal;
            totalWeightedScore += weightedScore;
            totalCoefficients += firstGrade.getCourseCoefficient();
        }
        
        // Calculate weighted average
        double weightedAverage = totalCoefficients > 0 ? totalWeightedScore / totalCoefficients : 0.0;
        
        // Calculate class rank and total students
        Integer classRank = calculateClassRank(studentId, enrollment.getClassEntity().getId(), semester);
        Long totalStudents = enhancedGradeRepository.countDistinctStudentsByClassIdAndSemester(enrollment.getClassEntity().getId(), semester);
        
        // Calculate attendance
        Double attendanceRate = calculateStudentAttendanceRate(studentId);
        Integer totalAbsences = calculateStudentTotalAbsences(studentId);
        
        // Check approval status
        StudentGradeSheet.ApprovedBy approvedBy = null;
        if (studentGrades.stream().allMatch(EnhancedGrade::getIsApproved)) {
            String approvedByName = getApprovalBy(studentGrades);
            String approvedAt = getApprovalDate(studentGrades);
            if (approvedByName != null && approvedAt != null) {
                approvedBy = StudentGradeSheet.ApprovedBy.builder()
                        .staffId(1L) // You might want to get actual staff ID
                        .staffName(approvedByName)
                        .approvedAt(approvedAt)
                        .build();
            }
        }
        
        return StudentGradeSheet.builder()
                .studentId(studentId)
                .studentFirstName(student.getFirstName())
                .studentLastName(student.getLastName())
                .studentEmail(student.getEmail())
                .classId(enrollment.getClassEntity().getId())
                .className(enrollment.getClassEntity().getName())
                .yearOfStudy(enrollment.getClassEntity().getYearOfStudy())
                .semester(semester)
                .subjects(subjects)
                .totalScore(totalScore)
                .totalMaxScore(totalMaxScore)
                .weightedAverage(weightedAverage)
                .classRank(classRank)
                .totalStudents(totalStudents.intValue())
                .attendanceRate(attendanceRate)
                .totalAbsences(totalAbsences)
                .generatedAt(LocalDateTime.now().toString())
                .approvedBy(approvedBy)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] exportStudentGradeSheet(Long studentId, CreateEnhancedGradeRequest.Semester semester) {
        log.debug("Exporting grade sheet for student: {}, semester: {}", studentId, semester);
        
        // Get the grade sheet data
        StudentGradeSheet gradeSheet = getStudentGradeSheet(studentId, semester);
        
        // For now, return a simple PDF placeholder
        // In a real implementation, you would use a PDF library like iText or Apache PDFBox
        String pdfContent = generateGradeSheetPdfContent(gradeSheet);
        return pdfContent.getBytes();
    }
    
    // ===== HELPER METHODS =====
    
    private TeacherGradeClassView.TeacherGradeStudent.CurrentGrades buildCurrentGrades(List<EnhancedGrade> grades) {
        TeacherGradeClassView.TeacherGradeStudent.CurrentGrades.CurrentGradesBuilder builder = 
                TeacherGradeClassView.TeacherGradeStudent.CurrentGrades.builder();
        
        for (EnhancedGrade grade : grades) {
            TeacherGradeClassView.TeacherGradeStudent.CurrentGrades.ExamGrade examGrade = 
                    TeacherGradeClassView.TeacherGradeStudent.CurrentGrades.ExamGrade.builder()
                            .score(grade.getScore())
                            .maxScore(grade.getMaxScore())
                            .percentage(grade.getPercentage())
                            .teacherRemarks(grade.getTeacherRemarks())
                            .gradedAt(grade.getGradedAt() != null ? grade.getGradedAt().toString() : null)
                            .build();
            
            switch (grade.getExamType()) {
                case FIRST_EXAM:
                    builder.firstExam(examGrade);
                    break;
                case SECOND_EXAM:
                    builder.secondExam(examGrade);
                    break;
                case FINAL_EXAM:
                    builder.finalExam(examGrade);
                    break;
                case QUIZ:
                    List<TeacherGradeClassView.TeacherGradeStudent.CurrentGrades.ExamGrade> quizzes = 
                            builder.build().getQuizzes();
                    if (quizzes == null) quizzes = new ArrayList<>();
                    quizzes.add(examGrade);
                    builder.quizzes(quizzes);
                    break;
                case ASSIGNMENT:
                    List<TeacherGradeClassView.TeacherGradeStudent.CurrentGrades.ExamGrade> assignments = 
                            builder.build().getAssignments();
                    if (assignments == null) assignments = new ArrayList<>();
                    assignments.add(examGrade);
                    builder.assignments(assignments);
                    break;
                case PROJECT:
                    // Handle project grades similar to assignments
                    List<TeacherGradeClassView.TeacherGradeStudent.CurrentGrades.ExamGrade> projects = 
                            builder.build().getAssignments();
                    if (projects == null) projects = new ArrayList<>();
                    projects.add(examGrade);
                    builder.assignments(projects);
                    break;
                case PARTICIPATION:
                    // Handle participation grades similar to quizzes
                    List<TeacherGradeClassView.TeacherGradeStudent.CurrentGrades.ExamGrade> participation = 
                            builder.build().getQuizzes();
                    if (participation == null) participation = new ArrayList<>();
                    participation.add(examGrade);
                    builder.quizzes(participation);
                    break;
            }
        }
        
        return builder.build();
    }
    
    private Double calculateStudentCourseAverage(List<EnhancedGrade> grades) {
        if (grades.isEmpty()) return 0.0;
        
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        
        for (EnhancedGrade grade : grades) {
            double weight = getExamTypeWeight(grade.getExamType());
            totalWeightedScore += grade.getPercentage() * weight;
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? totalWeightedScore / totalWeight : 0.0;
    }
    
    private double getExamTypeWeight(CreateEnhancedGradeRequest.ExamType examType) {
        switch (examType) {
            case FINAL_EXAM: return 0.4; // 40%
            case FIRST_EXAM:
            case SECOND_EXAM: return 0.25; // 25% each
            case QUIZ: return 0.05; // 5% each
            case ASSIGNMENT: return 0.1; // 10% each
            case PROJECT: return 0.15; // 15%
            case PARTICIPATION: return 0.05; // 5%
            default: return 0.1;
        }
    }
    
    private Double calculateStudentAttendanceRate(Long studentId) {
        // This is a simplified implementation
        // In a real system, you would integrate with the attendance system
        // For now, return a random value between 80-100%
        return 85.0 + (Math.random() * 15.0);
    }
    
    private EnhancedGradeResponse mapToEnhancedGradeResponse(EnhancedGrade grade) {
        return EnhancedGradeResponse.builder()
                .id(grade.getId())
                .studentId(grade.getStudentId())
                .studentFirstName(grade.getStudentFirstName())
                .studentLastName(grade.getStudentLastName())
                .studentEmail(grade.getStudentEmail())
                .classId(grade.getClassId())
                .className(grade.getClassName())
                .courseId(grade.getCourseId())
                .courseName(grade.getCourseName())
                .courseCode(grade.getCourseCode())
                .courseCoefficient(grade.getCourseCoefficient())
                .teacherId(grade.getTeacherId())
                .teacherFirstName(grade.getTeacherFirstName())
                .teacherLastName(grade.getTeacherLastName())
                .teacherEmail(grade.getTeacherEmail())
                .examType(grade.getExamType())
                .semester(grade.getSemester())
                .score(grade.getScore())
                .maxScore(grade.getMaxScore())
                .percentage(grade.getPercentage())
                .teacherRemarks(grade.getTeacherRemarks())
                .teacherSignature(grade.getTeacherSignature())
                .gradedAt(grade.getGradedAt())
                .createdAt(grade.getCreatedAt())
                .updatedAt(grade.getUpdatedAt())
                .build();
    }
    
    private Double getGradeByExamType(List<EnhancedGrade> grades, CreateEnhancedGradeRequest.ExamType examType) {
        return grades.stream()
                .filter(g -> g.getExamType() == examType)
                .findFirst()
                .map(EnhancedGrade::getScore)
                .orElse(null);
    }
    
    private List<Double> getGradesByExamType(List<EnhancedGrade> grades, CreateEnhancedGradeRequest.ExamType examType) {
        return grades.stream()
                .filter(g -> g.getExamType() == examType)
                .map(EnhancedGrade::getScore)
                .collect(Collectors.toList());
    }
    
    private double calculateCourseAverage(List<EnhancedGrade> grades) {
        if (grades.isEmpty()) return 0.0;
        
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        
        for (EnhancedGrade grade : grades) {
            double weight = getExamTypeWeight(grade.getExamType());
            totalWeightedScore += grade.getPercentage() * weight;
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? totalWeightedScore / totalWeight : 0.0;
    }
    
    private String getLatestTeacherRemarks(List<EnhancedGrade> grades) {
        return grades.stream()
                .filter(g -> g.getTeacherRemarks() != null && !g.getTeacherRemarks().isEmpty())
                .max(Comparator.comparing(EnhancedGrade::getGradedAt))
                .map(EnhancedGrade::getTeacherRemarks)
                .orElse(null);
    }
    
    private String getLatestTeacherSignature(List<EnhancedGrade> grades) {
        return grades.stream()
                .filter(g -> g.getTeacherSignature() != null && !g.getTeacherSignature().isEmpty())
                .max(Comparator.comparing(EnhancedGrade::getGradedAt))
                .map(EnhancedGrade::getTeacherSignature)
                .orElse(null);
    }
    
    private Integer calculateClassRank(Long studentId, Long classId, CreateEnhancedGradeRequest.Semester semester) {
        // This is a simplified implementation
        // In a real system, you would calculate the actual rank based on weighted averages
        return (int) (Math.random() * 30) + 1; // Random rank between 1-30
    }
    
    private String getApprovalDate(List<EnhancedGrade> grades) {
        return grades.stream()
                .filter(g -> g.getApprovedAt() != null)
                .max(Comparator.comparing(EnhancedGrade::getApprovedAt))
                .map(g -> g.getApprovedAt().toString())
                .orElse(null);
    }
    
    private String getApprovalBy(List<EnhancedGrade> grades) {
        return grades.stream()
                .filter(g -> g.getApprovedBy() != null)
                .findFirst()
                .map(EnhancedGrade::getApprovedBy)
                .orElse(null);
    }
    
    private Integer calculateStudentTotalAbsences(Long studentId) {
        // This is a simplified implementation
        // In a real system, you would integrate with the attendance system
        return (int) (Math.random() * 10); // Random absences between 0-10
    }
    
    private String generateGradeSheetPdfContent(StudentGradeSheet gradeSheet) {
        // This is a placeholder for PDF generation
        // In a real implementation, you would use a PDF library
        StringBuilder content = new StringBuilder();
        content.append("GRADE SHEET\n");
        content.append("Student: ").append(gradeSheet.getStudentFirstName()).append(" ").append(gradeSheet.getStudentLastName()).append("\n");
        content.append("Class: ").append(gradeSheet.getClassName()).append("\n");
        content.append("Semester: ").append(gradeSheet.getSemester()).append("\n");
        content.append("Weighted Average: ").append(String.format("%.2f", gradeSheet.getWeightedAverage())).append("\n");
        content.append("Class Rank: ").append(gradeSheet.getClassRank()).append("/").append(gradeSheet.getTotalStudents()).append("\n");
        content.append("Attendance Rate: ").append(String.format("%.1f%%", gradeSheet.getAttendanceRate())).append("\n");
        
        for (StudentGradeSheet.SubjectGrade subject : gradeSheet.getSubjects()) {
            content.append("\nSubject: ").append(subject.getCourseName()).append(" (").append(subject.getCourseCode()).append(")\n");
            content.append("Teacher: ").append(subject.getTeacherFirstName()).append(" ").append(subject.getTeacherLastName()).append("\n");
            content.append("Average: ").append(String.format("%.2f", subject.getAverage())).append(" (").append(subject.getLetterGrade()).append(")\n");
            if (subject.getTeacherRemarks() != null) {
                content.append("Remarks: ").append(subject.getTeacherRemarks()).append("\n");
            }
        }
        
        return content.toString();
    }
} 