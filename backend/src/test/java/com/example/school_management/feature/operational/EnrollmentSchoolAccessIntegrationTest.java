package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
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
import com.example.school_management.feature.operational.dto.AutoEnrollmentResultDto;
import com.example.school_management.feature.operational.dto.EnrollmentDto;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.service.EnrollmentService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

/**
 * Enrollment belongs to the School that owns its Class's AcademicYear. Foreign-school records behave as missing,
 * and a new ACTIVE Enrollment needs an ACTIVE STUDENT membership of the current School plus an ACTIVE account.
 * Existing history stays readable and closable whatever the membership status becomes.
 */
@IntegrationTest
@Transactional
class EnrollmentSchoolAccessIntegrationTest {
    @Autowired EnrollmentService enrollmentService;
    @Autowired EnrollmentRepository enrollments;
    @Autowired StudentRepository students;
    @Autowired SchoolMembershipRepository memberships;
    @Autowired SchoolRepository schools;
    @Autowired AcademicYearRepository years;
    @Autowired ClassRepository classes;
    @Autowired EntityManager em;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;

    private School school;
    private School otherSchool;
    private AcademicYear year;
    private AcademicYear otherYear;
    private ClassEntity classA;
    private ClassEntity classB;
    private ClassEntity foreignClass;

    @BeforeEach
    void twoSchools() {
        school = school("Current school");
        otherSchool = school("Other school");
        year = year(school);
        otherYear = year(otherSchool);
        classA = clazz(year, "Room A", 30);
        classB = clazz(year, "Room B", 30);
        foreignClass = clazz(otherYear, "Foreign room", 30);
        doReturn(school).when(currentSchool).resolve();
    }

    // ---- creating an enrollment ------------------------------------------------------------------------

