package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.operational.dto.TranscriptDto;
import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional
class TranscriptSchoolAccessIntegrationTest {
    private static final LocalDate SCHOOL_START = LocalDate.of(2022, 9, 1);
    private static final LocalDate RANGE_START = LocalDate.of(2024, 1, 1);
    private static final LocalDate RANGE_END = LocalDate.of(2024, 1, 31);
    private static final String BASE = "/api/v1/transcripts";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    @MockitoSpyBean TemplateEngine templateEngine;

    private School school;
    private School foreignSchool;
    private ClassEntity ownClass;
    private ClassEntity foreignClass;
    private Student student;
    private Teacher teacher;
    private SchoolMembership studentMembership;

    @BeforeEach
    void setUp() {
        school = school("Transcript current");
        foreignSchool = school("Transcript foreign");
        doReturn(school).when(currentSchool).resolve();
        ownClass = clazz(school, "Current class");
        foreignClass = clazz(foreignSchool, "Foreign class");
        student = account(new Student(), UserRole.STUDENT);
        student.setEnrolledAt(SCHOOL_START.minusYears(10).atStartOfDay());
        teacher = account(new Teacher(), UserRole.TEACHER);
        studentMembership = membership(student, school, MembershipRole.STUDENT);
        membership(teacher, school, MembershipRole.TEACHER);
        em.flush();
    }

