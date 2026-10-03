package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.operational.entity.*;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.example.school_management.feature.operational.entity.enums.RoomType;
import com.example.school_management.feature.operational.repository.TimetableRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.operational.service.TimetableService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

import com.example.school_management.feature.operational.service.TimetableExportService;
import com.example.school_management.feature.operational.service.TimetableOptimizationService;
import com.example.school_management.feature.operational.service.impl.SmartTimetableServiceImpl;
import com.example.school_management.feature.operational.dto.TimetableExportRequest;
import com.example.school_management.feature.operational.dto.TimetableOptimizationRequest;
import com.example.school_management.feature.operational.repository.*;
import com.example.school_management.feature.academic.repository.*;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import static org.mockito.Mockito.mock;

@IntegrationTest
@Transactional
class TimetableWorkflowSchoolIntegrationTest {
    @Autowired EntityManager em;
    @Autowired TimetableRepository timetables;
    @Autowired TimetableSlotRepository slots;
    @Autowired TeachingAssignmentRepository assignments;
    @Autowired TeacherRepository teachers;
    @Autowired RoomRepository rooms;
    @Autowired PeriodRepository periods;
    @Autowired ClassRepository classes;
    @Autowired CourseRepository courses;
    @Autowired TimetableExportService exports;
    @Autowired TimetableService timetableService;
    @Autowired com.example.school_management.feature.operational.service.TimetableSlotService slotService;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    private School ownSchool, foreignSchool;
    private ClassEntity ownClass, foreignClass;
    private Course ownCourse, foreignCourse;
    private Teacher teacher, foreignTeacher;
    private Period ownPeriod, foreignPeriod;
    private Room ownRoom, foreignRoom;
    private Timetable ownTimetable, foreignTimetable;
    private TimetableSlot ownSlot, foreignSlot;

    @BeforeEach
    void setUp() {
        ownSchool = school("Schedule current"); foreignSchool = school("Schedule foreign");
        doReturn(ownSchool).when(currentSchool).resolve();
        ownClass = clazz(ownSchool, "Current class"); foreignClass = clazz(foreignSchool, "Foreign class");
        ownCourse = course(ownSchool); foreignCourse = course(foreignSchool);
        ownClass.getCourses().add(ownCourse); foreignClass.getCourses().add(foreignCourse);
        teacher = teacher(); foreignTeacher = teacher();
        membership(teacher, ownSchool); membership(teacher, foreignSchool); membership(foreignTeacher, foreignSchool);
        ownPeriod = period(ownSchool); foreignPeriod = period(foreignSchool);
        ownRoom = room(ownSchool); foreignRoom = room(foreignSchool);
        ownTimetable = timetable(ownSchool, ownClass, ownRoom); foreignTimetable = timetable(foreignSchool, foreignClass, foreignRoom);
        ownSlot = slot(ownTimetable, ownClass, ownCourse, ownPeriod, ownRoom);
        foreignSlot = slot(foreignTimetable, foreignClass, foreignCourse, foreignPeriod, foreignRoom);
        em.flush();
    }


