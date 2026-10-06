package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.membership.entity.*;
import com.example.school_management.feature.operational.entity.*;
import com.example.school_management.feature.operational.entity.enums.*;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
class DashboardSchoolAccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    private School ownSchool, foreignSchool;
    private AcademicYear ownYear;
    private ClassEntity ownClass, foreignClass;
    private Student student, foreignStudent;
    private Teacher teacher, foreignTeacher;
    private Parent parent, foreignParent;
    private Administration admin, foreignAdmin;
    private SchoolMembership studentMembership;
    private Enrollment ownEnrollment;

    @BeforeEach
    void setUp() {
        ownSchool = school("Dashboard current"); foreignSchool = school("Dashboard foreign");
        doReturn(ownSchool).when(currentSchool).resolve();
        ownYear = year(ownSchool, "Current year", true);
        ownClass = clazz(ownYear, "Current class"); foreignClass = clazz(year(foreignSchool, "Foreign year", true), "Foreign class");
        student = account(new Student(), UserRole.STUDENT); foreignStudent = account(new Student(), UserRole.STUDENT);
        teacher = account(new Teacher(), UserRole.TEACHER); foreignTeacher = account(new Teacher(), UserRole.TEACHER);
        parent = account(new Parent(), UserRole.PARENT); foreignParent = account(new Parent(), UserRole.PARENT);
        admin = account(new Administration(), UserRole.ADMIN); foreignAdmin = account(new Administration(), UserRole.ADMIN);
        studentMembership = membership(student, ownSchool, MembershipRole.STUDENT);
        membership(student, foreignSchool, MembershipRole.STUDENT); membership(foreignStudent, foreignSchool, MembershipRole.STUDENT);
        membership(teacher, ownSchool, MembershipRole.TEACHER); membership(teacher, foreignSchool, MembershipRole.TEACHER);
        membership(foreignTeacher, foreignSchool, MembershipRole.TEACHER);
        membership(parent, ownSchool, MembershipRole.GUARDIAN); membership(parent, foreignSchool, MembershipRole.GUARDIAN);
        membership(foreignParent, foreignSchool, MembershipRole.GUARDIAN);
        membership(admin, ownSchool, MembershipRole.ADMIN); membership(foreignAdmin, foreignSchool, MembershipRole.ADMIN);
        ownEnrollment = enrollment(student, ownClass, EnrollmentStatus.ACTIVE);
        Enrollment foreignEnrollment = enrollment(student, foreignClass, EnrollmentStatus.ACTIVE);
        enrollment(foreignStudent, foreignClass, EnrollmentStatus.ACTIVE);
        grade(ownEnrollment, 14f); grade(foreignEnrollment, 2f);
        assignment(ownClass, course(ownSchool, "Current course")); assignment(foreignClass, course(foreignSchool, "Foreign course"));
        parent.getChildren().addAll(List.of(student, foreignStudent));
        absent(student, ownSchool, ownClass, LocalDate.of(2031, 10, 1));
        absent(student, foreignSchool, foreignClass, LocalDate.of(2031, 10, 2));
        em.flush();
    }

    @Test
    void foreignRoleDashboardsAndBaseUserAreNotFound() throws Exception {
        for (var route : List.of(get("/api/v1/dashboard/student/{id}", foreignStudent.getId()),
                get("/api/v1/dashboard/teacher/{id}", foreignTeacher.getId()),
                get("/api/v1/dashboard/parent/{id}", foreignParent.getId()),
                get("/api/v1/dashboard/admin/{id}", foreignAdmin.getId()),
                get("/api/v1/dashboard/base/{id}", foreignStudent.getId()))) response(route, 404);
    }

    @Test
    void studentUsesSchoolGradesHistoryAndOnlyActiveCurrentYearClass() throws Exception {
        ClassEntity historical = clazz(year(ownSchool, "Historical year", false), "Historical class");
        enrollment(student, historical, EnrollmentStatus.ACTIVE);
        enrollment(student, clazz(ownYear, "Completed class"), EnrollmentStatus.COMPLETED);
        em.flush();
        JsonNode data = response(get("/api/v1/dashboard/student/{id}", student.getId()), 200);
        assertThat(data.path("stats").path("totalEnrollments").asInt()).isEqualTo(3);
        assertThat(data.path("stats").path("averageGrade").asDouble()).isEqualTo(14);
        assertThat(data.path("recentGrades")).hasSize(1);
        assertThat(data.path("recentGrades").get(0).path("score").asDouble()).isEqualTo(14);
        assertThat(data.path("enrolledClasses")).hasSize(1);
        JsonNode clazz = data.path("enrolledClasses").get(0);
        assertThat(clazz.path("classId").asLong()).isEqualTo(ownClass.getId());
        assertThat(clazz.path("totalStudents").asLong()).isEqualTo(1);
        assertThat(clazz.path("schedule").asText()).isEmpty();
        assertThat(data.path("stats").path("completedCourses").isNull()).isTrue();
        assertThat(data.path("stats").path("totalAssignments").isNull()).isTrue();
        assertThat(data.path("stats").path("currentGPA").asText()).isEqualTo("N/A");
        assertThat(data.path("upcomingEvents")).isEmpty();
        assertThat(data.toString()).doesNotContain("85.5", "Good Standing", "Mathematics Exam", "Room 101", "Foreign class", "Historical class");
    }

    @Test
    void teacherUsesScopedAssignmentsDirectClassesGradesAndActiveRosters() throws Exception {
        ClassEntity direct = clazz(ownYear, "Direct class"); direct.getTeachers().add(teacher);
        ClassEntity foreignDirect = clazz(foreignClass.getAcademicYear(), "Foreign direct"); foreignDirect.getTeachers().add(teacher);
        Student terminal = account(new Student(), UserRole.STUDENT); membership(terminal, ownSchool, MembershipRole.STUDENT);
        enrollment(terminal, ownClass, EnrollmentStatus.COMPLETED);
        ownClass.getCourses().add(course(ownSchool, "Direct current course"));
        em.flush();
        JsonNode data = response(get("/api/v1/dashboard/teacher/{id}", teacher.getId()), 200);
        assertThat(data.path("classes")).hasSize(2);
        assertThat(data.path("stats").path("totalClasses").asInt()).isEqualTo(2);
        assertThat(data.path("stats").path("totalStudents").asInt()).isEqualTo(1);
        assertThat(data.path("stats").path("totalCourses").asInt()).isEqualTo(2);
        assertThat(data.path("stats").path("activeCourses").asInt()).isEqualTo(2);
        assertThat(data.path("stats").path("averageClassGrade").asDouble()).isEqualTo(14);
        assertThat(data.path("stats").path("pendingGrades").isNull()).isTrue();
        for (JsonNode clazz : data.path("classes")) {
            assertThat(clazz.path("totalAssignments").isNull()).isTrue();
            assertThat(clazz.path("pendingGrades").isNull()).isTrue();
            if (clazz.path("classId").asLong() == ownClass.getId()) {
                assertThat(clazz.path("enrolledStudents").asInt()).isEqualTo(1);
                assertThat(clazz.path("averageGrade").asDouble()).isEqualTo(14);
            }
        }
        assertThat(data.path("pendingTasks")).isEmpty(); assertThat(data.path("studentAlerts")).isEmpty();
        assertThat(data.toString()).doesNotContain("Foreign class", "Foreign direct", "John Doe", "82.3");
    }

    @Test
    void teacherGradeSummaryPreservesWeightedAcrossClassAverageAndLastActivity() throws Exception {
        ClassEntity second = clazz(ownYear, "Second class"); second.getTeachers().add(teacher);
        Student other = account(new Student(), UserRole.STUDENT);
        Enrollment secondEnrollment = enrollment(other, second, EnrollmentStatus.ACTIVE);
        grade(secondEnrollment, 20f); grade(secondEnrollment, 26f);
        Grade latest = new Grade(); latest.setEnrollment(ownEnrollment); latest.setAssignedBy(teacher);
        latest.setContent("Recent quiz"); latest.setScore(12f);
        latest.setGradedAt(LocalDateTime.of(2031, 10, 5, 11, 0)); em.persist(latest);
        em.flush(); em.clear();
        JsonNode data = response(get("/api/v1/dashboard/teacher/{id}", teacher.getId()), 200);
        assertThat(data.path("stats").path("averageClassGrade").asDouble()).isEqualTo(18);
        assertThat(data.path("stats").path("totalStudents").asInt()).isEqualTo(2);
        for (JsonNode clazz : data.path("classes")) {
            assertThat(clazz.path("enrolledStudents").asInt()).isEqualTo(1);
            if (clazz.path("classId").asLong() == ownClass.getId()) {
                assertThat(clazz.path("averageGrade").asDouble()).isEqualTo(13);
                assertThat(clazz.path("lastActivity").asText()).startsWith("2031-10-05T11:00");
            } else assertThat(clazz.path("averageGrade").asDouble()).isEqualTo(23);
        }
    }

    @Test
    void parentChildrenAndCanonicalChildInformationAreSchoolScoped() throws Exception {
        JsonNode data = response(get("/api/v1/dashboard/parent/{id}", parent.getId()), 200);
        assertThat(data.path("children")).hasSize(1);
        JsonNode child = data.path("children").get(0);
        assertThat(child.path("studentId").asLong()).isEqualTo(student.getId());
        assertThat(child.path("currentClass").asText()).isEqualTo("Current class");
        assertThat(child.path("averageGrade").asDouble()).isEqualTo(14);
        assertThat(child.path("totalAbsences").asInt()).isEqualTo(1);
        assertThat(child.path("academicStanding").asText()).isEqualTo("N/A");
        assertThat(data.path("schoolUpdates")).isEmpty(); assertThat(data.path("upcomingEvents")).isEmpty();
        assertThat(data.toString()).doesNotContain("Grade 10A", "88.5", "Holiday Schedule", "Parent-Teacher Conference", "John Doe");
    }

    @Test
    void adminCountsSchoolMembershipsClassesCoursesAndActiveEnrollments() throws Exception {
        enrollment(student, clazz(year(ownSchool, "Old completed year", false), "Completed class"), EnrollmentStatus.COMPLETED);
        studentMembership.setStatus(MembershipStatus.INACTIVE);
        em.flush();
        JsonNode data = response(get("/api/v1/dashboard/admin/{id}", admin.getId()), 200);
        JsonNode stats = data.path("systemStats");
        assertThat(stats.path("totalStudents").asInt()).isEqualTo(1);
        assertThat(stats.path("totalTeachers").asInt()).isEqualTo(1);
        assertThat(stats.path("totalParents").asInt()).isEqualTo(1);
        assertThat(stats.path("totalClasses").asInt()).isEqualTo(2);
        assertThat(stats.path("totalCourses").asInt()).isEqualTo(1);
        assertThat(stats.path("activeEnrollments").asInt()).isEqualTo(1);
        assertThat(stats.path("systemHealth").isNull()).isTrue();
        assertThat(stats.path("serverStatus").asText()).isEqualTo("N/A");
        for (String field : List.of("systemAlerts", "enrollmentTrends", "performanceMetrics", "recentSystemActivities")) assertThat(data.path(field)).isEmpty();
        assertThat(data.toString()).doesNotContain("98.5", "3.2", "92.5", "System Maintenance", "New student registered", "January");
    }

    @ParameterizedTest
    @EnumSource(value = MembershipStatus.class, names = {"ACTIVE", "SUSPENDED", "INACTIVE"})
    void studentHistoricalDashboardSurvivesMembershipStatusChanges(MembershipStatus membershipStatus) throws Exception {
        studentMembership.setStatus(membershipStatus); em.flush();
        JsonNode data = response(get("/api/v1/dashboard/student/{id}", student.getId()), 200);
        assertThat(data.path("recentGrades")).hasSize(1);
    }

    @Test
    void accountRoleAloneDoesNotAuthorizeDashboardResource() throws Exception {
        studentMembership.setRoles(Set.of(MembershipRole.TEACHER));
        em.flush(); response(get("/api/v1/dashboard/student/{id}", student.getId()), 404);
    }

    @Test
    void currentUserRoutesReuseScopedRoleDashboardPaths() throws Exception {
        for (BaseUser account : List.of(student, teacher, parent, admin)) {
            JsonNode current = response(get("/api/v1/dashboard/current-user").with(user(account.getEmail()).roles(account.getRole().name())), 200, false);
            String resource = account.getRole() == UserRole.PARENT ? "parent" : account.getRole().name().toLowerCase();
            JsonNode explicit = response(get("/api/v1/dashboard/" + resource + "/{id}", account.getId()), 200);
            assertThat(current).isEqualTo(explicit);
        }
    }

    @Test
    void staffSelfDashboardAndBaseRemainAvailableWithoutMembershipAndContainNoFakeActivity() throws Exception {
        Staff staff = account(new Staff(), UserRole.STAFF); em.flush();
        JsonNode data = response(get("/api/v1/dashboard/current-user").with(user(staff.getEmail()).roles("STAFF")), 200, false);
        assertThat(data.path("type").asText()).isEqualTo("STAFF");
        for (String field : List.of("assignedTasks", "maintenanceAlerts", "recentActivities")) assertThat(data.path(field)).isEmpty();
        assertThat(data.path("baseInfo").path("recentActivities")).isEmpty();
        assertThat(data.path("baseInfo").path("lastLogin").isNull()).isTrue();
        response(get("/api/v1/dashboard/base/{id}", staff.getId()).with(user(staff.getEmail()).roles("STAFF")), 200, false);
    }

    private JsonNode response(MockHttpServletRequestBuilder request, int expected) throws Exception { return response(request, expected, true); }
    private JsonNode response(MockHttpServletRequestBuilder request, int expected, boolean adminRequest) throws Exception {
        if (adminRequest) request.with(user(DevFixtureLoader.ADMIN_EMAIL).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ADMIN_READ_WRITE")));
        return json.readTree(mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString()).path("data");
    }
    private School school(String name) { School school = new School(); school.setName(name); em.persist(school); return school; }
    private AcademicYear year(School school, String name, boolean active) {
        AcademicYear year = new AcademicYear(); year.setSchool(school); year.setName(name); year.setActive(active);
        year.setStartDate(LocalDate.of(2031, 9, 1)); year.setEndDate(LocalDate.of(2032, 6, 30)); em.persist(year); return year;
    }
    private ClassEntity clazz(AcademicYear year, String name) { ClassEntity clazz = new ClassEntity(); clazz.setAcademicYear(year); clazz.setName(name); em.persist(clazz); return clazz; }
    private Course course(School school, String name) { Course course = new Course(); course.setSchool(school); course.setName(name); course.setCode(UUID.randomUUID().toString().substring(0, 8)); em.persist(course); return course; }
    private <T extends BaseUser> T account(T account, UserRole role) {
        account.setEmail(UUID.randomUUID() + "@dashboard.school.test"); account.setFirstName("Dashboard"); account.setLastName(role.name());
        account.setPassword("unused"); account.setRole(role); account.setStatus(Status.ACTIVE); account.setIsEmailVerified(true); em.persist(account); return account;
    }
    private SchoolMembership membership(BaseUser user, School school, MembershipRole role) {
        SchoolMembership membership = new SchoolMembership(); membership.setUser(user); membership.setSchool(school); membership.setRoles(Set.of(role));
        membership.setStatus(MembershipStatus.ACTIVE); em.persist(membership); return membership;
    }
    private Enrollment enrollment(Student student, ClassEntity clazz, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment(); enrollment.setStudent(student); enrollment.setClassEntity(clazz); enrollment.setStatus(status); em.persist(enrollment); return enrollment;
    }
    private void grade(Enrollment enrollment, float score) { Grade grade = new Grade(); grade.setEnrollment(enrollment); grade.setAssignedBy(teacher); grade.setContent("Dashboard quiz"); grade.setScore(score); grade.setGradedAt(LocalDateTime.of(2031, 10, 1, 10, 0)); em.persist(grade); }
    private void assignment(ClassEntity clazz, Course course) { TeachingAssignment assignment = new TeachingAssignment(); assignment.setTeacher(teacher); assignment.setClazz(clazz); assignment.setCourse(course); em.persist(assignment); }
    private void absent(Student student, School school, ClassEntity clazz, LocalDate date) {
        Attendance attendance = new Attendance(); attendance.setUser(student); attendance.setSchool(school); attendance.setClassEntity(clazz);
        attendance.setDate(date); attendance.setStatus(AttendanceStatus.ABSENT); attendance.setUserType(UserType.STUDENT); em.persist(attendance);
    }
}