    @Test
    void anActiveStudentMemberCanBeEnrolledInACurrentSchoolClass() {
        Student student = enrollable(school);

        EnrollmentDto dto = enrollmentService.enrollStudent(student.getId(), classA.getId());

        assertThat(dto.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollments.findByStudentId(student.getId())).hasSize(1);
        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), classB.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "no-membership", "other-school-only", "wrong-role", "deleted"})
    void aStudentOutsideTheCurrentSchoolIsNotFound(String kind) {
        Long studentId = switch (kind) {
            case "missing" -> Long.MAX_VALUE;
            case "no-membership" -> student(Status.ACTIVE).getId();
            case "other-school-only" -> enrollable(otherSchool).getId();
            case "wrong-role" -> {
                Student s = student(Status.ACTIVE);
                member(s, school, MembershipStatus.ACTIVE, MembershipRole.GUARDIAN);
                yield s.getId();
            }
            default -> {
                Student s = enrollable(school);
                s.setStatus(Status.DELETED);
                students.saveAndFlush(s);
                em.clear();
                yield s.getId();
            }
        };

        assertThatThrownBy(() -> enrollmentService.enrollStudent(studentId, classA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        em.flush();
        assertThat(enrollments.findByStudentId(studentId)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"membership-suspended", "membership-inactive", "user-suspended"})
    void aVisibleButIneligibleStudentIsAConflict(String kind) {
        Student student = ineligible(kind);

        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), classA.getId()))
                .isInstanceOf(ConflictException.class);
        assertThat(enrollments.findByStudentId(student.getId())).isEmpty();
    }

    @Test
    void aForeignClassIsNotFound() {
        Student student = enrollable(school);

        assertThatThrownBy(() -> enrollmentService.enrollStudent(student.getId(), foreignClass.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(enrollments.findByStudentId(student.getId())).isEmpty();
    }

    // ---- transfer -----------------------------------------------------------------------------------------

    @Test
    void aCurrentSchoolTransferStartsANewActiveEnrollment() {
        Student student = enrollable(school);
        Enrollment source = enrollment(student, classA, EnrollmentStatus.ACTIVE);

        EnrollmentDto result = enrollmentService.transferStudent(source.getId(), classB.getId());

        em.flush();
        em.clear();
        assertThat(enrollments.findById(source.getId()).orElseThrow().getStatus()).isEqualTo(EnrollmentStatus.TRANSFERRED);
        Enrollment created = enrollments.findById(result.getId()).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(created.getClassEntity().getId()).isEqualTo(classB.getId());
    }

    @Test
    void aForeignSourceEnrollmentCannotBeTransferred() {
        Student foreignStudent = enrollable(otherSchool);
        Enrollment foreign = enrollment(foreignStudent, foreignClass, EnrollmentStatus.ACTIVE);
        ClassEntity foreignTarget = clazz(otherYear, "Foreign target", 30);

        assertThatThrownBy(() -> enrollmentService.transferStudent(foreign.getId(), classA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.transferStudent(foreign.getId(), foreignTarget.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertUnchanged(foreign);
    }

    @Test
    void aForeignTargetClassCannotReceiveATransfer() {
        Student student = enrollable(school);
        Enrollment source = enrollment(student, classA, EnrollmentStatus.ACTIVE);

        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), foreignClass.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertUnchanged(source);
    }

    @ParameterizedTest
    @ValueSource(strings = {"membership-suspended", "membership-inactive", "user-suspended"})
    void aTransferOfAnIneligibleStudentIsAConflictAndLeavesTheSourceUnchanged(String kind) {
        Student student = enrollable(school);
        Enrollment source = enrollment(student, classA, EnrollmentStatus.ACTIVE);
        degrade(student, kind);

        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), classB.getId()))
                .isInstanceOf(ConflictException.class);
        assertUnchanged(source);
    }

    @Test
    void aTransferOfAStudentWhoLeftTheStudentRoleIsNotFound() {
        Student student = enrollable(school);
        Enrollment source = enrollment(student, classA, EnrollmentStatus.ACTIVE);
        SchoolMembership membership = memberships.findByUserIdAndSchoolId(student.getId(), school.getId()).orElseThrow();
        membership.setRoles(new HashSet<>(Set.of(MembershipRole.GUARDIAN)));
        memberships.saveAndFlush(membership);
        em.clear();

        assertThatThrownBy(() -> enrollmentService.transferStudent(source.getId(), classB.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertUnchanged(source);
    }

    // ---- history survives membership changes ----------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"membership-inactive", "membership-suspended", "user-suspended"})
    void anExistingEnrollmentCanStillBeReadAndCompleted(String kind) {
        Student student = enrollable(school);
        Enrollment active = enrollment(student, classA, EnrollmentStatus.ACTIVE);
        degrade(student, kind);

        assertThat(enrollmentService.getEnrollment(active.getId()).getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollmentService.updateEnrollmentStatus(active.getId(), EnrollmentStatus.COMPLETED).getStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"membership-inactive", "membership-suspended", "user-suspended"})
    void anExistingEnrollmentCanStillBeWithdrawn(String kind) {
        Student student = enrollable(school);
        Enrollment active = enrollment(student, classA, EnrollmentStatus.ACTIVE);
        degrade(student, kind);

        enrollmentService.withdrawEnrollment(active.getId(), "Left the school");

        em.flush();
        em.clear();
        assertThat(enrollments.findById(active.getId()).orElseThrow().getStatus()).isEqualTo(EnrollmentStatus.WITHDRAWN);
    }

    // ---- foreign enrollments ------------------------------------------------------------------------------------

    @Test
    void aForeignEnrollmentCannotBeReadUpdatedOrWithdrawn() {
        Student foreignStudent = enrollable(otherSchool);
        Enrollment foreign = enrollment(foreignStudent, foreignClass, EnrollmentStatus.ACTIVE);

        assertThatThrownBy(() -> enrollmentService.getEnrollment(foreign.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.updateEnrollmentStatus(foreign.getId(), EnrollmentStatus.COMPLETED))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.withdrawEnrollment(foreign.getId(), "Reason"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.getEnrollment(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
        assertUnchanged(foreign);
    }

    // ---- lists --------------------------------------------------------------------------------------------------

    @Test
    void everyListReadReturnsOnlyCurrentSchoolRowsAndTotals() {
        Student foreignStudent;
        for (int i = 0; i < 3; i++) {
            enrollment(named("Zed", "Current" + i, school), classA, i < 2 ? EnrollmentStatus.ACTIVE : EnrollmentStatus.COMPLETED);
        }
        ClassEntity foreignRoom = clazz(otherYear, "Room A", 30);
        for (int i = 0; i < 7; i++) {
            foreignStudent = named("Zed", "Foreign" + i, otherSchool);
            enrollment(foreignStudent, i < 5 ? foreignClass : foreignRoom, i < 5 ? EnrollmentStatus.ACTIVE : EnrollmentStatus.COMPLETED);
        }
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);

        Page<EnrollmentDto> all = enrollmentService.getAllEnrollments(PageRequest.of(0, 2), null, null);
        assertThat(all.getTotalElements()).isEqualTo(3);
        assertThat(all.getContent()).hasSize(2);
        assertThat(enrollmentService.getAllEnrollments(PageRequest.of(1, 2), null, null).getContent()).hasSize(1);
        assertThat(enrollmentService.getAllEnrollments(PageRequest.of(0, 10, Sort.by("status")), null, null)
                .getTotalElements()).isEqualTo(3);

        assertThat(enrollmentService.getAllEnrollments(PageRequest.of(0, 10), "zed", null).getTotalElements()).isEqualTo(3);
        assertThat(enrollmentService.getAllEnrollments(PageRequest.of(0, 10), "room a", null).getTotalElements()).isEqualTo(3);
        assertThat(enrollmentService.getAllEnrollments(PageRequest.of(0, 10), "foreign", null).getTotalElements()).isZero();
        assertThat(enrollmentService.getAllEnrollments(PageRequest.of(0, 10), "zed", EnrollmentStatus.ACTIVE).getTotalElements())
                .isEqualTo(2);
        assertThat(enrollmentService.getAllEnrollments(PageRequest.of(0, 10), null, EnrollmentStatus.COMPLETED).getTotalElements())
                .isEqualTo(1);

        assertThat(enrollmentService.getEnrollmentsByStatus(EnrollmentStatus.ACTIVE, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);
        assertThat(enrollmentService.getEnrollmentsByDateRange(from, to, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
    }

    @Test
    void classReadsAndStatsAreLimitedToCurrentSchoolClasses() {
        enrollment(enrollable(school), classA, EnrollmentStatus.ACTIVE);
        enrollment(enrollable(school), classA, EnrollmentStatus.COMPLETED);
        enrollment(enrollable(otherSchool), foreignClass, EnrollmentStatus.ACTIVE);

        assertThat(enrollmentService.getClassEnrollments(classA.getId(), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2);
        var stats = enrollmentService.getClassEnrollmentStats(classA.getId());
        assertThat(stats.getTotalEnrollments()).isEqualTo(2);
        assertThat(stats.getActiveEnrollments()).isEqualTo(1);

        assertThatThrownBy(() -> enrollmentService.getClassEnrollments(foreignClass.getId(), PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.getClassEnrollmentStats(foreignClass.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- student history ---------------------------------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({"ACTIVE", "SUSPENDED", "INACTIVE"})
    void aStudentMemberKeepsReadableHistoryInTheCurrentSchoolOnly(MembershipStatus membershipStatus) {
        Student student = enrollable(school);
        member(student, otherSchool, MembershipStatus.ACTIVE, MembershipRole.STUDENT);
        enrollment(student, classA, EnrollmentStatus.ACTIVE);
        enrollment(student, foreignClass, EnrollmentStatus.WITHDRAWN);
        SchoolMembership membership = memberships.findByUserIdAndSchoolId(student.getId(), school.getId()).orElseThrow();
        membership.setStatus(membershipStatus);
        memberships.saveAndFlush(membership);

        Page<EnrollmentDto> history = enrollmentService.getStudentEnrollments(student.getId(), PageRequest.of(0, 10));
        var stats = enrollmentService.getStudentEnrollmentStats(student.getId());

        assertThat(history.getTotalElements()).isEqualTo(1);
        assertThat(history.getContent().get(0).getClassName()).isEqualTo(classA.getName());
        assertThat(stats.getTotalEnrollments()).isEqualTo(1);
        assertThat(stats.getWithdrawnEnrollments()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-membership", "other-school-only", "wrong-role"})
    void studentHistoryOfANonStudentIsNotFound(String kind) {
        Student student = switch (kind) {
            case "no-membership" -> student(Status.ACTIVE);
            case "other-school-only" -> enrollable(otherSchool);
            default -> {
                Student s = student(Status.ACTIVE);
                member(s, school, MembershipStatus.ACTIVE, MembershipRole.TEACHER);
                yield s;
            }
        };
        enrollment(student, foreignClass, EnrollmentStatus.ACTIVE);

        assertThatThrownBy(() -> enrollmentService.getStudentEnrollments(student.getId(), PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.getStudentEnrollmentStats(student.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- eligibility predicate --------------------------------------------------------------------------------

    @Test
    void canEnrollAnswersTrueOnlyWhenEveryRequirementPasses() {
        Student eligible = enrollable(school);
        assertThat(enrollmentService.canEnrollStudent(eligible.getId(), classA.getId())).isTrue();

        ClassEntity full = clazz(year, "Full", 1);
        enrollment(enrollable(school), full, EnrollmentStatus.ACTIVE);
        assertThat(enrollmentService.canEnrollStudent(eligible.getId(), full.getId())).isFalse();

        Student placed = enrollable(school);
        enrollment(placed, classA, EnrollmentStatus.ACTIVE);
        assertThat(enrollmentService.canEnrollStudent(placed.getId(), classB.getId())).isFalse();

        for (String kind : List.of("membership-suspended", "membership-inactive", "user-suspended")) {
            assertThat(enrollmentService.canEnrollStudent(ineligible(kind).getId(), classA.getId())).as(kind).isFalse();
        }
    }

    @Test
    void canEnrollHidesResourcesOutsideTheCurrentSchool() {
        Student eligible = enrollable(school);
        Student wrongRole = student(Status.ACTIVE);
        member(wrongRole, school, MembershipStatus.ACTIVE, MembershipRole.GUARDIAN);

        assertThatThrownBy(() -> enrollmentService.canEnrollStudent(student(Status.ACTIVE).getId(), classA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.canEnrollStudent(enrollable(otherSchool).getId(), classA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.canEnrollStudent(wrongRole.getId(), classA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.canEnrollStudent(Long.MAX_VALUE, classA.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.canEnrollStudent(eligible.getId(), foreignClass.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.canEnrollStudent(eligible.getId(), Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- bulk enrollment ---------------------------------------------------------------------------------------

    @Test
    void bulkEnrollmentOfAForeignClassIsNotFoundBeforeAnyStudentIsProcessed() {
        Student student = enrollable(school);

        assertThatThrownBy(() -> enrollmentService.bulkEnrollStudents(foreignClass.getId(), List.of(student.getId())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(enrollments.findByStudentId(student.getId())).isEmpty();
    }

    @Test
    void bulkEnrollmentOnlyEnrollsStudentsWhoPassTheNormalEligibilityRules() {
        Student eligible = enrollable(school);
        Student suspendedMember = ineligible("membership-suspended");
        Student inactiveMember = ineligible("membership-inactive");
        Student suspendedUser = ineligible("user-suspended");
        Student otherSchoolStudent = enrollable(otherSchool);
        Student noMembership = student(Status.ACTIVE);

        enrollmentService.bulkEnrollStudents(classA.getId(), List.of(eligible.getId(), suspendedMember.getId(),
                inactiveMember.getId(), suspendedUser.getId(), otherSchoolStudent.getId(), noMembership.getId()));

        assertThat(enrollments.findByStudentId(eligible.getId())).hasSize(1);
        for (Student rejected : List.of(suspendedMember, inactiveMember, suspendedUser, otherSchoolStudent, noMembership)) {
            assertThat(enrollments.findByStudentId(rejected.getId())).as(rejected.getEmail()).isEmpty();
        }
    }

    // ---- auto-enrollment ---------------------------------------------------------------------------------------

    @Test
    void autoEnrollmentConsidersOnlyActiveStudentMembersOfTheCurrentSchool() {
        classA.setGradeLevel(GradeLevel.MIDDLE.name());
        classes.saveAndFlush(classA);

        Student eligible = middle(enrollable(school));
        member(eligible, otherSchool, MembershipStatus.ACTIVE, MembershipRole.STUDENT);
        enrollment(eligible, foreignClass, EnrollmentStatus.ACTIVE);

        List<Student> rejected = List.of(
                middle(ineligible("membership-suspended")),
                middle(ineligible("membership-inactive")),
                middle(ineligible("user-suspended")),
                middle(enrollable(otherSchool)),
                middle(wrongRoleMember()));

        AutoEnrollmentResultDto preview = enrollmentService.previewAutoEnrollment();

        assertThat(preview.success()).isTrue();
        assertThat(preview.totalStudentsProcessed()).isEqualTo(1);
        assertThat(preview.studentsEnrolled()).isEqualTo(1);
        assertThat(preview.errors()).isEmpty();
        assertThat(enrollments.findByStudentId(eligible.getId())).hasSize(1);

        AutoEnrollmentResultDto result = enrollmentService.autoEnrollAllStudents();

        assertThat(result.success()).isTrue();
        assertThat(result.totalStudentsProcessed()).isEqualTo(1);
        assertThat(result.studentsEnrolled()).isEqualTo(1);
        assertThat(enrollments.findActiveByStudentIdAndClassId(eligible.getId(), classA.getId())).isPresent();
        for (Student student : rejected) {
            assertThat(enrollments.findByStudentId(student.getId())).as(student.getEmail())
                    .noneMatch(e -> e.getClassEntity().getId().equals(classA.getId()));
        }

        assertThat(enrollmentService.previewAutoEnrollment().totalStudentsProcessed()).isZero();
    }

    // ---- fixtures ----------------------------------------------------------------------------------------------

    private School school(String name) {
        School created = new School();
        created.setName(name);
        return schools.saveAndFlush(created);
    }

    private AcademicYear year(School owner) {
        AcademicYear created = new AcademicYear();
        created.setSchool(owner);
        created.setName("Year " + UUID.randomUUID());
        created.setStartDate(LocalDate.of(2026, 9, 1));
        created.setEndDate(LocalDate.of(2027, 6, 30));
        created.setActive(true);
        return years.saveAndFlush(created);
    }

    private ClassEntity clazz(AcademicYear owner, String name, int capacity) {
        ClassEntity created = new ClassEntity();
        created.setAcademicYear(owner);
        created.setName(name);
        created.setCapacity(capacity);
        return classes.saveAndFlush(created);
    }

    private Student student(Status status) {
        return named("Sam", "Student", status);
    }

    private Student named(String first, String last, Status status) {
        Student created = new Student();
        created.setRole(UserRole.STUDENT);
        created.setEmail("access-" + UUID.randomUUID() + "@fixtures.school.test");
        created.setFirstName(first);
        created.setLastName(last);
        created.setPassword("not-a-real-hash");
        created.setStatus(status);
        created.setIsEmailVerified(true);
        return students.saveAndFlush(created);
    }

    private Student named(String first, String last, School memberOf) {
        Student created = named(first, last, Status.ACTIVE);
        member(created, memberOf, MembershipStatus.ACTIVE, MembershipRole.STUDENT);
        return created;
    }

    private Student enrollable(School memberOf) {
        return named("Sam", "Student", memberOf);
    }

    private Student middle(Student student) {
        student.setGradeLevel(GradeLevel.MIDDLE);
        return students.saveAndFlush(student);
    }

    private Student wrongRoleMember() {
        Student created = student(Status.ACTIVE);
        member(created, school, MembershipStatus.ACTIVE, MembershipRole.GUARDIAN);
        return created;
    }

    private SchoolMembership member(Student user, School owner, MembershipStatus status, MembershipRole role) {
        SchoolMembership membership = new SchoolMembership();
        membership.setUser(user);
        membership.setSchool(owner);
        membership.setStatus(status);
        membership.setRoles(new HashSet<>(Set.of(role)));
        return memberships.saveAndFlush(membership);
    }

    private Student ineligible(String kind) {
        Student created = enrollable(school);
        degrade(created, kind);
        return created;
    }

    /** Makes a current-school student ineligible for new enrollments without touching its history. */
    private void degrade(Student student, String kind) {
        if (kind.equals("user-suspended")) {
            Student loaded = students.findById(student.getId()).orElseThrow();
            loaded.setStatus(Status.SUSPENDED);
            students.saveAndFlush(loaded);
        } else {
            SchoolMembership membership = memberships.findByUserIdAndSchoolId(student.getId(), school.getId()).orElseThrow();
            membership.setStatus(kind.equals("membership-suspended") ? MembershipStatus.SUSPENDED : MembershipStatus.INACTIVE);
            memberships.saveAndFlush(membership);
        }
        em.clear();
    }

    private Enrollment enrollment(Student owner, ClassEntity classEntity, EnrollmentStatus status) {
        Enrollment created = new Enrollment();
        created.setStudent(owner);
        created.setClassEntity(classEntity);
        created.setStatus(status);
        return enrollments.saveAndFlush(created);
    }

    private void assertUnchanged(Enrollment source) {
        em.flush();
        em.clear();
        Enrollment reloaded = enrollments.findById(source.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(reloaded.getClassEntity().getId()).isEqualTo(source.getClassEntity().getId());
        assertThat(enrollments.findByStudentId(source.getStudent().getId())).hasSize(1);
    }
}