    private SmartTimetableServiceImpl smart() {
        return new SmartTimetableServiceImpl(mock(TimetableOptimizationService.class), timetableService, currentSchool, slots, assignments, teachers, rooms);
    }
    private TimetableOptimizationService optimizer() {
        return new TimetableOptimizationService(mock(org.optaplanner.core.api.solver.SolverManager.class), timetableService, currentSchool, slots, classes, courses, teachers, rooms, periods);
    }
    @ParameterizedTest
    @ValueSource(strings = {"PDF", "EXCEL", "CSV"})
    void foreignTimetableCannotBeExportedOrPreviewed(String format) {
        TimetableExportRequest request = new TimetableExportRequest(); request.setFormat(format);
        assertThatThrownBy(() -> exports.exportTimetable(foreignTimetable.getId(), request)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> exports.previewExport(foreignTimetable.getId(), format)).isInstanceOf(ResourceNotFoundException.class);
    }
    @ParameterizedTest
    @ValueSource(strings = {"teacher", "class", "course", "room"})
    void exportRejectsForeignFilterResourcesInEveryFormat(String field) {
        for (String format : List.of("PDF", "EXCEL", "CSV")) {
            TimetableExportRequest request = new TimetableExportRequest(); request.setFormat(format);
            switch (field) {
                case "teacher" -> request.setTeacherIds(List.of(foreignTeacher.getId()));
                case "class" -> request.setClassIds(List.of(foreignClass.getId()));
                case "course" -> request.setCourseIds(List.of(foreignCourse.getId()));
                default -> request.setRoomIds(List.of(foreignRoom.getId()));
            }
            assertThatThrownBy(() -> exports.exportTimetable(ownTimetable.getId(), request)).isInstanceOf(ResourceNotFoundException.class);
        }
    }
    @Test
    void smartForeignTimetableGuardsPropagateInsteadOfReturningFailedOptimization() {
        var smart = smart(); Long id = foreignTimetable.getId();
        var request = TimetableOptimizationRequest.builder().timetableId(id).build();
        List<Runnable> actions = List.of(
            () -> smart.optimizeWithAI(request), () -> smart.reoptimizeWithConstraints(id, List.of()),
            () -> smart.balanceTeacherWorkloads(id), () -> smart.detectConflicts(id),
            () -> smart.resolveConflicts(id, List.of()), () -> smart.optimizeRoomUsage(id),
            () -> smart.generateMultipleScenarios(request, 2), () -> smart.predictOptimalSchedule(request),
            () -> smart.validateScheduleChange(id, ownSlot.getId(), null, null),
            () -> smart.applyScheduleChange(id, ownSlot.getId(), null, null),
            () -> optimizer().getCurrentSolution(id));
        for (Runnable action : actions) assertThatThrownBy(action::run).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void smartValidatesSlotParentAndReplacementResourcesBeforeMutation() {
        var smart = smart(); Long id = ownTimetable.getId();
        assertThatThrownBy(() -> smart.validateScheduleChange(id, foreignSlot.getId(), null, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> smart.applyScheduleChange(id, ownSlot.getId(), foreignTeacher.getId(), null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> smart.applyScheduleChange(id, ownSlot.getId(), null, foreignRoom.getId())).isInstanceOf(ResourceNotFoundException.class);
        Timetable other = timetable(ownSchool, ownClass, ownRoom); em.flush();
        assertThatThrownBy(() -> smart.validateScheduleChange(other.getId(), ownSlot.getId(), null, null))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(e -> assertThat(((org.springframework.web.server.ResponseStatusException)e).getStatusCode().value()).isEqualTo(409));
        assertThat(ownSlot.getTeacher()).isEqualTo(teacher); assertThat(ownSlot.getRoom()).isEqualTo(ownRoom);
    }
    @Test
    void smartTeacherWorkloadsUseOnlyCurrentSchoolAssignmentsAndSlots() {
        assignment(ownClass, ownCourse); assignment(foreignClass, foreignCourse); em.flush();
        var smart = smart(); var analysis = smart.analyzeTeacherWorkload(teacher.getId());
        assertThat(analysis.getTotalWeeklyHours()).isEqualTo(1);
        assertThat(analysis.getTotalCourses()).isEqualTo(1);
        assertThat(analysis.getCourseWorkloads()).extracting(w -> w.getCourseId()).containsExactly(ownCourse.getId());
        assertThat(smart.analyzeAllTeacherWorkloads()).extracting(w -> w.getTeacherId()).containsExactly(teacher.getId());
        assertThatThrownBy(() -> smart.analyzeTeacherWorkload(foreignTeacher.getId())).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void optimizationCandidatePoolsAndExportDataAreSchoolScoped() {
        var problem = optimizer().getCurrentSolution(ownTimetable.getId());
        assertThat(problem.getPeriods()).extracting(Period::getId).containsExactly(ownPeriod.getId());
        assertThat(problem.getRooms()).extracting(Room::getId).containsExactly(ownRoom.getId());
        assertThat(problem.getTeachers()).extracting(Teacher::getId).containsExactly(teacher.getId());
        assertThat(problem.getClasses()).extracting(ClassEntity::getId).containsExactly(ownClass.getId());
        assertThat(problem.getCourses()).extracting(Course::getId).containsExactly(ownCourse.getId());
        TimetableExportRequest request = new TimetableExportRequest(); request.setFormat("CSV");
        String csv = new String(exports.exportTimetable(ownTimetable.getId(), request), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains(ownClass.getName()).doesNotContain(foreignClass.getName(), foreignRoom.getName(), foreignCourse.getName());
    }
    @ParameterizedTest
    @ValueSource(strings = {"period", "class", "course", "teacher", "room"})
    void optimizedSolutionPrevalidatesEveryDetachedReferenceBeforeDeletingSlots(String field) {
        var lesson = new com.example.school_management.feature.operational.domain.TimetableLesson(1L, "Course", 1);
        lesson.setDay(DayOfWeek.MONDAY);
        lesson.setPeriod(field.equals("period") ? foreignPeriod : ownPeriod);
        lesson.setClassEntity(field.equals("class") ? foreignClass : ownClass);
        lesson.setCourse(field.equals("course") ? foreignCourse : ownCourse);
        lesson.setTeacher(field.equals("teacher") ? foreignTeacher : teacher);
        lesson.setRoom(field.equals("room") ? foreignRoom : ownRoom);
        var solution = new com.example.school_management.feature.operational.domain.TimetableSolution();
        solution.setLessons(List.of(lesson));
        assertThatThrownBy(() -> optimizer().saveOptimizedSolution(ownTimetable, solution)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(slots.existsById(ownSlot.getId())).isTrue();
        assertThat(slots.existsById(foreignSlot.getId())).isTrue();
    }

    @Test
    void optimizedDetachedResourcesAreReplacedWithCanonicalSchoolEntities() {
        var lesson = new com.example.school_management.feature.operational.domain.TimetableLesson(1L, "Optimized", 1);
        lesson.setDay(DayOfWeek.MONDAY);
        Period period = new Period(); period.setId(ownPeriod.getId()); lesson.setPeriod(period);
        Room room = new Room(); room.setId(ownRoom.getId()); room.setName("Forged room"); lesson.setRoom(room);
        Teacher t = new Teacher(); t.setId(teacher.getId()); lesson.setTeacher(t);
        ClassEntity clazz = new ClassEntity(); clazz.setId(ownClass.getId()); clazz.setName("Forged class"); lesson.setClassEntity(clazz);
        Course course = new Course(); course.setId(ownCourse.getId()); course.setName("Forged course"); lesson.setCourse(course);
        var solution = new com.example.school_management.feature.operational.domain.TimetableSolution(); solution.setLessons(List.of(lesson));
        Long timetableId = ownTimetable.getId(), originalSlotId = ownSlot.getId();
        optimizer().saveOptimizedSolution(ownTimetable, solution); em.flush(); em.clear();
        assertThat(slots.existsById(originalSlotId)).isFalse();
        var saved = slots.findByTimetableIdAndPeriodSchoolId(timetableId, ownSchool.getId());
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getForClass().getName()).isEqualTo("Current class");
        assertThat(saved.get(0).getRoom().getName()).isEqualTo(ownRoom.getName());
        assertThat(saved.get(0).getForCourse().getName()).isEqualTo(ownCourse.getName());
        assertThat(saved.get(0).getTimetable().getSchool().getId()).isEqualTo(ownSchool.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher", "room"})
    void optimizedSolutionRejectsForeignNestedCanonicalResourcesBeforeDeletingSlots(String kind) {
        ClassEntity clazz = clazz(ownSchool, "Optimized candidate");
        Course course = course(ownSchool);
        if (kind.equals("teacher")) course.setTeacher(foreignTeacher);
        else clazz.setAssignedRoom(foreignRoom);
        em.flush();
        var lesson = new com.example.school_management.feature.operational.domain.TimetableLesson(1L, "Course", 1);
        lesson.setDay(DayOfWeek.MONDAY); lesson.setPeriod(ownPeriod); lesson.setRoom(ownRoom);
        lesson.setTeacher(teacher); lesson.setClassEntity(clazz); lesson.setCourse(course);
        var solution = new com.example.school_management.feature.operational.domain.TimetableSolution();
        solution.setLessons(List.of(lesson));
        assertContextConflict(() -> optimizer().saveOptimizedSolution(ownTimetable, solution));
        assertThat(slots.existsById(ownSlot.getId())).isTrue();
        assertThat(slots.existsById(foreignSlot.getId())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher", "room"})
    void predictionRejectsForeignNestedCandidateResources(String kind) {
        if (kind.equals("teacher")) course(ownSchool).setTeacher(foreignTeacher);
        else clazz(ownSchool, "Unscheduled candidate").setAssignedRoom(foreignRoom);
        em.flush();
        assertContextConflict(() -> optimizer().getCurrentSolution(ownTimetable.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher", "room"})
    void rawTimetableAndSlotResponsesRejectForeignNestedResources(String kind) {
        if (kind.equals("teacher")) ownCourse.setTeacher(foreignTeacher);
        else ownClass.setAssignedRoom(foreignRoom);
        em.flush();
        em.clear();
        assertContextConflict(() -> timetableService.get(ownTimetable.getId()));
        assertContextConflict(() -> slotService.getSlot(ownSlot.getId()));
    }

    private void assertContextConflict(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .satisfies(e -> assertThat(((org.springframework.web.server.ResponseStatusException)e).getStatusCode().value()).isEqualTo(409));
    }

    private void assignment(ClassEntity clazz, Course course) {
        var a = new com.example.school_management.feature.academic.entity.TeachingAssignment();
        a.setClazz(clazz); a.setCourse(course); a.setTeacher(teacher); a.setWeeklyHours(2); em.persist(a);
    }
    private School school(String name) { School s = new School(); s.setName(name); em.persist(s); return s; }
    private ClassEntity clazz(School school, String name) {
        AcademicYear y = new AcademicYear(); y.setSchool(school); y.setName(name + " year");
        y.setStartDate(LocalDate.of(2030,9,1)); y.setEndDate(LocalDate.of(2031,6,30)); em.persist(y);
        ClassEntity c = new ClassEntity(); c.setName(name); c.setAcademicYear(y); em.persist(c); return c;
    }
    private Course course(School school) { Course c = new Course(); c.setSchool(school); c.setName("Course " + school.getName()); c.setCode(UUID.randomUUID().toString().substring(0, 8)); em.persist(c); return c; }
    private Teacher teacher() { Teacher t = new Teacher(); t.setEmail(UUID.randomUUID() + "@schedule.test"); t.setFirstName("Schedule"); t.setLastName("Teacher"); t.setRole(UserRole.TEACHER); t.setPassword("unused"); t.setStatus(Status.ACTIVE); t.setIsEmailVerified(true); em.persist(t); return t; }
    private void membership(Teacher teacher, School school) { SchoolMembership m = new SchoolMembership(); m.setSchool(school); m.setUser(teacher); m.setStatus(MembershipStatus.ACTIVE); m.setRoles(Set.of(MembershipRole.TEACHER)); em.persist(m); }
    private Period period(School school) { Period p = new Period(); p.setSchool(school); p.setIndex(1); p.setStartTime(LocalTime.of(8,0)); p.setEndTime(LocalTime.of(9,0)); em.persist(p); return p; }
    private Room room(School school) { Room r = new Room(); r.setSchool(school); r.setName("Room " + school.getName()); r.setCapacity(30); r.setRoomType(RoomType.CLASSROOM); em.persist(r); return r; }
    private Timetable timetable(School school, ClassEntity clazz, Room room) {
        Timetable t = new Timetable(); t.setName("Matching timetable"); t.setAcademicYear("2030-2031"); t.setSemester("Fall");
        t.getClasses().add(clazz); t.getRooms().add(room); t.getTeachers().add(teacher);
        t.setSchool(school);
        em.persist(t);
        return t;
    }
    private TimetableSlot slot(Timetable timetable, ClassEntity clazz, Course course, Period period, Room room) {
        TimetableSlot s = new TimetableSlot(); s.setTimetable(timetable); s.setForClass(clazz); s.setForCourse(course);
        s.setPeriod(period); s.setRoom(room); s.setTeacher(teacher); s.setDayOfWeek(DayOfWeek.MONDAY); em.persist(s); return s;
    }
}
