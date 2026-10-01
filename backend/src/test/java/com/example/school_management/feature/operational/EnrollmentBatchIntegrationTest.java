package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.entity.enums.GradeLevel;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.operational.repository.AuditEventRepository;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.EnrollmentService;
import com.example.school_management.feature.operational.service.impl.RealTimeNotificationService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.example.school_management.feature.operational.service.impl.AutoEnrollmentClassWriter;
import com.example.school_management.feature.operational.service.impl.EnrollmentWriteExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Committed fixtures are essential: each command must read and commit through its own transaction. */
@IntegrationTest
class EnrollmentBatchIntegrationTest {
    @Autowired EnrollmentService service;
    @Autowired EnrollmentRepository enrollments;
    @Autowired StudentRepository students;
    @Autowired SchoolMembershipRepository memberships;
    @Autowired SchoolRepository schools;
    @Autowired AcademicYearRepository years;
    @Autowired ClassRepository classes;
    @Autowired AuditEventRepository audits;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoSpyBean AutoEnrollmentClassWriter classWriter;
    @MockitoSpyBean EnrollmentWriteExecutor enrollmentWriter;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    @MockitoSpyBean RealTimeNotificationService notifications;

    private School foreignSchool;
    private School school;
    private AcademicYear year;
    private final List<Long> studentIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(DevFixtureLoader.ADMIN_EMAIL, null, List.of()));
        school = new School();
        school.setName("Batch " + UUID.randomUUID());
        school = schools.saveAndFlush(school);
        doReturn(school).when(currentSchool).resolve();
        doNothing().when(notifications).broadcastAdminFeed(any(), anyString(), anyString(), anyString(), anyString(), anyLong());
        doNothing().when(notifications).notifyEnrollmentChange(anyString(), anyString(), anyString(), anyLong(), isNull());
        year = new AcademicYear();
        year.setSchool(school);
        year.setName("Batch year");
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 6, 30));
        year.setActive(true);
        year = years.saveAndFlush(year);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        jdbc.execute("DROP TRIGGER IF EXISTS trg_test_batch_failure ON enrollments");
        jdbc.execute("DROP FUNCTION IF EXISTS test_batch_failure()");
        enrollments.deleteAll(enrollments.findAll().stream()
                .filter(e -> studentIds.contains(e.getStudent().getId())).toList());
        classes.deleteAll(classes.findByAcademicYearId(year.getId()));
        years.deleteById(year.getId());
        studentIds.forEach(id -> {
            memberships.deleteAll(memberships.findAllByUserId(id));
            students.deleteById(id);
        });
        schools.deleteById(school.getId());
        if (foreignSchool != null) schools.deleteById(foreignSchool.getId());
    }

    @Test
    void bulkReturnsCommittedSuccessesAndSafeDomainFailuresInRequestOrder() {
        ClassEntity target = clazz(GradeLevel.MIDDLE, "Bulk", 30);
        Student first = student(GradeLevel.MIDDLE);
        Student suspended = student(GradeLevel.MIDDLE);
        Student foreign = student(GradeLevel.MIDDLE);
        Student last = student(GradeLevel.MIDDLE);
        var suspendedMembership = memberships.findByUserIdAndSchoolId(suspended.getId(), school.getId()).orElseThrow();
        suspendedMembership.setStatus(MembershipStatus.SUSPENDED);
        memberships.saveAndFlush(suspendedMembership);
        foreignSchool = new School();
        foreignSchool.setName("Foreign batch");
        foreignSchool = schools.saveAndFlush(foreignSchool);
        var foreignMembership = memberships.findByUserIdAndSchoolId(foreign.getId(), school.getId()).orElseThrow();
        foreignMembership.setSchool(foreignSchool);
        memberships.saveAndFlush(foreignMembership);
        long auditCount = audits.count();

        var result = service.bulkEnrollStudents(target.getId(), List.of(first.getId(), suspended.getId(), foreign.getId(), last.getId()));

        assertThat(result.requestedStudents()).isEqualTo(4);
        assertThat(result.uniqueStudentsProcessed()).isEqualTo(4);
        assertThat(result.duplicatesIgnored()).isZero();
        assertThat(result.studentsEnrolled()).isEqualTo(2);
        assertThat(result.studentsFailed()).isEqualTo(2);
        assertThat(result.enrolledStudentIds()).containsExactly(first.getId(), last.getId());
        assertThat(result.failures()).extracting(f -> f.studentId()).containsExactly(suspended.getId(), foreign.getId());
        assertThat(result.failures()).extracting(f -> f.code()).containsExactly("CONFLICT", "NOT_FOUND");
        assertCommittedWithSideEffects(first);
        assertCommittedWithSideEffects(last);
        assertThat(audits.count()).isEqualTo(auditCount + 2);
        for (Student rejected : List.of(suspended, foreign)) {
            assertThat(enrollments.findByStudentId(rejected.getId())).isEmpty();
            verify(notifications, never()).notifyEnrollmentChange(anyString(), anyString(), eq("ENROLLED"), eq(rejected.getId()), isNull());
        }
    }

    @Test
    void bulkDeduplicatesBeforeCapacityAndCountsOnlyCommittedRows() {
        ClassEntity target = clazz(GradeLevel.MIDDLE, "Small bulk", 2);
        Student first = student(GradeLevel.MIDDLE);
        Student second = student(GradeLevel.MIDDLE);
        Student third = student(GradeLevel.MIDDLE);

        var result = service.bulkEnrollStudents(target.getId(), List.of(first.getId(), first.getId(), second.getId(), third.getId(), third.getId()));

        assertThat(result.requestedStudents()).isEqualTo(5);
        assertThat(result.uniqueStudentsProcessed()).isEqualTo(3);
        assertThat(result.duplicatesIgnored()).isEqualTo(2);
        assertThat(result.studentsEnrolled()).isEqualTo(2);
        assertThat(result.studentsFailed()).isEqualTo(1);
        assertThat(result.enrolledStudentIds()).containsExactly(first.getId(), second.getId());
        assertThat(result.failures()).extracting(f -> f.code()).containsExactly("CONFLICT");
        assertThat(result.failures().get(0).studentId()).isEqualTo(third.getId());
        assertThat(enrollments.countActiveByClassId(target.getId())).isEqualTo(2);
        assertCommittedWithSideEffects(first);
        assertCommittedWithSideEffects(second);
        assertThat(enrollments.findByStudentId(third.getId())).isEmpty();
    }

    @Test
    void bulkReportsDatabaseRollbackWithoutPoisoningLaterItemsOrSideEffects() {
        ClassEntity target = clazz(GradeLevel.MIDDLE, "DB bulk", 30);
        Student first = student(GradeLevel.MIDDLE);
        Student rejected = student(GradeLevel.MIDDLE);
        Student last = student(GradeLevel.MIDDLE);
        rejectInsertFor(rejected.getId());
        long auditCount = audits.count();

        var result = service.bulkEnrollStudents(target.getId(), List.of(first.getId(), rejected.getId(), last.getId()));

        assertThat(result.studentsEnrolled()).isEqualTo(2);
        assertThat(result.studentsFailed()).isEqualTo(1);
        assertThat(result.enrolledStudentIds()).containsExactly(first.getId(), last.getId());
        assertThat(result.failures().get(0).studentId()).isEqualTo(rejected.getId());
        assertThat(result.failures().get(0).code()).isEqualTo("FAILED");
        assertThat(result.failures().get(0).message()).isEqualTo("Enrollment failed");
        assertCommittedWithSideEffects(first);
        assertCommittedWithSideEffects(last);
        assertThat(audits.count()).isEqualTo(auditCount + 2);
        assertThat(enrollments.findByStudentId(rejected.getId())).isEmpty();
        verify(notifications, never()).notifyEnrollmentChange(anyString(), anyString(), eq("ENROLLED"), eq(rejected.getId()), isNull());
    }

    @Test
    void previewIsPureAndMatchesCommittedExecutionAndTheSecondRun() {
        ClassEntity existing = clazz(GradeLevel.MIDDLE, "Existing", 1);
        Student already = student(GradeLevel.MIDDLE);
        service.enrollStudent(already.getId(), existing.getId());
        student(GradeLevel.MIDDLE);
        student(GradeLevel.MIDDLE);
        student(GradeLevel.HIGH);
        student(null);
        long classCount = classes.count();
        long enrollmentCount = enrollments.count();
        long auditCount = audits.count();
        clearInvocations(notifications);

        var preview = service.previewAutoEnrollment();

        assertThat(preview.success()).isTrue();
        assertThat(preview.totalStudentsProcessed()).isEqualTo(5);
        assertThat(preview.studentsAlreadyEnrolled()).isEqualTo(1);
        assertThat(preview.studentsEnrolled()).isEqualTo(3);
        assertThat(preview.classesCreated()).isEqualTo(2);
        assertThat(preview.classesUsed()).isEqualTo(2);
        assertThat(preview.createdClasses()).containsExactlyInAnyOrder("M-A", "H-A");
        assertThat(preview.enrollmentsByGradeLevel()).containsEntry("MIDDLE", 2).containsEntry("HIGH", 1);
        assertThat(preview.errors()).containsExactly("1 eligible students have no grade level assigned");
        assertThat(classes.count()).isEqualTo(classCount);
        assertThat(enrollments.count()).isEqualTo(enrollmentCount);
        assertThat(audits.count()).isEqualTo(auditCount);
        verify(notifications, never()).notifyEnrollmentChange(anyString(), anyString(), anyString(), anyLong(), any());

        var execution = service.autoEnrollAllStudents();

        assertThat(execution.success()).isTrue();
        assertThat(execution.studentsEnrolled()).isEqualTo(preview.studentsEnrolled());
        assertThat(execution.classesCreated()).isEqualTo(preview.classesCreated());
        assertThat(execution.classesUsed()).isEqualTo(preview.classesUsed());
        assertThat(execution.createdClasses()).containsExactlyInAnyOrderElementsOf(preview.createdClasses());
        assertThat(execution.enrollmentsByGradeLevel()).isEqualTo(preview.enrollmentsByGradeLevel());
        assertThat(enrollments.count()).isEqualTo(enrollmentCount + 3);
        assertThat(classes.count()).isEqualTo(classCount + 2);

        var repeated = service.autoEnrollAllStudents();
        assertThat(repeated.totalStudentsProcessed()).isEqualTo(5);
        assertThat(repeated.studentsAlreadyEnrolled()).isEqualTo(4);
        assertThat(repeated.studentsEnrolled()).isZero();
        assertThat(repeated.classesCreated()).isZero();
        assertThat(repeated.classesUsed()).isZero();
        assertThat(repeated.createdClasses()).isEmpty();
        assertThat(enrollments.count()).isEqualTo(enrollmentCount + 3);
        assertThat(classes.count()).isEqualTo(classCount + 2);
    }

    @Test
    void autoDatabaseFailureDoesNotRollBackOtherStudentsOrCreatedClasses() {
        Student first = student(GradeLevel.MIDDLE);
        Student rejected = student(GradeLevel.MIDDLE);
        Student last = student(GradeLevel.MIDDLE);
        rejectInsertFor(rejected.getId());

        var result = service.autoEnrollAllStudents();

        assertThat(result.success()).isTrue();
        assertThat(result.studentsEnrolled()).isEqualTo(2);
        assertThat(result.classesCreated()).isEqualTo(1);
        assertThat(result.createdClasses()).containsExactly("M-A");
        assertThat(result.classesUsed()).isEqualTo(1);
        assertThat(result.enrollmentsByGradeLevel()).containsEntry("MIDDLE", 2);
        assertThat(result.errors()).hasSize(1).allSatisfy(error ->
                assertThat(error).contains("Enrollment failed").doesNotContain("private", "SQL", "23514"));
        assertCommittedWithSideEffects(first);
        assertCommittedWithSideEffects(last);
        assertThat(enrollments.findByStudentId(rejected.getId())).isEmpty();
        verify(notifications, never()).notifyEnrollmentChange(anyString(), anyString(), eq("ENROLLED"), eq(rejected.getId()), isNull());
    }

    @Test
    void gradeSpecificRunCountsOnlyThatGradesPopulation() {
        ClassEntity middle = clazz(GradeLevel.MIDDLE, "Middle", 2);
        Student placed = student(GradeLevel.MIDDLE);
        service.enrollStudent(placed.getId(), middle.getId());
        Student candidate = student(GradeLevel.MIDDLE);
        Student other = student(GradeLevel.HIGH);
        student(null);

        var result = service.autoEnrollByGradeLevel("middle");

        assertThat(result.totalStudentsProcessed()).isEqualTo(2);
        assertThat(result.studentsAlreadyEnrolled()).isEqualTo(1);
        assertThat(result.studentsEnrolled()).isEqualTo(1);
        assertThat(result.classesCreated()).isZero();
        assertThat(result.createdClasses()).isEmpty();
        assertThat(result.classesUsed()).isEqualTo(1);
        assertThat(result.enrollmentsByGradeLevel()).containsExactlyEntriesOf(java.util.Map.of("MIDDLE", 1));
        assertThat(enrollments.findByStudentId(candidate.getId())).hasSize(1);
        assertThat(enrollments.findByStudentId(other.getId())).isEmpty();
    }

    @Test
    void writeRevalidatesMembershipAfterPlanning() {
        Student candidate = student(GradeLevel.MIDDLE);
        ClassEntity target = clazz(GradeLevel.MIDDLE, "Target", 2);
        assertThat(service.previewAutoEnrollment().studentsEnrolled()).isEqualTo(1);
        var membership = memberships.findByUserIdAndSchoolId(candidate.getId(), school.getId()).orElseThrow();
        membership.setStatus(MembershipStatus.SUSPENDED);
        memberships.saveAndFlush(membership);

        assertThatThrownBy(() -> service.enrollStudent(candidate.getId(), target.getId()))
                .isInstanceOf(ConflictException.class);
        assertThat(enrollments.findByStudentId(candidate.getId())).isEmpty();
        verify(notifications, never()).notifyEnrollmentChange(anyString(), anyString(), eq("ENROLLED"), eq(candidate.getId()), isNull());
    }

    @ParameterizedTest
    @ValueSource(strings = {"membership", "account"})
    void autoHttpRevalidatesEligibilityChangedAfterPlanning(String change) throws Exception {
        Student candidate = student(GradeLevel.MIDDLE);
        doAnswer(invocation -> {
            Object created = invocation.callRealMethod();
            if (change.equals("membership")) {
                jdbc.update("UPDATE school_memberships SET status = 'SUSPENDED' WHERE user_id = ? AND school_id = ?",
                        candidate.getId(), school.getId());
            } else {
                jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE id = ?", candidate.getId());
            }
            return created;
        }).when(classWriter).create(eq(year.getId()), eq(GradeLevel.MIDDLE), anyString(), anyString());

        mvc.perform(post("/api/v1/enrollments/auto-enroll").with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.totalStudentsProcessed").value(1))
                .andExpect(jsonPath("$.data.studentsEnrolled").value(0))
                .andExpect(jsonPath("$.data.errors.length()").value(1));
        assertThat(enrollments.findByStudentId(candidate.getId())).isEmpty();
        verify(notifications, never()).notifyEnrollmentChange(anyString(), anyString(), eq("ENROLLED"), eq(candidate.getId()), isNull());
    }

    @Test
    void autoRetriesAnotherGeneratedClassWhenPlannedCapacityIsConsumed() {
        Student candidate = student(GradeLevel.MIDDLE);
        Student occupant = student(null);
        ClassEntity target = clazz(GradeLevel.MIDDLE, "Last seat", 1);
        doAnswer(invocation -> {
            service.enrollStudent(occupant.getId(), target.getId());
            return invocation.callRealMethod();
        }).when(enrollmentWriter).enroll(candidate.getId(), target.getId());

        var result = service.autoEnrollByGradeLevel("MIDDLE");

        assertThat(result.success()).isTrue();
        assertThat(result.studentsEnrolled()).isEqualTo(1);
        assertThat(result.classesCreated()).isEqualTo(1);
        assertThat(result.classesUsed()).isEqualTo(1);
        assertThat(result.createdClasses()).containsExactly("M-A");
        assertThat(result.errors()).isEmpty();
        assertThat(enrollments.countActiveByClassId(target.getId())).isEqualTo(1);
        assertThat(enrollments.findByStudentId(candidate.getId())).hasSize(1)
                .allSatisfy(e -> assertThat(e.getClassEntity().getId()).isNotEqualTo(target.getId()));
    }

    @Test
    void concurrentAutoRunsReuseTheWinningGeneratedClassWithoutRawConstraintErrors() throws Exception {
        student(GradeLevel.MIDDLE);
        student(GradeLevel.MIDDLE);
        var createBarrier = new java.util.concurrent.CyclicBarrier(2);
        doAnswer(invocation -> {
            createBarrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(classWriter).create(eq(year.getId()), eq(GradeLevel.MIDDLE), eq("A"), eq("M-A"));
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<com.example.school_management.feature.operational.dto.AutoEnrollmentResultDto> run = () -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(DevFixtureLoader.ADMIN_EMAIL, null, List.of()));
                try {
                    return service.autoEnrollAllStudents();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
            var first = pool.submit(run);
            var second = pool.submit(run);
            var results = List.of(first.get(20, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(results).allSatisfy(result -> {
                assertThat(result.success()).isTrue();
                assertThat(result.errors()).allSatisfy(error ->
                        assertThat(error).doesNotContain("constraint", "SQL", "uk_classes"));
            });
            assertThat(results.stream().mapToInt(r -> r.classesCreated()).sum()).isEqualTo(1);
            assertThat(results.stream().mapToInt(r -> r.studentsEnrolled()).sum()).isEqualTo(2);
            var generated = classes.findByAcademicYearId(year.getId());
            assertThat(generated).hasSize(1);
            assertThat(enrollments.countActiveByClassId(generated.get(0).getId())).isEqualTo(2);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void anAuditDatabaseFailureRollsBackTheEnrollmentAndSuppressesEnrolledNotification() {
        Student candidate = student(GradeLevel.MIDDLE);
        ClassEntity target = clazz(GradeLevel.MIDDLE, "Audit failure", 1);
        long auditCount = audits.count();
        jdbc.execute("""
                CREATE FUNCTION test_batch_audit_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.entity_type = 'Enrollment' AND EXISTS
                       (SELECT 1 FROM enrollments WHERE id = NEW.entity_id AND student_id = %d) THEN
                        RAISE EXCEPTION 'private audit database failure' USING ERRCODE = '23514';
                    END IF;
                    RETURN NEW;
                END $$
                """.formatted(candidate.getId()));
        jdbc.execute("CREATE TRIGGER trg_test_batch_audit_failure BEFORE INSERT ON audit_events "
                + "FOR EACH ROW EXECUTE FUNCTION test_batch_audit_failure()");
        try {
            var result = service.bulkEnrollStudents(target.getId(), List.of(candidate.getId()));
            assertThat(result.studentsEnrolled()).isZero();
            assertThat(result.studentsFailed()).isEqualTo(1);
            assertThat(result.failures().get(0).message()).isEqualTo("Enrollment failed");
            assertThat(enrollments.findByStudentId(candidate.getId())).isEmpty();
            assertThat(audits.count()).isEqualTo(auditCount);
            verify(notifications, never()).notifyEnrollmentChange(anyString(), anyString(), eq("ENROLLED"), eq(candidate.getId()), isNull());
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS trg_test_batch_audit_failure ON audit_events");
            jdbc.execute("DROP FUNCTION IF EXISTS test_batch_audit_failure()");
        }
    }

    @Test
    void aDeferredDatabaseFailureAtCommitSuppressesAllEnrollmentNotifications() {
        Student candidate = student(GradeLevel.MIDDLE);
        ClassEntity target = clazz(GradeLevel.MIDDLE, "Commit failure", 1);
        long auditCount = audits.count();
        jdbc.execute("""
                CREATE FUNCTION test_batch_commit_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.student_id = %d THEN
                        RAISE EXCEPTION 'private deferred failure' USING ERRCODE = '23514';
                    END IF;
                    RETURN NEW;
                END $$
                """.formatted(candidate.getId()));
        jdbc.execute("CREATE CONSTRAINT TRIGGER trg_test_batch_commit_failure AFTER INSERT ON enrollments "
                + "DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION test_batch_commit_failure()");
        try {
            var result = service.bulkEnrollStudents(target.getId(), List.of(candidate.getId()));
            assertThat(result.studentsEnrolled()).isZero();
            assertThat(result.studentsFailed()).isEqualTo(1);
            assertThat(result.failures().get(0).message()).isEqualTo("Enrollment failed");
            assertThat(enrollments.findByStudentId(candidate.getId())).isEmpty();
            assertThat(audits.count()).isEqualTo(auditCount);
            verify(notifications, never()).notifyEnrollmentChange(anyString(), anyString(), eq("ENROLLED"), eq(candidate.getId()), isNull());
            verify(notifications, never()).broadcastAdminFeed(eq(AuditEventType.ENROLLMENT_CREATED),
                    anyString(), anyString(), anyString(), eq("Enrollment"), anyLong());
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS trg_test_batch_commit_failure ON enrollments");
            jdbc.execute("DROP FUNCTION IF EXISTS test_batch_commit_failure()");
        }
    }

    private void assertCommittedWithSideEffects(Student student) {
        var created = enrollments.findByStudentId(student.getId());
        assertThat(created).hasSize(1);
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("Enrollment", created.get(0).getId())).hasSize(1);
        verify(notifications, times(1)).notifyEnrollmentChange(anyString(), anyString(), eq("ENROLLED"), eq(student.getId()), isNull());
        verify(notifications, times(1)).broadcastAdminFeed(eq(AuditEventType.ENROLLMENT_CREATED),
                anyString(), anyString(), anyString(), eq("Enrollment"), eq(created.get(0).getId()));
    }

    private void rejectInsertFor(Long id) {
        jdbc.execute("""
                CREATE FUNCTION test_batch_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.student_id = %d THEN
                        RAISE EXCEPTION 'private SQL database failure' USING ERRCODE = '23514';
                    END IF;
                    RETURN NEW;
                END $$
                """.formatted(id));
        jdbc.execute("CREATE TRIGGER trg_test_batch_failure BEFORE INSERT ON enrollments "
                + "FOR EACH ROW EXECUTE FUNCTION test_batch_failure()");
    }

    private ClassEntity clazz(GradeLevel grade, String name, int capacity) {
        ClassEntity created = new ClassEntity();
        created.setAcademicYear(year);
        created.setName(name);
        created.setGradeLevel(grade.name());
        created.setCapacity(capacity);
        return classes.saveAndFlush(created);
    }

    private Student student(GradeLevel grade) {
        Student created = new Student();
        created.setRole(UserRole.STUDENT);
        created.setEmail("batch-" + UUID.randomUUID() + "@fixtures.school.test");
        created.setFirstName("Batch");
        created.setLastName("Student");
        created.setPassword("not-a-real-hash");
        created.setStatus(Status.ACTIVE);
        created.setIsEmailVerified(true);
        created.setGradeLevel(grade);
        created = students.saveAndFlush(created);
        studentIds.add(created.getId());
        SchoolMembership member = new SchoolMembership();
        member.setUser(created);
        member.setSchool(school);
        member.setStatus(MembershipStatus.ACTIVE);
        member.setRoles(new HashSet<>(Set.of(MembershipRole.STUDENT)));
        memberships.saveAndFlush(member);
        return created;
    }
}