    @Test
    void multiSchoolTranscriptAndSummaryUseOnlyCurrentSchoolHistoryAndAttendance() throws Exception {
        membership(student, foreignSchool, MembershipRole.STUDENT);
        Enrollment own = enrollment(ownClass, EnrollmentStatus.ACTIVE, SCHOOL_START);
        Enrollment foreign = enrollment(foreignClass, EnrollmentStatus.ACTIVE, SCHOOL_START.minusYears(1));
        grade(own, 80f, RANGE_START.atStartOfDay());
        grade(foreign, 20f, RANGE_START.atStartOfDay());
        attendance(school, SCHOOL_START, AttendanceStatus.PRESENT);
        attendance(foreignSchool, SCHOOL_START, AttendanceStatus.ABSENT);
        attendance(school, LocalDate.now().plusDays(1), AttendanceStatus.ABSENT);
        attendance(school, SCHOOL_START.minusDays(1), AttendanceStatus.ABSENT);

        JsonNode transcript = response(get(BASE + "/{id}", student.getId()));
        assertThat(transcript.path("allCourses")).hasSize(1);
        assertThat(transcript.findValuesAsText("courseName")).containsExactly("Current class");
        assertThat(transcript.toString()).doesNotContain("Foreign class");
        assertThat(transcript.path("enrollmentDate").asText()).isEqualTo(SCHOOL_START.toString());
        assertThat(transcript.path("overallGPA").asDouble()).isEqualTo(80d);
        assertThat(transcript.path("totalCredits").asInt()).isEqualTo(3);
        JsonNode summary = response(get(BASE + "/{id}/summary", student.getId()));
        assertThat(summary).isEqualTo(transcript.path("summary"));
        assertThat(summary.path("attendancePercentage").asDouble()).isEqualTo(100d);
        assertThat(summary.path("totalAbsences").asInt()).isZero();
        assertThat(summary.path("currentClass").asText()).isEqualTo("Current class");
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMPLETED", "TRANSFERRED", "WITHDRAWN"})
    void terminalEnrollmentsKeepGradesWithoutRequiringAnActiveEnrollment(EnrollmentStatus terminal) throws Exception {
        Enrollment historical = enrollment(ownClass, terminal, SCHOOL_START);
        grade(historical, 75f, RANGE_START.atStartOfDay());
        enrollment(clazz(school, "Ungraded history"), EnrollmentStatus.COMPLETED, SCHOOL_START.plusYears(1));
        JsonNode transcript = response(get(BASE + "/{id}", student.getId()));
        assertThat(transcript.path("allCourses")).hasSize(1);
        assertThat(transcript.path("allCourses").get(0).path("grade").asDouble()).isEqualTo(75d);
        assertThat(transcript.path("summary").path("currentClass").asText()).isEqualTo("Not Enrolled");
        assertThat(transcript.path("summary").path("totalCourses").asInt()).isEqualTo(1);
    }

    @Test
    void completeTranscriptIncludesEveryTerminalStatusTogether() throws Exception {
        enrollment(ownClass, EnrollmentStatus.COMPLETED, SCHOOL_START);
        int day = 0;
        for (EnrollmentStatus terminal : List.of(EnrollmentStatus.COMPLETED,
                EnrollmentStatus.TRANSFERRED, EnrollmentStatus.WITHDRAWN)) {
            Enrollment history = enrollment(clazz(school, terminal.name() + " class"), terminal,
                    SCHOOL_START.plusYears(1));
            grade(history, 60f + day, RANGE_START.plusDays(day).atStartOfDay());
            day++;
        }
        JsonNode transcript = response(get(BASE + "/{id}", student.getId()));
        assertThat(transcript.findValuesAsText("courseName"))
                .containsExactly("COMPLETED class", "TRANSFERRED class", "WITHDRAWN class");
        assertThat(transcript.path("summary").path("currentClass").asText()).isEqualTo("Not Enrolled");
        assertThat(transcript.path("totalCredits").asInt()).isEqualTo(9);
    }

    @Test
    void enrollmentHistoryWithoutGradesDoesNotInventAcademicEntries() throws Exception {
        enrollment(ownClass, EnrollmentStatus.COMPLETED, SCHOOL_START);
        JsonNode transcript = response(get(BASE + "/{id}", student.getId()));
        assertThat(transcript.path("allCourses")).isEmpty();
        assertThat(transcript.path("overallGPA").asDouble()).isZero();
        assertThat(transcript.path("totalCredits").asInt()).isZero();
        assertThat(transcript.path("summary").path("totalCourses").asInt()).isZero();
    }

    @Test
    void currentClassComesFromActiveEnrollmentWhileHistoryIncludesOldClasses() throws Exception {
        Enrollment historical = enrollment(ownClass, EnrollmentStatus.TRANSFERRED, SCHOOL_START);
        ClassEntity activeClass = clazz(school, "Active class");
        Enrollment active = enrollment(activeClass, EnrollmentStatus.ACTIVE, SCHOOL_START.plusYears(1));
        grade(historical, 60f, RANGE_START.atStartOfDay());
        grade(active, 90f, RANGE_START.plusDays(1).atStartOfDay());
        JsonNode transcript = response(get(BASE + "/{id}", student.getId()));
        assertThat(transcript.findValuesAsText("courseName")).containsExactly("Current class", "Active class");
        assertThat(transcript.path("summary").path("currentClass").asText()).isEqualTo("Active class");
        assertThat(transcript.path("enrollmentDate").asText()).isEqualTo(SCHOOL_START.toString());
    }

    @Test
    void foreignActiveEnrollmentDoesNotBecomeCurrentClass() throws Exception {
        Enrollment own = enrollment(ownClass, EnrollmentStatus.COMPLETED, SCHOOL_START);
        grade(own, 75f, RANGE_START.atStartOfDay());
        enrollment(foreignClass, EnrollmentStatus.ACTIVE, SCHOOL_START.plusYears(1));
        JsonNode transcript = response(get(BASE + "/{id}", student.getId()));
        assertThat(transcript.path("summary").path("currentClass").asText()).isEqualTo("Not Enrolled");
        assertThat(transcript.toString()).doesNotContain("Foreign class");
    }

    @Test
    void periodUsesInclusiveWholeDaysAndTheSameAttendanceWindow() throws Exception {
        Enrollment own = enrollment(ownClass, EnrollmentStatus.COMPLETED, SCHOOL_START);
        grade(own, 10f, RANGE_START.atStartOfDay().minusSeconds(1));
        grade(own, 60f, RANGE_START.atStartOfDay());
        grade(own, 90f, RANGE_END.atTime(23, 59, 59));
        grade(own, 20f, RANGE_END.plusDays(1).atStartOfDay());
        attendance(school, RANGE_START.minusDays(1), AttendanceStatus.ABSENT);
        attendance(school, RANGE_START, AttendanceStatus.PRESENT);
        attendance(school, RANGE_END, AttendanceStatus.EXCUSED);
        attendance(school, RANGE_END.plusDays(1), AttendanceStatus.ABSENT);
        attendance(foreignSchool, RANGE_START, AttendanceStatus.ABSENT);
        JsonNode transcript = response(period(get(BASE + "/{id}/period", student.getId())));
        assertThat(transcript.path("allCourses")).hasSize(2);
        assertThat(transcript.path("allCourses").findValuesAsText("gradedDate"))
                .containsExactly(RANGE_START.toString(), RANGE_END.toString());
        assertThat(transcript.path("overallGPA").asDouble()).isEqualTo(75d);
        assertThat(transcript.path("totalCredits").asInt()).isEqualTo(6);
        JsonNode summary = transcript.path("summary");
        assertThat(summary.path("overallGPA").asDouble()).isEqualTo(75d);
        assertThat(summary.path("totalCredits").asInt()).isEqualTo(6);
        assertThat(summary.path("totalCourses").asInt()).isEqualTo(2);
        assertThat(summary.path("attendancePercentage").asDouble()).isEqualTo(50d);
        assertThat(summary.path("totalAbsences").asInt()).isZero();
        assertThat(summary.path("totalExcusedAbsences").asInt()).isEqualTo(1);
    }

    @Test
    void reversedPeriodReturnsBadRequestForJsonAndPdf() throws Exception {
        enrollment(ownClass, EnrollmentStatus.COMPLETED, SCHOOL_START);
        for (String suffix : List.of("/period", "/pdf/period")) {
            mvc.perform(as(get(BASE + "/{id}" + suffix, student.getId())
                            .param("startDate", RANGE_END.toString()).param("endDate", RANGE_START.toString())))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUSPENDED", "INACTIVE"})
    void historicalMembershipStatusPreservesFullPeriodAndSummaryReads(MembershipStatus membershipStatus) throws Exception {
        studentMembership.setStatus(membershipStatus);
        Enrollment historical = enrollment(ownClass, EnrollmentStatus.COMPLETED, SCHOOL_START);
        grade(historical, 85f, RANGE_START.atStartOfDay());
        assertThat(response(get(BASE + "/{id}", student.getId())).path("allCourses")).hasSize(1);
        assertThat(response(period(get(BASE + "/{id}/period", student.getId()))).path("allCourses")).hasSize(1);
        assertThat(response(get(BASE + "/{id}/summary", student.getId())).path("totalCourses").asInt()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void foreignStudentIsNotFoundForEveryTranscriptRoute(String role) throws Exception {
        Student foreign = account(new Student(), UserRole.STUDENT);
        membership(foreign, foreignSchool, MembershipRole.STUDENT);
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(foreign);
        enrollment.setClassEntity(foreignClass);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        em.persist(enrollment);
        grade(enrollment, 70f, RANGE_START.atStartOfDay());
        for (String suffix : List.of("", "/summary", "/period", "/pdf", "/pdf/period")) {
            MockHttpServletRequestBuilder request = get(BASE + "/{id}" + suffix, foreign.getId());
            if (suffix.endsWith("period")) period(request);
            mvc.perform(request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles(role)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void membershipMustContainStudentRoleEvenWhenAccountIsStudent() throws Exception {
        studentMembership.setRoles(Set.of(MembershipRole.TEACHER));
        enrollment(ownClass, EnrollmentStatus.ACTIVE, SCHOOL_START);
        mvc.perform(as(get(BASE + "/{id}", student.getId()))).andExpect(status().isNotFound());
    }

    @Test
    void currentSchoolMembershipWithoutSchoolEnrollmentHistoryReturnsNotFound() throws Exception {
        enrollment(foreignClass, EnrollmentStatus.ACTIVE, SCHOOL_START);
        for (String suffix : List.of("", "/summary", "/period", "/pdf", "/pdf/period")) {
            MockHttpServletRequestBuilder request = get(BASE + "/{id}" + suffix, student.getId());
            if (suffix.endsWith("period")) period(request);
            mvc.perform(as(request)).andExpect(status().isNotFound());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/period"})
    void pdfRenderingReceivesTheSameCanonicalTranscriptAsJson(String suffix) throws Exception {
        Enrollment own = enrollment(ownClass, EnrollmentStatus.COMPLETED, SCHOOL_START);
        grade(own, 85f, RANGE_START.atStartOfDay());
        grade(own, 35f, RANGE_START.minusDays(1).atStartOfDay());
        Enrollment foreign = enrollment(foreignClass, EnrollmentStatus.COMPLETED, SCHOOL_START.minusYears(1));
        grade(foreign, 10f, RANGE_START.atStartOfDay());
        MockHttpServletRequestBuilder jsonRequest = get(BASE + "/{id}" + suffix, student.getId());
        if (!suffix.isEmpty()) period(jsonRequest);
        JsonNode expected = response(jsonRequest);
        AtomicReference<TranscriptDto> rendered = new AtomicReference<>();
        // Render controlled XHTML so this contract test isolates PDF data from legacy template formatting.
        doAnswer(invocation -> {
            IContext context = invocation.getArgument(1);
            rendered.set((TranscriptDto) context.getVariable("transcript"));
            return "<html><head><title>Transcript</title></head><body>Transcript</body></html>";
        }).when(templateEngine).process(eq("transcript/transcript"), any(IContext.class));
        MockHttpServletRequestBuilder pdfRequest = get(BASE + "/{id}/pdf" + suffix, student.getId());
        if (!suffix.isEmpty()) period(pdfRequest);
        byte[] pdf = mvc.perform(as(pdfRequest)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdf).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        JsonNode renderedJson = json.readTree(json.writeValueAsString(rendered.get()));
        assertThat(renderedJson).isEqualTo(expected);
    }

    private JsonNode response(MockHttpServletRequestBuilder request) throws Exception {
        em.flush();
        return json.readTree(mvc.perform(as(request)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request) {
        return request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"));
    }

    private MockHttpServletRequestBuilder period(MockHttpServletRequestBuilder request) {
        return request.param("startDate", RANGE_START.toString()).param("endDate", RANGE_END.toString());
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
        year.setStartDate(SCHOOL_START);
        year.setEndDate(SCHOOL_START.plusYears(1));
        em.persist(year);
        ClassEntity value = new ClassEntity();
        value.setAcademicYear(year);
        value.setName(name);
        em.persist(value);
        return value;
    }

    private <T extends BaseUser> T account(T value, UserRole role) {
        value.setEmail(UUID.randomUUID() + "@fixtures.school.test");
        value.setFirstName("Transcript");
        value.setLastName(role.name());
        value.setPassword("unused");
        value.setRole(role);
        value.setStatus(Status.ACTIVE);
        value.setIsEmailVerified(true);
        em.persist(value);
        return value;
    }

    private SchoolMembership membership(BaseUser target, School owner, MembershipRole role) {
        SchoolMembership value = new SchoolMembership();
        value.setUser(target);
        value.setSchool(owner);
        value.setRoles(Set.of(role));
        value.setStatus(MembershipStatus.ACTIVE);
        em.persist(value);
        return value;
    }

    private Enrollment enrollment(ClassEntity clazz, EnrollmentStatus status, LocalDate date) {
        Enrollment value = new Enrollment();
        value.setStudent(student);
        value.setClassEntity(clazz);
        value.setStatus(status);
        value.setEnrolledAt(date.atStartOfDay());
        em.persist(value);
        return value;
    }

    private void grade(Enrollment enrollment, float score, LocalDateTime gradedAt) {
        Grade value = new Grade();
        value.setEnrollment(enrollment);
        value.setAssignedBy(teacher);
        value.setScore(score);
        value.setGradedAt(gradedAt);
        em.persist(value);
    }

    private void attendance(School owner, LocalDate date, AttendanceStatus status) {
        Attendance value = new Attendance();
        value.setSchool(owner);
        value.setUser(student);
        value.setDate(date);
        value.setStatus(status);
        value.setUserType(UserType.STUDENT);
        em.persist(value);
    }
}
