package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional
class StudentTimetableIntegrationTest {
    private static final String BASE = "/api/v1/timetables";
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    // As in TimetableSchoolAccessIntegrationTest, select one school while storing foreign-school rows.
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    @MockitoSpyBean AcademicYearRepository academicYears;

    private School school, foreignSchool;
    private AcademicYear year;
    private Student studentA, studentB;
    private SchoolMembership membershipA;
    private Teacher teacher;
    private ClassEntity classA, classB;
    private Course course;
    private Period period;
    private Room room;
    private Enrollment enrollmentA;
    private TimetableSlot slotA, slotB;

    @BeforeEach
    void setUp() {
        school = school("Student timetable school");
        foreignSchool = school("Foreign timetable school");
        doReturn(school).when(currentSchool).resolve();
        year = year(school, "2030-2031", true);
        studentA = student();
        studentB = student();
        membershipA = membership(studentA, school, MembershipRole.STUDENT);
        membership(studentB, school, MembershipRole.STUDENT);
        teacher = new Teacher();
        account(teacher, UserRole.TEACHER, "Timetable", "Teacher");
        membership(teacher, school, MembershipRole.TEACHER);
        classA = clazz(year, "Class A");
        classB = clazz(year, "Class B");
        enrollmentA = enroll(studentA, classA, EnrollmentStatus.ACTIVE);
        enroll(studentB, classB, EnrollmentStatus.ACTIVE);
        course = new Course(); course.setSchool(school); course.setName("Mathematics");
        course.setCode("MATH"); course.setColor("#123456"); em.persist(course);
        period = period(school);
        room = new Room(); room.setSchool(school); room.setName("Room A");
        room.setCapacity(28); room.setRoomType(RoomType.CLASSROOM); em.persist(room);
        slotA = slot(classA, period);
        slotB = slot(classB, period);
        em.flush();
    }

    @Test
    void readsOnlyOwnCurrentClassWithFlattenedPersistedData() throws Exception {
        JsonNode data = ownTimetable();
        assertOnlySlot(data, slotA.getId());
        JsonNode slot = data.path("slots").get(0);
        assertThat(slot.path("dayOfWeek").asText()).isEqualTo("MONDAY");
        assertThat(slot.path("description").asText()).isEqualTo("Algebra lesson");
        assertThat(slot.path("periodId").asLong()).isEqualTo(period.getId());
        assertThat(slot.path("periodIndex").asInt()).isEqualTo(2);
        assertThat(slot.path("periodStartTime").asText()).isEqualTo("09:00:00");
        assertThat(slot.path("periodEndTime").asText()).isEqualTo("10:00:00");
        assertThat(slot.path("forClassId").asLong()).isEqualTo(classA.getId());
        assertThat(slot.path("forClassName").asText()).isEqualTo("Class A");
        assertThat(slot.path("forCourseId").asLong()).isEqualTo(course.getId());
        assertThat(slot.path("forCourseName").asText()).isEqualTo("Mathematics");
        assertThat(slot.path("forCourseCode").asText()).isEqualTo("MATH");
        assertThat(slot.path("forCourseColor").asText()).isEqualTo("#123456");
        assertThat(slot.path("teacherId").asLong()).isEqualTo(teacher.getId());
        assertThat(slot.path("teacherFirstName").asText()).isEqualTo("Timetable");
        assertThat(slot.path("teacherLastName").asText()).isEqualTo("Teacher");
        assertThat(slot.path("teacherEmail").asText()).isEqualTo(teacher.getEmail());
        assertThat(slot.path("roomId").asLong()).isEqualTo(room.getId());
        assertThat(slot.path("roomName").asText()).isEqualTo("Room A");
        assertThat(slot.path("roomCapacity").asInt()).isEqualTo(28);
        assertUnknownMetadata(data);
        assertOnlySlot(response(get(BASE + "/me").with(user(studentB.getEmail()).roles("STUDENT")), 200), slotB.getId());
    }

