package com.example.school_management.dev;

import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.membership.service.SchoolMembershipProvisioningService;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.entity.enums.RoomType;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Opt-in local demo, persisted once in one transaction. Existing reserved identities
 * prevent reseeding, including after local edits or deletion of individual demo rows.
 */
@Component
@Profile("dev & !prod & !production")
@ConditionalOnProperty(name = "school.dev.timetable-fixtures.enabled", havingValue = "true")
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DevTimetableFixtureLoader implements ApplicationRunner {
    public static final String EMPTY_STUDENT_EMAIL = "empty.student@fixtures.school.test";
    private static final String DEMO_YEAR = "Development demo year";
    private static final List<String> COURSE_CODES = List.of("DEMO-MATH", "DEMO-PHYS", "DEMO-ART", "DEMO-HIST");

    private final EntityManager em;
    private final CurrentSchoolResolver schools;
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final SchoolMembershipRepository memberships;
    private final SchoolMembershipProvisioningService membershipProvisioner;
    private final PasswordEncoder passwords;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        School school = schools.resolve();
        if (reservedDataExists(school)) {
            log.warn("Development timetable demo skipped: reserved demo data already exists; existing records are unchanged");
            return;
        }
        Student student = students.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElse(null);
        Teacher teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElse(null);
        if (!isFixtureAccount(student, school, UserRole.STUDENT, MembershipRole.STUDENT, "Sam", "Student")
                || !isFixtureAccount(teacher, school, UserRole.TEACHER, MembershipRole.TEACHER, "Theo", "Teacher")) {
            log.warn("Development timetable demo skipped: fixture accounts or memberships are absent or changed");
            return;
        }
        if (em.createQuery("select count(e) from Enrollment e where e.student.id=:id", Long.class)
                .setParameter("id", student.getId()).getSingleResult() != 0) {
            log.warn("Development timetable demo skipped: fixture student already has enrollment history");
            return;
        }
        var activeYears = em.createQuery("select y from AcademicYear y where y.school=:school and y.active=true", AcademicYear.class)
                .setParameter("school", school).getResultList();
        if (activeYears.size() > 1 || (activeYears.isEmpty() && em.createQuery(
                "select count(y) from AcademicYear y where y.school=:school and y.name=:name", Long.class)
                .setParameter("school", school).setParameter("name", DEMO_YEAR).getSingleResult() != 0)) {
            log.warn("Development timetable demo skipped: active academic year configuration is ambiguous or reserved year already exists");
            return;
        }

        AcademicYear year;
        if (activeYears.isEmpty()) {
            year = new AcademicYear(); year.setSchool(school); year.setName(DEMO_YEAR);
            int startYear = LocalDate.now().getYear();
            year.setStartDate(LocalDate.of(startYear, 9, 1)); year.setEndDate(LocalDate.of(startYear + 1, 6, 30));
            year.setActive(true); em.persist(year);
        } else {
            year = activeYears.get(0);
        }
        ClassEntity clazz = new ClassEntity(); clazz.setName("Demo 7A"); clazz.setAcademicYear(year);
        clazz.setGradeLevel("7"); clazz.setSection("A"); clazz.setYearOfStudy(7); clazz.setMaxStudents(30);
        em.persist(clazz);
        Enrollment enrollment = new Enrollment(); enrollment.setStudent(student); enrollment.setClassEntity(clazz);
        enrollment.setStatus(EnrollmentStatus.ACTIVE); em.persist(enrollment);

        Course maths = course(school, "DEMO-MATH", "Mathematics", "#2563EB", clazz, teacher);
        Course physics = course(school, "DEMO-PHYS", "Physics", "#7C3AED", clazz, teacher);
        Course art = course(school, "DEMO-ART", "Art", "#DB2777", clazz, teacher);
        Course history = course(school, "DEMO-HIST", "History", "#D97706", clazz, teacher);
        Room classroom = room(school, "Demo Room 101", RoomType.CLASSROOM);
        Room lab = room(school, "Demo Lab 201", RoomType.LABORATORY);
        Period first = period(school, 101, "08:15", "09:00");
        Period second = period(school, 102, "09:15", "10:00");
        Timetable timetable = new Timetable(); timetable.setSchool(school); timetable.setName("Development timetable demo");
        timetable.setDescription("Local development timetable fixture");
        // Legacy timetable metadata is limited to 20 characters; the class retains the canonical year link.
        timetable.setAcademicYear(year.getStartDate().getYear() + "-" + year.getEndDate().getYear());
        timetable.setSemester("FIRST");
        timetable.getClasses().add(clazz);
        timetable.getTeachers().add(teacher);
        timetable.getRooms().add(classroom);
        timetable.getRooms().add(lab);
        em.persist(timetable);
        slot(timetable, clazz, teacher, DayOfWeek.MONDAY, first, physics, lab);
        slot(timetable, clazz, teacher, DayOfWeek.MONDAY, second, maths, classroom);
        slot(timetable, clazz, teacher, DayOfWeek.SATURDAY, second, art, classroom);
        slot(timetable, clazz, teacher, DayOfWeek.SUNDAY, second, history, classroom);
        createEmptyStudentIfAbsent();
        em.flush();
        log.info("Development timetable demo created with four persisted lessons");
    }

    private boolean reservedDataExists(School school) {
        return em.createQuery("select count(c) from ClassEntity c where c.academicYear.school=:school and lower(c.name)='demo 7a'", Long.class)
                .setParameter("school", school).getSingleResult() != 0
                || em.createQuery("select count(c) from Course c where c.school=:school and c.code in :codes", Long.class)
                .setParameter("school", school).setParameter("codes", COURSE_CODES).getSingleResult() != 0
                || em.createQuery("select count(r) from Room r where r.school=:school and r.name in ('Demo Room 101','Demo Lab 201')", Long.class)
                .setParameter("school", school).getSingleResult() != 0
                || em.createQuery("select count(p) from Period p where p.school=:school and p.index in (101,102)", Long.class)
                .setParameter("school", school).getSingleResult() != 0
                || em.createQuery("select count(t) from Timetable t where t.school=:school and t.name='Development timetable demo'", Long.class)
                .setParameter("school", school).getSingleResult() != 0;
    }

    private boolean isFixtureAccount(BaseUser account, School school, UserRole role, MembershipRole membershipRole,
                                     String firstName, String lastName) {
        return account != null && account.getRole() == role && account.getStatus() == Status.ACTIVE
                && firstName.equals(account.getFirstName()) && lastName.equals(account.getLastName())
                && Boolean.TRUE.equals(account.getIsEmailVerified()) && !account.isPasswordChangeRequired()
                && passwords.matches(DevFixtureLoader.PASSWORD, account.getPassword())
                && memberships.findByUserIdAndSchoolId(account.getId(), school.getId())
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE && m.getRoles().contains(membershipRole)).isPresent();
    }

    private Course course(School school, String code, String name, String color, ClassEntity clazz, Teacher teacher) {
        Course course = new Course(); course.setSchool(school); course.setCode(code); course.setName(name); course.setColor(color);
        em.persist(course);
        TeachingAssignment assignment = new TeachingAssignment(); assignment.setClazz(clazz); assignment.setCourse(course);
        assignment.setTeacher(teacher); assignment.setWeeklyHours(1); em.persist(assignment);
        return course;
    }

    private Room room(School school, String name, RoomType type) {
        Room room = new Room(); room.setSchool(school); room.setName(name); room.setRoomType(type); room.setCapacity(30);
        em.persist(room); return room;
    }

    private Period period(School school, int index, String start, String end) {
        Period period = new Period(); period.setSchool(school); period.setIndex(index);
        period.setStartTime(LocalTime.parse(start)); period.setEndTime(LocalTime.parse(end)); em.persist(period); return period;
    }

    private void slot(Timetable timetable, ClassEntity clazz, Teacher teacher, DayOfWeek day,
                      Period period, Course course, Room room) {
        TimetableSlot slot = new TimetableSlot(); slot.setTimetable(timetable); slot.setForClass(clazz); slot.setTeacher(teacher);
        slot.setDayOfWeek(day); slot.setPeriod(period); slot.setForCourse(course); slot.setRoom(room); em.persist(slot);
    }

    private void createEmptyStudentIfAbsent() {
        // Include soft-deleted/reserved accounts in this check; never replace or reconcile them.
        if (((Number) em.createNativeQuery("select count(*) from users where email=:email")
                .setParameter("email", EMPTY_STUDENT_EMAIL).getSingleResult()).longValue() != 0) {
            return;
        }
        Student empty = new Student(); empty.setEmail(EMPTY_STUDENT_EMAIL); empty.setFirstName("Emmy"); empty.setLastName("Empty");
        empty.setRole(UserRole.STUDENT); empty.setStatus(Status.ACTIVE); empty.setIsEmailVerified(true);
        empty.setPasswordChangeRequired(false); empty.setPassword(passwords.encode(DevFixtureLoader.PASSWORD));
        em.persist(empty); membershipProvisioner.provisionFor(empty);
    }
}
