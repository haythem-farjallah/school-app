package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.operational.dto.CreateEnhancedGradeRequest;
import com.example.school_management.feature.operational.dto.BulkGradeEntryRequest;
import com.example.school_management.feature.operational.dto.BulkEnhancedGradeEntryRequest;
import com.example.school_management.feature.operational.entity.EnhancedGrade;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnhancedGradeRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.example.school_management.feature.operational.service.GradeService;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Transactional
class GradeSchoolAccessIntegrationTest {
    private static final String BASE = "/api/v1/grades";
    private static final CreateEnhancedGradeRequest.Semester SEMESTER = CreateEnhancedGradeRequest.Semester.FIRST;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired GradeRepository grades;
    @Autowired EnhancedGradeRepository enhancedGrades;
    @Autowired GradeService gradeService;
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
    private SchoolMembership studentMembership;
    private Enrollment ownEnrollment;
    private Enrollment foreignEnrollment;
    private Grade ownGrade;
    private Grade secondOwnGrade;
    private Grade foreignGrade;

    @BeforeEach
    void setUp() {
        school = school("Grade current");
        foreignSchool = school("Grade foreign");
        doReturn(school).when(currentSchool).resolve();
        ownClass = clazz(school, "Current class", true);
        foreignClass = clazz(foreignSchool, "Foreign class", true);
        ownCourse = course(school, "Current course");
        foreignCourse = course(foreignSchool, "Foreign course");
        ownClass.getCourses().add(ownCourse);
        foreignClass.getCourses().add(foreignCourse);
        student = account(new Student(), UserRole.STUDENT);
        foreignStudent = account(new Student(), UserRole.STUDENT);
        teacher = account(new Teacher(), UserRole.TEACHER);
        foreignTeacher = account(new Teacher(), UserRole.TEACHER);
        studentMembership = membership(student, school, MembershipRole.STUDENT);
        membership(student, foreignSchool, MembershipRole.STUDENT);
        membership(foreignStudent, foreignSchool, MembershipRole.STUDENT);
        membership(teacher, school, MembershipRole.TEACHER);
        membership(teacher, foreignSchool, MembershipRole.TEACHER);
        membership(foreignTeacher, foreignSchool, MembershipRole.TEACHER);
        foreignEnrollment = enrollment(student, foreignClass, EnrollmentStatus.ACTIVE);
        ownEnrollment = enrollment(student, ownClass, EnrollmentStatus.ACTIVE);
        ownGrade = grade(ownEnrollment, 12f);
        secondOwnGrade = grade(ownEnrollment, 16f);
        foreignGrade = grade(foreignEnrollment, 2f);
        assignment(ownClass, ownCourse);
        assignment(foreignClass, foreignCourse);
        em.flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void foreignGradeAndAuditHistoryAreNotFound(String role) throws Exception {
        for (String suffix : List.of("", "/audit-history")) {
            mvc.perform(get(BASE + "/{id}" + suffix, foreignGrade.getId())
                            .with(user(DevFixtureLoader.ADMIN_EMAIL).roles(role)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    void assigningTeacherCannotUpdateOrDeleteTheirForeignGrade() throws Exception {
        for (MockHttpServletRequestBuilder route : List.of(
                patch(BASE + "/{id}", foreignGrade.getId()).content("{\"score\":18}"),
                delete(BASE + "/{id}", foreignGrade.getId()).content("{\"reason\":\"Incorrect entry\"}"))) {
            mvc.perform(route.contentType(MediaType.APPLICATION_JSON).with(user(teacher.getEmail()).roles("TEACHER")))
                    .andExpect(status().isNotFound());
        }
        em.flush();
        em.clear();
        assertThat(grades.findById(foreignGrade.getId())).hasValueSatisfying(g -> assertThat(g.getScore()).isEqualTo(2f));
    }

    @Test
    void multiSchoolStudentAndTeacherReadsAndStatisticsContainOnlyCurrentSchoolGrades() throws Exception {
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE + "/student/{id}", student.getId()),
                get(BASE + "/class/{id}", ownClass.getId()),
                get(BASE + "/teacher/{id}", teacher.getId()),
                get(BASE + "/enrollment/{id}", ownEnrollment.getId()))) {
            JsonNode data = response(route, 200);
            assertThat(data).hasSize(2);
            assertThat(data.findValuesAsText("id")).containsExactlyInAnyOrder(ownGrade.getId().toString(), secondOwnGrade.getId().toString());
            assertThat(data.toString()).doesNotContain("Foreign class");
        }
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE + "/statistics/student/{id}", student.getId()),
                get(BASE + "/statistics/student/{id}/class/{clazz}", student.getId(), ownClass.getId()),
                get(BASE + "/statistics/class/{id}", ownClass.getId()),
                get(BASE + "/statistics/student/{id}/date-range", student.getId())
                        .param("startDate", "2020-01-01T00:00:00").param("endDate", "2040-01-01T00:00:00"))) {
            JsonNode data = response(route, 200);
            assertThat(data.path("totalGrades").asInt()).isEqualTo(2);
            assertThat(data.path("averageGrade").asDouble()).isEqualTo(14.0);
        }
        JsonNode self = response(get(BASE + "/student/{id}", student.getId()).with(user(student.getEmail()).roles("STUDENT")), 200, false);
        assertThat(self).hasSize(2);
        assertThat(self.get(0).path("canEdit").asBoolean()).isFalse();
        assertThat(self.get(0).path("canDelete").asBoolean()).isFalse();
    }

    @Test
    void foreignResourcesAreNotFoundBeforeReadsOrStatistics() throws Exception {
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE + "/student/{id}", foreignStudent.getId()),
                get(BASE + "/student/{id}/paged", foreignStudent.getId()),
                get(BASE + "/statistics/student/{id}", foreignStudent.getId()),
                get(BASE + "/statistics/student/{id}/date-range", foreignStudent.getId())
                        .param("startDate", "2020-01-01T00:00:00").param("endDate", "2040-01-01T00:00:00"),
                get(BASE + "/statistics/student/{id}/class/{clazz}", foreignStudent.getId(), ownClass.getId()),
                get(BASE + "/statistics/student/{id}/class/{clazz}", student.getId(), foreignClass.getId()),
                get(BASE + "/class/{id}", foreignClass.getId()),
                get(BASE + "/class/{id}/paged", foreignClass.getId()),
                get(BASE + "/statistics/class/{id}", foreignClass.getId()),
                get(BASE + "/enrollment/{id}", foreignEnrollment.getId()),
                get(BASE + "/enrollment/{id}/paged", foreignEnrollment.getId()),
                get(BASE + "/teacher/{id}", foreignTeacher.getId()),
                get(BASE + "/teacher/{id}/classes", foreignTeacher.getId()))) {
            response(route, 404);
        }
    }

    @Test
    @WithMockUser(username = DevFixtureLoader.ADMIN_EMAIL, roles = "ADMIN")
    void allPagedReadsScopeContentAndTotalsBeforePagination() throws Exception {
        PageRequest pageable = PageRequest.of(0, 1);
        for (var page : List.of(gradeService.getAllGrades(pageable, null, null),
                gradeService.getGradesByStudentId(student.getId(), pageable),
                gradeService.getGradesByClassId(ownClass.getId(), pageable),
                gradeService.getGradesByEnrollmentId(ownEnrollment.getId(), pageable))) {
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
        for (String path : List.of(BASE, BASE + "/student/" + student.getId() + "/paged",
                BASE + "/class/" + ownClass.getId() + "/paged", BASE + "/enrollment/" + ownEnrollment.getId() + "/paged")) {
            for (int page = 0; page < 2; page++) {
                JsonNode data = response(get(path).param("size", "1").param("page", String.valueOf(page)), 200);
                assertThat(data.path("totalElements").asInt()).isEqualTo(2);
                assertThat(data.path("content")).hasSize(1);
                assertThat(data.path("content").get(0).path("id").asLong()).isIn(ownGrade.getId(), secondOwnGrade.getId());
            }
        }
    }

    @Test
    @WithMockUser(username = DevFixtureLoader.ADMIN_EMAIL, roles = "ADMIN")
    void searchAndLegacyCourseFiltersExcludeForeignRowsEvenWithSharedCourseLinks() throws Exception {
        // The legacy filter selects classes associated with a course, so deliberately create
        // a bad cross-school course link to ensure that the mandatory Grade boundary wins.
        foreignClass.getCourses().add(ownCourse);
        em.flush();
        PageRequest pageable = PageRequest.of(0, 1);
        for (var page : List.of(gradeService.getAllGrades(pageable, "School boundary quiz", null),
                gradeService.getAllGrades(pageable, null, ownCourse.getId()),
                gradeService.getAllGrades(pageable, "School boundary quiz", ownCourse.getId()))) {
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }
        for (MockHttpServletRequestBuilder route : List.of(
                get(BASE).param("search", "School boundary quiz"),
                get(BASE).param("courseId", ownCourse.getId().toString()),
                get(BASE).param("search", "School boundary quiz").param("courseId", ownCourse.getId().toString()))) {
            JsonNode data = response(route.param("size", "1"), 200);
            assertThat(data.path("totalElements").asInt()).isEqualTo(2);
            assertThat(data.path("content").get(0).path("id").asLong()).isIn(ownGrade.getId(), secondOwnGrade.getId());
        }
        response(get(BASE).param("courseId", foreignCourse.getId().toString()), 404);
        response(get(BASE).param("search", "quiz").param("courseId", foreignCourse.getId().toString()), 404);
    }

    @Test
    @WithMockUser(username = DevFixtureLoader.ADMIN_EMAIL, roles = "ADMIN")
    void advancedFiltersCannotOverrideSchoolBoundaryOrInflateCounts() throws Exception {
        var page = gradeService.findWithAdvancedFilters(PageRequest.of(0, 1), Map.of("content_eq", new String[]{"School boundary quiz"}));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        JsonNode data = response(get(BASE + "/filter").param("content_eq", "School boundary quiz").param("size", "1"), 200);
        assertThat(data.path("totalElements").asInt()).isEqualTo(2);
        assertThat(data.path("content").get(0).path("id").asLong()).isIn(ownGrade.getId(), secondOwnGrade.getId());
        JsonNode foreign = response(get(BASE + "/filter").param("enrollment.classEntity.id_eq", foreignClass.getId().toString()), 200);
        assertThat(foreign.path("totalElements").asInt()).isZero();
        assertThat(foreign.path("content")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVE", "SUSPENDED"})
    void studentHistoricalReadsSurviveMembershipStatusChanges(MembershipStatus status) throws Exception {
        studentMembership.setStatus(status);
        student.setStatus(Status.SUSPENDED);
        ownEnrollment.setStatus(EnrollmentStatus.COMPLETED);
        em.flush();
        assertThat(response(get(BASE + "/student/{id}", student.getId()), 200)).hasSize(2);
        assertThat(response(get(BASE + "/statistics/student/{id}", student.getId()), 200).path("totalGrades").asInt()).isEqualTo(2);
    }

    @Test
    void accountRoleDoesNotReplaceRequiredSchoolMembershipRole() throws Exception {
        studentMembership.setRoles(Set.of(MembershipRole.TEACHER));
        em.createQuery("select m from SchoolMembership m where m.user.id = :id and m.school.id = :school", SchoolMembership.class)
                .setParameter("id", teacher.getId()).setParameter("school", school.getId()).getSingleResult()
                .setRoles(Set.of(MembershipRole.STUDENT));
        em.flush();
        response(get(BASE + "/student/{id}", student.getId()), 404);
        response(get(BASE + "/teacher/{id}", teacher.getId()), 404);
        response(get(BASE + "/teacher/{id}/classes", teacher.getId()), 404);
    }

    @Test
    void teacherAssignmentsRequireBothCurrentSchoolClassAndCourse() throws Exception {
        assignment(ownClass, foreignCourse);
        assignment(foreignClass, ownCourse);
        em.flush();
        JsonNode data = response(get(BASE + "/teacher/{id}/classes", teacher.getId()), 200);
        assertThat(data).hasSize(1);
        assertThat(data.get(0).path("classId").asLong()).isEqualTo(ownClass.getId());
        assertThat(data.get(0).path("courseId").asLong()).isEqualTo(ownCourse.getId());
        response(get(BASE + "/teacher/{id}/class/{clazz}/course/{course}", teacher.getId(), foreignClass.getId(), ownCourse.getId()), 404);
        response(get(BASE + "/teacher/{id}/class/{clazz}/course/{course}", teacher.getId(), ownClass.getId(), foreignCourse.getId()), 404);
        response(get(BASE + "/teacher/{id}/class/{clazz}/course/{course}", foreignTeacher.getId(), ownClass.getId(), ownCourse.getId()), 404);
        Course unassigned = course(school, "Unassigned course");
        em.flush();
        response(get(BASE + "/teacher/{id}/class/{clazz}/course/{course}", teacher.getId(), ownClass.getId(), unassigned.getId()), 403);
    }

    @Test
    void enhancedClassViewAndReviewsUseExactClassCourseSemesterAndActiveRoster() throws Exception {
        enhanced(student, ownClass, ownCourse, SEMESTER, 15.0);
        enhanced(student, foreignClass, ownCourse, SEMESTER, CreateEnhancedGradeRequest.ExamType.SECOND_EXAM, 1.0);
        enhanced(student, ownClass, foreignCourse, SEMESTER, 2.0);
        enhanced(student, ownClass, ownCourse, CreateEnhancedGradeRequest.Semester.SECOND, 3.0);
        Student terminalStudent = account(new Student(), UserRole.STUDENT);
        membership(terminalStudent, school, MembershipRole.STUDENT);
        enrollment(terminalStudent, ownClass, EnrollmentStatus.TRANSFERRED);
        enhanced(terminalStudent, ownClass, ownCourse, SEMESTER, 4.0);
        em.flush();
        JsonNode view = response(get(BASE + "/teacher/{id}/class/{clazz}/course/{course}", teacher.getId(), ownClass.getId(), ownCourse.getId())
                .with(user(teacher.getEmail()).roles("TEACHER")), 200, false);
        assertThat(view.path("students")).hasSize(1);
        assertThat(view.path("students").get(0).path("currentGrades").path("firstExam").path("score").asDouble()).isEqualTo(15.0);
        assertThat(view.path("students").get(0).path("currentGrades").path("secondExam").isNull()).isTrue();
        assertThat(view.path("students").get(0).path("average").asDouble()).isEqualTo(75.0);
        JsonNode reviews = response(get(BASE + "/staff/reviews").param("classId", ownClass.getId().toString()).param("semester", "FIRST"), 200);
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).path("subjects")).hasSize(1);
        assertThat(reviews.get(0).path("subjects").get(0).path("courseId").asLong()).isEqualTo(ownCourse.getId());
        assertThat(reviews.get(0).path("subjects").get(0).path("grades").path("firstExam").asDouble()).isEqualTo(15.0);
        assertThat(reviews.get(0).path("subjects").get(0).path("grades").path("secondExam").isNull()).isTrue();
        response(get(BASE + "/staff/reviews").param("classId", foreignClass.getId().toString()).param("semester", "FIRST"), 404);
    }

    @Test
    void approvalValidatesStudentsAndMutatesOnlyCurrentSchoolClassAndCourseRows() throws Exception {
        EnhancedGrade own = enhanced(student, ownClass, ownCourse, SEMESTER, 15.0);
        EnhancedGrade foreign = enhanced(student, foreignClass, foreignCourse, SEMESTER, 2.0);
        EnhancedGrade foreignClassRow = enhanced(student, foreignClass, ownCourse, SEMESTER, CreateEnhancedGradeRequest.ExamType.SECOND_EXAM, 3.0);
        EnhancedGrade foreignCourseRow = enhanced(student, ownClass, foreignCourse, SEMESTER, CreateEnhancedGradeRequest.ExamType.SECOND_EXAM, 4.0);
        EnhancedGrade otherSemester = enhanced(student, ownClass, ownCourse, CreateEnhancedGradeRequest.Semester.SECOND, 5.0);
        em.flush();
        response(post(BASE + "/approve").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "studentIds", List.of(student.getId()), "semester", "FIRST", "approvedBy", "Impostor"))), 200);
        em.flush();
        em.clear();
        assertThat(enhancedGrades.findById(own.getId())).hasValueSatisfying(g -> {
            assertThat(g.getIsApproved()).isTrue();
            assertThat(g.getApprovedBy()).isEqualTo(DevFixtureLoader.ADMIN_EMAIL);
        });
        for (EnhancedGrade row : List.of(foreign, foreignClassRow, foreignCourseRow, otherSemester)) {
            assertThat(enhancedGrades.findById(row.getId())).hasValueSatisfying(g -> {
                assertThat(g.getIsApproved()).isFalse();
                assertThat(g.getApprovedBy()).isNull();
            });
        }
        response(post(BASE + "/approve").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "studentIds", List.of(foreignStudent.getId()), "semester", "FIRST"))), 404);
    }

    @Test
    void gradeSheetSelectsCurrentSchoolCurrentAcademicYearAndExactEnhancedRows() throws Exception {
        ClassEntity historicalClass = clazz(school, "Historical class", false);
        enrollment(student, historicalClass, EnrollmentStatus.ACTIVE);
        enhanced(student, ownClass, ownCourse, SEMESTER, 15.0);
        enhanced(student, foreignClass, foreignCourse, SEMESTER, 1.0);
        enhanced(student, historicalClass, ownCourse, SEMESTER, CreateEnhancedGradeRequest.ExamType.SECOND_EXAM, 2.0);
        enhanced(student, ownClass, foreignCourse, SEMESTER, CreateEnhancedGradeRequest.ExamType.SECOND_EXAM, 3.0);
        enhanced(student, ownClass, ownCourse, CreateEnhancedGradeRequest.Semester.SECOND, 4.0);
        em.flush();
        JsonNode sheet = response(get(BASE + "/student/{id}/sheet", student.getId()).param("semester", "FIRST"), 200);
        assertThat(sheet.path("classId").asLong()).isEqualTo(ownClass.getId());
        assertThat(sheet.path("subjects")).hasSize(1);
        assertThat(sheet.path("subjects").get(0).path("courseId").asLong()).isEqualTo(ownCourse.getId());
        assertThat(sheet.path("subjects").get(0).path("grades").path("secondExam").isNull()).isTrue();
        assertThat(sheet.path("totalScore").asDouble()).isEqualTo(15.0);
        assertThat(sheet.toString()).doesNotContain("Foreign class", "Foreign course", "Historical class");
        ownEnrollment.setStatus(EnrollmentStatus.COMPLETED);
        em.flush();
        response(get(BASE + "/student/{id}/sheet", student.getId()).param("semester", "FIRST"), 404);
        response(get(BASE + "/student/{id}/export", student.getId()).param("semester", "FIRST"), 404);
        response(get(BASE + "/student/{id}/sheet", foreignStudent.getId()).param("semester", "FIRST"), 404);
    }

    @Test
    void gradeSheetExportIsARealPdfWithEscapedStudentAndSubjectText() throws Exception {
        student.setFirstName("Alice & <Admin>");
        ownCourse.setName("Math <script>alert('grade')</script> & Geometry");
        ownClass.setName("Class <A> & B");
        EnhancedGrade row = enhanced(student, ownClass, ownCourse, SEMESTER, 15.0);
        row.setTeacherRemarks("Keep <img src=\"https://example.test/image\" /> as text & study");
        enhanced(student, foreignClass, foreignCourse, SEMESTER, 1.0);
        em.flush();

        var response = mvc.perform(get(BASE + "/student/{id}/export", student.getId())
                        .param("semester", "FIRST").with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse();
        byte[] pdf = response.getContentAsByteArray();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(response.getHeader("Content-Disposition"))
                .isEqualTo("attachment; filename=\"grade-sheet-" + student.getId() + "-FIRST.pdf\"");
        PdfReader reader = new PdfReader(pdf);
        try {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text).contains("Alice & <Admin>", "Class <A> & B", ownCourse.getName(),
                    row.getTeacherRemarks(), "75.00", "N/A").doesNotContain("Foreign course");
        } finally {
            reader.close();
        }
    }

    @Test
    void gradeSheetWithoutSubjectsStillExportsARealPdf() throws Exception {
        byte[] pdf = mvc.perform(get(BASE + "/student/{id}/export", student.getId())
                        .param("semester", "FIRST").with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void gradeStatisticsLeaveUnimplementedTrendUnavailable(boolean hasGrades) throws Exception {
        if (!hasGrades) {
            em.remove(ownGrade);
            em.remove(secondOwnGrade);
            em.flush();
        }
        JsonNode statistics = response(get(BASE + "/statistics/student/{id}", student.getId()), 200);
        assertThat(statistics.path("totalGrades").asLong()).isEqualTo(hasGrades ? 2 : 0);
        assertThat(statistics.path("trend").isNull()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void internalBulkGradesPersistAllSameSchoolStudents(boolean enhanced) {
        Student secondStudent = account(new Student(), UserRole.STUDENT);
        membership(secondStudent, school, MembershipRole.STUDENT);
        enrollment(secondStudent, ownClass, EnrollmentStatus.ACTIVE);
        long before = enhanced ? enhancedGrades.count() : grades.count();
        em.flush();

        runBulkGrades(enhanced, secondStudent.getId());

        em.flush();
        assertThat(enhanced ? enhancedGrades.count() : grades.count()).isEqualTo(before + 2);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void internalBulkGradesValidateEveryStudentBeforePersistingAnyEntry(boolean enhanced) {
        EnhancedGrade existing = enhanced(student, ownClass, ownCourse, SEMESTER,
                CreateEnhancedGradeRequest.ExamType.QUIZ, 10.0);
        em.flush();
        long before = enhanced ? enhancedGrades.count() : grades.count();

        assertThatThrownBy(() -> runBulkGrades(enhanced, foreignStudent.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        em.flush();
        em.clear();
        assertThat(enhanced ? enhancedGrades.count() : grades.count()).isEqualTo(before);
        assertThat(enhancedGrades.findById(existing.getId())).hasValueSatisfying(row ->
                assertThat(row.getScore()).isEqualTo(10.0));
    }

    private void runBulkGrades(boolean enhanced, Long secondStudentId) {
        var previousContext = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(teacher.getEmail(), "unused", List.of()));
        SecurityContextHolder.setContext(context);
        try {
            if (enhanced) {
                gradeService.createBulkEnhancedGrades(json.convertValue(Map.of(
                        "classId", ownClass.getId(), "courseId", ownCourse.getId(), "examType", "QUIZ",
                        "semester", "FIRST", "maxScore", 20,
                        "grades", List.of(Map.of("studentId", student.getId(), "score", 14),
                                Map.of("studentId", secondStudentId, "score", 16))), BulkEnhancedGradeEntryRequest.class));
            } else {
                gradeService.enterBulkGrades(json.convertValue(Map.of(
                        "classId", ownClass.getId(), "courseId", ownCourse.getId(), "assessmentType", "EXAM",
                        "grades", List.of(Map.of("studentId", student.getId(), "value", 14),
                                Map.of("studentId", secondStudentId, "value", 16))), BulkGradeEntryRequest.class));
            }
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"student", "class", "course", "teacher"})
    void quarantinedInternalWritesRejectForeignResources(String reference) throws Exception {
        Long studentId = reference.equals("student") ? foreignStudent.getId() : student.getId();
        Long classId = reference.equals("class") ? foreignClass.getId() : ownClass.getId();
        Long courseId = reference.equals("course") ? foreignCourse.getId() : ownCourse.getId();
        String email = reference.equals("teacher") ? foreignTeacher.getEmail() : teacher.getEmail();
        BulkGradeEntryRequest canonical = json.convertValue(Map.of("classId", classId, "courseId", courseId,
                "assessmentType", "EXAM", "grades", List.of(Map.of("studentId", studentId, "value", 14))), BulkGradeEntryRequest.class);
        CreateEnhancedGradeRequest enhanced = json.convertValue(Map.of("studentId", studentId, "classId", classId,
                "courseId", courseId, "examType", "QUIZ", "semester", "FIRST", "score", 14, "maxScore", 20), CreateEnhancedGradeRequest.class);
        BulkEnhancedGradeEntryRequest bulk = json.convertValue(Map.of("classId", classId, "courseId", courseId,
                "examType", "QUIZ", "semester", "FIRST", "maxScore", 20,
                "grades", List.of(Map.of("studentId", studentId, "score", 14))), BulkEnhancedGradeEntryRequest.class);
        long canonicalCount = grades.count();
        long enhancedCount = enhancedGrades.count();
        var previousContext = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(email, "unused", List.of()));
        SecurityContextHolder.setContext(context);
        try {
            assertThatThrownBy(() -> gradeService.enterBulkGrades(canonical)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> gradeService.createEnhancedGrade(enhanced)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> gradeService.createBulkEnhancedGrades(bulk)).isInstanceOf(ResourceNotFoundException.class);
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
        assertThat(grades.count()).isEqualTo(canonicalCount);
        assertThat(enhancedGrades.count()).isEqualTo(enhancedCount);
    }

    @Test
    void repeatedGradeReadsHaveNeutralUndefinedMetricsAndApprovalIdentity() throws Exception {
        EnhancedGrade approved = enhanced(student, ownClass, ownCourse, SEMESTER, 15.0);
        approved.setIsApproved(true);
        approved.setApprovedBy(DevFixtureLoader.ADMIN_EMAIL);
        approved.setApprovedAt(LocalDateTime.now());
        em.flush();
        for (int read = 0; read < 3; read++) {
            JsonNode sheet = response(get(BASE + "/student/{id}/sheet", student.getId()).param("semester", "FIRST"), 200);
            for (String field : List.of("classRank", "attendanceRate", "totalAbsences")) {
                assertThat(sheet.has(field)).isTrue();
                assertThat(sheet.path(field).isNull()).isTrue();
            }
            assertThat(sheet.path("weightedAverage").asDouble()).isEqualTo(75.0);
            assertThat(sheet.path("approvedBy").path("staffId").isNull()).isTrue();
            assertThat(sheet.path("approvedBy").path("staffName").asText()).isEqualTo(DevFixtureLoader.ADMIN_EMAIL);
            JsonNode review = response(get(BASE + "/staff/reviews").param("classId", ownClass.getId().toString())
                    .param("semester", "FIRST"), 200).get(0);
            assertThat(review.path("classRank").isNull()).isTrue();
            assertThat(review.path("attendanceRate").isNull()).isTrue();
            JsonNode view = response(get(BASE + "/teacher/{id}/class/{clazz}/course/{course}",
                    teacher.getId(), ownClass.getId(), ownCourse.getId()), 200);
            assertThat(view.path("students").get(0).path("attendanceRate").isNull()).isTrue();
        }
    }

    @Test
    void approvalUsesAnExistingCanonicalStaffIdAndUnknownApproverRemainsNeutral() throws Exception {
        var approver = account(new com.example.school_management.feature.auth.entity.Staff(), UserRole.STAFF);
        EnhancedGrade approved = enhanced(student, ownClass, ownCourse, SEMESTER, 15.0);
        approved.setIsApproved(true);
        approved.setApprovedAt(LocalDateTime.now());
        approved.setApprovedBy(approver.getEmail());
        em.flush();
        JsonNode sheet = response(get(BASE + "/student/{id}/sheet", student.getId()).param("semester", "FIRST"), 200);
        assertThat(sheet.path("approvedBy").path("staffId").asLong()).isEqualTo(approver.getId());
        approved.setApprovedBy("historical-approver@example.test");
        em.flush();
        JsonNode historical = response(get(BASE + "/student/{id}/sheet", student.getId()).param("semester", "FIRST"), 200);
        assertThat(historical.path("approvedBy").path("staffId").isNull()).isTrue();
    }

    @Test
    @WithMockUser(username = DevFixtureLoader.ADMIN_EMAIL, roles = "ADMIN")
    void quarantinedGradeServicesHaveExplicitHttpErrorsWithoutOpeningProductionWrites() throws Exception {
        // A standalone controller has no method-security proxy. Production routes stay denyAll.
        MockMvc serviceMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new com.example.school_management.feature.operational.controller.GradeController(gradeService))
                .setControllerAdvice(new com.example.school_management.commons.exceptions.GlobalExceptionHandler()).build();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(teacher.getEmail(), "unused", List.of()));
        SecurityContextHolder.setContext(context);
        serviceMvc.perform(post(BASE + "/bulk").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "classId", ownClass.getId(), "courseId", ownCourse.getId(), "assessmentType", "EXAM"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        for (List<Map<String, Object>> entries : List.of(
                java.util.Collections.<Map<String, Object>>singletonList(null),
                List.<Map<String, Object>>of(Map.of("studentId", student.getId())),
                List.<Map<String, Object>>of(Map.of("studentId", student.getId(), "value", 21)),
                List.<Map<String, Object>>of(Map.of("studentId", student.getId(), "value", "NaN")),
                List.<Map<String, Object>>of(Map.of("studentId", student.getId(), "value", "Infinity")),
                List.<Map<String, Object>>of(Map.of("studentId", student.getId(), "value", "-Infinity")),
                List.<Map<String, Object>>of(Map.of("studentId", student.getId(), "value", 14),
                        Map.of("studentId", student.getId(), "value", 15)))) {
            serviceMvc.perform(post(BASE + "/bulk").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                            "classId", ownClass.getId(), "courseId", ownCourse.getId(), "assessmentType", "EXAM", "grades", entries))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.type").exists()).andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.detail").exists()).andExpect(jsonPath("$.instance").value(BASE + "/bulk"));
        }
        enhanced(student, ownClass, ownCourse, SEMESTER, 15.0);
        em.flush();
        serviceMvc.perform(post(BASE + "/enhanced").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "studentId", student.getId(), "classId", ownClass.getId(), "courseId", ownCourse.getId(),
                        "examType", "FIRST_EXAM", "semester", "FIRST", "score", 14, "maxScore", 20))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").exists()).andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists()).andExpect(jsonPath("$.instance").value(BASE + "/enhanced"));
    }

    private JsonNode response(MockHttpServletRequestBuilder route, int expectedStatus) throws Exception {
        return response(route, expectedStatus, true);
    }

    private JsonNode response(MockHttpServletRequestBuilder route, int expectedStatus, boolean admin) throws Exception {
        if (admin) route.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"));
        String body = mvc.perform(route).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data");
    }

    private Grade grade(Enrollment enrollment, float score) {
        Grade grade = new Grade();
        grade.setEnrollment(enrollment);
        grade.setAssignedBy(teacher);
        grade.setContent("School boundary quiz");
        grade.setScore(score);
        grade.setWeight(1f);
        grade.setGradedAt(LocalDateTime.now().minusHours(1));
        em.persist(grade);
        return grade;
    }

    private EnhancedGrade enhanced(Student target, ClassEntity clazz, Course course, CreateEnhancedGradeRequest.Semester semester, double score) {
        return enhanced(target, clazz, course, semester, CreateEnhancedGradeRequest.ExamType.FIRST_EXAM, score);
    }

    private EnhancedGrade enhanced(Student target, ClassEntity clazz, Course course, CreateEnhancedGradeRequest.Semester semester,
                                   CreateEnhancedGradeRequest.ExamType examType, double score) {
        EnhancedGrade grade = new EnhancedGrade();
        grade.setStudentId(target.getId());
        grade.setClassId(clazz.getId());
        grade.setClassName(clazz.getName());
        grade.setCourseId(course.getId());
        grade.setCourseName(course.getName());
        grade.setCourseCode(course.getCode());
        grade.setCourseCoefficient(1.0);
        grade.setTeacherId(teacher.getId());
        grade.setTeacherFirstName(teacher.getFirstName());
        grade.setTeacherLastName(teacher.getLastName());
        grade.setExamType(examType);
        grade.setSemester(semester);
        grade.setScore(score);
        grade.setMaxScore(20.0);
        grade.setGradedAt(LocalDateTime.now());
        em.persist(grade);
        return grade;
    }

    private void assignment(ClassEntity clazz, Course course) {
        TeachingAssignment assignment = new TeachingAssignment();
        assignment.setTeacher(teacher);
        assignment.setClazz(clazz);
        assignment.setCourse(course);
        em.persist(assignment);
    }

    private School school(String name) {
        School school = new School();
        school.setName(name);
        em.persist(school);
        return school;
    }

    private ClassEntity clazz(School school, String name, boolean activeYear) {
        AcademicYear year = new AcademicYear();
        year.setSchool(school);
        year.setName(name + " year");
        year.setStartDate(LocalDate.of(2029, 9, 1));
        year.setEndDate(LocalDate.of(2030, 6, 30));
        year.setActive(activeYear);
        em.persist(year);
        ClassEntity clazz = new ClassEntity();
        clazz.setAcademicYear(year);
        clazz.setName(name);
        em.persist(clazz);
        return clazz;
    }

    private Course course(School school, String name) {
        Course course = new Course();
        course.setSchool(school);
        course.setName(name);
        course.setCode(UUID.randomUUID().toString().substring(0, 8));
        em.persist(course);
        return course;
    }

    private <T extends BaseUser> T account(T account, UserRole role) {
        account.setEmail(UUID.randomUUID() + "@fixtures.school.test");
        account.setFirstName("Grade");
        account.setLastName(role.name());
        account.setPassword("unused");
        account.setRole(role);
        account.setStatus(Status.ACTIVE);
        account.setIsEmailVerified(true);
        em.persist(account);
        return account;
    }

    private SchoolMembership membership(BaseUser account, School school, MembershipRole role) {
        SchoolMembership membership = new SchoolMembership();
        membership.setUser(account);
        membership.setSchool(school);
        membership.setRoles(Set.of(role));
        membership.setStatus(MembershipStatus.ACTIVE);
        em.persist(membership);
        return membership;
    }

    private Enrollment enrollment(Student student, ClassEntity clazz, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassEntity(clazz);
        enrollment.setStatus(status);
        em.persist(enrollment);
        return enrollment;
    }
}
