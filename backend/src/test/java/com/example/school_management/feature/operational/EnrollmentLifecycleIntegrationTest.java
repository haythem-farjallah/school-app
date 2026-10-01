package com.example.school_management.feature.operational;

import com.example.school_management.AcademicYearTestFixtures;
import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.dto.EnrollmentDto;
import com.example.school_management.feature.operational.entity.AuditEvent;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.AuditEventRepository;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.example.school_management.feature.operational.service.EnrollmentService;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Enrollment is immutable history of Student and Class: a transfer ends the old Enrollment and starts a new one,
 * terminal Enrollments never change, and a Student has at most one ACTIVE Enrollment per AcademicYear.
 */
@IntegrationTest
class EnrollmentLifecycleIntegrationTest {

    private static final AtomicInteger clientAddress = new AtomicInteger();

    @Autowired EnrollmentService enrollmentService;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired ClassRepository classRepository;
    @Autowired AcademicYearRepository academicYears;
    @Autowired GradeRepository gradeRepository;
    @Autowired AuditEventRepository auditEvents;
    @Autowired CurrentSchoolResolver currentSchool;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private final List<Long> students = new ArrayList<>();
    private final List<Long> classes = new ArrayList<>();
    private final List<Long> years = new ArrayList<>();
    private final List<Long> grades = new ArrayList<>();

    private AcademicYear year;
    private ClassEntity classA;
    private ClassEntity classB;
    private Student student;

