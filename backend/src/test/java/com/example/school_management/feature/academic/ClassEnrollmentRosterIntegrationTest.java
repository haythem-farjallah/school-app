package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.utils.QueryParams;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.dto.ClassCardDto;
import com.example.school_management.feature.academic.dto.ClassDto;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.service.ClassService;
import com.example.school_management.feature.academic.service.TeacherClassService;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * The current roster of a Class is its ACTIVE Enrollments. Every roster read agrees on that: the class DTOs,
 * the class cards, the teacher's class counts and a student's current classes. Terminal enrollments are history.
 */
@IntegrationTest
@Transactional
class ClassEnrollmentRosterIntegrationTest {
    @Autowired ClassService classService;
    @Autowired TeacherClassService teacherClassService;
    @Autowired EnrollmentService enrollmentService;
    @Autowired EnrollmentRepository enrollments;
    @Autowired ClassRepository classes;
    @Autowired AcademicYearRepository years;
    @Autowired SchoolRepository schools;
    @Autowired StudentRepository students;
    @Autowired TeacherRepository teachers;
    @Autowired SchoolMembershipRepository memberships;
    @Autowired EntityManager em;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;

    private School school;
    private AcademicYear year;
    private ClassEntity classA;
    private ClassEntity classB;
    private Teacher teacher;

