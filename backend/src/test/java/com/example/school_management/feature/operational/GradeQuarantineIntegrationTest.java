package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.AcademicYearTestFixtures;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
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
import com.example.school_management.feature.operational.entity.Grade;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnhancedGradeRepository;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.operational.repository.GradeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Grade reads are administrative, or a student's own; parents and teachers have no
 * role-only access to student grades, and the grade creation routes run for nobody.
 * The fixture student is student A, graded by the fixture teacher; student B is graded
 * by teacher B in the same class.
 */
@IntegrationTest
class GradeQuarantineIntegrationTest {

    private static final String DATE_FROM = "2020-01-01T00:00:00";
    private static final String DATE_TO = "2040-01-01T00:00:00";

    @Autowired
    CurrentSchoolResolver currentSchool;

    @Autowired
    AcademicYearRepository academicYears;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    ClassRepository classRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    GradeRepository gradeRepository;

    @Autowired
    EnhancedGradeRepository enhancedGradeRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired SchoolMembershipRepository memberships;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Student studentA;
    private Student studentB;
    private Teacher teacherA;
    private Teacher teacherB;
    private ClassEntity schoolClass;
    private Enrollment enrollmentA;
    private Enrollment enrollmentB;
    private Grade gradeA;
    private Grade gradeB;

    @BeforeEach
    void gradeTwoStudents() {
        studentA = studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        teacherA = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();

        Student s = new Student();
        s.setRole(UserRole.STUDENT);
        s.setEmail("student-b-" + UUID.randomUUID() + "@fixtures.school.test");
        s.setFirstName("Bea");
        s.setLastName("Other");
        s.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        s.setStatus(Status.ACTIVE);
        s.setIsEmailVerified(true);
        studentB = studentRepository.save(s);
        SchoolMembership studentMembership = new SchoolMembership();
        studentMembership.setUser(studentB);
        studentMembership.setSchool(currentSchool.resolve());
        studentMembership.setRoles(Set.of(MembershipRole.STUDENT));
        studentMembership.setStatus(MembershipStatus.ACTIVE);
        memberships.save(studentMembership);

        Teacher t = new Teacher();
        t.setRole(UserRole.TEACHER);
        t.setEmail("teacher-b-" + UUID.randomUUID() + "@fixtures.school.test");
        t.setFirstName("Tina");
        t.setLastName("Other");
        t.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        t.setStatus(Status.ACTIVE);
        t.setIsEmailVerified(true);
        teacherB = teacherRepository.save(t);

        ClassEntity c = new ClassEntity();
        c.setAcademicYear(AcademicYearTestFixtures.create(academicYears, currentSchool));
        c.setName("Grade quarantine " + UUID.randomUUID());
        schoolClass = classRepository.save(c);

        enrollmentA = enrollmentRepository.save(activeEnrollment(studentA));
        enrollmentB = enrollmentRepository.save(activeEnrollment(studentB));
        gradeA = gradeRepository.save(grade(enrollmentA, teacherA));
        gradeB = gradeRepository.save(grade(enrollmentB, teacherB));
    }

    @AfterEach
    void removeGrades() {
        gradeRepository.deleteAll(gradeRepository.findAllById(List.of(gradeA.getId(), gradeB.getId())));
        enrollmentRepository.deleteAll(enrollmentRepository.findAllById(List.of(enrollmentA.getId(), enrollmentB.getId())));
        classRepository.deleteById(schoolClass.getId());
        academicYears.deleteById(schoolClass.getAcademicYear().getId());
        memberships.deleteAll(memberships.findAllByUserId(studentB.getId()));
        studentRepository.deleteById(studentB.getId());
        teacherRepository.deleteById(teacherB.getId());
    }

