package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.repository.AttendanceRepository;
import com.example.school_management.feature.operational.repository.NotificationRepository;
import com.example.school_management.feature.operational.service.AttendanceService;
import com.example.school_management.feature.operational.dto.AttendanceDto;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Transactional
class AttendanceSchoolAccessIntegrationTest {
    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final String BASE = "/api/v1/attendance";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired AttendanceRepository attendance;
    @Autowired NotificationRepository notifications;
    @Autowired AttendanceService attendanceService;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;

    private School school;
    private School foreignSchool;
    private ClassEntity ownClass;
    private ClassEntity foreignClass;
    private Course ownCourse;
    private Course foreignCourse;
    private Student student;
    private Student foreignStudent;
    private Teacher teacher;
    private Teacher foreignTeacher;
    private Staff staff;
    private TimetableSlot ownSlot;
    private TimetableSlot foreignSlot;
    private SchoolMembership studentMembership;

    @BeforeEach
    void setUp() {
        school = school("Attendance current");
        foreignSchool = school("Attendance foreign");
        doReturn(school).when(currentSchool).resolve();
        ownClass = clazz(school, "Current class");
        foreignClass = clazz(foreignSchool, "Foreign class");
        ownCourse = course(school, "Current course");
        foreignCourse = course(foreignSchool, "Foreign course");
        student = account(new Student(), UserRole.STUDENT);
        foreignStudent = account(new Student(), UserRole.STUDENT);
        teacher = account(new Teacher(), UserRole.TEACHER);
        foreignTeacher = account(new Teacher(), UserRole.TEACHER);
        staff = account(new Staff(), UserRole.STAFF);
        studentMembership = membership(student, school, MembershipRole.STUDENT);
        membership(foreignStudent, foreignSchool, MembershipRole.STUDENT);
        membership(teacher, school, MembershipRole.TEACHER);
        membership(foreignTeacher, foreignSchool, MembershipRole.TEACHER);
        enrollment(student, ownClass, EnrollmentStatus.ACTIVE);
        enrollment(foreignStudent, foreignClass, EnrollmentStatus.ACTIVE);
        ownSlot = slot(school, ownClass, ownCourse, teacher);
        foreignSlot = slot(foreignSchool, foreignClass, foreignCourse, foreignTeacher);
        em.flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void foreignAttendanceCannotBeMutated(String role) throws Exception {
        Attendance foreign = row(foreignSchool, foreignStudent, foreignClass, foreignCourse, foreignSlot,
                MONDAY, AttendanceStatus.PRESENT);
        Map<String, Object> body = request(student, MONDAY);
        body.put("status", "LATE");
        for (MockHttpServletRequestBuilder route : List.of(
                put(BASE + "/{id}", foreign.getId()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)),
                delete(BASE + "/{id}", foreign.getId()),
                patch(BASE + "/{id}/excuse", foreign.getId()).param("excuse", "Ill"),
                patch(BASE + "/{id}/late", foreign.getId()).param("remarks", "Bus"))) {
            mvc.perform(as(route, role)).andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        em.clear();
        Attendance unchanged = attendance.findById(foreign.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(unchanged.getSchool().getId()).isEqualTo(foreignSchool.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void foreignResourcesAreInvisibleToEveryAttendanceRoute(String role) throws Exception {
        String marks = json.writeValueAsString(List.of(request(student, MONDAY)));
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE + "/class/{id}", foreignClass.getId()).param("date", MONDAY.toString()),
                get(BASE + "/class/{id}/students", foreignClass.getId()).param("date", MONDAY.toString()),
                get(BASE + "/class/{id}/students-simple", foreignClass.getId()),
                get(BASE + "/statistics/class/{id}", foreignClass.getId())
                        .param("startDate", MONDAY.toString()).param("endDate", MONDAY.plusDays(1).toString()),
                post(BASE + "/class/{id}/mark", foreignClass.getId()).param("date", MONDAY.toString())
                        .contentType(MediaType.APPLICATION_JSON).content(marks),
                get(BASE + "/course/{id}", foreignCourse.getId()).param("date", MONDAY.toString()),
                range(get(BASE + "/user/{id}", foreignStudent.getId()), MONDAY, MONDAY),
                range(get(BASE + "/statistics/user/{id}", foreignStudent.getId()), MONDAY, MONDAY),
                range(get(BASE + "/user/{id}", foreignTeacher.getId()), MONDAY, MONDAY),
                range(get(BASE + "/statistics/user/{id}", foreignTeacher.getId()), MONDAY, MONDAY),
                get(BASE + "/slot/{id}/students", foreignSlot.getId()).param("date", MONDAY.toString()),
                post(BASE + "/slot/{id}/mark", foreignSlot.getId()).param("date", MONDAY.toString())
                        .contentType(MediaType.APPLICATION_JSON).content(marks),
                get(BASE + "/teacher/{id}/today", foreignTeacher.getId()).param("date", MONDAY.toString()),
                get(BASE + "/teacher/{id}/absent-students", foreignTeacher.getId()).param("date", MONDAY.toString()),
                get(BASE + "/teacher/{id}/weekly-summary", foreignTeacher.getId()).param("startOfWeek", MONDAY.toString()),
                get(BASE + "/teacher/{id}/can-mark/{slot}", foreignTeacher.getId(), ownSlot.getId()).param("date", MONDAY.toString()),
                get(BASE + "/teacher/{id}/can-mark/{slot}", teacher.getId(), foreignSlot.getId()).param("date", MONDAY.toString()),
                get(BASE + "/teacher/{id}/class/{clazz}/course/{course}", teacher.getId(), foreignClass.getId(), ownCourse.getId()),
                get(BASE + "/teacher/{id}/class/{clazz}/course/{course}", teacher.getId(), ownClass.getId(), foreignCourse.getId()))) {
            mvc.perform(as(route, role)).andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(attendance.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"classId", "courseId", "timetableSlotId", "STUDENT", "TEACHER"})
    void genericCreationRejectsForeignReferencesBeforeWritingOrNotifying(String reference) throws Exception {
        Map<String, Object> body = request(student, MONDAY);
        switch (reference) {
            case "classId" -> body.put(reference, foreignClass.getId());
            case "courseId" -> body.put(reference, foreignCourse.getId());
            case "timetableSlotId" -> body.put(reference, foreignSlot.getId());
            case "STUDENT" -> body = request(foreignStudent, MONDAY);
            case "TEACHER" -> body = request(foreignTeacher, MONDAY);
            default -> throw new AssertionError(reference);
        }
        body.put("status", "ABSENT");
        long notificationsBefore = notifications.count();
        for (String role : List.of("ADMIN", "STAFF")) {
            mvc.perform(as(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), role))
                    .andExpect(status().isNotFound());
        }
        assertThat(attendance.count()).isZero();
        assertThat(notifications.count()).isEqualTo(notificationsBefore);
    }

    @Test
    void foreignTeacherCannotUseCurrentSchoolSlotEvenWhenAssignedAsItsTeacher() throws Exception {
        ownSlot.setTeacher(foreignTeacher);
        em.flush();
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE + "/slot/{id}/students", ownSlot.getId()).param("date", MONDAY.toString()),
                post(BASE + "/slot/{id}/mark", ownSlot.getId()).param("date", MONDAY.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(List.of(request(student, MONDAY)))))) {
            mvc.perform(route.with(user(foreignTeacher.getEmail()).roles("TEACHER")))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(attendance.count()).isZero();
    }

    @Test
    void schoolOwnershipIsAssignedByServerAndSlotMetadataIsNormalized() throws Exception {
        Map<String, Object> body = request(student, MONDAY);
        body.put("timetableSlotId", ownSlot.getId());
        body.put("schoolId", foreignSchool.getId());
        JsonNode data = response(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), 201);
        assertThat(data.path("classId").asLong()).isEqualTo(ownClass.getId());
        assertThat(data.path("courseId").asLong()).isEqualTo(ownCourse.getId());
        assertThat(data.has("schoolId")).isFalse();
        assertThat(data.has("school")).isFalse();
        em.flush();
        em.clear();
        Attendance saved = attendance.findByUserIdAndCourseIdAndDateAndSchoolId(
                student.getId(), ownCourse.getId(), MONDAY, school.getId()).orElseThrow();
        assertThat(saved.getSchool().getId()).isEqualTo(school.getId());
        assertThat(saved.getTimetableSlot().getId()).isEqualTo(ownSlot.getId());
        assertThat(saved.getClassEntity().getId()).isEqualTo(ownClass.getId());
        assertThat(saved.getCourse().getId()).isEqualTo(ownCourse.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"classId", "courseId"})
    void slotAndRequestMetadataMustAgreeEvenWithinOneSchool(String reference) throws Exception {
        Map<String, Object> body = request(student, MONDAY);
        body.put("timetableSlotId", ownSlot.getId());
        body.put(reference, reference.equals("classId") ? clazz(school, "Other current class").getId()
                : course(school, "Other current course").getId());
        em.flush();
        mvc.perform(as(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), "ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(attendance.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"class", "course"})
    void internallyInconsistentSlotsAreRejectedByReadsAndWrites(String relation) throws Exception {
        if (relation.equals("class")) ownSlot.setForClass(foreignClass);
        else ownSlot.setForCourse(foreignCourse);
        em.flush();
        Map<String, Object> body = request(student, MONDAY);
        body.put("timetableSlotId", ownSlot.getId());
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE + "/slot/{id}/students", ownSlot.getId()).param("date", MONDAY.toString()),
                post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)),
                post(BASE + "/slot/{id}/mark", ownSlot.getId()).param("date", MONDAY.toString())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(body))))) {
            mvc.perform(as(route, "ADMIN")).andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
        assertThat(attendance.count()).isZero();
    }

    @Test
    void sameUserAttendanceAndStatisticsAreIsolatedByExplicitSchoolOwnership() throws Exception {
        membership(student, foreignSchool, MembershipRole.STUDENT);
        row(school, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.PRESENT);
        row(school, student, ownClass, ownCourse, null, MONDAY.plusDays(1), AttendanceStatus.LATE);
        row(foreignSchool, student, foreignClass, foreignCourse, foreignSlot, MONDAY, AttendanceStatus.ABSENT);

        JsonNode records = response(range(get(BASE + "/user/{id}", student.getId()), MONDAY, MONDAY.plusDays(1)), 200);
        assertThat(records).hasSize(2);
        assertThat(records.findValuesAsText("status")).containsExactlyInAnyOrder("PRESENT", "LATE");
        assertThat(records.findValuesAsText("date")).containsExactlyInAnyOrder(MONDAY.toString(), MONDAY.plusDays(1).toString());
        for (String route : List.of("/statistics", "/statistics/user/" + student.getId())) {
            JsonNode stats = response(range(get(BASE + route), MONDAY, MONDAY.plusDays(1)), 200);
            assertThat(stats.path("totalDays").asLong()).isEqualTo(2);
            assertThat(stats.path("presentDays").asLong()).isEqualTo(1);
            assertThat(stats.path("lateDays").asLong()).isEqualTo(1);
            assertThat(stats.path("absentDays").asLong()).isZero();
            assertThat(stats.path("absencePercentage").asDouble()).isZero();
        }
    }

    @Test
    void paginationAndDynamicFiltersApplySchoolBeforeCounting() throws Exception {
        row(school, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.PRESENT);
        row(school, student, ownClass, ownCourse, null, MONDAY.plusDays(1), AttendanceStatus.PRESENT);
        row(foreignSchool, student, foreignClass, foreignCourse, foreignSlot, MONDAY.plusDays(2), AttendanceStatus.PRESENT);
        JsonNode type = response(range(get(BASE + "/type/STUDENT"), MONDAY, MONDAY.plusDays(2))
                .param("size", "1").param("sort", "date,desc"), 200);
        assertThat(type.path("totalElements").asLong()).isEqualTo(2);
        assertThat(type.path("totalPages").asLong()).isEqualTo(2);
        assertThat(type.path("content")).hasSize(1);
        assertThat(type.path("content").get(0).path("date").asText()).isEqualTo(MONDAY.plusDays(1).toString());

        JsonNode filtered = response(get(BASE + "/filter").param("user.id_eq", student.getId().toString())
                .param("status_eq", "PRESENT").param("size", "1").param("sort", "date,asc"), 200);
        assertThat(filtered.path("totalElements").asLong()).isEqualTo(2);
        assertThat(filtered.path("content")).hasSize(1);
        assertThat(filtered.path("content").get(0).path("date").asText()).isEqualTo(MONDAY.toString());
        var page = attendanceService.findWithAdvancedFilters(PageRequest.of(0, 1),
                Map.of("user.id_eq", new String[]{student.getId().toString()}, "status_eq", new String[]{"PRESENT"}));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(AttendanceDto::getDate).containsAnyOf(MONDAY, MONDAY.plusDays(1));
        mvc.perform(as(get(BASE + "/filter").param("school.id_eq", foreignSchool.getId().toString()), "ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void classAndCourseReadsTrustAttendanceOwnerAfterRelatedResourcesDrift() throws Exception {
        Attendance own = row(school, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.LATE);
        // Deliberately corrupt the related resource links: persisted Attendance.school still bounds every read.
        row(foreignSchool, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.ABSENT);
        for (String route : List.of("/class/" + ownClass.getId(), "/course/" + ownCourse.getId())) {
            JsonNode data = response(get(BASE + route).param("date", MONDAY.toString()), 200);
            assertThat(data).hasSize(1);
            assertThat(data.get(0).path("userId").asLong()).isEqualTo(student.getId());
            assertThat(data.get(0).path("status").asText()).isEqualTo(own.getStatus().name());
        }
        for (String route : List.of("/class/" + ownClass.getId() + "/students", "/slot/" + ownSlot.getId() + "/students")) {
            JsonNode data = response(get(BASE + route).param("date", MONDAY.toString()), 200);
            assertThat(data).hasSize(1);
            assertThat(data.get(0).path("userId").asLong()).isEqualTo(student.getId());
            assertThat(data.get(0).path("status").asText()).isEqualTo(own.getStatus().name());
        }
    }

    @Test
    void classStatisticsUseFullDateRangeAndOnlyThatClassInsideCurrentSchool() throws Exception {
        row(school, student, ownClass, ownCourse, null, MONDAY, AttendanceStatus.PRESENT);
        row(school, student, ownClass, ownCourse, null, MONDAY.plusDays(1), AttendanceStatus.LATE);
        row(school, student, ownClass, ownCourse, null, MONDAY.plusDays(2), AttendanceStatus.EXCUSED);
        row(school, student, ownClass, ownCourse, null, MONDAY.minusDays(1), AttendanceStatus.ABSENT);
        row(school, student, ownClass, ownCourse, null, MONDAY.plusDays(3), AttendanceStatus.ABSENT);
        row(foreignSchool, student, foreignClass, foreignCourse, null, MONDAY.plusDays(1), AttendanceStatus.ABSENT);
        ClassEntity anotherClass = clazz(school, "Other class statistics");
        row(school, student, anotherClass, ownCourse, null, MONDAY.plusDays(1), AttendanceStatus.ABSENT);
        Student laterStudent = account(new Student(), UserRole.STUDENT);
        membership(laterStudent, school, MembershipRole.STUDENT);
        row(school, laterStudent, ownClass, ownCourse, null, MONDAY.plusDays(1), AttendanceStatus.PRESENT);
        JsonNode data = response(range(get(BASE + "/statistics/class/{id}", ownClass.getId()), MONDAY, MONDAY.plusDays(2)), 200);
        assertThat(data).hasSize(2);
        JsonNode studentStats = null;
        for (JsonNode stats : data) {
            if (stats.path("userId").asLong() == student.getId()) studentStats = stats;
        }
        assertThat(studentStats).isNotNull();
        assertThat(studentStats.path("totalDays").asLong()).isEqualTo(3);
        assertThat(studentStats.path("presentDays").asLong()).isEqualTo(1);
        assertThat(studentStats.path("lateDays").asLong()).isEqualTo(1);
        assertThat(studentStats.path("excusedDays").asLong()).isEqualTo(1);
        assertThat(studentStats.path("absentDays").asLong()).isZero();
    }

    @Test
    void teacherQueriesDoNotMixSchoolsEvenWhenSameTeacherOwnsBothSchedules() throws Exception {
        membership(teacher, foreignSchool, MembershipRole.TEACHER);
        foreignSlot.setTeacher(teacher);
        Attendance own = row(school, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.ABSENT);
        row(foreignSchool, student, foreignClass, foreignCourse, foreignSlot, MONDAY, AttendanceStatus.ABSENT);
        // A foreign record referring to a current slot must also be excluded by Attendance.school.
        row(foreignSchool, foreignStudent, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.ABSENT);
        for (String route : List.of("/today", "/absent-students")) {
            JsonNode data = response(get(BASE + "/teacher/" + teacher.getId() + route).param("date", MONDAY.toString()), 200);
            assertThat(data).hasSize(1);
            assertThat(data.get(0).path("userId").asLong()).isEqualTo(student.getId());
            assertThat(data.get(0).path("status").asText()).isEqualTo(own.getStatus().name());
        }
        JsonNode weekly = response(get(BASE + "/teacher/{id}/weekly-summary", teacher.getId()).param("startOfWeek", MONDAY.toString()), 200);
        assertThat(weekly.path("MONDAY")).hasSize(1);
        assertThat(weekly.path("MONDAY").get(0).path("userId").asLong()).isEqualTo(student.getId());
        assertThat(weekly.path("MONDAY").get(0).path("timetableSlotId").asLong()).isEqualTo(ownSlot.getId());
    }

    @Test
    void teacherHistoryQueriesCannotConsumeForeignSlotsAfterRelationshipDrift() throws Exception {
        foreignSlot.setTeacher(teacher);
        row(school, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.ABSENT);
        row(school, student, foreignClass, foreignCourse, foreignSlot, MONDAY, AttendanceStatus.ABSENT);

        JsonNode absent = response(get(BASE + "/teacher/{id}/absent-students", teacher.getId())
                .param("date", MONDAY.toString()), 200);
        assertThat(absent).hasSize(1);
        assertThat(absent.get(0).path("timetableSlotId").asLong()).isEqualTo(ownSlot.getId());
        JsonNode weekly = response(get(BASE + "/teacher/{id}/weekly-summary", teacher.getId())
                .param("startOfWeek", MONDAY.toString()), 200);
        assertThat(weekly.path("MONDAY")).hasSize(1);
        assertThat(weekly.path("MONDAY").get(0).path("timetableSlotId").asLong()).isEqualTo(ownSlot.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"class", "course"})
    void teacherHistoryQueriesRejectInternallyInconsistentSlots(String relation) throws Exception {
        row(school, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.ABSENT);
        if (relation.equals("class")) ownSlot.setForClass(foreignClass);
        else ownSlot.setForCourse(foreignCourse);
        em.flush();
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE + "/teacher/{id}/today", teacher.getId()).param("date", MONDAY.toString()),
                get(BASE + "/teacher/{id}/absent-students", teacher.getId()).param("date", MONDAY.toString()),
                get(BASE + "/teacher/{id}/weekly-summary", teacher.getId()).param("startOfWeek", MONDAY.toString()))) {
            mvc.perform(as(route, "ADMIN")).andExpect(status().isConflict());
        }
    }

    @Test
    void virtualTeacherScheduleIncludesOnlyCurrentSchoolAssignments() throws Exception {
        membership(teacher, foreignSchool, MembershipRole.TEACHER);
        assignment(teacher, ownClass, ownCourse);
        assignment(teacher, foreignClass, foreignCourse);
        em.flush();
        JsonNode data = response(get(BASE + "/teacher/{id}/today", teacher.getId()).param("date", MONDAY.plusDays(1).toString()), 200);
        assertThat(data).hasSize(1);
        assertThat(data.get(0).path("classId").asLong()).isEqualTo(ownClass.getId());
        assertThat(data.get(0).path("courseId").asLong()).isEqualTo(ownCourse.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVE", "SUSPENDED"})
    void historicalStudentOwnershipAndAdministrativeCorrectionSurviveMembershipChanges(MembershipStatus membershipStatus) throws Exception {
        Attendance history = row(school, student, ownClass, ownCourse, null, MONDAY, AttendanceStatus.PRESENT);
        studentMembership.setStatus(membershipStatus);
        student.setStatus(Status.SUSPENDED);
        em.flush();
        JsonNode data = response(range(get(BASE + "/user/{id}", student.getId()), MONDAY, MONDAY), 200);
        assertThat(data).hasSize(1);
        assertThat(data.get(0).path("userId").asLong()).isEqualTo(student.getId());
        assertThat(data.get(0).path("date").asText()).isEqualTo(MONDAY.toString());
        assertThat(data.get(0).path("status").asText()).isEqualTo("PRESENT");
        Map<String, Object> correction = request(student, MONDAY);
        correction.put("status", "LATE");
        response(put(BASE + "/{id}", history.getId()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(correction)), 200);
        response(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request(student, MONDAY.plusDays(1)))), 201);
        em.flush();
        em.clear();
        assertThat(attendance.findById(history.getId()).orElseThrow().getSchool().getId()).isEqualTo(school.getId());
        assertThat(attendance.findById(history.getId()).orElseThrow().getStatus()).isEqualTo(AttendanceStatus.LATE);
    }

    @Test
    void genericTeacherAndStaffRecordsPreserveMembershipAndStaffCompatibility() throws Exception {
        em.createQuery("select m from SchoolMembership m where m.user.id = :id", SchoolMembership.class)
                .setParameter("id", teacher.getId()).getSingleResult().setStatus(MembershipStatus.INACTIVE);
        em.flush();
        for (BaseUser target : List.of(teacher, staff)) {
            JsonNode data = response(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request(target, MONDAY))), 201);
            assertThat(data.path("userId").asLong()).isEqualTo(target.getId());
            assertThat(attendance.findByUserIdAndDateBetweenAndSchoolId(target.getId(), MONDAY, MONDAY, school.getId()))
                    .singleElement().satisfies(row -> assertThat(row.getSchool().getId()).isEqualTo(school.getId()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"WITHDRAWN", "COMPLETED", "TRANSFERRED"})
    void administratorsAndStaffReceiveConflictForStudentsOutsideActiveClassRoster(EnrollmentStatus ended) throws Exception {
        em.createQuery("select e from Enrollment e where e.student.id = :id and e.classEntity.id = :clazz", Enrollment.class)
                .setParameter("id", student.getId()).setParameter("clazz", ownClass.getId()).getSingleResult().setStatus(ended);
        em.flush();
        for (String role : List.of("ADMIN", "STAFF")) {
            mvc.perform(as(post(BASE + "/class/{id}/mark", ownClass.getId()).param("date", MONDAY.toString())
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(request(student, MONDAY)))), role))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value("Student is not actively enrolled in this class"));
        }
        assertThat(attendance.count()).isZero();
    }

    @Test
    void activeEnrollmentRemainsMarkableAndNewClassAndSlotRowsHaveSchoolOwnership() throws Exception {
        JsonNode classData = response(post(BASE + "/class/{id}/mark", ownClass.getId()).param("date", MONDAY.toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(request(student, MONDAY)))), 201);
        JsonNode slotData = response(post(BASE + "/slot/{id}/mark", ownSlot.getId()).param("date", MONDAY.plusWeeks(1).toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(request(student, MONDAY.plusWeeks(1))))), 201);
        for (JsonNode result : List.of(classData.get(0), slotData.get(0))) {
            assertThat(attendance.findByUserIdAndDateBetweenAndSchoolId(student.getId(),
                    LocalDate.parse(result.path("date").asText()), LocalDate.parse(result.path("date").asText()), school.getId()))
                    .singleElement().satisfies(row -> assertThat(row.getSchool().getId()).isEqualTo(school.getId()));
        }
    }

    @Test
    void batchValidatesEachEntryWithoutAnUnsafeForeignWrite() throws Exception {
        Map<String, Object> valid = request(student, MONDAY);
        valid.put("courseId", ownCourse.getId());
        mvc.perform(as(post(BASE + "/batch").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(List.of(valid, request(foreignStudent, MONDAY)))), "ADMIN"))
                .andExpect(status().isNotFound());
        assertThat(attendance.findAll()).noneMatch(a -> a.getUser().getId().equals(foreignStudent.getId()));
    }

    @Test
    void existingForeignContextDoesNotMaskValidationOrBecomeADuplicate() throws Exception {
        Attendance foreign = row(foreignSchool, student, ownClass, ownCourse, null, MONDAY, AttendanceStatus.PRESENT);
        Map<String, Object> valid = request(student, MONDAY);
        valid.put("classId", ownClass.getId());
        valid.put("courseId", ownCourse.getId());
        JsonNode created = response(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(valid)), 201);
        assertThat(created.path("userId").asLong()).isEqualTo(student.getId());
        Attendance own = attendance.findByUserIdAndCourseIdAndDateAndSchoolId(student.getId(), ownCourse.getId(), MONDAY, school.getId()).orElseThrow();
        assertThat(own.getId()).isNotEqualTo(foreign.getId());
        assertThat(own.getSchool().getId()).isEqualTo(school.getId());
        valid.put("classId", foreignClass.getId());
        mvc.perform(as(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(valid)), "ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void twoSlotsInOneClassKeepExistingAttendanceKeysWithoutBreakingClassReads() throws Exception {
        TimetableSlot second = new TimetableSlot();
        second.setPeriod(ownSlot.getPeriod());
        second.setDayOfWeek(DayOfWeek.MONDAY);
        second.setForClass(ownClass);
        second.setForCourse(ownCourse);
        second.setTeacher(teacher);
        em.persist(second);
        em.flush();
        for (TimetableSlot slot : List.of(ownSlot, second)) {
            response(post(BASE + "/slot/{id}/mark", slot.getId()).param("date", MONDAY.toString())
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(request(student, MONDAY)))), 201);
        }
        assertThat(attendance.findByUserIdAndDateBetweenAndSchoolId(student.getId(), MONDAY, MONDAY, school.getId())).hasSize(1);
        assertThat(response(get(BASE + "/class/{id}/students", ownClass.getId()).param("date", MONDAY.toString()), 200)).hasSize(1);
        Map<String, Object> correction = request(student, MONDAY);
        correction.put("status", "LATE");
        response(post(BASE + "/class/{id}/mark", ownClass.getId()).param("date", MONDAY.toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(correction))), 201);
        assertThat(attendance.findByUserIdAndDateBetweenAndSchoolId(student.getId(), MONDAY, MONDAY, school.getId()))
                .singleElement().satisfies(record -> assertThat(record.getStatus()).isEqualTo(AttendanceStatus.LATE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"classId", "courseId", "timetableSlotId"})
    void classMarkRejectsForeignBodyReferencesBeforeUpdatingExistingAttendance(String reference) throws Exception {
        Attendance existing = row(school, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.PRESENT);
        Map<String, Object> body = request(student, MONDAY);
        body.put("status", "LATE");
        body.put(reference, switch (reference) {
            case "classId" -> foreignClass.getId();
            case "courseId" -> foreignCourse.getId();
            case "timetableSlotId" -> foreignSlot.getId();
            default -> throw new AssertionError(reference);
        });
        for (String role : List.of("ADMIN", "STAFF")) {
            mvc.perform(as(post(BASE + "/class/{id}/mark", ownClass.getId()).param("date", MONDAY.toString())
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(body))), role))
                    .andExpect(status().isNotFound());
        }
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"classId", "timetableSlotId", "courseId"})
    void classMarkRejectsSameSchoolContextMismatches(String reference) throws Exception {
        row(school, student, ownClass, ownCourse, ownSlot, MONDAY, AttendanceStatus.PRESENT);
        ClassEntity otherClass = clazz(school, "Other mark class");
        Course otherCourse = course(school, "Other mark course");
        TimetableSlot otherSlot = new TimetableSlot();
        otherSlot.setPeriod(ownSlot.getPeriod());
        otherSlot.setDayOfWeek(DayOfWeek.MONDAY);
        otherSlot.setForClass(otherClass);
        otherSlot.setForCourse(otherCourse);
        otherSlot.setTeacher(teacher);
        em.persist(otherSlot);
        em.flush();
        Map<String, Object> body = request(student, MONDAY);
        body.put(reference, switch (reference) {
            case "classId" -> otherClass.getId();
            case "timetableSlotId" -> otherSlot.getId();
            case "courseId" -> otherCourse.getId();
            default -> throw new AssertionError(reference);
        });
        // A course mismatch is meaningful when the request also identifies a slot.
        if (reference.equals("courseId")) body.put("timetableSlotId", ownSlot.getId());
        mvc.perform(as(post(BASE + "/class/{id}/mark", ownClass.getId()).param("date", MONDAY.toString())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(body))), "ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void newClassMarkWithSuppliedSlotNormalizesItsCourseAndSchool() throws Exception {
        Map<String, Object> body = request(student, MONDAY);
        body.put("timetableSlotId", ownSlot.getId());
        JsonNode data = response(post(BASE + "/class/{id}/mark", ownClass.getId()).param("date", MONDAY.toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(body))), 201);
        assertThat(data).hasSize(1);
        assertThat(data.get(0).path("classId").asLong()).isEqualTo(ownClass.getId());
        assertThat(data.get(0).path("courseId").asLong()).isEqualTo(ownCourse.getId());
        assertThat(data.get(0).path("timetableSlotId").asLong()).isEqualTo(ownSlot.getId());
        assertThat(attendance.findByUserIdAndClassIdAndDateAndSchoolId(student.getId(), ownClass.getId(), MONDAY, school.getId()))
                .hasValueSatisfying(record -> {
                    assertThat(record.getSchool().getId()).isEqualTo(school.getId());
                    assertThat(record.getCourse().getId()).isEqualTo(ownCourse.getId());
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"WITHDRAWN", "COMPLETED", "TRANSFERRED"})
    void administrativeSlotMarkRejectsTerminalEnrollment(EnrollmentStatus ended) throws Exception {
        em.createQuery("select e from Enrollment e where e.student.id = :id and e.classEntity.id = :clazz", Enrollment.class)
                .setParameter("id", student.getId()).setParameter("clazz", ownClass.getId()).getSingleResult().setStatus(ended);
        em.flush();
        for (String role : List.of("ADMIN", "STAFF")) {
            mvc.perform(as(post(BASE + "/slot/{id}/mark", ownSlot.getId()).param("date", MONDAY.toString())
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(request(student, MONDAY)))), role))
                    .andExpect(status().isConflict());
        }
        assertThat(attendance.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "TEACHER"})
    void foreignStudentAndTeacherCannotBypassMembershipByClaimingStaffType(String targetType) throws Exception {
        Map<String, Object> body = request(targetType.equals("STUDENT") ? foreignStudent : foreignTeacher, MONDAY);
        body.put("userType", "STAFF");
        body.put("status", "ABSENT");
        body.put("courseId", ownCourse.getId());
        long notificationsBefore = notifications.count();
        mvc.perform(as(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)), "ADMIN"))
                .andExpect(status().isNotFound());
        assertThat(attendance.count()).isZero();
        assertThat(notifications.count()).isEqualTo(notificationsBefore);
    }

    @Test
    void classMarkStillAcceptsTheVirtualSlotMarkerReturnedByItsRoster() throws Exception {
        JsonNode virtual = response(get(BASE + "/class/{id}/students", ownClass.getId())
                .param("date", MONDAY.toString()), 200).get(0);
        assertThat(virtual.path("timetableSlotId").asLong()).isEqualTo(-1);
        Attendance existing = row(school, student, ownClass, ownCourse, null, MONDAY, AttendanceStatus.PRESENT);
        ((com.fasterxml.jackson.databind.node.ObjectNode) virtual).put("status", "LATE");
        response(post(BASE + "/class/{id}/mark", ownClass.getId()).param("date", MONDAY.toString())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(virtual))), 201);
        assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(existing.getTimetableSlot()).isNull();
        assertThat(existing.getSchool().getId()).isEqualTo(school.getId());
    }

    private JsonNode response(MockHttpServletRequestBuilder route, int expectedStatus) throws Exception {
        String response = mvc.perform(as(route, "ADMIN")).andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("data");
    }

    private MockHttpServletRequestBuilder range(MockHttpServletRequestBuilder route, LocalDate start, LocalDate end) {
        return route.param("startDate", start.toString()).param("endDate", end.toString());
    }

    private void assignment(Teacher teacher, ClassEntity clazz, Course course) {
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setTeacher(teacher);
        assignment.setClazz(clazz);
        assignment.setCourse(course);
        em.persist(assignment);
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder route, String role) {
        return route.with(user(role.equals("STAFF") ? staff.getEmail() : DevFixtureLoader.ADMIN_EMAIL).roles(role));
    }

    private Map<String, Object> request(BaseUser target, LocalDate date) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", target.getId());
        body.put("date", date.toString());
        body.put("status", "PRESENT");
        body.put("userType", target.getRole().name());
        return body;
    }

    private Attendance row(School owner, BaseUser target, ClassEntity clazz, Course course, TimetableSlot slot,
                           LocalDate date, AttendanceStatus status) {
        Attendance row = new Attendance();
        row.setSchool(owner);
        row.setUser(target);
        row.setClassEntity(clazz);
        row.setCourse(course);
        row.setTimetableSlot(slot);
        row.setDate(date);
        row.setStatus(status);
        row.setUserType(UserType.valueOf(target.getRole().name()));
        em.persist(row);
        em.flush();
        return row;
    }

    private School school(String name) {
        School value = new School();
        value.setName(name);
        em.persist(value);
        return value;
    }

    private ClassEntity clazz(School owner, String name) {
        AcademicYear year = new AcademicYear();
        year.setSchool(owner);
        year.setName(name + " year");
        year.setStartDate(LocalDate.of(2029, 9, 1));
        year.setEndDate(LocalDate.of(2030, 6, 30));
        em.persist(year);
        ClassEntity value = new ClassEntity();
        value.setAcademicYear(year);
        value.setName(name);
        em.persist(value);
        return value;
    }

    private Course course(School owner, String name) {
        Course value = new Course();
        value.setSchool(owner);
        value.setName(name);
        value.setCode(UUID.randomUUID().toString().substring(0, 8));
        em.persist(value);
        return value;
    }

    private <T extends BaseUser> T account(T value, UserRole role) {
        value.setEmail(UUID.randomUUID() + "@fixtures.school.test");
        value.setFirstName("Attendance");
        value.setLastName(role.name());
        value.setPassword("unused");
        value.setRole(role);
        value.setStatus(Status.ACTIVE);
        value.setIsEmailVerified(true);
        em.persist(value);
        return value;
    }

    private SchoolMembership membership(BaseUser target, School owner, MembershipRole role) {
        SchoolMembership membership = new SchoolMembership();
        membership.setUser(target);
        membership.setSchool(owner);
        membership.setRoles(Set.of(role));
        membership.setStatus(MembershipStatus.ACTIVE);
        em.persist(membership);
        return membership;
    }

    private Enrollment enrollment(Student target, ClassEntity clazz, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(target);
        enrollment.setClassEntity(clazz);
        enrollment.setStatus(status);
        em.persist(enrollment);
        return enrollment;
    }

    private TimetableSlot slot(School owner, ClassEntity clazz, Course course, Teacher teacher) {
        Period period = new Period();
        period.setSchool(owner);
        period.setIndex(99);
        period.setStartTime(LocalTime.of(8, 0));
        period.setEndTime(LocalTime.of(9, 0));
        em.persist(period);
        TimetableSlot slot = new TimetableSlot();
        slot.setPeriod(period);
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setForClass(clazz);
        slot.setForCourse(course);
        slot.setTeacher(teacher);
        em.persist(slot);
        return slot;
    }
}