    @Test
    void clientIdentityAndScopeParametersCannotSelectAnotherTimetable() throws Exception {
        assertOnlySlot(response(asStudent(get(BASE + "/me")
                .param("studentId", studentB.getId().toString()).param("classId", classB.getId().toString())
                .param("schoolId", foreignSchool.getId().toString()).param("academicYearId", "0")), 200), slotA.getId());
    }

    @Test
    void oldActiveAndCurrentHistoricalEnrollmentsAreNotSelected() throws Exception {
        ClassEntity oldClass = clazz(year(school, "2029-2030", false), "Old class");
        enroll(studentA, oldClass, EnrollmentStatus.ACTIVE); slot(oldClass, period);
        for (EnrollmentStatus state : List.of(EnrollmentStatus.COMPLETED, EnrollmentStatus.TRANSFERRED, EnrollmentStatus.WITHDRAWN)) {
            ClassEntity historical = clazz(year, state.name());
            enroll(studentA, historical, state); slot(historical, period);
        }
        em.flush();
        assertOnlySlot(ownTimetable(), slotA.getId());
        em.remove(enrollmentA); em.flush();
        assertEmpty(ownTimetable());
    }

    @Test
    void activeClassWithoutSlotsReturnsEmpty() throws Exception {
        em.remove(slotA); em.flush();
        assertEmpty(ownTimetable());
    }

    @Test
    void noEnrollmentReturnsEmpty() throws Exception {
        em.remove(enrollmentA); em.flush();
        assertEmpty(ownTimetable());
    }

