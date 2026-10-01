package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.operational.dto.EnrollmentDto;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** One ordinary ACTIVE Enrollment write; callers receive a result only after this transaction commits. */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentWriteExecutor {
    private final EnrollmentRepository enrollments;
    private final StudentRepository students;
    private final ClassRepository classes;
    private final SchoolMembershipRepository memberships;
    private final CurrentSchoolResolver currentSchool;
    private final BaseUserRepository<BaseUser> users;
    private final AuditService audits;
    private final RealTimeNotificationService notifications;

    /** Allows auto orchestration to distinguish exhausted capacity from other Student conflicts. */
    public static class CapacityConflict extends ConflictException {
        public CapacityConflict() {
            super("Class has reached its capacity");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EnrollmentDto enroll(Long studentId, Long classId) {
        Long schoolId = currentSchool.resolve().getId();
        ClassEntity target = classes.findSchoolClassForUpdate(classId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found in current school"));
        var membership = memberships.findByUserIdAndSchoolId(studentId, schoolId)
                .filter(m -> m.getRoles().contains(MembershipRole.STUDENT))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found in current school"));
        var student = students.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found in current school"));
        if (membership.getStatus() != MembershipStatus.ACTIVE || student.getStatus() != Status.ACTIVE) {
            throw new ConflictException("Student cannot receive a new enrollment: membership or account is not active");
        }
        if (enrollments.existsActiveInAcademicYear(studentId, target.getAcademicYear().getId())) {
            throw new ConflictException("Student already has an active enrollment in this academic year");
        }
        if (enrollments.countActiveByClassId(classId) >= (target.getCapacity() == null ? 30 : target.getCapacity())) {
            throw new CapacityConflict();
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassEntity(target);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        try {
            enrollment = enrollments.saveAndFlush(enrollment);
        } catch (DataIntegrityViolationException e) {
            String detail = e.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("uk_enrollments_one_active_per_academic_year")) {
                throw new ConflictException("Student already has an active enrollment in this academic year");
            }
            throw e;
        }

        String studentName = student.getFirstName() + " " + student.getLastName();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        BaseUser actor = authentication == null ? null : users.findByEmail(authentication.getName()).orElse(null);
        audits.createAuditEvent(AuditEventType.ENROLLMENT_CREATED, "Enrollment", enrollment.getId(),
                "Student enrolled in class", "Student " + studentName + " enrolled in class " + target.getName(), actor);

        String className = target.getName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    notifications.notifyEnrollmentChange(studentName, className, "ENROLLED", studentId, null);
                } catch (Exception e) {
                    log.warn("Enrollment notification failed for studentId={} classId={}: {}",
                            studentId, classId, e.getClass().getSimpleName());
                }
            }
        });
        return new EnrollmentDto(enrollment.getId(), studentName, student.getEmail(), className, 0,
                enrollment.getEnrolledAt().toLocalDate(), enrollment.getStatus(), null, studentId, classId,
                enrollment.getEnrolledAt().toLocalDate(), null);
    }
}
