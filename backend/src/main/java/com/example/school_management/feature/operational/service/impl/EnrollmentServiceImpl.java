package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.commons.utils.FilterFields;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
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
import com.example.school_management.feature.school.service.CurrentSchoolResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    /** Properties a paged enrollment read may be sorted by. */
    private static final FilterFields SORT_FIELDS = new FilterFields(Set.of(), Set.of("enrolledAt", "status"));

    /** Name of the database rule that allows one ACTIVE Enrollment per Student per AcademicYear (V59). */
    private static final String ONE_ACTIVE_PER_ACADEMIC_YEAR = "uk_enrollments_one_active_per_academic_year";

    private final EnrollmentRepository enrollmentRepo;
    private final StudentRepository studentRepo;
    private final ClassRepository classRepo;
    private final AuditService auditService;
    private final BaseUserRepository<BaseUser> userRepo;
    private final RealTimeNotificationService realTimeNotificationService;
    private final CurrentAcademicYearResolver currentAcademicYear;
    private final CurrentSchoolResolver currentSchool;
    private final SchoolMembershipRepository memberships;

    @Override
    public EnrollmentDto enrollStudent(Long studentId, Long classId) {
        log.debug("Enrolling student {} in class {}", studentId, classId);

        SchoolStudent schoolStudent = requireSchoolStudent(studentId);
        Student student = schoolStudent.student();
        ClassEntity classEntity = requireSchoolClass(classId);

        requireEligibleForNewEnrollment(schoolStudent);
        requireNoActiveEnrollmentInAcademicYear(studentId, classEntity);
        requireCapacity(classEntity);

        Enrollment savedEnrollment = persistActiveEnrollment(student, classEntity);
        log.info("Student {} enrolled in class {} with enrollment id {}", studentId, classId, savedEnrollment.getId());

        recordAudit(AuditEventType.ENROLLMENT_CREATED, savedEnrollment.getId(), "Student enrolled in class",
                String.format("Student %s %s enrolled in class %s",
                        student.getFirstName(), student.getLastName(), classEntity.getName()));

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
            log.warn("Failed to send real-time enrollment notification: {}", e.getClass().getSimpleName());
        }

        return toDto(savedEnrollment);
    }

    /**
     * Ends the ACTIVE Enrollment as TRANSFERRED and starts a new ACTIVE Enrollment in the target Class.
     * The source Enrollment keeps its Class and its grades; the returned Enrollment is the new one.
     */
    @Override
    public EnrollmentDto transferStudent(Long enrollmentId, Long newClassId) {
        log.debug("Transferring enrollment {} to class {}", enrollmentId, newClassId);

        Enrollment source = requireSchoolEnrollment(enrollmentId);
        ClassEntity newClass = requireSchoolClass(newClassId);
        SchoolStudent schoolStudent = requireSchoolStudent(source.getStudent().getId());
        ClassEntity oldClass = source.getClassEntity();

        requireEligibleForNewEnrollment(schoolStudent);
        if (source.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ConflictException("Only an active enrollment can be transferred");
        }
        if (oldClass.getId().equals(newClass.getId())) {
            throw new ConflictException("Student is already enrolled in this class");
        }
        if (!oldClass.getAcademicYear().getId().equals(newClass.getAcademicYear().getId())) {
            throw new ConflictException("A transfer must stay within the same academic year");
        }
        requireCapacity(newClass);

        // Flush the old status first: the database allows one ACTIVE enrollment per student and academic year,
        // so the old row must already be TRANSFERRED when the new ACTIVE row is inserted.
        Student student = schoolStudent.student();
        source.setStatus(EnrollmentStatus.TRANSFERRED);
        enrollmentRepo.saveAndFlush(source);
        Enrollment created = persistActiveEnrollment(student, newClass);

        recordAudit(AuditEventType.ENROLLMENT_UPDATED, source.getId(), "Student transferred between classes",
                String.format("Enrollment status changed from ACTIVE to TRANSFERRED: student %s %s moved from class %s to %s",
                        student.getFirstName(), student.getLastName(), oldClass.getName(), newClass.getName()));
        recordAudit(AuditEventType.ENROLLMENT_CREATED, created.getId(), "Enrollment created by transfer",
                String.format("New ACTIVE enrollment of student %s %s in class %s, created by transfer from class %s",
                        student.getFirstName(), student.getLastName(), newClass.getName(), oldClass.getName()));

        return toDto(created);
    }

    @Override
    public EnrollmentDto updateEnrollmentStatus(Long enrollmentId, EnrollmentStatus status) {
        log.debug("Updating enrollment {} status to {}", enrollmentId, status);

        Enrollment enrollment = requireSchoolEnrollment(enrollmentId);

        EnrollmentStatus oldStatus = enrollment.getStatus();
        if (status == oldStatus) {
            return toDto(enrollment);
        }
        if (status == EnrollmentStatus.TRANSFERRED) {
            throw new ConflictException("An enrollment becomes TRANSFERRED only through a transfer");
        }
        if (oldStatus.isTerminal()) {
            throw new ConflictException(
                    String.format("A %s enrollment cannot change status", oldStatus));
        }

        enrollment.setStatus(status);
        Enrollment updatedEnrollment = enrollmentRepo.saveAndFlush(enrollment);

        recordAudit(AuditEventType.ENROLLMENT_UPDATED, enrollmentId, "Enrollment status updated",
                String.format("Enrollment status changed from %s to %s for student %s %s in class %s",
                        oldStatus, status,
                        enrollment.getStudent().getFirstName(), enrollment.getStudent().getLastName(),
                        enrollment.getClassEntity().getName()));

        return toDto(updatedEnrollment);
    }

    @Override
    public void withdrawEnrollment(Long enrollmentId, String reason) {
        log.debug("Withdrawing enrollment {}", enrollmentId);

        Enrollment enrollment = requireSchoolEnrollment(enrollmentId);

        if (enrollment.getStatus() == EnrollmentStatus.WITHDRAWN) {
            return;
        }
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ConflictException(
                    String.format("A %s enrollment cannot be withdrawn", enrollment.getStatus()));
        }

        enrollment.setStatus(EnrollmentStatus.WITHDRAWN);
        enrollmentRepo.saveAndFlush(enrollment);

        recordAudit(AuditEventType.ENROLLMENT_DELETED, enrollmentId, "Student withdrawn from enrollment",
                String.format("Student %s %s withdrawn from class %s. Reason: %s",
                        enrollment.getStudent().getFirstName(), enrollment.getStudent().getLastName(),
                        enrollment.getClassEntity().getName(), reason));

        log.info("Enrollment {} withdrawn", enrollmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentDto getEnrollment(Long enrollmentId) {
        Enrollment enrollment = requireSchoolEnrollment(enrollmentId);
        return toDto(enrollment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getAllEnrollments(Pageable pageable, String search, EnrollmentStatus status) {
        SORT_FIELDS.requireSortable(pageable.getSort());
        log.debug("Getting all enrollments with search: {}, status: {}", search, status);

        Long schoolId = currentSchool.resolve().getId();
        Page<Enrollment> enrollments;
        
        if (search != null && !search.trim().isEmpty() && status != null) {
            // Both search and status filters
            enrollments = enrollmentRepo.findBySearchAndStatusAndSchoolId(search.trim(), status, schoolId, pageable);
        } else if (search != null && !search.trim().isEmpty()) {
            // Only search filter
            enrollments = enrollmentRepo.findBySearchAndSchoolId(search.trim(), schoolId, pageable);
        } else if (status != null) {
            // Only status filter
            enrollments = enrollmentRepo.findByStatusAndSchoolId(status, schoolId, pageable);
        } else {
            // No filters - get all
            enrollments = enrollmentRepo.findBySchoolId(schoolId, pageable);
        }
        
        return enrollments.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getStudentEnrollments(Long studentId, Pageable pageable) {
        SORT_FIELDS.requireSortable(pageable.getSort());
        requireSchoolStudent(studentId);
        return enrollmentRepo.findByStudentIdAndSchoolId(studentId, currentSchool.resolve().getId(), pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getClassEnrollments(Long classId, Pageable pageable) {
        SORT_FIELDS.requireSortable(pageable.getSort());
        requireSchoolClass(classId);
        return enrollmentRepo.findByClassId(classId, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getEnrollmentsByStatus(EnrollmentStatus status, Pageable pageable) {
        SORT_FIELDS.requireSortable(pageable.getSort());
        return enrollmentRepo.findByStatusAndSchoolId(status, currentSchool.resolve().getId(), pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EnrollmentDto> getEnrollmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        SORT_FIELDS.requireSortable(pageable.getSort());
        return enrollmentRepo.findByEnrolledAtBetweenAndSchoolId(startDate, endDate, currentSchool.resolve().getId(), pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentStatsDto getClassEnrollmentStats(Long classId) {
        requireSchoolClass(classId);
        List<Enrollment> enrollments = enrollmentRepo.findAllByClassId(classId);
        return calculateStats(enrollments);
    }

    @Override
    @Transactional(readOnly = true)
    public EnrollmentStatsDto getStudentEnrollmentStats(Long studentId) {
        requireSchoolStudent(studentId);
        List<Enrollment> enrollments = enrollmentRepo.findAllByStudentIdAndSchoolId(studentId, currentSchool.resolve().getId());
        return calculateStats(enrollments);
    }

    @Override
    public void bulkEnrollStudents(Long classId, List<Long> studentIds) {
        log.debug("Bulk enrolling {} students in class {}", studentIds.size(), classId);

        ClassEntity classEntity = requireSchoolClass(classId);

        log.debug("Bulk enrolling students in class: {}", classEntity.getName());

        int enrolled = 0;
        for (Long studentId : studentIds) {
            try {
                enrollStudent(studentId, classId);
                enrolled++;
            } catch (Exception e) {
                log.warn("Failed to enroll student id={} in class id={}: {}", studentId, classId, e.getClass().getSimpleName());
            }
        }

        log.info("Bulk enrollment completed: {}/{} students enrolled in class {}", enrolled, studentIds.size(), classId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEnrollStudent(Long studentId, Long classId) {
        SchoolStudent schoolStudent = requireSchoolStudent(studentId);
        ClassEntity classEntity = requireSchoolClass(classId);

        return schoolStudent.canReceiveActiveEnrollment()
                && getAvailableCapacity(classEntity) > 0
                && !enrollmentRepo.existsActiveInAcademicYear(studentId, classEntity.getAcademicYear().getId());
    }

    /** A Student of the current School: its membership holds the STUDENT role, in any membership status. */
    private record SchoolStudent(Student student, SchoolMembership membership) {
        boolean canReceiveActiveEnrollment() {
            return membership.getStatus() == MembershipStatus.ACTIVE && student.getStatus() == Status.ACTIVE;
        }
    }

    /**
     * Resolves a Student resource of the current School. Anyone else, including a Student of another School, is
     * reported as missing. Membership status does not matter here: history stays readable after a student leaves.
     */
    private SchoolStudent requireSchoolStudent(Long studentId) {
        Long schoolId = currentSchool.resolve().getId();
        SchoolMembership membership = memberships.findByUserIdAndSchoolId(studentId, schoolId)
                .filter(m -> m.getRoles().contains(MembershipRole.STUDENT))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return new SchoolStudent(student, membership);
    }

    /** A new ACTIVE Enrollment needs an ACTIVE STUDENT membership and an ACTIVE account. */
    private void requireEligibleForNewEnrollment(SchoolStudent schoolStudent) {
        if (!schoolStudent.canReceiveActiveEnrollment()) {
            throw new ConflictException("Student cannot receive a new enrollment: membership or account is not active");
        }
    }

    private ClassEntity requireSchoolClass(Long classId) {
        return classRepo.findByIdAndAcademicYearSchoolId(classId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
    }

    private Enrollment requireSchoolEnrollment(Long enrollmentId) {
        return enrollmentRepo.findByIdAndSchoolId(enrollmentId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
    }

    private void requireNoActiveEnrollmentInAcademicYear(Long studentId, ClassEntity classEntity) {
        if (enrollmentRepo.existsActiveInAcademicYear(studentId, classEntity.getAcademicYear().getId())) {
            throw new ConflictException("Student already has an active enrollment in this academic year");
        }
    }

    private void requireCapacity(ClassEntity classEntity) {
        if (getAvailableCapacity(classEntity) <= 0) {
            throw new ConflictException("Class has reached its capacity");
        }
    }

    /**
     * Inserts a new ACTIVE Enrollment and flushes, so a violation of the database's one-active-per-academic-year
     * rule surfaces here, before any audit or notification side effect.
     */
    private Enrollment persistActiveEnrollment(Student student, ClassEntity classEntity) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassEntity(classEntity);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setFinalGrad(null);
        try {
            return enrollmentRepo.saveAndFlush(enrollment);
        } catch (DataIntegrityViolationException e) {
            String detail = e.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains(ONE_ACTIVE_PER_ACADEMIC_YEAR)) {
                throw new ConflictException("Student already has an active enrollment in this academic year");
            }
            throw e;
        }
    }

    private void recordAudit(AuditEventType type, Long enrollmentId, String summary, String details) {
        try {
            auditService.createAuditEvent(type, "Enrollment", enrollmentId, summary, details, getCurrentUser());
        } catch (Exception e) {
            log.warn("Failed to create audit event {}: {}", type, e.getClass().getSimpleName());
        }
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
        long active = countWithStatus(enrollments, EnrollmentStatus.ACTIVE);
        long completed = countWithStatus(enrollments, EnrollmentStatus.COMPLETED);
        long transferred = countWithStatus(enrollments, EnrollmentStatus.TRANSFERRED);
        long withdrawn = countWithStatus(enrollments, EnrollmentStatus.WITHDRAWN);

        double completionRate = total > 0 ? (double) completed / total * 100 : 0.0;
        double averageFinalGrade = enrollments.stream()
                .filter(e -> e.getFinalGrad() != null)
                .mapToDouble(Enrollment::getFinalGrad)
                .average().orElse(0.0);

        return new EnrollmentStatsDto(total, active, completed, transferred, withdrawn, completionRate, averageFinalGrade);
    }

    private static long countWithStatus(List<Enrollment> enrollments, EnrollmentStatus status) {
        return enrollments.stream().filter(e -> e.getStatus() == status).count();
    }

    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));
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
        AcademicYear academicYear = currentAcademicYear.resolve();
        List<String> errors = new ArrayList<>();
        List<String> createdClasses = new ArrayList<>();
        Map<String, Integer> enrollmentsByGradeLevel = new HashMap<>();
        
        int totalStudentsProcessed = 0;
        int studentsEnrolled = 0;
        int studentsAlreadyEnrolled = 0;
        int classesCreated = 0;
        int classesUsed = 0;
        
        try {
            // Students of the current School who can still be enrolled in this academic year
            List<Student> unenrolledStudents = getUnenrolledStudents(targetGradeLevel, academicYear);
            totalStudentsProcessed = unenrolledStudents.size();
            
            log.info("Found {} unenrolled students", totalStudentsProcessed);
            
            // Group students by grade level
            Map<GradeLevel, List<Student>> studentsByGrade = unenrolledStudents.stream()
                .filter(student -> student.getGradeLevel() != null)
                .collect(Collectors.groupingBy(Student::getGradeLevel));
            
            List<ClassEntity> currentYearClasses = new ArrayList<>(classRepo.findByAcademicYearId(academicYear.getId()));

            // Process each grade level
            for (Map.Entry<GradeLevel, List<Student>> entry : studentsByGrade.entrySet()) {
                GradeLevel gradeLevel = entry.getKey();
                List<Student> studentsInGrade = entry.getValue();
                
                log.info("Processing {} students for grade level: {}", studentsInGrade.size(), gradeLevel);
                
                // Find or create classes for this grade level
                List<ClassEntity> availableClasses = findOrCreateClassesForGradeLevel(gradeLevel, studentsInGrade.size(), academicYear, currentYearClasses);
                
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
            log.error("Error during auto-enrollment process: {}", e.getClass().getSimpleName());
            // The exception's message may carry SQL or internal state, so the caller only gets a generic error.
            errors.add("Auto-enrollment failed");
            
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

    /** Students of the current School who can receive a new ACTIVE Enrollment in the AcademicYear. */
    private List<Student> getUnenrolledStudents(String targetGradeLevel, AcademicYear academicYear) {
        List<Student> candidates = studentRepo.findEnrollableStudents(currentSchool.resolve().getId(), academicYear.getId());
        if (targetGradeLevel == null) {
            return candidates;
        }
        GradeLevel gradeLevel = GradeLevel.valueOf(targetGradeLevel.toUpperCase());
        return candidates.stream()
            .filter(student -> student.getGradeLevel() == gradeLevel)
            .collect(Collectors.toList());
    }

    private List<ClassEntity> findOrCreateClassesForGradeLevel(
            GradeLevel gradeLevel, int studentCount, AcademicYear academicYear, List<ClassEntity> currentYearClasses) {
        List<ClassEntity> availableClasses = new ArrayList<>();
        
        // Don't create classes if no students to enroll
        if (studentCount == 0) {
            log.info("No students to enroll for grade level: {}", gradeLevel);
            return availableClasses;
        }
        
        // Find existing classes for this grade level with available capacity
        List<ClassEntity> existingClasses = currentYearClasses.stream()
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
            
            for (int i = 0; i < newClassesNeeded; i++) {
                String nextSection = getNextAvailableSection(gradeLevel, currentYearClasses);
                ClassEntity newClass = createNewClass(gradeLevel, nextSection, academicYear);
                availableClasses.add(newClass);
                currentYearClasses.add(newClass);
            }
        }
        
        return availableClasses;
    }

    private int getAvailableCapacity(ClassEntity classEntity) {
        long currentEnrollments = enrollmentRepo.countActiveByClassId(classEntity.getId());
        return (int) Math.max(0, (classEntity.getCapacity() != null ? classEntity.getCapacity() : 30) - currentEnrollments);
    }

    private String getNextAvailableSection(GradeLevel gradeLevel, List<ClassEntity> currentYearClasses) {
        // Find all existing sections for this grade level
        List<String> existingSections = currentYearClasses.stream()
            .filter(cls -> gradeLevel.name().equals(cls.getGradeLevel()))
            .map(ClassEntity::getSection)
            .filter(section -> section != null && !section.isEmpty())
            .sorted()
            .collect(Collectors.toList());

        // Name-only classes also reserve names, even without grade/section metadata.
        Set<String> existingNames = currentYearClasses.stream()
            .map(ClassEntity::getName)
            .filter(name -> name != null)
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        
        // Start from 'A' and find the first available section
        char sectionChar = 'A';
        while (existingSections.contains(String.valueOf(sectionChar))
                || existingNames.contains(generateClassName(gradeLevel, String.valueOf(sectionChar)).toLowerCase(Locale.ROOT))) {
            sectionChar++;
        }
        
        return String.valueOf(sectionChar);
    }

    private ClassEntity createNewClass(GradeLevel gradeLevel, String section, AcademicYear academicYear) {
        ClassEntity newClass = new ClassEntity();
        
        // Generate class name based on grade level and section
        String className = generateClassName(gradeLevel, section);
        
        newClass.setName(className);
        newClass.setGradeLevel(gradeLevel.name());
        newClass.setSection(section);
        newClass.setAcademicYear(academicYear);
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
                
                // Check if student is currently enrolled in this specific class
                if (!isPreview && isStudentEnrolledInClass(student.getId(), classEntity.getId())) {
                    log.debug("Student id={} is already enrolled in class id={}, skipping",
                        student.getId(), classEntity.getId());
                    studentIndex++;
                    continue;
                }
                
                if (!isPreview) {
                    try {
                        // Create enrollment
                        Enrollment enrollment = new Enrollment();
                        enrollment.setStudent(student);
                        enrollment.setClassEntity(classEntity);
                        enrollment.setStatus(EnrollmentStatus.ACTIVE);
                        enrollment.setEnrolledAt(LocalDateTime.now());
                        
                        enrollmentRepo.save(enrollment);
                        log.debug("Enrolled student id={} in class id={}", student.getId(), classEntity.getId());
                    } catch (Exception e) {
                        log.warn("Failed to enroll student id={} in class id={}: {}",
                            student.getId(), classEntity.getId(), e.getClass().getSimpleName());
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
        return enrollmentRepo.existsActiveInClass(studentId, classId);
    }
}