    @Test
    void foreignSchoolEnrollmentsAndSlotsNeverAppear() throws Exception {
        ClassEntity foreignClass = clazz(year(foreignSchool, "2030-2031", true), "Foreign class");
        enroll(studentA, foreignClass, EnrollmentStatus.ACTIVE);
        Period foreignPeriod = period(foreignSchool);
        slot(foreignClass, foreignPeriod);
        // Even a legacy slot pointing at our class is excluded when its period belongs to another school.
        slot(classA, foreignPeriod);
        em.flush();
        assertOnlySlot(ownTimetable(), slotA.getId());
        em.remove(enrollmentA); em.flush();
        assertEmpty(ownTimetable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"course", "teacher", "room", "timetable"})
    void invalidNestedSchoolResourcesAreStillRejected(String resource) throws Exception {
        switch (resource) {
            case "course" -> course.setSchool(foreignSchool);
            case "teacher" -> em.remove(em.createQuery("select m from SchoolMembership m where m.user.id = :id", SchoolMembership.class)
                    .setParameter("id", teacher.getId()).getSingleResult());
            case "room" -> room.setSchool(foreignSchool);
            case "timetable" -> {
                Timetable timetable = new Timetable(); timetable.setSchool(foreignSchool);
                timetable.setName("Foreign parent"); timetable.setAcademicYear("2030-2031"); timetable.setSemester("FIRST");
                em.persist(timetable); slotA.setTimetable(timetable);
            }
        }
        em.flush();
        response(asStudent(get(BASE + "/me")), 404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "ambiguous"})
    void academicYearConfigurationErrorsAreNotEmptySuccesses(String configuration) throws Exception {
        if (configuration.equals("missing")) year.setActive(false);
        else {
            // The database prevents duplicate active years; exercise the resolver's defensive branch without altering constraints.
            AcademicYear other = year(school, "2031-2032", false);
            doReturn(List.of(year, other)).when(academicYears).findAllBySchoolIdAndActiveTrue(school.getId());
        }
        em.flush();
        JsonNode problem = problem(asStudent(get(BASE + "/me")), 500);
        assertThat(problem.path("detail").asText()).isEqualTo("UNEXPECTED_ERROR");
        assertThat(problem.has("data")).isFalse();
        em.remove(enrollmentA); em.flush();
        problem(asStudent(get(BASE + "/me")), 500);
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        problem(get(BASE + "/me"), 401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PARENT", "TEACHER", "ADMIN", "STAFF"})
    void otherRolesAreForbidden(String role) throws Exception {
        JsonNode problem = problem(get(BASE + "/me").with(user(DevFixtureLoader.ADMIN_EMAIL).roles(role)), 403);
        assertThat(problem.path("detail").asText()).isEqualTo("ACCESS_DENIED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "non-student", "missing-membership", "wrong-membership-role", "foreign-membership", "wrong-account-role"})
    void forgedStudentAuthorityCannotBypassPersistedStudentRelationship(String invalidIdentity) throws Exception {
        String principal = studentA.getEmail();
        switch (invalidIdentity) {
            case "unknown" -> principal = "unknown@timetable.test";
            case "non-student" -> principal = teacher.getEmail();
            case "missing-membership" -> em.remove(membershipA);
            case "wrong-membership-role" -> membershipA.setRoles(Set.of(MembershipRole.GUARDIAN));
            case "foreign-membership" -> membershipA.setSchool(foreignSchool);
            case "wrong-account-role" -> studentA.setRole(UserRole.PARENT);
        }
        em.flush();
        JsonNode problem = problem(get(BASE + "/me").with(user(principal).roles("STUDENT")), 403);
        assertThat(problem.path("detail").asText()).isEqualTo("ACCESS_DENIED");
        assertThat(problem.has("data")).isFalse();
    }

    @ParameterizedTest
    @EnumSource(MembershipStatus.class)
    void membershipReadStatusConventionIsPreserved(MembershipStatus membershipStatus) throws Exception {
        membershipA.setStatus(membershipStatus); em.flush();
        assertOnlySlot(ownTimetable(), slotA.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEACHER", "ADMIN", "STAFF"})
    void existingClassAndTeacherReadsKeepTheirPermissionsAndData(String role) throws Exception {
        assertOnlySlot(response(get(BASE + "/class/{id}", classA.getId())
                .with(user(DevFixtureLoader.ADMIN_EMAIL).roles(role)), 200), slotA.getId());
        JsonNode teacherSlots = response(get(BASE + "/teacher/{id}", teacher.getId())
                .with(user(DevFixtureLoader.ADMIN_EMAIL).roles(role)), 200);
        assertThat(teacherSlots).hasSize(2);
        assertThat(List.of(teacherSlots.get(0).path("id").asLong(), teacherSlots.get(1).path("id").asLong()))
                .containsExactlyInAnyOrder(slotA.getId(), slotB.getId());
    }

    @Test
    void studentCannotReadArbitraryTimetableClassTeacherOrExports() throws Exception {
        for (String route : List.of(BASE + "/class/" + classB.getId(), BASE + "/teacher/" + teacher.getId(),
                BASE + "/1", BASE, BASE + "/1/export/pdf", BASE + "/class/" + classB.getId() + "/export/pdf",
                BASE + "/teacher/" + teacher.getId() + "/export/pdf")) {
            problem(asStudent(get(route)), 403);
        }
        problem(asStudent(get(BASE + "/1/export/preview").param("format", "PDF")), 403);
        problem(asStudent(post(BASE + "/1/export").contentType(MediaType.APPLICATION_JSON)
                .content("{\"format\":\"PDF\"}")), 403);
    }

    @Test
    void studentCannotWriteOrOptimizeAndSlotsRemainUnchanged() throws Exception {
        String payload = "{\"name\":\"Forbidden\",\"academicYear\":\"2030-2031\",\"semester\":\"FIRST\","
                + "\"dayOfWeek\":\"MONDAY\",\"periodId\":" + period.getId() + ",\"forClassId\":" + classA.getId() + "}";
        for (MockHttpServletRequestBuilder request : List.of(post(BASE), post(BASE + "/slots"),
                put(BASE + "/slots/{id}", slotA.getId()))) {
            problem(asStudent(request.contentType(MediaType.APPLICATION_JSON).content(payload)), 403);
        }
        problem(asStudent(put(BASE + "/class/{id}/slots", classA.getId())
                .contentType(MediaType.APPLICATION_JSON).content("[]")), 403);
        for (MockHttpServletRequestBuilder request : List.of(delete(BASE + "/1"), delete(BASE + "/slots/{id}", slotA.getId()),
                post(BASE + "/1/optimize"), post(BASE + "/class/{id}/optimize", classA.getId()))) {
            problem(asStudent(request), 403);
        }
        assertOnlySlot(ownTimetable(), slotA.getId());
    }

    private JsonNode ownTimetable() throws Exception { return response(asStudent(get(BASE + "/me")), 200); }
    private MockHttpServletRequestBuilder asStudent(MockHttpServletRequestBuilder request) {
        return request.with(user(studentA.getEmail()).roles("STUDENT"));
    }
    private JsonNode response(MockHttpServletRequestBuilder request, int expected) throws Exception {
        return json.readTree(mvc.perform(request).andExpect(status().is(expected)).andReturn()
                .getResponse().getContentAsString()).path("data");
    }
    private JsonNode problem(MockHttpServletRequestBuilder request, int expected) throws Exception {
        var result = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse();
        assertThat(result.getContentType()).startsWith("application/problem+json");
        JsonNode problem = json.readTree(result.getContentAsString());
        assertThat(problem.path("status").asInt()).isEqualTo(expected);
        return problem;
    }
    private void assertOnlySlot(JsonNode data, Long slotId) {
        assertThat(data.path("slots")).hasSize(1);
        assertThat(data.path("slots").get(0).path("id").asLong()).isEqualTo(slotId);
    }
    private void assertEmpty(JsonNode data) {
        assertThat(data.path("slots").isArray()).isTrue();
        assertThat(data.path("slots")).isEmpty();
        assertUnknownMetadata(data);
    }
    private void assertUnknownMetadata(JsonNode data) {
        for (String field : List.of("id", "name", "description", "academicYear", "semester", "createdAt", "updatedAt")) {
            assertThat(data.path(field).isNull()).as(field).isTrue();
        }
    }
    private School school(String name) { School s = new School(); s.setName(name); em.persist(s); return s; }
    private AcademicYear year(School school, String name, boolean active) {
        AcademicYear y = new AcademicYear(); y.setSchool(school); y.setName(name); y.setActive(active);
        int start = Integer.parseInt(name.substring(0, 4));
        y.setStartDate(LocalDate.of(start, 9, 1)); y.setEndDate(LocalDate.of(start + 1, 6, 30)); em.persist(y); return y;
    }
    private ClassEntity clazz(AcademicYear year, String name) {
        ClassEntity c = new ClassEntity(); c.setAcademicYear(year); c.setName(name); em.persist(c); return c;
    }
    private Student student() { Student s = new Student(); account(s, UserRole.STUDENT, "Student", "Timetable"); return s; }
    private void account(BaseUser account, UserRole role, String firstName, String lastName) {
        account.setEmail(UUID.randomUUID() + "@timetable.test"); account.setRole(role); account.setFirstName(firstName);
        account.setLastName(lastName); account.setPassword("unused"); account.setStatus(Status.ACTIVE);
        account.setIsEmailVerified(true); em.persist(account);
    }
    private SchoolMembership membership(BaseUser user, School school, MembershipRole role) {
        SchoolMembership m = new SchoolMembership(); m.setUser(user); m.setSchool(school);
        m.setRoles(Set.of(role)); m.setStatus(MembershipStatus.ACTIVE); em.persist(m); return m;
    }
    private Enrollment enroll(Student student, ClassEntity clazz, EnrollmentStatus status) {
        Enrollment e = new Enrollment(); e.setStudent(student); e.setClassEntity(clazz); e.setStatus(status); em.persist(e); return e;
    }
    private Period period(School school) {
        Period p = new Period(); p.setSchool(school); p.setIndex(2);
        p.setStartTime(LocalTime.of(9, 0)); p.setEndTime(LocalTime.of(10, 0)); em.persist(p); return p;
    }
    private TimetableSlot slot(ClassEntity clazz, Period period) {
        TimetableSlot s = new TimetableSlot(); s.setForClass(clazz); s.setForCourse(course); s.setPeriod(period);
        s.setTeacher(teacher); s.setRoom(room); s.setDescription("Algebra lesson"); s.setDayOfWeek(DayOfWeek.MONDAY);
        em.persist(s); return s;
    }
}
