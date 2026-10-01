package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.commons.utils.FilterFields;
import com.example.school_management.feature.academic.entity.ClassEntity;
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
import com.example.school_management.feature.operational.dto.BulkEnrollmentResultDto;
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
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
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
    private final EnrollmentWriteExecutor enrollmentWriter;
    private final AutoEnrollmentClassWriter autoClassWriter;
    private final CurrentAcademicYearResolver currentAcademicYear;
    private final CurrentSchoolResolver currentSchool;
    private final SchoolMembershipRepository memberships;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public EnrollmentDto enrollStudent(Long studentId, Long classId) {
        return enrollmentWriter.enroll(studentId, classId);
    }

    /**
     * Ends the ACTIVE Enrollment as TRANSFERRED and starts a new ACTIVE Enrollment in the target Class.
     * The source Enrollment keeps its Class and its grades; the returned Enrollment is the new one.
     */
    @Override
    public EnrollmentDto transferStudent(Long enrollmentId, Long newClassId) {
        log.debug("Transferring enrollment {} to class {}", enrollmentId, newClassId);

        Enrollment source = requireSchoolEnrollment(enrollmentId);
        ClassEntity newClass = classRepo.findSchoolClassForUpdate(newClassId, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + newClassId));
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
        Enrollment created = persistTransferEnrollment(student, newClass);

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BulkEnrollmentResultDto bulkEnrollStudents(Long classId, List<Long> studentIds) {
        requireSchoolClass(classId); // Invalid target invalidates the command before any Student is processed.
        Set<Long> unique = new LinkedHashSet<>(studentIds);
        List<Long> enrolled = new ArrayList<>();
        List<BulkEnrollmentResultDto.Failure> failures = new ArrayList<>();
        for (Long studentId : unique) {
            try {
                enrollmentWriter.enroll(studentId, classId);
                enrolled.add(studentId);
            } catch (Exception e) {
                failures.add(enrollmentFailure(studentId, classId, e));
            }
        }
        return new BulkEnrollmentResultDto(studentIds.size(), unique.size(), enrolled.size(), failures.size(),
                studentIds.size() - unique.size(), List.copyOf(enrolled), List.copyOf(failures));
    }

    private BulkEnrollmentResultDto.Failure enrollmentFailure(Long studentId, Long classId, Exception e) {
        log.warn("Enrollment failed for studentId={} classId={}: {}", studentId, classId, e.getClass().getSimpleName());
        if (e instanceof ResourceNotFoundException) {
            return new BulkEnrollmentResultDto.Failure(studentId, "NOT_FOUND", e.getMessage());
        }
        if (e instanceof ConflictException) {
            return new BulkEnrollmentResultDto.Failure(studentId, "CONFLICT", e.getMessage());
        }
        return new BulkEnrollmentResultDto.Failure(studentId, "FAILED", "Enrollment failed");
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

    private void requireCapacity(ClassEntity classEntity) {
        if (getAvailableCapacity(classEntity) <= 0) {
            throw new ConflictException("Class has reached its capacity");
        }
    }

    /**
     * Inserts a new ACTIVE Enrollment and flushes, so a violation of the database's one-active-per-academic-year
     * rule surfaces here, before any audit or notification side effect.
     */
    private Enrollment persistTransferEnrollment(Student student, ClassEntity classEntity) {
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AutoEnrollmentResultDto autoEnrollAllStudents() {
        return performAutoEnrollment(null, false);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AutoEnrollmentResultDto autoEnrollByGradeLevel(String gradeLevel) {
        try {
            GradeLevel.valueOf(gradeLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid grade level: " + gradeLevel);
        }
        return performAutoEnrollment(GradeLevel.valueOf(gradeLevel.toUpperCase()), false);
    }

    // Pure planning has no writes; failed resolver reads must also leave the safe fatal result returnable.
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AutoEnrollmentResultDto previewAutoEnrollment() {
        return performAutoEnrollment(null, true);
    }

    private record StudentPlan(Long id, GradeLevel grade) {}
    private record ClassPlan(Long id, String name, GradeLevel grade, String section, int available) {}
    private record AutoPlan(Long yearId, int considered, int alreadyEnrolled,
                            List<StudentPlan> students, List<ClassPlan> classes, List<String> errors) {}

    private AutoEnrollmentResultDto performAutoEnrollment(GradeLevel targetGrade, boolean preview) {
        List<String> errors = new ArrayList<>();
        List<String> createdClasses = new ArrayList<>();
        Map<String, Integer> byGrade = new LinkedHashMap<>();
        Set<String> usedClasses = new HashSet<>();
        int considered = 0;
        int alreadyEnrolled = 0;
        int enrolled = 0;
        try {
            AutoPlan plan = planAutoEnrollment(targetGrade);
            considered = plan.considered();
            alreadyEnrolled = plan.alreadyEnrolled();
            errors.addAll(plan.errors());
            List<ClassPlan> available = new ArrayList<>(plan.classes());
            Map<String, Integer> remaining = new HashMap<>();
            available.forEach(c -> remaining.put(c.name(), c.available()));
            Map<String, Long> resolvedIds = new HashMap<>();
            available.stream().filter(c -> c.id() != null).forEach(c -> resolvedIds.put(c.name(), c.id()));

            for (StudentPlan student : plan.students()) {
                if (student.grade() == null) {
                    continue;
                }
                boolean done = false;
                while (!done) {
                    ClassPlan target = available.stream()
                            .filter(c -> c.grade() == student.grade() && remaining.get(c.name()) > 0)
                            .findFirst().orElse(null);
                    if (target == null) {
                        // Final locked capacity may have changed since planning. Generate the next normal section.
                        target = nextClass(student.grade(), available);
                        available.add(target);
                        remaining.put(target.name(), target.available());
                    }
                    if (preview) {
                        if (target.id() == null && !createdClasses.contains(target.name())) {
                            createdClasses.add(target.name());
                        }
                    } else {
                        if (!resolvedIds.containsKey(target.name())) {
                            AutoEnrollmentClassWriter.Result created = resolveGeneratedClass(plan.yearId(), target);
                            resolvedIds.put(target.name(), created.id());
                            if (created.created()) {
                                createdClasses.add(created.name());
                            }
                        }
                        Long classId = resolvedIds.get(target.name());
                        try {
                            enrollmentWriter.enroll(student.id(), classId);
                        } catch (EnrollmentWriteExecutor.CapacityConflict e) {
                            remaining.put(target.name(), 0);
                            continue;
                        } catch (Exception e) {
                            var failure = enrollmentFailure(student.id(), classId, e);
                            errors.add("Student " + student.id() + ": " + failure.message());
                            done = true;
                            continue;
                        }
                    }
                    enrolled++;
                    byGrade.merge(student.grade().name(), 1, Integer::sum);
                    usedClasses.add(target.name());
                    remaining.compute(target.name(), (name, slots) -> slots - 1);
                    done = true;
                }
            }
            String message = preview
                    ? "Preview: Would enroll %d students and create %d classes".formatted(enrolled, createdClasses.size())
                    : "Auto-enrollment completed: enrolled %d students into %d classes (%d new classes created)"
                            .formatted(enrolled, usedClasses.size(), createdClasses.size());
            return autoResult(true, message, considered, enrolled, alreadyEnrolled, usedClasses.size(),
                    byGrade, createdClasses, errors, preview);
        } catch (Exception e) {
            log.error("Auto-enrollment failed: {}", e.getClass().getSimpleName());
            errors.add("Auto-enrollment failed");
            return autoResult(false, "Auto-enrollment process failed", considered, enrolled, alreadyEnrolled,
                    usedClasses.size(), byGrade, createdClasses, errors, preview);
        }
    }

    private AutoEnrollmentResultDto autoResult(boolean success, String message, int considered, int enrolled,
            int alreadyEnrolled, int classesUsed, Map<String, Integer> byGrade, List<String> createdClasses,
            List<String> errors, boolean preview) {
        return new AutoEnrollmentResultDto(success, message, considered, enrolled, alreadyEnrolled,
                createdClasses.size(), classesUsed, LocalDateTime.now(), Map.copyOf(byGrade),
                List.copyOf(createdClasses), List.copyOf(errors), preview);
    }

    /** Side-effect-free population, roster and capacity planning. Never passes entities to a write transaction. */
    private AutoPlan planAutoEnrollment(GradeLevel targetGrade) {
        Long yearId = currentAcademicYear.resolve().getId();
        List<StudentPlan> eligible = studentRepo.findEligibleSchoolStudents(currentSchool.resolve().getId()).stream()
                .filter(s -> targetGrade == null || s.getGradeLevel() == targetGrade)
                .map(s -> new StudentPlan(s.getId(), s.getGradeLevel())).toList();
        Set<Long> activeIds = new HashSet<>(enrollmentRepo.findStudentIdsByAcademicYearIdAndStatus(
                yearId, EnrollmentStatus.ACTIVE));
        List<StudentPlan> needingEnrollment = eligible.stream().filter(s -> !activeIds.contains(s.id())).toList();
        List<ClassEntity> existing = classRepo.findByAcademicYearId(yearId);
        Map<Long, Long> rosterCounts = enrollmentRepo.countActiveRosters(existing.stream().map(ClassEntity::getId).toList())
                .stream().collect(Collectors.toMap(EnrollmentRepository.RosterCountRow::getClassId,
                        EnrollmentRepository.RosterCountRow::getStudentCount));
        List<ClassPlan> planned = new ArrayList<>();
        for (ClassEntity c : existing) {
            GradeLevel grade = null;
            if (c.getGradeLevel() != null) {
                try {
                    grade = GradeLevel.valueOf(c.getGradeLevel());
                } catch (IllegalArgumentException ignored) {
                    // Name-only or legacy Classes still reserve their names, but cannot route this population.
                }
            }
            int capacity = c.getCapacity() == null ? 30 : c.getCapacity();
            int free = (int) Math.max(0, capacity - rosterCounts.getOrDefault(c.getId(), 0L));
            planned.add(new ClassPlan(c.getId(), c.getName(), grade, c.getSection(), free));
        }
        planned.sort(Comparator.comparing(ClassPlan::section, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ClassPlan::id));
        for (GradeLevel grade : GradeLevel.values()) {
            long demand = needingEnrollment.stream().filter(s -> s.grade() == grade).count();
            long capacity = planned.stream().filter(c -> c.grade() == grade).mapToLong(ClassPlan::available).sum();
            while (capacity < demand) {
                ClassPlan created = nextClass(grade, planned);
                planned.add(created);
                capacity += created.available();
            }
        }
        long withoutGrade = needingEnrollment.stream().filter(s -> s.grade() == null).count();
        List<String> errors = withoutGrade == 0 ? List.of()
                : List.of(withoutGrade + " eligible students have no grade level assigned");
        return new AutoPlan(yearId, eligible.size(), eligible.size() - needingEnrollment.size(),
                needingEnrollment, List.copyOf(planned), errors);
    }

    private AutoEnrollmentClassWriter.Result resolveGeneratedClass(Long yearId, ClassPlan target) {
        try {
            return autoClassWriter.create(yearId, target.grade(), target.section(), target.name());
        } catch (DataIntegrityViolationException e) {
            String detail = e.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("uk_classes_academic_year_name")) {
                return autoClassWriter.findExisting(yearId, target.grade(), target.section(), target.name())
                        .orElseThrow(() -> e);
            }
            throw e;
        }
    }

    private ClassPlan nextClass(GradeLevel grade, List<ClassPlan> classes) {
        Set<String> names = classes.stream().map(c -> c.name().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> sections = classes.stream().filter(c -> c.grade() == grade).map(ClassPlan::section)
                .filter(section -> section != null).collect(Collectors.toSet());
        char section = 'A';
        while (sections.contains(String.valueOf(section))
                || names.contains(generateClassName(grade, String.valueOf(section)).toLowerCase(Locale.ROOT))) {
            section++;
        }
        String value = String.valueOf(section);
        return new ClassPlan(null, generateClassName(grade, value), grade, value, 30);
    }

    private String generateClassName(GradeLevel gradeLevel, String section) {
        return switch (gradeLevel) {
            case KINDERGARTEN -> "K-" + section;
            case ELEMENTARY -> "E-" + section;
            case MIDDLE -> "M-" + section;
            case HIGH -> "H-" + section;
            case UNIVERSITY -> "U-" + section;
        };
    }

    private int getAvailableCapacity(ClassEntity classEntity) {
        long currentEnrollments = enrollmentRepo.countActiveByClassId(classEntity.getId());
        return (int) Math.max(0, (classEntity.getCapacity() != null ? classEntity.getCapacity() : 30) - currentEnrollments);
    }
}
