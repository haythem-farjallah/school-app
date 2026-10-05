package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.membership.entity.*;
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
class TeacherAttendanceSchoolAccessIntegrationTest {
    private static final String BASE = "/api/v1/teacher-attendance";
    private static final LocalDate DAY = LocalDate.now().minusDays(2);
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    private School school;
    private School foreignSchool;
    private Teacher teacher;
    private Teacher foreignTeacher;
    private Teacher substitute;
    private Staff recorder;
    private ClassEntity clazz;
    private ClassEntity foreignClass;
    private Course course;
    private Course foreignCourse;
    private SchoolMembership teacherMembership;

    @BeforeEach
    void setup() {
        school = school("Teacher attendance current");
        foreignSchool = school("Teacher attendance foreign");
        doReturn(school).when(currentSchool).resolve();
        teacher = account(new Teacher(), UserRole.TEACHER);
        foreignTeacher = account(new Teacher(), UserRole.TEACHER);
        substitute = account(new Teacher(), UserRole.TEACHER);
        recorder = account(new Staff(), UserRole.STAFF);
        teacherMembership = membership(teacher, school);
        membership(foreignTeacher, foreignSchool);
        membership(substitute, school);
        clazz = clazz(school, "Canonical class");
        foreignClass = clazz(foreignSchool, "Foreign class");
        course = course(school, "Canonical course");
        foreignCourse = course(foreignSchool, "Foreign course");
        em.flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void foreignTeacherCannotBeReadCreatedOrUsedForStatisticsOrExistence(String role) throws Exception {
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE).param("teacherId", foreignTeacher.getId().toString()),
                get(BASE + "/statistics/{id}", foreignTeacher.getId()),
                get(BASE + "/exists").param("teacherId", foreignTeacher.getId().toString()).param("date", DAY.toString()),
                write(post(BASE), request(foreignTeacher, DAY)))) {
            mvc.perform(as(route, role)).andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"classId", "courseId", "substituteTeacherId"})
    void foreignOptionalResourcesCannotBeWritten(String reference) throws Exception {
        Map<String, Object> body = request(teacher, DAY);
        body.put(reference, switch (reference) {
            case "classId" -> foreignClass.getId();
            case "courseId" -> foreignCourse.getId();
            default -> foreignTeacher.getId();
        });
        mvc.perform(as(write(post(BASE), body), "ADMIN")).andExpect(status().isNotFound());
        assertThat(em.createQuery("select count(a) from TeacherAttendance a", Long.class).getSingleResult()).isZero();
    }

    @Test
    void creationCanonicalizesResourcesAndIgnoresSpoofedRecorderAndSchool() throws Exception {
        Map<String, Object> body = request(teacher, DAY);
        body.put("classId", clazz.getId());
        body.put("className", "Spoof class");
        body.put("courseId", course.getId());
        body.put("courseName", "Spoof course");
        body.put("substituteTeacherId", substitute.getId());
        body.put("substituteTeacherName", "Spoof substitute");
        body.put("schoolId", foreignSchool.getId());
        JsonNode result = response(write(post(BASE), body), 201);
        assertThat(result.path("teacherFirstName").asText()).isEqualTo(teacher.getFirstName());
        assertThat(result.path("teacherLastName").asText()).isEqualTo(teacher.getLastName());
        assertThat(result.path("teacherEmail").asText()).isEqualTo(teacher.getEmail());
        assertThat(result.path("className").asText()).isEqualTo(clazz.getName());
        assertThat(result.path("courseName").asText()).isEqualTo(course.getName());
        assertThat(result.path("substituteTeacherName").asText()).isEqualTo(fullName(substitute));
        assertThat(result.path("recordedById").asLong()).isEqualTo(recorder.getId());
        assertThat(result.path("recordedByName").asText()).isEqualTo(fullName(recorder));
        assertThat(result.has("school")).isFalse();
        assertThat(result.has("schoolId")).isFalse();
        assertThat(em.createNativeQuery("select school_id from teacher_attendance where id = :id")
                .setParameter("id", result.path("id").asLong()).getSingleResult()).isEqualTo(school.getId());
    }

    @Test
    void requestRecorderFieldsAreOptional() throws Exception {
        Map<String, Object> body = request(teacher, DAY);
        body.remove("recordedById");
        body.remove("recordedByName");
        JsonNode result = response(write(post(BASE), body), 201);
        assertThat(result.path("recordedById").asLong()).isEqualTo(recorder.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void unfilteredDateRangeAndDateReadsExcludeForeignRowsAndForeignRowsCannotBeMutated(String role) throws Exception {
        JsonNode own = response(write(post(BASE), request(teacher, DAY)), 201);
        doReturn(foreignSchool).when(currentSchool).resolve();
        JsonNode foreign = response(write(post(BASE), request(foreignTeacher, DAY)), 201);
        doReturn(school).when(currentSchool).resolve();
        for (MockHttpServletRequestBuilder route : List.of(get(BASE),
                get(BASE).param("startDate", DAY.toString()).param("endDate", DAY.toString()),
                get(BASE + "/date/{day}", DAY))) {
            String raw = mvc.perform(as(route, role)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            JsonNode result = json.readTree(raw).path("data");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).path("id").asLong()).isEqualTo(own.path("id").asLong());
        }
        mvc.perform(as(write(put(BASE + "/{id}", foreign.path("id").asLong()), request(teacher, DAY)), role))
                .andExpect(status().isNotFound());
        mvc.perform(as(delete(BASE + "/{id}", foreign.path("id").asLong()), role)).andExpect(status().isNotFound());
    }

    @Test
    void multiSchoolTeacherCanHaveOneSameDateRecordPerSchoolAndStatisticsRemainIsolated() throws Exception {
        membership(teacher, foreignSchool);
        em.flush();
        response(write(post(BASE), request(teacher, DAY)), 201);
        doReturn(foreignSchool).when(currentSchool).resolve();
        Map<String, Object> absent = request(teacher, DAY);
        absent.put("status", "ABSENT");
        response(write(post(BASE), absent), 201);
        doReturn(school).when(currentSchool).resolve();
        mvc.perform(as(write(post(BASE), request(teacher, DAY)), "ADMIN"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").exists()).andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists()).andExpect(jsonPath("$.instance").value(BASE));
        assertThat(em.createQuery("select count(a) from TeacherAttendance a", Long.class).getSingleResult()).isEqualTo(2);
        JsonNode stats = response(get(BASE + "/statistics/{id}", teacher.getId()), 200);
        assertThat(stats.path("totalDays").asInt()).isEqualTo(1);
        assertThat(stats.path("presentDays").asInt()).isEqualTo(1);
        assertThat(stats.path("absentDays").asInt()).isZero();
        assertThat(stats.path("teacherName").asText()).isEqualTo(fullName(teacher));
        assertThat(stats.path("monthlyBreakdown")).hasSize(1);
        assertThat(stats.path("monthlyBreakdown").get(0).path("present").asInt()).isEqualTo(1);
        assertThat(stats.path("monthlyBreakdown").get(0).path("absent").asInt()).isZero();
        String raw = mvc.perform(get(BASE).with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(raw).path("data")).hasSize(1);
    }

    @Test
    void foreignAttendanceDoesNotExposeExistenceForAMultiSchoolTeacher() throws Exception {
        membership(teacher, foreignSchool);
        em.flush();
        doReturn(foreignSchool).when(currentSchool).resolve();
        response(write(post(BASE), request(teacher, DAY)), 201);
        doReturn(school).when(currentSchool).resolve();
        assertThat(response(get(BASE + "/exists").param("teacherId", teacher.getId().toString())
                .param("date", DAY.toString()), 200).asBoolean()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = MembershipStatus.class, names = {"SUSPENDED", "INACTIVE"})
    void historySurvivesMembershipStatusChangesAndUpdateKeepsOwnership(MembershipStatus status) throws Exception {
        Map<String, Object> body = request(teacher, DAY);
        body.put("classId", clazz.getId());
        body.put("courseId", course.getId());
        JsonNode created = response(write(post(BASE), body), 201);
        teacherMembership.setStatus(status);
        em.flush();
        assertThat(response(get(BASE).param("teacherId", teacher.getId().toString()), 200)).hasSize(1);
        assertThat(response(get(BASE + "/statistics/{id}", teacher.getId()), 200).path("totalDays").asInt()).isEqualTo(1);
        Map<String, Object> update = request(foreignTeacher, DAY.plusDays(3));
        update.put("classId", foreignClass.getId());
        update.put("courseId", foreignCourse.getId());
        update.put("status", "LATE");
        update.put("substituteTeacherId", substitute.getId());
        JsonNode changed = response(write(put(BASE + "/{id}", created.path("id").asLong()), update), 200);
        assertThat(changed.path("teacherId").asLong()).isEqualTo(teacher.getId());
        assertThat(changed.path("date").asText()).isEqualTo(DAY.toString());
        assertThat(changed.path("classId").asLong()).isEqualTo(clazz.getId());
        assertThat(changed.path("courseId").asLong()).isEqualTo(course.getId());
        assertThat(changed.path("recordedById").asLong()).isEqualTo(recorder.getId());
        assertThat(changed.path("status").asText()).isEqualTo("LATE");
        assertThat(changed.path("substituteTeacherName").asText()).isEqualTo(fullName(substitute));
        update.put("substituteTeacherId", foreignTeacher.getId());
        mvc.perform(as(write(put(BASE + "/{id}", created.path("id").asLong()), update), "ADMIN"))
                .andExpect(status().isNotFound());
        assertThat(em.createNativeQuery("select school_id from teacher_attendance where id = :id")
                .setParameter("id", created.path("id").asLong()).getSingleResult()).isEqualTo(school.getId());
    }

    @Test
    void teacherSelfCannotReadAnotherCurrentSchoolTeacher() throws Exception {
        response(write(post(BASE), request(teacher, DAY)), 201);
        response(write(post(BASE), request(substitute, DAY)), 201);
        String raw = mvc.perform(get(BASE).with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode result = json.readTree(raw).path("data");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).path("teacherId").asLong()).isEqualTo(teacher.getId());
        mvc.perform(get(BASE).param("teacherId", substitute.getId().toString()).with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    private JsonNode response(MockHttpServletRequestBuilder route, int expected) throws Exception {
        String raw = mvc.perform(as(route, "ADMIN")).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return json.readTree(raw).path("data");
    }
    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder route, String role) {
        return route.with(user(recorder.getEmail()).roles(role));
    }
    private MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder route, Map<String, Object> body) throws Exception {
        return route.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }
    private Map<String, Object> request(Teacher target, LocalDate day) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("teacherId", target.getId());
        body.put("teacherFirstName", "Spoof first");
        body.put("teacherLastName", "Spoof last");
        body.put("teacherEmail", "spoof@example.test");
        body.put("date", day.toString());
        body.put("status", "PRESENT");
        body.put("recordedById", foreignTeacher.getId());
        body.put("recordedByName", "Spoof recorder");
        return body;
    }
    private String fullName(BaseUser value) { return value.getFirstName() + " " + value.getLastName(); }
    private School school(String name) {
        School value = new School(); value.setName(name); em.persist(value); return value;
    }
    private <T extends BaseUser> T account(T value, UserRole role) {
        value.setEmail(UUID.randomUUID() + "@fixtures.school.test");
        value.setFirstName("Canonical"); value.setLastName(role.name()); value.setPassword("unused");
        value.setRole(role); value.setStatus(Status.ACTIVE); value.setIsEmailVerified(true); em.persist(value); return value;
    }
    private SchoolMembership membership(Teacher target, School owner) {
        SchoolMembership value = new SchoolMembership(); value.setUser(target); value.setSchool(owner);
        value.setRoles(Set.of(MembershipRole.TEACHER)); value.setStatus(MembershipStatus.ACTIVE); em.persist(value); return value;
    }
    private ClassEntity clazz(School owner, String name) {
        AcademicYear year = new AcademicYear(); year.setSchool(owner); year.setName(name + " year");
        year.setStartDate(LocalDate.of(2029, 9, 1)); year.setEndDate(LocalDate.of(2030, 6, 30)); em.persist(year);
        ClassEntity value = new ClassEntity(); value.setAcademicYear(year); value.setName(name); em.persist(value); return value;
    }
    private Course course(School owner, String name) {
        Course value = new Course(); value.setSchool(owner); value.setName(name);
        value.setCode(UUID.randomUUID().toString().substring(0, 8)); em.persist(value); return value;
    }
}
