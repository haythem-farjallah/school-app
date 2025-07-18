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
        List<Enrollment> enrollments = enrollmentRepo.findByClassIdAndStatus(classId, null);
        return calculateStats(enrollments);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentStatsDto getStudentEnrollmentStats(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepo.findByStudentIdAndStatus(studentId, null);
        return calculateStats(enrollments);
    }

    @Override
    public void bulkEnrollStudents(Long classId, List<Long> studentIds) {
        log.debug("Bulk enrolling {} students in class {}", studentIds.size(), classId);
        
        ClassEntity classEntity = classRepo.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        
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
        Long activeEnrollments = enrollmentRepo.countActiveEnrollmentsByClassId(classId);
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
} 