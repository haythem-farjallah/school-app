package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.operational.dto.EnrollmentDto;
import com.example.school_management.feature.operational.dto.EnrollmentStatsDto;
import com.example.school_management.feature.operational.dto.AutoEnrollmentResultDto;
import com.example.school_management.feature.auth.entity.enums.GradeLevel;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.service.EnrollmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepo;
    private final StudentRepository studentRepo;
    private final ClassRepository classRepo;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;
    private final RealTimeNotificationService realTimeNotificationService;

    @Override
    public EnrollmentDto enrollStudent(Long studentId, Long classId) {
        log.debug("Enrolling student {} in class {}", studentId, classId);
        
        // Check if already enrolled
        if (enrollmentRepo.findByStudentIdAndClassId(studentId, classId).isPresent()) {
            throw new ConflictException("Student is already enrolled in this class");
        }
        
        // Get student and class entities
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        ClassEntity classEntity = classRepo.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        
        // Check enrollment capacity
        if (!canEnrollStudent(studentId, classId)) {
            throw new ConflictException("Cannot enroll student: capacity limit reached or prerequisites not met");
        }
        
        // Create enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassEntity(classEntity);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledAt(LocalDateTime.now());
        
        Enrollment savedEnrollment = enrollmentRepo.save(enrollment);
        log.info("Student {} enrolled in class {} with enrollment id {}", studentId, classId, savedEnrollment.getId());
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Student enrolled in class";
            String details = String.format("Student %s %s enrolled in class %s", 
                student.getFirstName(), student.getLastName(), classEntity.getName());
            
            auditService.createAuditEvent(
                AuditEventType.ENROLLMENT_CREATED,
                "Enrollment",
                savedEnrollment.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for enrollment: {}", e.getMessage());
        }
        
        // Send real-time enrollment notification
        try {
            String studentName = student.getFirstName() + " " + student.getLastName();
            
            realTimeNotificationService.notifyEnrollmentChange(
                studentName, 
                classEntity.getName(), 
                "ENROLLED", 
                student.getId(), 
                null // Parent ID - could be enhanced with a repository lookup if needed
            );
        } catch (Exception e) {
            log.warn("Failed to send real-time enrollment notification: {}", e.getMessage());
        }
        
        return toDto(savedEnrollment);
    }

    @Override
    public EnrollmentDto transferStudent(Long enrollmentId, Long newClassId) {
        log.debug("Transferring enrollment {} to class {}", enrollmentId, newClassId);
        
        Enrollment enrollment = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        
        ClassEntity oldClass = enrollment.getClassEntity();
        ClassEntity newClass = classRepo.findById(newClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + newClassId));
        
        enrollment.setClassEntity(newClass);
        Enrollment updatedEnrollment = enrollmentRepo.save(enrollment);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Student transferred between classes";
            String details = String.format("Student %s %s transferred from class %s to %s", 
                enrollment.getStudent().getFirstName(), enrollment.getStudent().getLastName(),
                oldClass.getName(), newClass.getName());
            
            auditService.createAuditEvent(
                AuditEventType.ENROLLMENT_UPDATED,
                "Enrollment",
                enrollmentId,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for enrollment transfer: {}", e.getMessage());
        }
        
        return toDto(updatedEnrollment);
    }

    @Override
    public EnrollmentDto updateEnrollmentStatus(Long enrollmentId, EnrollmentStatus status) {
        log.debug("Updating enrollment {} status to {}", enrollmentId, status);
        
        Enrollment enrollment = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        
        EnrollmentStatus oldStatus = enrollment.getStatus();
        enrollment.setStatus(status);
        Enrollment updatedEnrollment = enrollmentRepo.save(enrollment);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Enrollment status updated";
            String details = String.format("Enrollment status changed from %s to %s for student %s %s in class %s", 
                oldStatus, status,
                enrollment.getStudent().getFirstName(), enrollment.getStudent().getLastName(),
                enrollment.getClassEntity().getName());
            
            auditService.createAuditEvent(
                AuditEventType.ENROLLMENT_UPDATED,
                "Enrollment",
                enrollmentId,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for enrollment status update: {}", e.getMessage());
        }
        
        return toDto(updatedEnrollment);
    }

    @Override
    public void dropEnrollment(Long enrollmentId, String reason) {
        log.debug("Dropping enrollment {} with reason: {}", enrollmentId, reason);
        
        Enrollment enrollment = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        
        String studentInfo = String.format("%s %s from class %s", 
            enrollment.getStudent().getFirstName(), 
            enrollment.getStudent().getLastName(),
            enrollment.getClassEntity().getName());
        
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepo.save(enrollment);
        
        // Create audit event
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = "Student dropped from enrollment";
            String details = String.format("Student %s dropped. Reason: %s", studentInfo, reason);
            
            auditService.createAuditEvent(
                AuditEventType.ENROLLMENT_DELETED,
                "Enrollment",
                enrollmentId,
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for enrollment drop: {}", e.getMessage());
        }
        
        log.info("Enrollment {} dropped: {}", enrollmentId, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentDto getEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepo.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        return toDto(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getAllEnrollments(Pageable pageable, String search, EnrollmentStatus status) {
        log.debug("Getting all enrollments with search: {}, status: {}", search, status);
        
        Page<Enrollment> enrollments;
        
        if (search != null && !search.trim().isEmpty() && status != null) {
            // Both search and status filters
            enrollments = enrollmentRepo.findBySearchAndStatus(search.trim(), status, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            // Only search filter
            enrollments = enrollmentRepo.findBySearch(search.trim(), pageable);
        } else if (status != null) {
            // Only status filter
            enrollments = enrollmentRepo.findByStatus(status, pageable);
        } else {
            // No filters - get all
            enrollments = enrollmentRepo.findAll(pageable);
        }
        
        return enrollments.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getStudentEnrollments(Long studentId, Pageable pageable) {
        return enrollmentRepo.findByStudentId(studentId, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getClassEnrollments(Long classId, Pageable pageable) {
        return enrollmentRepo.findByClassId(classId, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getEnrollmentsByStatus(EnrollmentStatus status, Pageable pageable) {
        return enrollmentRepo.findByStatus(status, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getEnrollmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return enrollmentRepo.findByEnrolledAtBetween(startDate, endDate, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentStatsDto getClassEnrollmentStats(Long classId) {
        List<Enrollment> enrollments = enrollmentRepo.findAllByClassId(classId);
        return calculateStats(enrollments);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentStatsDto getStudentEnrollmentStats(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepo.findAllByStudentId(studentId);
        return calculateStats(enrollments);
    }

    @Override
    public void bulkEnrollStudents(Long classId, List<Long> studentIds) {
        log.debug("Bulk enrolling {} students in class {}", studentIds.size(), classId);
        
        ClassEntity classEntity = classRepo.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        

                log.debug("Bulk enrolling students in class: {}", classEntity.getName());
        
        int enrolled = 0;
        for (Long studentId : studentIds) {
            try {
                if (!enrollmentRepo.findByStudentIdAndClassId(studentId, classId).isPresent()) {
                    enrollStudent(studentId, classId);
                    enrolled++;
                }
            } catch (Exception e) {
                log.warn("Failed to enroll student {} in class {}: {}", studentId, classId, e.getMessage());
            }
        }
        
        log.info("Bulk enrollment completed: {}/{} students enrolled in class {}", enrolled, studentIds.size(), classId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEnrollStudent(Long studentId, Long classId) {
        // Check if already enrolled
        if (enrollmentRepo.findByStudentIdAndClassId(studentId, classId).isPresent()) {
            return false;
        }
        
        // Check class capacity (simple implementation - can be enhanced)
        Long activeEnrollments = enrollmentRepo.countActiveEnrollmentsByClassId(classId, EnrollmentStatus.ACTIVE);
        // Assuming max capacity of 30 students per class (can be configured)
        return activeEnrollments < 30;
    }

    private EnrollmentDto toDto(Enrollment enrollment) {
        return new EnrollmentDto(
            enrollment.getId(),
            enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName(),
            enrollment.getStudent().getEmail(),
            enrollment.getClassEntity().getName(),
            enrollment.getGrades().size(),
            enrollment.getEnrolledAt().toLocalDate(),
            enrollment.getStatus(),
            enrollment.getFinalGrad(),
            enrollment.getStudent().getId(),
            enrollment.getClassEntity().getId(),
            enrollment.getEnrolledAt().toLocalDate(),
            null
        );
    }

    private EnrollmentStatsDto calculateStats(List<Enrollment> enrollments) {
        long total = enrollments.size();
        long active = enrollments.stream().mapToLong(e -> e.getStatus() == EnrollmentStatus.ACTIVE ? 1 : 0).sum();
        long pending = enrollments.stream().mapToLong(e -> e.getStatus() == EnrollmentStatus.PENDING ? 1 : 0).sum();
        long completed = enrollments.stream().mapToLong(e -> e.getStatus() == EnrollmentStatus.COMPLETED ? 1 : 0).sum();
        long dropped = enrollments.stream().mapToLong(e -> e.getStatus() == EnrollmentStatus.DROPPED ? 1 : 0).sum();
        
        double completionRate = total > 0 ? (double) completed / total * 100 : 0.0;
        double averageFinalGrade = enrollments.stream()
                .filter(e -> e.getFinalGrad() != null)
                .mapToDouble(Enrollment::getFinalGrad)
                .average().orElse(0.0);
        
        return new EnrollmentStatsDto(total, active, pending, completed, dropped, completionRate, averageFinalGrade);
    }

    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + email));
    }

    @Override
    @Transactional
    public AutoEnrollmentResultDto autoEnrollAllStudents() {
        log.info("Starting auto-enrollment process for all students");
        return performAutoEnrollment(null, false);
    }

    @Override
    @Transactional
    public AutoEnrollmentResultDto autoEnrollByGradeLevel(String gradeLevel) {
        log.info("Starting auto-enrollment process for grade level: {}", gradeLevel);
        
        try {
            GradeLevel.valueOf(gradeLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid grade level: " + gradeLevel);
        }
        
        return performAutoEnrollment(gradeLevel, false);
    }

    @Override
    @Transactional(readOnly = true)
    public AutoEnrollmentResultDto previewAutoEnrollment() {
        log.info("Generating auto-enrollment preview");
        return performAutoEnrollment(null, true);
    }

    private AutoEnrollmentResultDto performAutoEnrollment(String targetGradeLevel, boolean isPreview) {
        List<String> errors = new ArrayList<>();
        List<String> createdClasses = new ArrayList<>();
        Map<String, Integer> enrollmentsByGradeLevel = new HashMap<>();
        
        int totalStudentsProcessed = 0;
        int studentsEnrolled = 0;
        int studentsAlreadyEnrolled = 0;
        int classesCreated = 0;
        int classesUsed = 0;
        
        try {
            // Get all students without active enrollments
            List<Student> unenrolledStudents = getUnenrolledStudents(targetGradeLevel);
            totalStudentsProcessed = unenrolledStudents.size();
            
            log.info("Found {} unenrolled students", totalStudentsProcessed);
            
            // Group students by grade level
            Map<GradeLevel, List<Student>> studentsByGrade = unenrolledStudents.stream()
                .filter(student -> student.getGradeLevel() != null)
                .collect(Collectors.groupingBy(Student::getGradeLevel));
            
            // Process each grade level
            for (Map.Entry<GradeLevel, List<Student>> entry : studentsByGrade.entrySet()) {
                GradeLevel gradeLevel = entry.getKey();
                List<Student> studentsInGrade = entry.getValue();
                
                log.info("Processing {} students for grade level: {}", studentsInGrade.size(), gradeLevel);
                
                // Find or create classes for this grade level
                List<ClassEntity> availableClasses = findOrCreateClassesForGradeLevel(gradeLevel, studentsInGrade.size(), isPreview);
                
                if (!isPreview) {
                    classesCreated += (int) availableClasses.stream()
                        .filter(cls -> cls.getId() == null)
                        .count();
                    
                    classesUsed += availableClasses.size();
                    
                    // Save new classes
                    availableClasses = availableClasses.stream()
                        .map(cls -> cls.getId() == null ? classRepo.save(cls) : cls)
                        .collect(Collectors.toList());
                }
                
                // Track created classes
                availableClasses.stream()
                    .filter(cls -> isPreview || cls.getId() != null)
                    .forEach(cls -> createdClasses.add(cls.getName()));
                
                // Enroll students in classes
                int enrolledInGrade = enrollStudentsInClasses(studentsInGrade, availableClasses, isPreview);
                studentsEnrolled += enrolledInGrade;
                enrollmentsByGradeLevel.put(gradeLevel.name(), enrolledInGrade);
            }
            
            // Check for students with null grade levels
            long studentsWithoutGrade = unenrolledStudents.stream()
                .filter(student -> student.getGradeLevel() == null)
                .count();
            
            if (studentsWithoutGrade > 0) {
                errors.add(String.format("%d students have no grade level assigned", studentsWithoutGrade));
            }
            
            String message = isPreview 
                ? String.format("Preview: Would enroll %d students and create %d classes", studentsEnrolled, classesCreated)
                : String.format("Successfully enrolled %d students into %d classes (%d new classes created)", 
                    studentsEnrolled, classesUsed, classesCreated);
            
            return new AutoEnrollmentResultDto(
                true,
                message,
                totalStudentsProcessed,
                studentsEnrolled,
                studentsAlreadyEnrolled,
                classesCreated,
                classesUsed,
                LocalDateTime.now(),
                enrollmentsByGradeLevel,
                createdClasses,
                errors,
                isPreview
            );
            
        } catch (Exception e) {
            log.error("Error during auto-enrollment process", e);
            errors.add("Auto-enrollment failed: " + e.getMessage());
            
            return new AutoEnrollmentResultDto(
                false,
                "Auto-enrollment process failed",
                totalStudentsProcessed,
                studentsEnrolled,
                studentsAlreadyEnrolled,
                classesCreated,
                classesUsed,
                LocalDateTime.now(),
                enrollmentsByGradeLevel,
                createdClasses,
                errors,
                isPreview
            );
        }
    }

    private List<Student> getUnenrolledStudents(String targetGradeLevel) {
        if (targetGradeLevel != null) {
            GradeLevel gradeLevel = GradeLevel.valueOf(targetGradeLevel.toUpperCase());
            return studentRepo.findAll().stream()
                .filter(student -> student.getGradeLevel() == gradeLevel)
                .filter(student -> !hasActiveEnrollment(student.getId()))
                .collect(Collectors.toList());
        } else {
            return studentRepo.findAll().stream()
                .filter(student -> !hasActiveEnrollment(student.getId()))
                .collect(Collectors.toList());
        }
    }

    private boolean hasActiveEnrollment(Long studentId) {
        return !enrollmentRepo.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE).isEmpty();
    }

    private List<ClassEntity> findOrCreateClassesForGradeLevel(GradeLevel gradeLevel, int studentCount, boolean isPreview) {
        List<ClassEntity> availableClasses = new ArrayList<>();
        
        // Don't create classes if no students to enroll
        if (studentCount == 0) {
            log.info("No students to enroll for grade level: {}", gradeLevel);
            return availableClasses;
        }
        
        // Find existing classes for this grade level with available capacity
        List<ClassEntity> existingClasses = classRepo.findAll().stream()
            .filter(cls -> gradeLevel.name().equals(cls.getGradeLevel()))
            .filter(cls -> getAvailableCapacity(cls) > 0)
            .sorted((a, b) -> a.getSection() != null && b.getSection() != null ? 
                a.getSection().compareTo(b.getSection()) : 0) // Sort by section
            .collect(Collectors.toList());
        
        availableClasses.addAll(existingClasses);
        
        // Calculate how many students can be accommodated in existing classes
        int availableCapacity = existingClasses.stream()
            .mapToInt(this::getAvailableCapacity)
            .sum();
        
        // Create new classes if needed
        int remainingStudents = studentCount - availableCapacity;
        if (remainingStudents > 0) {
            int newClassesNeeded = (int) Math.ceil((double) remainingStudents / 30); // Default capacity of 30
            
            // Find the next available section letter
            String nextSection = getNextAvailableSection(gradeLevel);
            
            for (int i = 0; i < newClassesNeeded; i++) {
                ClassEntity newClass = createNewClass(gradeLevel, nextSection);
                availableClasses.add(newClass);
                
                // Move to next section letter (A -> B -> C, etc.)
                nextSection = String.valueOf((char) (nextSection.charAt(0) + 1));
            }
        }
        
        return availableClasses;
    }

    private int getAvailableCapacity(ClassEntity classEntity) {
        int currentEnrollments = enrollmentRepo.findByClassIdAndStatus(classEntity.getId(), EnrollmentStatus.ACTIVE).size();
        return Math.max(0, (classEntity.getCapacity() != null ? classEntity.getCapacity() : 30) - currentEnrollments);
    }

    private String getNextAvailableSection(GradeLevel gradeLevel) {
        // Find all existing sections for this grade level
        List<String> existingSections = classRepo.findAll().stream()
            .filter(cls -> gradeLevel.name().equals(cls.getGradeLevel()))
            .map(ClassEntity::getSection)
            .filter(section -> section != null && !section.isEmpty())
            .sorted()
            .collect(Collectors.toList());
        
        // Start from 'A' and find the first available section
        char sectionChar = 'A';
        while (existingSections.contains(String.valueOf(sectionChar))) {
            sectionChar++;
        }
        
        return String.valueOf(sectionChar);
    }

    private ClassEntity createNewClass(GradeLevel gradeLevel, String section) {
        ClassEntity newClass = new ClassEntity();
        
        // Generate class name based on grade level and section
        String className = generateClassName(gradeLevel, section);
        
        newClass.setName(className);
        newClass.setGradeLevel(gradeLevel.name());
        newClass.setSection(section);
        newClass.setAcademicYear("2024-2025"); // Current academic year
        newClass.setCapacity(30);
        newClass.setWeeklyHours(30);
        
        log.info("Creating new class: {}", className);
        return newClass;
    }

    private String generateClassName(GradeLevel gradeLevel, String section) {
        return switch (gradeLevel) {
            case KINDERGARTEN -> "K-" + section;      // K-A, K-B
            case ELEMENTARY -> "E-" + section;        // E-A, E-B (grades 1-6)
            case MIDDLE -> "M-" + section;            // M-A, M-B (grades 7-9)
            case HIGH -> "H-" + section;              // H-A, H-B (grades 10-12)
            case UNIVERSITY -> "U-" + section;        // U-A, U-B
        };
    }

    /**
     * Generate class name with specific grade number if available
     * This method can be enhanced to use specific grade numbers when that information is available
     */
    private String generateClassNameWithGrade(GradeLevel gradeLevel, String section, Integer specificGrade) {
        if (specificGrade != null) {
            return specificGrade + "-" + section;  // 7-A, 8-B, 10-C, etc.
        }
        return generateClassName(gradeLevel, section);
    }

    private int enrollStudentsInClasses(List<Student> students, List<ClassEntity> classes, boolean isPreview) {
        int enrolled = 0;
        int studentIndex = 0;
        
        for (ClassEntity classEntity : classes) {
            int availableCapacity = isPreview ? 30 : getAvailableCapacity(classEntity);
            
            while (availableCapacity > 0 && studentIndex < students.size()) {
                Student student = students.get(studentIndex);
                
                // Check if student is already enrolled in this specific class
                if (!isPreview && isStudentEnrolledInClass(student.getId(), classEntity.getId())) {
                    log.debug("Student {} is already enrolled in class {}, skipping", 
                        student.getEmail(), classEntity.getName());
                    studentIndex++;
                    continue;
                }
                
                if (!isPreview) {
                    try {
                        // Double-check enrollment doesn't exist (race condition protection)
                        if (enrollmentRepo.findByStudentIdAndClassId(student.getId(), classEntity.getId()).isPresent()) {
                            log.debug("Student {} already enrolled in class {} (race condition detected)", 
                                student.getEmail(), classEntity.getName());
                            studentIndex++;
                            continue;
                        }
                        
                        // Create enrollment
                        Enrollment enrollment = new Enrollment();
                        enrollment.setStudent(student);
                        enrollment.setClassEntity(classEntity);
                        enrollment.setStatus(EnrollmentStatus.ACTIVE);
                        enrollment.setEnrolledAt(LocalDateTime.now());
                        
                        enrollmentRepo.save(enrollment);
                        log.debug("Enrolled student {} in class {}", student.getEmail(), classEntity.getName());
                    } catch (Exception e) {
                        log.warn("Failed to enroll student {} in class {}: {}", 
                            student.getEmail(), classEntity.getName(), e.getMessage());
                        studentIndex++;
                        continue;
                    }
                }
                
                enrolled++;
                studentIndex++;
                availableCapacity--;
            }
            
            if (studentIndex >= students.size()) {
                break;
            }
        }
        
        return enrolled;
    }

    private boolean isStudentEnrolledInClass(Long studentId, Long classId) {
        return enrollmentRepo.findByStudentIdAndClassId(studentId, classId).isPresent();
    }
} 