    @Test
    void aStudentReadsTheirOwnGradesWithoutEditRights() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);
        long id = studentA.getId();

        // Before the mapper fix every one of these failed with 500: it required a Teacher caller.
        mockMvc.perform(get("/api/v1/grades/student/{id}", id).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(gradeA.getId()))
                .andExpect(jsonPath("$.data[0].canEdit").value(false))
                .andExpect(jsonPath("$.data[0].canDelete").value(false));
        mockMvc.perform(get("/api/v1/grades/student/{id}/paged", id).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(gradeA.getId()));
        mockMvc.perform(get("/api/v1/grades/statistics/student/{id}", id).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentGrades[0].canEdit").value(false));
        mockMvc.perform(get("/api/v1/grades/statistics/student/{id}/class/{classId}", id, schoolClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalGrades").value(1));
        mockMvc.perform(get("/api/v1/grades/statistics/student/{id}/date-range", id)
                        .param("startDate", DATE_FROM).param("endDate", DATE_TO)
                        .header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalGrades").value(1));
    }

    @Test
    void aStudentCannotReadAnotherStudentsGrades() throws Exception {
        expectStudentScopedRoutesForbidden(bearer(DevFixtureLoader.STUDENT_EMAIL), studentB.getId());
    }

    @Test
    void aParentHasNoStudentGradeAccessEvenForTheirOwnChild() throws Exception {
        String parent = bearer(DevFixtureLoader.PARENT_EMAIL);

        // The fixture parent's recorded child is student A.
        expectStudentScopedRoutesForbidden(parent, studentA.getId());
        expectStudentScopedRoutesForbidden(parent, studentB.getId());
        expectForbidden(mockMvc.perform(get("/api/v1/grades/{id}", gradeA.getId()).header(HttpHeaders.AUTHORIZATION, parent)));
        expectForbidden(mockMvc.perform(get("/api/v1/grades/enrollment/{id}", enrollmentA.getId())
                .header(HttpHeaders.AUTHORIZATION, parent)));
    }

    @Test
    void aTeacherHasNoRoleOnlyAccessToStudentGrades() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);
        long classId = schoolClass.getId();

        expectStudentScopedRoutesForbidden(teacher, studentA.getId());
        for (String uri : List.of("/api/v1/grades", "/api/v1/grades/filter",
                "/api/v1/grades/" + gradeA.getId(), "/api/v1/grades/" + gradeA.getId() + "/audit-history",
                "/api/v1/grades/class/" + classId, "/api/v1/grades/class/" + classId + "/paged",
                "/api/v1/grades/statistics/class/" + classId,
                "/api/v1/grades/enrollment/" + enrollmentA.getId(),
                "/api/v1/grades/enrollment/" + enrollmentA.getId() + "/paged")) {
            expectForbidden(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, teacher)));
        }
    }

    @Test
    void teacherIdGradeRoutesAreSelfOnly() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        // The assigning teacher keeps the creator-owned edit and delete rights.
        mockMvc.perform(get("/api/v1/grades/teacher/{id}", teacherA.getId()).header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(gradeA.getId()))
                .andExpect(jsonPath("$.data[0].canEdit").value(true))
                .andExpect(jsonPath("$.data[0].canDelete").value(true));
        mockMvc.perform(get("/api/v1/grades/teacher/{id}/classes", teacherA.getId()).header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isOk());

        expectForbidden(mockMvc.perform(get("/api/v1/grades/teacher/{id}", teacherB.getId())
                .header(HttpHeaders.AUTHORIZATION, teacher)));
        expectForbidden(mockMvc.perform(get("/api/v1/grades/teacher/{id}/classes", teacherB.getId())
                .header(HttpHeaders.AUTHORIZATION, teacher)));
        expectForbidden(mockMvc.perform(get("/api/v1/grades/teacher/{id}/class/{classId}/course/{courseId}",
                teacherB.getId(), schoolClass.getId(), 1L).header(HttpHeaders.AUTHORIZATION, teacher)));
    }

    @Test
    void anAdministratorReadsGradesWithoutEditRights() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        // These used to fail with 500 because the response mapper required a Teacher caller.
        mockMvc.perform(get("/api/v1/grades/{id}", gradeB.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(gradeB.getId()))
                .andExpect(jsonPath("$.data.canEdit").value(false))
                .andExpect(jsonPath("$.data.canDelete").value(false));
        mockMvc.perform(get("/api/v1/grades/student/{id}", studentB.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(gradeB.getId()));
        mockMvc.perform(get("/api/v1/grades/class/{id}/paged", schoolClass.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/grades/enrollment/{id}", enrollmentA.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(gradeA.getId()));
        mockMvc.perform(get("/api/v1/grades/filter").param("enrollment.classEntity.id_eq", String.valueOf(schoolClass.getId()))
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/grades/statistics/class/{id}", schoolClass.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalGrades").value(2));
    }

    @Test
    void theGradeCreationRoutesRunForNobody() throws Exception {
        long grades = gradeRepository.count();
        long enhancedGrades = enhancedGradeRepository.count();
        String bulk = json(Map.of("classId", schoolClass.getId(), "courseId", 1, "assessmentType", "EXAM",
                "grades", List.of(Map.of("studentId", studentB.getId(), "value", 3))));
        String enhanced = json(Map.of("studentId", studentB.getId(), "classId", schoolClass.getId(), "courseId", 1,
                "examType", "QUIZ", "semester", "FIRST", "score", 3, "maxScore", 20));
        String bulkEntry = json(Map.of("classId", schoolClass.getId(), "courseId", 1, "examType", "QUIZ",
                "semester", "FIRST", "maxScore", 20, "grades", List.of(Map.of("studentId", studentB.getId(), "score", 3))));

        for (String caller : List.of(DevFixtureLoader.TEACHER_EMAIL, DevFixtureLoader.ADMIN_EMAIL)) {
            String bearer = bearer(caller);
            expectForbidden(mockMvc.perform(post("/api/v1/grades/bulk").header(HttpHeaders.AUTHORIZATION, bearer)
                    .contentType(MediaType.APPLICATION_JSON).content(bulk)));
            expectForbidden(mockMvc.perform(post("/api/v1/grades/enhanced").header(HttpHeaders.AUTHORIZATION, bearer)
                    .contentType(MediaType.APPLICATION_JSON).content(enhanced)));
            expectForbidden(mockMvc.perform(post("/api/v1/grades/bulk-entry").header(HttpHeaders.AUTHORIZATION, bearer)
                    .contentType(MediaType.APPLICATION_JSON).content(bulkEntry)));
        }

        assertThat(gradeRepository.count()).isEqualTo(grades);
        assertThat(enhancedGradeRepository.count()).isEqualTo(enhancedGrades);
    }

    @Test
    void theAuthorshipOracleRoutesAreGone() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        mockMvc.perform(get("/api/v1/grades/{id}/can-edit", gradeB.getId()).param("userId", String.valueOf(teacherB.getId()))
                        .header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/grades/{id}/can-delete", gradeB.getId()).param("userId", String.valueOf(teacherB.getId()))
                        .header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isNotFound());
    }

    @Test
    void pagedGradeReadsSortOnlyByApprovedProperties() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        mockMvc.perform(get("/api/v1/grades/student/{id}/paged", studentA.getId()).param("sort", "gradedAt,desc")
                        .header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(gradeA.getId()));
        mockMvc.perform(get("/api/v1/grades").param("sort", "score,asc").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());

        expectBadRequest(mockMvc.perform(get("/api/v1/grades/student/{id}/paged", studentA.getId())
                        .param("sort", "enrollment.student.password,asc").header(HttpHeaders.AUTHORIZATION, student)),
                "Unsupported sort field 'enrollment.student.password'");
        expectBadRequest(mockMvc.perform(get("/api/v1/grades").param("sort", "assignedBy.email,desc")
                        .header(HttpHeaders.AUTHORIZATION, admin)),
                "Unsupported sort field 'assignedBy.email'");
        expectBadRequest(mockMvc.perform(get("/api/v1/grades/class/{id}/paged", schoolClass.getId())
                        .param("sort", "assignedBy.password").header(HttpHeaders.AUTHORIZATION, admin)),
                "Unsupported sort field 'assignedBy.password'");
        expectBadRequest(mockMvc.perform(get("/api/v1/grades/enrollment/{id}/paged", enrollmentA.getId())
                        .param("sort", "enrollment.student.email").header(HttpHeaders.AUTHORIZATION, admin)),
                "Unsupported sort field 'enrollment.student.email'");
    }

    private void expectStudentScopedRoutesForbidden(String bearer, long studentId) throws Exception {
        for (String uri : List.of("/api/v1/grades/student/{id}", "/api/v1/grades/student/{id}/paged",
                "/api/v1/grades/statistics/student/{id}")) {
            expectForbidden(mockMvc.perform(get(uri, studentId).header(HttpHeaders.AUTHORIZATION, bearer)));
        }
        expectForbidden(mockMvc.perform(get("/api/v1/grades/statistics/student/{id}/class/{classId}", studentId, schoolClass.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer)));
        expectForbidden(mockMvc.perform(get("/api/v1/grades/statistics/student/{id}/date-range", studentId)
                .param("startDate", DATE_FROM).param("endDate", DATE_TO).header(HttpHeaders.AUTHORIZATION, bearer)));
        for (String uri : List.of("/api/v1/grades/student/{id}/sheet", "/api/v1/grades/student/{id}/export")) {
            expectForbidden(mockMvc.perform(get(uri, studentId).param("semester", "FIRST")
                    .header(HttpHeaders.AUTHORIZATION, bearer)));
        }
    }

    private Enrollment activeEnrollment(Student student) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassEntity(schoolClass);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
    }

    private static Grade grade(Enrollment enrollment, Teacher assignedBy) {
        Grade grade = new Grade();
        grade.setEnrollment(enrollment);
        grade.setAssignedBy(assignedBy);
        grade.setContent("Quiz");
        grade.setScore(14f);
        grade.setGradedAt(LocalDateTime.now().minusHours(1));
        return grade;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private void expectForbidden(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("ACCESS_DENIED"));
    }

    private void expectBadRequest(ResultActions result, String detail) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.27." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