    @BeforeEach
    void school() {
        school = new School();
        school.setName("Roster school");
        school = schools.saveAndFlush(school);
        year = year();
        teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        classA = clazz("Roster A");
        classB = clazz("Roster B");
        doReturn(school).when(currentSchool).resolve();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(DevFixtureLoader.ADMIN_EMAIL, null, List.of()));
    }

    @Test
    void onlyActiveEnrollmentsAreOnTheRosterEverywhere() {
        Student active = student();
        enrollment(active, classA, EnrollmentStatus.ACTIVE);
        enrollment(student(), classA, EnrollmentStatus.COMPLETED);
        enrollment(student(), classA, EnrollmentStatus.TRANSFERRED);
        enrollment(student(), classA, EnrollmentStatus.WITHDRAWN);

        assertThat(roster(classA)).containsExactly(active.getId());
        assertThat(classService.list(PageRequest.of(0, 10), "roster a").getContent())
                .singleElement().extracting(ClassDto::studentIds).isEqualTo(Set.of(active.getId()));
        assertThat(classService.listClasses(query(0, 10)).getContent())
                .filteredOn(c -> c.id().equals(classA.getId()))
                .singleElement().extracting(ClassDto::studentIds).isEqualTo(Set.of(active.getId()));
        assertThat(classService.getClassesByTeacherId(teacher.getId(), PageRequest.of(0, 10)).getContent())
                .filteredOn(c -> c.id().equals(classA.getId()))
                .singleElement().extracting(ClassDto::studentIds).isEqualTo(Set.of(active.getId()));
        assertThat(enrollments.countActiveByClassId(classA.getId())).isEqualTo(1);
    }

    @Test
    void cardsAndTeacherCountsMatchTheRoster() {
        enrollment(student(), classA, EnrollmentStatus.ACTIVE);
        enrollment(student(), classA, EnrollmentStatus.ACTIVE);
        enrollment(student(), classA, EnrollmentStatus.COMPLETED);
        enrollment(student(), classA, EnrollmentStatus.WITHDRAWN);
        enrollment(student(), classB, EnrollmentStatus.TRANSFERRED);

        var cards = classService.listCards(query(0, 10)).getContent();
        assertThat(cards).filteredOn(c -> c.id().equals(classA.getId())).singleElement()
                .extracting(ClassCardDto::studentCount).isEqualTo(2);
        assertThat(cards).filteredOn(c -> c.id().equals(classB.getId())).singleElement()
                .extracting(ClassCardDto::studentCount).isEqualTo(0);

        var teacherClasses = teacherClassService.getAllTeacherClasses(DevFixtureLoader.TEACHER_EMAIL, null);
        assertThat(teacherClasses).filteredOn(c -> c.id().equals(classA.getId())).singleElement()
                .extracting(c -> c.enrolled()).isEqualTo(2);
        assertThat(teacherClasses).filteredOn(c -> c.id().equals(classB.getId())).singleElement()
                .extracting(c -> c.enrolled()).isEqualTo(0);
    }

    @Test
    void aStudentsCurrentClassesAreTheOnesWithAnActiveEnrollment() {
        Student student = student();
        ClassEntity earlier = clazz("Roster earlier");
        ClassEntity left = clazz("Roster left");
        enrollment(student, earlier, EnrollmentStatus.COMPLETED);
        enrollment(student, left, EnrollmentStatus.WITHDRAWN);
        enrollment(student, classB, EnrollmentStatus.TRANSFERRED);
        enrollment(student, classA, EnrollmentStatus.WITHDRAWN);
        enrollment(student, classA, EnrollmentStatus.ACTIVE);

        var current = classService.getClassesByStudentId(student.getId(), PageRequest.of(0, 10));

        assertThat(current.getContent()).extracting(ClassDto::id).containsExactly(classA.getId());
        assertThat(current.getTotalElements()).isEqualTo(1);
    }

    @Test
    void aTransferMovesTheStudentBetweenRosters() {
        Student student = student();
        Enrollment source = enrollment(student, classA, EnrollmentStatus.ACTIVE);
        assertThat(roster(classA)).containsExactly(student.getId());
        assertThat(roster(classB)).isEmpty();

        enrollmentService.transferStudent(source.getId(), classB.getId());

        assertThat(roster(classA)).isEmpty();
        assertThat(roster(classB)).containsExactly(student.getId());
        assertThat(classService.getClassesByStudentId(student.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(ClassDto::id).containsExactly(classB.getId());
    }

    @Test
    void withdrawingOrCompletingRemovesTheStudentWithoutDeletingHistory() {
        Student withdrawn = student();
        Student completed = student();
        Student staying = student();
        Enrollment toWithdraw = enrollment(withdrawn, classA, EnrollmentStatus.ACTIVE);
        Enrollment toComplete = enrollment(completed, classA, EnrollmentStatus.ACTIVE);
        enrollment(staying, classA, EnrollmentStatus.ACTIVE);
        assertThat(roster(classA)).hasSize(3);

        enrollmentService.withdrawEnrollment(toWithdraw.getId(), "Left the school");
        assertThat(roster(classA)).containsExactlyInAnyOrder(completed.getId(), staying.getId());
        enrollmentService.updateEnrollmentStatus(toComplete.getId(), EnrollmentStatus.COMPLETED);

        assertThat(roster(classA)).containsExactly(staying.getId());
        assertThat(classService.listCards(query(0, 10)).getContent())
                .filteredOn(c -> c.id().equals(classA.getId())).singleElement()
                .extracting(ClassCardDto::studentCount).isEqualTo(1);
        assertThat(enrollments.findById(toWithdraw.getId())).isPresent();
        assertThat(enrollments.findById(toComplete.getId())).isPresent();
    }

    @Test
    void aStudentReEnrolledInTheSameClassAppearsOnceOnTheRoster() {
        Student student = student();
        enrollment(student, classA, EnrollmentStatus.WITHDRAWN);
        enrollment(student, classA, EnrollmentStatus.TRANSFERRED);
        enrollment(student, classA, EnrollmentStatus.ACTIVE);

        assertThat(roster(classA)).containsExactly(student.getId());
        assertThat(enrollments.findActiveStudentIdsByClassId(classA.getId())).containsExactly(student.getId());
        assertThat(enrollments.countActiveByClassId(classA.getId())).isEqualTo(1);
        assertThat(classService.listCards(query(0, 10)).getContent())
                .filteredOn(c -> c.id().equals(classA.getId())).singleElement()
                .extracting(ClassCardDto::studentCount).isEqualTo(1);
    }

    @Test
    void classPaginationKeepsItsTotalsAndEachPageGetsItsOwnRosters() {
        List<ClassEntity> created = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ClassEntity c = clazz("Page " + i);
            created.add(c);
            enrollment(student(), c, EnrollmentStatus.ACTIVE);
            enrollment(student(), c, EnrollmentStatus.WITHDRAWN);
        }
        QueryParams qp = query(1, 2);
        qp.setSort(List.of(Sort.Order.asc("name")));

        var page = classService.listClasses(qp);

        // The school holds the seven classes of this test; a page of two keeps the full total.
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allSatisfy(c -> assertThat(c.studentIds()).hasSize(
                created.stream().anyMatch(p -> p.getId().equals(c.id())) ? 1 : 0));
        var cards = classService.listCards(qp).getContent();
        assertThat(cards).hasSize(2);
        assertThat(cards).allSatisfy(c -> assertThat(c.studentCount()).isEqualTo(
                created.stream().anyMatch(p -> p.getId().equals(c.id())) ? 1 : 0));
    }

    // ---- fixtures -------------------------------------------------------------------------------------------

    private Set<Long> roster(ClassEntity clazz) {
        em.flush();
        return new HashSet<>(classService.get(clazz.getId()).studentIds());
    }

    private QueryParams query(int page, int size) {
        QueryParams qp = new QueryParams();
        qp.setPage(page);
        qp.setSize(size);
        return qp;
    }

    private AcademicYear year() {
        AcademicYear created = new AcademicYear();
        created.setSchool(school);
        created.setName("Year " + UUID.randomUUID());
        created.setStartDate(LocalDate.of(2026, 9, 1));
        created.setEndDate(LocalDate.of(2027, 6, 30));
        created.setActive(true);
        return years.saveAndFlush(created);
    }

    private ClassEntity clazz(String name) {
        ClassEntity created = new ClassEntity();
        created.setAcademicYear(year);
        created.setName(name);
        created.getTeachers().add(teacher);
        return classes.saveAndFlush(created);
    }

    private Student student() {
        Student created = new Student();
        created.setRole(UserRole.STUDENT);
        created.setEmail("roster-" + UUID.randomUUID() + "@fixtures.school.test");
        created.setFirstName("Rosa");
        created.setLastName("Roster");
        created.setPassword("not-a-real-hash");
        created.setStatus(Status.ACTIVE);
        created.setIsEmailVerified(true);
        created = students.saveAndFlush(created);
        SchoolMembership membership = new SchoolMembership();
        membership.setUser(created);
        membership.setSchool(school);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setRoles(new HashSet<>(Set.of(MembershipRole.STUDENT)));
        memberships.saveAndFlush(membership);
        return created;
    }

    private Enrollment enrollment(Student student, ClassEntity clazz, EnrollmentStatus status) {
        Enrollment created = new Enrollment();
        created.setStudent(student);
        created.setClassEntity(clazz);
        created.setStatus(status);
        return enrollments.saveAndFlush(created);
    }
}
