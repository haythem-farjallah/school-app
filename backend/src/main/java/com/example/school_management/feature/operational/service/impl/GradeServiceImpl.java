package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.BadRequestException;
import com.example.school_management.commons.exceptions.ConflictException;
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
import com.example.school_management.commons.utils.FilterFields;
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
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.lowagie.text.DocumentException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class GradeServiceImpl implements GradeService {
    /** Paths accepted by GET /api/v1/grades/filter; its sortable paths also bound every paged grade read. */
    private static final FilterFields FILTER_FIELDS = new FilterFields(
            Set.of("content", "score", "weight", "gradedAt",
                    "enrollment.id", "enrollment.student.id", "enrollment.classEntity.id", "assignedBy.id"),
            Set.of("gradedAt", "score", "content"));

    private final GradeRepository gradeRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnhancedGradeRepository enhancedGradeRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    private final CurrentSchoolResolver currentSchool;
    private final CurrentAcademicYearResolver currentAcademicYear;
    private final SchoolMembershipRepository memberships;

    private final AuditService auditService;
    private final OperationalMapper mapper;
    private final TemplateEngine templateEngine;

    @Override
    @Transactional
    public void enterBulkGrades(BulkGradeEntryRequest request) {
        if (request.getClassId() == null || request.getGrades() == null) {
            throw new BadRequestException("Class ID and grades are required");
        }
        Set<Long> seen = new HashSet<>();
        // Get current teacher from security context
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Teacher teacher = teacherRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        requireSchoolTeacher(teacher.getId());
        requireSchoolClass(request.getClassId());
        if (request.getCourseId() != null) requireSchoolCourse(request.getCourseId());
        for (BulkGradeEntryRequest.StudentGradeEntry entry : request.getGrades()) {
            if (entry == null || entry.getStudentId() == null || entry.getValue() == null) {
                throw new BadRequestException("Student ID and grade value are required");
            }
            if (!Double.isFinite(entry.getValue()) || entry.getValue() < 0 || entry.getValue() > 20) {
                throw new BadRequestException("Grade value must be between 0 and 20");
            }
            if (!seen.add(entry.getStudentId())) {
                throw new BadRequestException("Duplicate grade for student ID: " + entry.getStudentId());
            }
        }
        List<Enrollment> enrollments = request.getGrades().stream()
                .map(entry -> requireSchoolActiveEnrollment(entry.getStudentId(), request.getClassId())).toList();
        for (int index = 0; index < request.getGrades().size(); index++) {
            BulkGradeEntryRequest.StudentGradeEntry entry = request.getGrades().get(index);
            Enrollment enrollment = enrollments.get(index);
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
        Grade grade = requireSchoolGrade(gradeId);
        BaseUser currentUser = getCurrentUser();
        if (!canEditGrade(grade, currentUser.getId())) {
            throw new AccessDeniedException("You can only update grades you assigned");
        }
        
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
        String auditDetails = String.format("Grade updated. Old values: %s. New values: %s. Reason: %s", 
            oldValues, newValues, request.getUpdateReason());
        auditService.createGradeAuditEvent(AuditEventType.GRADE_UPDATED, gradeId, 
            "Grade updated", auditDetails, currentUser);
        
        log.info("Grade {} updated by user {}", gradeId, currentUser.getId());
    }

    @Override
    @Transactional
    public void deleteGrade(Long gradeId, DeleteGradeRequest request) {
        Grade grade = requireSchoolGrade(gradeId);
        BaseUser currentUser = getCurrentUser();
        if (!canDeleteGrade(grade, currentUser.getId())) {
            throw new AccessDeniedException("You can only delete grades you assigned in the last 24 hours");
        }
        
        String gradeDetails = String.format("Student: %s %s, Score: %.2f, Content: %s", 
            grade.getEnrollment().getStudent().getFirstName(),
            grade.getEnrollment().getStudent().getLastName(),
            grade.getScore(), grade.getContent());
        
        String auditDetails = String.format("Grade deleted. Details: %s. Reason: %s. Notes: %s", 
            gradeDetails, request.getReason(), request.getAdditionalNotes());
        auditService.createGradeAuditEvent(AuditEventType.GRADE_DELETED, gradeId, 
            "Grade deleted", auditDetails, currentUser);
        
        gradeRepository.delete(grade);
        
        log.info("Grade {} deleted by user {}", gradeId, currentUser.getId());
    }

    @Override
    public GradeResponse getGradeById(Long gradeId) {
        Grade grade = requireSchoolGrade(gradeId);
        
        GradeResponse response = mapper.toGradeResponse(grade);
        BaseUser currentUser = getCurrentUser();
        response.setCanEdit(canEditGrade(grade, currentUser.getId()));
        response.setCanDelete(canDeleteGrade(grade, currentUser.getId()));
        
        return response;
    }

    @Override
    public List<GradeResponse> getGradesByStudentId(Long studentId) {
        requireSchoolStudent(studentId);
        List<Grade> grades = gradeRepository.findByStudentIdAndSchoolId(studentId, currentSchool.resolve().getId());
        return grades.stream()
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<GradeResponse> getGradesByClassId(Long classId) {
        requireSchoolClass(classId);
        List<Grade> grades = gradeRepository.findByClassIdAndSchoolId(classId, currentSchool.resolve().getId());
        return grades.stream()
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<GradeResponse> getGradesByTeacherId(Long teacherId) {
        requireSchoolTeacher(teacherId);
        List<Grade> grades = gradeRepository.findByTeacherIdAndSchoolId(teacherId, currentSchool.resolve().getId());
        return grades.stream()
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<GradeResponse> getGradesByEnrollmentId(Long enrollmentId) {
        requireSchoolEnrollment(enrollmentId);
        List<Grade> grades = gradeRepository.findByEnrollmentIdAndSchoolId(enrollmentId, currentSchool.resolve().getId());
        return grades.stream()
            .map(this::mapGradeToResponse)
            .collect(Collectors.toList());
    }

    @Override
    public Page<GradeResponse> getGradesByStudentId(Long studentId, Pageable pageable) {
        FILTER_FIELDS.requireSortable(pageable.getSort());
        requireSchoolStudent(studentId);
        Page<Grade> grades = gradeRepository.findByStudentIdAndSchoolId(studentId, currentSchool.resolve().getId(), pageable);
        return grades.map(this::mapGradeToResponse);
    }

    @Override
    public Page<GradeResponse> getGradesByClassId(Long classId, Pageable pageable) {
        FILTER_FIELDS.requireSortable(pageable.getSort());
        requireSchoolClass(classId);
        Page<Grade> grades = gradeRepository.findByClassIdAndSchoolId(classId, currentSchool.resolve().getId(), pageable);
        return grades.map(this::mapGradeToResponse);
    }

    @Override
    public Page<GradeResponse> getGradesByEnrollmentId(Long enrollmentId, Pageable pageable) {
        FILTER_FIELDS.requireSortable(pageable.getSort());
        requireSchoolEnrollment(enrollmentId);
        Page<Grade> grades = gradeRepository.findByEnrollmentIdAndSchoolId(enrollmentId, currentSchool.resolve().getId(), pageable);
        return grades.map(this::mapGradeToResponse);
    }

    @Override
    public GradeStatistics getStudentGradeStatistics(Long studentId) {
        requireSchoolStudent(studentId);
        List<Grade> grades = gradeRepository.findByStudentIdAndSchoolId(studentId, currentSchool.resolve().getId());
        return calculateGradeStatistics(grades, studentId, null, null);
    }

    @Override
    public GradeStatistics getStudentGradeStatisticsForClass(Long studentId, Long classId) {
        requireSchoolStudent(studentId);
        requireSchoolClass(classId);
        List<Grade> grades = gradeRepository.findByStudentIdAndClassIdAndSchoolId(studentId, classId, currentSchool.resolve().getId());
        return calculateGradeStatistics(grades, studentId, classId, null);
    }

    @Override
    public GradeStatistics getClassGradeStatistics(Long classId) {
        requireSchoolClass(classId);
        List<Grade> grades = gradeRepository.findByClassIdAndSchoolId(classId, currentSchool.resolve().getId());
        return calculateGradeStatistics(grades, null, classId, null);
    }

    @Override
    public GradeStatistics getGradeStatisticsForDateRange(Long studentId, LocalDateTime startDate, LocalDateTime endDate) {
        requireSchoolStudent(studentId);
        List<Grade> grades = gradeRepository.findByStudentIdAndDateRangeAndSchoolId(
                studentId, startDate, endDate, currentSchool.resolve().getId());
        return calculateGradeStatistics(grades, studentId, null, null);
    }

    @Override
    public List<AuditEvent> getGradeAuditHistory(Long gradeId) {
        requireSchoolGrade(gradeId);
        return auditService.getGradeAuditHistory(gradeId);
    }

    @Override
    public boolean canEditGrade(Long gradeId, Long userId) {
        return canEditGrade(requireSchoolGrade(gradeId), userId);
    }

    @Override
    public boolean canDeleteGrade(Long gradeId, Long userId) {
        return canDeleteGrade(requireSchoolGrade(gradeId), userId);
    }

    private boolean canEditGrade(Grade grade, Long userId) {
        return grade.getAssignedBy().getId().equals(userId);
    }

    private boolean canDeleteGrade(Grade grade, Long userId) {
        return canEditGrade(grade, userId) && LocalDateTime.now().isBefore(grade.getGradedAt().plusHours(24));
    }

    @Override
    public Page<GradeResponse> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap, pageable, FILTER_FIELDS);
        Long schoolId = currentSchool.resolve().getId();
        Specification<Grade> schoolSpec = (root, query, cb) -> cb.equal(
                root.get("enrollment").get("classEntity").get("academicYear").get("school").get("id"), schoolId);
        Specification<Grade> spec = schoolSpec.and(DynamicSpecificationBuilder.build(criteria));
        Page<Grade> grades = gradeRepository.findAll(spec, pageable);
        return grades.map(this::mapGradeToResponse);
    }

    @Override
    public Page<GradeResponse> getAllGrades(Pageable pageable, String search, Long courseId) {
        FILTER_FIELDS.requireSortable(pageable.getSort());
        Long schoolId = currentSchool.resolve().getId();
        if (courseId != null) requireSchoolCourse(courseId);
        Page<Grade> grades;

        if (search != null && !search.trim().isEmpty() && courseId != null) {
            // Both search and courseId filters
            grades = gradeRepository.findBySearchAndCourseIdAndSchoolId(search.trim(), courseId, schoolId, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            // Only search filter
            grades = gradeRepository.findBySearchAndSchoolId(search.trim(), schoolId, pageable);
        } else if (courseId != null) {
            // Only courseId filter
            grades = gradeRepository.findByCourseIdAndSchoolId(courseId, schoolId, pageable);
        } else {
            // No filters - get all
            grades = gradeRepository.findBySchoolId(schoolId, pageable);
        }
        
        return grades.map(this::mapGradeToResponse);
    }
    
    // Helper methods
    private GradeResponse mapGradeToResponse(Grade grade) {
        GradeResponse response = mapper.toGradeResponse(grade);
        BaseUser currentUser = getCurrentUser();
        response.setCanEdit(canEditGrade(grade, currentUser.getId()));
        response.setCanDelete(canDeleteGrade(grade, currentUser.getId()));
        return response;
    }
    
    private Grade requireSchoolGrade(Long gradeId) {
        return gradeRepository.findByIdAndSchoolId(gradeId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Grade not found"));
    }

    private void requireMembershipRole(Long userId, MembershipRole role, String resource) {
        memberships.findByUserIdAndSchoolId(userId, currentSchool.resolve().getId())
                .filter(m -> m.getRoles().contains(role))
                .orElseThrow(() -> new ResourceNotFoundException(resource + " not found"));
    }

    private Student requireSchoolStudent(Long studentId) {
        requireMembershipRole(studentId, MembershipRole.STUDENT, "Student");
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private Teacher requireSchoolTeacher(Long teacherId) {
        requireMembershipRole(teacherId, MembershipRole.TEACHER, "Teacher");
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
    }

    private ClassEntity requireSchoolClass(Long classId) {
        return classRepository.findByIdAndAcademicYearSchoolId(classId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private Course requireSchoolCourse(Long courseId) {
        return courseRepository.findByIdAndSchoolId(courseId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private Enrollment requireSchoolEnrollment(Long enrollmentId) {
        return enrollmentRepository.findByIdAndSchoolId(enrollmentId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
    }

    private Enrollment requireSchoolActiveEnrollment(Long studentId, Long classId) {
        requireSchoolStudent(studentId);
        requireSchoolClass(classId);
        Enrollment enrollment = enrollmentRepository.findActiveByStudentIdAndClassId(studentId, classId)
                .orElseThrow(() -> new ResourceNotFoundException("Active enrollment not found"));
        return requireSchoolEnrollment(enrollment.getId());
    }

    /** The authenticated account of any role; edit and delete flags are true only for the assigning teacher. */
    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Current user not found"));
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
                .trend(null)
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
            .trend(null)
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
        requireSchoolTeacher(teacherId);
        List<TeachingAssignment> assignments = teachingAssignmentRepository.findByTeacherIdAndSchoolId(teacherId, currentSchool.resolve().getId());
        
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
        
        requireSchoolTeacher(teacherId);
        requireSchoolClass(classId);
        requireSchoolCourse(courseId);
        TeachingAssignment assignment = teachingAssignmentRepository
                .findByTeacherIdAndSchoolId(teacherId, currentSchool.resolve().getId()).stream()
                .filter(ta -> ta.getClazz().getId().equals(classId) && ta.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Teacher is not assigned to this class and course"));

        // Get all students enrolled in this class
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        
        List<TeacherGradeClassView.TeacherGradeStudent> students = new ArrayList<>();
        
        for (Enrollment enrollment : enrollments) {
            // Get existing grades for this student in this course
            List<EnhancedGrade> existingGrades = enhancedGradeRepository.findByStudentIdAndClassIdAndCourseIdAndSemester(
                    enrollment.getStudent().getId(), classId, courseId, CreateEnhancedGradeRequest.Semester.FIRST);

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
                    .attendanceRate(null)
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
        
        Teacher teacher = requireSchoolTeacher(getCurrentUser().getId());
        requireSchoolClass(request.getClassId());
        Course course = requireSchoolCourse(request.getCourseId());
        List<EnhancedGradeResponse> responses = new ArrayList<>();

        List<Enrollment> enrollments = request.getGrades().stream()
                .map(entry -> requireSchoolActiveEnrollment(entry.getStudentId(), request.getClassId())).toList();
        for (int index = 0; index < request.getGrades().size(); index++) {
            BulkEnhancedGradeEntryRequest.StudentGradeEntry gradeEntry = request.getGrades().get(index);
            Enrollment enrollment = enrollments.get(index);

            // Check if grade already exists
            Optional<EnhancedGrade> existingGrade = enhancedGradeRepository.findByStudentIdAndClassIdAndCourseIdAndExamTypeAndSemester(
                    gradeEntry.getStudentId(), request.getClassId(), request.getCourseId(), request.getExamType(), request.getSemester());
            
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
                
                grade.setTeacherId(teacher.getId());
                grade.setTeacherFirstName(teacher.getFirstName());
                grade.setTeacherLastName(teacher.getLastName());
                grade.setTeacherEmail(teacher.getEmail());
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
        
        Teacher teacher = requireSchoolTeacher(getCurrentUser().getId());
        Enrollment enrollment = requireSchoolActiveEnrollment(request.getStudentId(), request.getClassId());
        Course course = requireSchoolCourse(request.getCourseId());

        // Check if grade already exists
        Optional<EnhancedGrade> existingGrade = enhancedGradeRepository.findByStudentIdAndClassIdAndCourseIdAndExamTypeAndSemester(
                request.getStudentId(), request.getClassId(), request.getCourseId(), request.getExamType(), request.getSemester());
        
        if (existingGrade.isPresent()) {
            throw new ConflictException("Grade already exists for this student, course, exam type, and semester");
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
        
        grade.setTeacherId(teacher.getId());
        grade.setTeacherFirstName(teacher.getFirstName());
        grade.setTeacherLastName(teacher.getLastName());
        grade.setTeacherEmail(teacher.getEmail());

        EnhancedGrade savedGrade = enhancedGradeRepository.save(grade);
        log.info("Created enhanced grade with ID: {}", savedGrade.getId());
        
        return mapToEnhancedGradeResponse(savedGrade);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<StaffGradeReview> getStaffGradeReviews(Long classId, CreateEnhancedGradeRequest.Semester semester) {
        log.debug("Getting staff grade reviews for class: {}, semester: {}", classId, semester);
        
        requireSchoolClass(classId);
        // Get all students in the class
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ACTIVE);
        List<StaffGradeReview> reviews = new ArrayList<>();
        
        for (Enrollment enrollment : enrollments) {
            // Get all grades for this student in this semester
            List<EnhancedGrade> studentGrades = enhancedGradeRepository.findByStudentIdAndClassIdAndSemesterAndSchoolId(
                    enrollment.getStudent().getId(), classId, semester, currentSchool.resolve().getId());
            
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
                    .classRank(null)
                    .attendanceRate(null)
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
        // The approver is the authenticated account, recorded by its e-mail.
        String approvedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Validate the entire request before mutating any rows.
        request.getStudentIds().forEach(this::requireSchoolStudent);
        Long schoolId = currentSchool.resolve().getId();
        for (Long studentId : request.getStudentIds()) {
            List<EnhancedGrade> studentGrades = enhancedGradeRepository.findByStudentIdAndSemesterAndSchoolId(
                    studentId, request.getSemester(), schoolId);
            
            for (EnhancedGrade grade : studentGrades) {
                grade.setIsApproved(true);
                grade.setApprovedAt(LocalDateTime.now());
                grade.setApprovedBy(approvedBy);
            }
            
            enhancedGradeRepository.saveAll(studentGrades);
        }
        
        log.info("Approved grades for {} students", request.getStudentIds().size());
    }
    
    @Override
    @Transactional(readOnly = true)
    public StudentGradeSheet getStudentGradeSheet(Long studentId, CreateEnhancedGradeRequest.Semester semester) {
        log.debug("Generating grade sheet for student: {}, semester: {}", studentId, semester);
        
        Student student = requireSchoolStudent(studentId);
        Long schoolId = currentSchool.resolve().getId();
        Enrollment enrollment = enrollmentRepository.findActiveByStudentIdAndAcademicYearIdAndSchoolId(
                        studentId, currentAcademicYear.resolve().getId(), schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("No active enrollment found for student in current academic year"));
        List<EnhancedGrade> studentGrades = enhancedGradeRepository.findByStudentIdAndClassIdAndSemesterAndSchoolId(
                studentId, enrollment.getClassEntity().getId(), semester, schoolId);

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
        
        // Count students with stored grades; rank and attendance semantics are not defined.

        Long totalStudents = enhancedGradeRepository.countDistinctStudentsByClassIdAndSemesterAndSchoolId(
                enrollment.getClassEntity().getId(), semester, schoolId);

        // Check approval status
        StudentGradeSheet.ApprovedBy approvedBy = null;
        if (studentGrades.stream().allMatch(EnhancedGrade::getIsApproved)) {
            String approvedByName = getApprovalBy(studentGrades);
            String approvedAt = getApprovalDate(studentGrades);
            if (approvedByName != null && approvedAt != null) {
                approvedBy = StudentGradeSheet.ApprovedBy.builder()
                        .staffId(userRepository.findByEmail(approvedByName)
                                .filter(Staff.class::isInstance)
                                .map(BaseUser::getId)
                                .orElse(null))
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
                .classRank(null)
                .totalStudents(totalStudents.intValue())
                .attendanceRate(null)
                .totalAbsences(null)
                .generatedAt(LocalDateTime.now().toString())
                .approvedBy(approvedBy)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public byte[] exportStudentGradeSheet(Long studentId, CreateEnhancedGradeRequest.Semester semester) {
        log.debug("Exporting grade sheet for student: {}, semester: {}", studentId, semester);
        
        StudentGradeSheet gradeSheet = getStudentGradeSheet(studentId, semester);
        Context context = new Context(Locale.ROOT);
        context.setVariable("sheet", gradeSheet);
        String xhtml = templateEngine.process("grades/grade-sheet", context);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(xhtml);
        renderer.layout();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            renderer.createPDF(output);
        } catch (DocumentException exception) {
            throw new IllegalStateException("Failed to render grade sheet PDF", exception);
        }
        return output.toByteArray();
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

}
