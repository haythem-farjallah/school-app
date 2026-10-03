package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.membership.entity.*;
import com.example.school_management.feature.operational.entity.*;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional
class TeacherClassSchoolAccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    private School ownSchool, foreignSchool;
    private Teacher teacher;
    private SchoolMembership ownMembership;
    private ClassEntity ownAssignmentClass, ownDirectClass, ownSlotClass, foreignClass;
    private Course ownCourse, foreignCourse;
    private int periodIndex;

    @BeforeEach
    void setUp() {
        ownSchool = school("TeacherClass current"); foreignSchool = school("TeacherClass foreign");
        doReturn(ownSchool).when(currentSchool).resolve();
        teacher = teacher();
        ownMembership = membership(teacher, ownSchool); membership(teacher, foreignSchool);
        ownAssignmentClass = clazz(ownSchool, "Own assignment"); ownDirectClass = clazz(ownSchool, "Own direct");
        ownSlotClass = clazz(ownSchool, "Own slot"); foreignClass = clazz(foreignSchool, "Foreign class");
        ownCourse = course(ownSchool); foreignCourse = course(foreignSchool);
        assignment(ownAssignmentClass, ownCourse); assignment(foreignClass, foreignCourse);
        ownDirectClass.getTeachers().add(teacher); ownDirectClass.getCourses().add(ownCourse);
        foreignClass.getTeachers().add(teacher); foreignClass.getCourses().add(foreignCourse);
        slot(ownSchool, ownSlotClass, ownCourse); slot(foreignSchool, foreignClass, foreignCourse);
        grade(enrollment(ownAssignmentClass, EnrollmentStatus.ACTIVE), 70f);
        grade(enrollment(ownDirectClass, EnrollmentStatus.ACTIVE), 90f);
        enrollment(ownDirectClass, EnrollmentStatus.WITHDRAWN);
        grade(enrollment(foreignClass, EnrollmentStatus.ACTIVE), 10f);
        enrollment(foreignClass, EnrollmentStatus.ACTIVE);
        em.flush(); em.clear();
    }

    @Test
    void multiSchoolTeacherOnlyReceivesCurrentSchoolClassesCoursesGradesAndActiveRosters() throws Exception {
        JsonNode all = response(get("/api/v1/teacher/classes/all"), 200);
        assertThat(all).hasSize(3);
        assertThat(ids(all)).containsExactlyInAnyOrder(ownAssignmentClass.getId(), ownDirectClass.getId(), ownSlotClass.getId());
        for (JsonNode clazz : all) {
            assertThat(clazz.path("schedule").asText()).isEmpty();
            for (JsonNode course : clazz.path("courses")) assertThat(course.path("id").asLong()).isEqualTo(ownCourse.getId());
            if (clazz.path("id").asLong() == ownAssignmentClass.getId()) {
                assertThat(clazz.path("enrolled").asInt()).isEqualTo(1);
                assertThat(clazz.path("averageGrade").asDouble()).isEqualTo(70);
            }
            if (clazz.path("id").asLong() == ownDirectClass.getId()) {
                assertThat(clazz.path("enrolled").asInt()).isEqualTo(1);
                assertThat(clazz.path("averageGrade").asDouble()).isEqualTo(90);
            }
        }
        JsonNode stats = response(get("/api/v1/teacher/classes/stats"), 200);
        assertThat(stats.path("totalClasses").asInt()).isEqualTo(3);
        assertThat(stats.path("totalStudents").asInt()).isEqualTo(2);
        assertThat(stats.path("averageGrade").asDouble()).isEqualTo(80);
        for (String route : List.of("/api/v1/teacher/classes", "/api/v1/classes/teacher/me")) {
            JsonNode page = response(get(route).param("size", "20"), 200);
            assertThat(page.path("totalElements").asLong()).isEqualTo(3);
            assertThat(ids(page.path("content"))).doesNotContain(foreignClass.getId());
        }
        assertThat(response(get("/api/v1/teacher/classes/all").param("search", "Foreign"), 200)).isEmpty();
    }

    @Test
    void foreignTeacherAndAccountRoleWithoutTeacherMembershipReturn404() throws Exception {
        doReturn(school("No teacher membership")).when(currentSchool).resolve();
        response(get("/api/v1/teacher/classes/all"), 404);
        doReturn(ownSchool).when(currentSchool).resolve();
        SchoolMembership managed = em.find(SchoolMembership.class, ownMembership.getId());
        managed.setRoles(Set.of(MembershipRole.STUDENT)); em.flush();
        response(get("/api/v1/teacher/classes"), 404);
        response(get("/api/v1/teacher/classes/stats"), 404);
    }

    @Test
    void foreignCourseOnCurrentDirectClassIsSafeConflict() throws Exception {
        em.find(ClassEntity.class, ownDirectClass.getId()).getCourses().add(em.find(Course.class, foreignCourse.getId()));
        em.flush();
        response(get("/api/v1/teacher/classes/all"), 409);
    }

    @Test
    void standaloneSlotWithCurrentSchoolPeriodAndClassStillContributesItsClass() throws Exception {
        ClassEntity standaloneClass = clazz(ownSchool, "Standalone slot class");
        Period period = new Period(); period.setSchool(ownSchool); period.setIndex(++periodIndex);
        period.setStartTime(LocalTime.of(10,0)); period.setEndTime(LocalTime.of(11,0)); em.persist(period);
        TimetableSlot slot = new TimetableSlot(); slot.setForClass(standaloneClass); slot.setPeriod(period);
        slot.setTeacher(em.find(Teacher.class, teacher.getId())); slot.setDayOfWeek(DayOfWeek.TUESDAY); em.persist(slot);
        em.flush();
        assertThat(ids(response(get("/api/v1/teacher/classes/all"), 200))).contains(standaloneClass.getId());
    }

    @Test
    void scopedSlotWithOptionalCourseAndRoomAbsentStillContributesItsClass() throws Exception {
        ClassEntity classWithoutCourse = clazz(ownSchool, "Slot without course");
        slot(ownSchool, classWithoutCourse, null); em.flush();
        assertThat(ids(response(get("/api/v1/teacher/classes/all"), 200))).contains(classWithoutCourse.getId());
    }

    @Test
    void invalidAssignmentAndForeignTimetableCannotContributeCurrentClasses() throws Exception {
        ClassEntity invalidAssignment = clazz(ownSchool, "Invalid assignment context");
        assignment(invalidAssignment, em.find(Course.class, foreignCourse.getId()));
        ClassEntity invalidSlot = clazz(ownSchool, "Invalid slot context");
        slot(foreignSchool, invalidSlot, em.find(Course.class, foreignCourse.getId()));
        em.flush();
        assertThat(ids(response(get("/api/v1/teacher/classes/all"), 200)))
                .doesNotContain(invalidAssignment.getId(), invalidSlot.getId());
    }

    private List<Long> ids(JsonNode values) {
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        values.forEach(value -> ids.add(value.path("id").asLong())); return ids;
    }
    private JsonNode response(MockHttpServletRequestBuilder request, int expected) throws Exception {
        String body = mvc.perform(request.with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data");
    }
    private School school(String name) { School school = new School(); school.setName(name); em.persist(school); return school; }
    private Teacher teacher() {
        Teacher t = new Teacher(); t.setEmail(UUID.randomUUID() + "@teacherclass.test"); t.setFirstName("Class"); t.setLastName("Teacher");
        t.setPassword("unused"); t.setRole(UserRole.TEACHER); t.setStatus(Status.ACTIVE); t.setIsEmailVerified(true); em.persist(t); return t;
    }
    private SchoolMembership membership(Teacher teacher, School school) {
        SchoolMembership m = new SchoolMembership(); m.setSchool(school); m.setUser(teacher); m.setRoles(Set.of(MembershipRole.TEACHER));
        m.setStatus(MembershipStatus.ACTIVE); em.persist(m); return m;
    }
    private ClassEntity clazz(School school, String name) {
        AcademicYear y = new AcademicYear(); y.setSchool(school); y.setName(name + " year"); y.setStartDate(LocalDate.of(2034,9,1)); y.setEndDate(LocalDate.of(2035,6,30)); em.persist(y);
        ClassEntity c = new ClassEntity(); c.setAcademicYear(y); c.setName(name); c.setGradeLevel("First Year"); em.persist(c); return c;
    }
    private Course course(School school) { Course c = new Course(); c.setSchool(school); c.setName("Course " + school.getName()); c.setCode(UUID.randomUUID().toString().substring(0,8)); em.persist(c); return c; }
    private void assignment(ClassEntity clazz, Course course) {
        TeachingAssignment a = new TeachingAssignment(); a.setClazz(clazz); a.setCourse(course); a.setTeacher(em.find(Teacher.class, teacher.getId())); a.setWeeklyHours(2); em.persist(a);
    }
    private Enrollment enrollment(ClassEntity clazz, EnrollmentStatus status) {
        Student s = new Student(); s.setEmail(UUID.randomUUID() + "@teacherclass.test"); s.setPassword("unused"); s.setFirstName("Roster"); s.setLastName("Student"); s.setRole(UserRole.STUDENT); s.setStatus(Status.ACTIVE); s.setIsEmailVerified(true); em.persist(s);
        Enrollment e = new Enrollment(); e.setClassEntity(clazz); e.setStudent(s); e.setStatus(status); em.persist(e); return e;
    }
    private void grade(Enrollment enrollment, float score) { Grade g = new Grade(); g.setEnrollment(enrollment); g.setAssignedBy(em.find(Teacher.class, teacher.getId())); g.setScore(score); g.setContent("Exam"); em.persist(g); }
    private void slot(School school, ClassEntity clazz, Course course) {
        Period p = new Period(); p.setSchool(school); p.setIndex(++periodIndex); p.setStartTime(LocalTime.of(8,0)); p.setEndTime(LocalTime.of(9,0)); em.persist(p);
        Timetable t = new Timetable(); t.setSchool(school); t.setName("Class timetable"); t.setAcademicYear("2034-2035"); t.setSemester("Fall"); em.persist(t);
        TimetableSlot slot = new TimetableSlot(); slot.setTimetable(t); slot.setForClass(clazz); slot.setForCourse(course); slot.setTeacher(em.find(Teacher.class, teacher.getId())); slot.setPeriod(p); slot.setDayOfWeek(DayOfWeek.MONDAY); em.persist(slot);
    }
}