    @BeforeEach
    void actAsAdministrator() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(DevFixtureLoader.ADMIN_EMAIL, null, List.of()));
        year = newYear();
        classA = newClass(year, "A", 30);
        classB = newClass(year, "B", 30);
        student = newStudent();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        jdbc.execute("DROP TRIGGER IF EXISTS trg_test_fail_enrollment_insert ON enrollments");
        jdbc.execute("DROP FUNCTION IF EXISTS test_fail_enrollment_insert()");
        grades.forEach(gradeRepository::deleteById);
        enrollmentRepository.deleteAll(enrollmentRepository.findAll().stream()
                .filter(e -> students.contains(e.getStudent().getId())).toList());
        classes.forEach(classRepository::deleteById);
        years.forEach(academicYears::deleteById);
        students.forEach(studentRepository::deleteById);
    }

    // ---- manual enrollment -------------------------------------------------------------------------------

    @Test
    void enrollingCreatesANewActiveEnrollmentForTheStudentAndClass() {
        EnrollmentDto dto = enrollmentService.enrollStudent(student.getId(), classA.getId());

        Enrollment created = enrollmentRepository.findById(dto.getId()).orElseThrow();
        assertThat(created.getStudent().getId()).isEqualTo(student.getId());
        assertThat(created.getClassEntity().getId()).isEqualTo(classA.getId());
        assertThat(created.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(created.getFinalGrad()).isNull();
        assertThat(created.getEnrolledAt()).isNotNull();
        assertThat(auditTypes(created.getId())).containsExactly(AuditEventType.ENROLLMENT_CREATED);
    }

    @Test
    void historicalEnrollmentInTheSameClassDoesNotBlockANewOne() {
        Enrollment old = insert(student, classA, EnrollmentStatus.WITHDRAWN);

        EnrollmentDto dto = enrollmentService.enrollStudent(student.getId(), classA.getId());

        assertThat(dto.getId()).isNotEqualTo(old.getId());
        assertThat(enrollmentRepository.findById(old.getId()).orElseThrow().getStatus()).isEqualTo(EnrollmentStatus.WITHDRAWN);
        assertThat(enrollmentRepository.findActiveByStudentIdAndClassId(student.getId(), classA.getId())).get()
                .extracting(Enrollment::getId).isEqualTo(dto.getId());
    }

    @Test
    void anActiveEnrollmentInTheSameAcademicYearIsAConflict() {
        insert(student, classA, EnrollmentStatus.ACTIVE);

        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), classB.getId()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), classA.getId()))
                .isInstanceOf(ConflictException.class);
        assertThat(enrollmentService.canEnrollStudent(student.getId(), classB.getId())).isFalse();
        assertThat(activeEnrollments(student)).hasSize(1);
    }

    @Test
    void anActiveEnrollmentInAnotherAcademicYearDoesNotBlock() {
        ClassEntity nextYearClass = newClass(newYear(), "Next", 30);
        insert(student, classA, EnrollmentStatus.ACTIVE);

        assertThat(enrollmentService.canEnrollStudent(student.getId(), nextYearClass.getId())).isTrue();
        enrollmentService.enrollStudent(student.getId(), nextYearClass.getId());

        assertThat(activeEnrollments(student)).hasSize(2);
    }

    @Test
    void aFullClassIsAConflictButTerminalEnrollmentsDoNotOccupyCapacity() {
        ClassEntity small = newClass(year, "Small", 1);
        Student occupant = newStudent();
        Student leaver = newStudent();
        insert(leaver, small, EnrollmentStatus.WITHDRAWN);
        Enrollment seat = insert(occupant, small, EnrollmentStatus.ACTIVE);

        assertThat(enrollmentService.canEnrollStudent(student.getId(), small.getId())).isFalse();
        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), small.getId()))
                .isInstanceOf(ConflictException.class);

        enrollmentService.updateEnrollmentStatus(seat.getId(), EnrollmentStatus.COMPLETED);

        assertThat(enrollmentService.canEnrollStudent(student.getId(), small.getId())).isTrue();
        enrollmentService.enrollStudent(student.getId(), small.getId());
    }

    @Test
    void missingStudentOrClassIsNotFound() {
        assertThatThrownBy(() -> enrollmentService.enrollStudent(Long.MAX_VALUE, classA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bulkEnrollmentUsesTheCanonicalRules() {
        Student returning = newStudent();
        Student alreadyPlaced = newStudent();
        insert(returning, classA, EnrollmentStatus.WITHDRAWN);
        insert(alreadyPlaced, classB, EnrollmentStatus.ACTIVE);

        enrollmentService.bulkEnrollStudents(classA.getId(),
                List.of(student.getId(), returning.getId(), alreadyPlaced.getId()));

        assertThat(activeEnrollments(student)).extracting(e -> e.getClassEntity().getId()).containsExactly(classA.getId());
        assertThat(activeEnrollments(returning)).extracting(e -> e.getClassEntity().getId()).containsExactly(classA.getId());
        assertThat(activeEnrollments(alreadyPlaced)).extracting(e -> e.getClassEntity().getId()).containsExactly(classB.getId());
    }

    // ---- transfer ----------------------------------------------------------------------------------------

    @Test
    void transferEndsTheOldEnrollmentAndStartsANewOneInTheTargetClass() {
        Enrollment e1 = insert(student, classA, EnrollmentStatus.ACTIVE);

        EnrollmentDto result = enrollmentService.transferStudent(e1.getId(), classB.getId());

        Enrollment old = enrollmentRepository.findById(e1.getId()).orElseThrow();
        assertThat(old.getClassEntity().getId()).isEqualTo(classA.getId());
        assertThat(old.getStudent().getId()).isEqualTo(student.getId());
        assertThat(old.getStatus()).isEqualTo(EnrollmentStatus.TRANSFERRED);

        assertThat(result.getId()).isNotEqualTo(e1.getId());
        Enrollment created = enrollmentRepository.findById(result.getId()).orElseThrow();
        assertThat(created.getStudent().getId()).isEqualTo(student.getId());
        assertThat(created.getClassEntity().getId()).isEqualTo(classB.getId());
        assertThat(created.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(created.getFinalGrad()).isNull();
        assertThat(result.getClassName()).isEqualTo(classB.getName());
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);

        assertThat(auditTypes(e1.getId())).containsExactly(AuditEventType.ENROLLMENT_UPDATED);
        assertThat(auditTypes(created.getId())).containsExactly(AuditEventType.ENROLLMENT_CREATED);
    }

    @Test
    void transferLeavesGradesWithTheHistoricalEnrollment() {
        Enrollment e1 = insert(student, classA, EnrollmentStatus.ACTIVE);
        Grade grade = new Grade();
        grade.setEnrollment(e1);
        grade.setAssignedBy(teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow());
        grade.setScore(15f);
        grades.add(gradeRepository.save(grade).getId());

        EnrollmentDto result = enrollmentService.transferStudent(e1.getId(), classB.getId());

        assertThat(gradeRepository.findById(grade.getId()).orElseThrow().getEnrollment().getId()).isEqualTo(e1.getId());
        assertThat(result.getGradeCount()).isZero();
    }

    @Test
    void aStudentCanReturnToAFormerClassThroughASecondTransfer() {
        Enrollment e1 = insert(student, classA, EnrollmentStatus.ACTIVE);
        Long e2 = enrollmentService.transferStudent(e1.getId(), classB.getId()).getId();

        Long e3 = enrollmentService.transferStudent(e2, classA.getId()).getId();

        assertThat(List.of(e1.getId(), e2, e3)).doesNotHaveDuplicates();
        assertThat(enrollmentRepository.findAllByStudentId(student.getId()))
                .extracting(e -> e.getClassEntity().getId() + ":" + e.getStatus())
                .containsExactlyInAnyOrder(
                        classA.getId() + ":TRANSFERRED", classB.getId() + ":TRANSFERRED", classA.getId() + ":ACTIVE");
    }

    @ParameterizedTest
    @CsvSource({"COMPLETED", "WITHDRAWN", "TRANSFERRED"})
    void aTerminalEnrollmentCannotBeTransferred(EnrollmentStatus terminal) {
        Enrollment source = insert(student, classA, terminal);

        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), classB.getId()))
                .isInstanceOf(ConflictException.class);

        assertThat(enrollmentRepository.findAllByStudentId(student.getId())).hasSize(1)
                .allSatisfy(e -> assertThat(e.getStatus()).isEqualTo(terminal));
    }

    @Test
    void aTransferToTheSameClassIsAConflict() {
        Enrollment source = insert(student, classA, EnrollmentStatus.ACTIVE);

        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), classA.getId()))
                .isInstanceOf(ConflictException.class);

        assertUnchangedActive(source);
    }

    @Test
    void aTransferToAnotherAcademicYearIsAConflict() {
        Enrollment source = insert(student, classA, EnrollmentStatus.ACTIVE);
        ClassEntity nextYearClass = newClass(newYear(), "Next", 30);

        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), nextYearClass.getId()))
                .isInstanceOf(ConflictException.class);

        assertUnchangedActive(source);
    }

    @Test
    void aTransferIntoAFullClassIsAConflict() {
        ClassEntity small = newClass(year, "Small", 1);
        insert(newStudent(), small, EnrollmentStatus.ACTIVE);
        Enrollment source = insert(student, classA, EnrollmentStatus.ACTIVE);

        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), small.getId()))
                .isInstanceOf(ConflictException.class);

        assertUnchangedActive(source);
    }

    @Test
    void transferToMissingEnrollmentOrClassIsNotFound() {
        Enrollment source = insert(student, classA, EnrollmentStatus.ACTIVE);

        assertThatThrownBy(() -> enrollmentService.transferStudent(Long.MAX_VALUE, classB.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
        assertUnchangedActive(source);
    }

    @Test
    void aFailedInsertOfTheNewEnrollmentRollsTheWholeTransferBack() {
        Enrollment source = insert(student, classA, EnrollmentStatus.ACTIVE);
        failEnrollmentInserts(classB, "23505", "injected failure");

        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), classB.getId()))
                .isInstanceOf(RuntimeException.class);

        assertUnchangedActive(source);
        assertThat(auditTypes(source.getId())).isEmpty();
    }

    // ---- database invariant translated to a conflict -----------------------------------------------------

    @Test
    void theOneActivePerAcademicYearRuleRaisedByTheDatabaseBecomesAConflict() {
        failEnrollmentInserts(classA, "23505",
                "uk_enrollments_one_active_per_academic_year: student already has an active enrollment");

        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), classA.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageNotContaining("uk_enrollments")
                .hasMessageNotContaining("ERROR");
    }

    @Test
    void anyOtherIntegrityViolationIsNotReportedAsAnEnrollmentConflict() {
        failEnrollmentInserts(classA, "23505", "some_other_unique_constraint");

        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), classA.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---- generic status updates and withdrawal -----------------------------------------------------------

    @ParameterizedTest
    @CsvSource({"COMPLETED", "WITHDRAWN"})
    void anActiveEnrollmentCanBeCompletedOrWithdrawn(EnrollmentStatus target) {
        Enrollment active = insert(student, classA, EnrollmentStatus.ACTIVE);

        EnrollmentDto dto = enrollmentService.updateEnrollmentStatus(active.getId(), target);

        assertThat(dto.getStatus()).isEqualTo(target);
        assertThat(enrollmentRepository.findById(active.getId()).orElseThrow().getStatus()).isEqualTo(target);
        assertThat(auditTypes(active.getId())).containsExactly(AuditEventType.ENROLLMENT_UPDATED);
    }

    @Test
    void theGenericStatusUpdateCannotProduceTransferred() {
        Enrollment active = insert(student, classA, EnrollmentStatus.ACTIVE);

        assertThatThrownBy(() -> enrollmentService.updateEnrollmentStatus(active.getId(), EnrollmentStatus.TRANSFERRED))
                .isInstanceOf(ConflictException.class);

        assertUnchangedActive(active);
    }

    @ParameterizedTest
    @CsvSource({
            "COMPLETED,ACTIVE", "COMPLETED,WITHDRAWN", "COMPLETED,TRANSFERRED",
            "TRANSFERRED,ACTIVE", "TRANSFERRED,COMPLETED", "TRANSFERRED,WITHDRAWN",
            "WITHDRAWN,ACTIVE", "WITHDRAWN,COMPLETED", "WITHDRAWN,TRANSFERRED"})
    void aTerminalEnrollmentNeverChangesStatus(EnrollmentStatus from, EnrollmentStatus to) {
        Enrollment terminal = insert(student, classA, from);

        assertThatThrownBy(() -> enrollmentService.updateEnrollmentStatus(terminal.getId(), to))
                .isInstanceOf(ConflictException.class);

        assertThat(enrollmentRepository.findById(terminal.getId()).orElseThrow().getStatus()).isEqualTo(from);
    }

    @ParameterizedTest
    @CsvSource({"ACTIVE", "COMPLETED", "TRANSFERRED", "WITHDRAWN"})
    void settingTheCurrentStatusAgainIsASafeNoOp(EnrollmentStatus status) {
        Enrollment enrollment = insert(student, classA, status);

        EnrollmentDto dto = enrollmentService.updateEnrollmentStatus(enrollment.getId(), status);

        assertThat(dto.getStatus()).isEqualTo(status);
        assertThat(auditTypes(enrollment.getId())).isEmpty();
    }

    @Test
    void withdrawingAnActiveEnrollmentKeepsTheRow() {
        Enrollment active = insert(student, classA, EnrollmentStatus.ACTIVE);

        enrollmentService.withdrawEnrollment(active.getId(), "Moved abroad");

        Enrollment withdrawn = enrollmentRepository.findById(active.getId()).orElseThrow();
        assertThat(withdrawn.getStatus()).isEqualTo(EnrollmentStatus.WITHDRAWN);
        assertThat(withdrawn.getClassEntity().getId()).isEqualTo(classA.getId());
        assertThat(auditTypes(active.getId())).containsExactly(AuditEventType.ENROLLMENT_DELETED);
    }

    @Test
    void withdrawingAWithdrawnEnrollmentIsIdempotent() {
        Enrollment withdrawn = insert(student, classA, EnrollmentStatus.WITHDRAWN);

        enrollmentService.withdrawEnrollment(withdrawn.getId(), "Again");

        assertThat(enrollmentRepository.findById(withdrawn.getId()).orElseThrow().getStatus()).isEqualTo(EnrollmentStatus.WITHDRAWN);
        assertThat(auditTypes(withdrawn.getId())).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"COMPLETED", "TRANSFERRED"})
    void aCompletedOrTransferredEnrollmentCannotBeWithdrawn(EnrollmentStatus terminal) {
        Enrollment enrollment = insert(student, classA, terminal);

        assertThatThrownBy(() -> enrollmentService.withdrawEnrollment(enrollment.getId(), "Reason"))
                .isInstanceOf(ConflictException.class);

        assertThat(enrollmentRepository.findById(enrollment.getId()).orElseThrow().getStatus()).isEqualTo(terminal);
    }

    // ---- statistics ----------------------------------------------------------------------------------------

    @Test
    void statisticsCountEveryCanonicalStatus() {
        Enrollment e1 = insert(student, classA, EnrollmentStatus.ACTIVE);
        enrollmentService.transferStudent(e1.getId(), classB.getId());
        insert(student, newClass(newYear(), "Earlier", 30), EnrollmentStatus.COMPLETED);
        insert(student, newClass(newYear(), "Left", 30), EnrollmentStatus.WITHDRAWN);

        var stats = enrollmentService.getStudentEnrollmentStats(student.getId());

        assertThat(stats.getTotalEnrollments()).isEqualTo(4);
        assertThat(stats.getActiveEnrollments()).isEqualTo(1);
        assertThat(stats.getCompletedEnrollments()).isEqualTo(1);
        assertThat(stats.getTransferredEnrollments()).isEqualTo(1);
        assertThat(stats.getWithdrawnEnrollments()).isEqualTo(1);
        assertThat(stats.getCompletionRate()).isEqualTo(25.0);
    }

    // ---- HTTP -----------------------------------------------------------------------------------------------

    @Test
    void httpMapsLifecycleConflictsToConflictAndTransfersToTheNewEnrollment() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);
        Enrollment active = insert(student, classA, EnrollmentStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/enrollments/{id}/status", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"TRANSFERRED\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/enrollments/{id}/status", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/enrollments/{id}/transfer", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("newClassId", classB.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.className").value(classB.getName()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        Long newId = activeEnrollments(student).get(0).getId();
        mockMvc.perform(delete("/api/v1/enrollments/{id}", newId)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Left the school\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/enrollments/{id}", active.getId())
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Too late\"}"))
                .andExpect(status().isConflict());
    }

    // ---- fixtures ---------------------------------------------------------------------------------------------

    private AcademicYear newYear() {
        AcademicYear created = AcademicYearTestFixtures.create(academicYears, currentSchool);
        years.add(created.getId());
        return created;
    }

    private ClassEntity newClass(AcademicYear owner, String name, int capacity) {
        ClassEntity c = new ClassEntity();
        c.setAcademicYear(owner);
        c.setName(name + " " + UUID.randomUUID());
        c.setCapacity(capacity);
        ClassEntity saved = classRepository.save(c);
        classes.add(saved.getId());
        return saved;
    }

    private Student newStudent() {
        Student s = new Student();
        s.setRole(UserRole.STUDENT);
        s.setEmail("lifecycle-" + UUID.randomUUID() + "@fixtures.school.test");
        s.setFirstName("Lena");
        s.setLastName("Lifecycle");
        s.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        s.setStatus(Status.ACTIVE);
        s.setIsEmailVerified(true);
        Student saved = studentRepository.save(s);
        students.add(saved.getId());
        return saved;
    }

    private Enrollment insert(Student owner, ClassEntity classEntity, EnrollmentStatus status) {
        Enrollment e = new Enrollment();
        e.setStudent(owner);
        e.setClassEntity(classEntity);
        e.setStatus(status);
        return enrollmentRepository.saveAndFlush(e);
    }

    private List<Enrollment> activeEnrollments(Student owner) {
        return enrollmentRepository.findByStudentIdAndStatus(owner.getId(), EnrollmentStatus.ACTIVE);
    }

    private void assertUnchangedActive(Enrollment source) {
        Enrollment reloaded = enrollmentRepository.findById(source.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(reloaded.getClassEntity().getId()).isEqualTo(source.getClassEntity().getId());
        assertThat(enrollmentRepository.findAllByStudentId(source.getStudent().getId())).hasSize(1);
    }

    private List<AuditEventType> auditTypes(Long enrollmentId) {
        return auditEvents.findAll().stream()
                .filter(event -> "Enrollment".equals(event.getEntityType()) && enrollmentId.equals(event.getEntityId()))
                .map(AuditEvent::getEventType)
                .toList();
    }

    /** Makes the database reject inserts into one class, to exercise failures that application checks cannot reach. */
    private void failEnrollmentInserts(ClassEntity target, String sqlState, String message) {
        jdbc.execute("""
                CREATE FUNCTION test_fail_enrollment_insert() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF NEW.class_id = TG_ARGV[0]::bigint THEN
                        RAISE EXCEPTION '%', TG_ARGV[2] USING ERRCODE = TG_ARGV[1];
                    END IF;
                    RETURN NEW;
                END $$""");
        jdbc.execute("CREATE TRIGGER trg_test_fail_enrollment_insert BEFORE INSERT ON enrollments FOR EACH ROW "
                + "EXECUTE FUNCTION test_fail_enrollment_insert('" + target.getId() + "', '" + sqlState + "', '" + message + "')");
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.59." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
