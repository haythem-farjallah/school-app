package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.AcademicYearTestFixtures;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StaffRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Enrollment and student-account administration belong to administrators and staff.
 * Teachers keep the read-only student directory; parents have no student-scoped access.
 * The fixture student is student A (the fixture parent's recorded child); student B is
 * enrolled in the same class.
 */
@IntegrationTest
class SchoolAdministrationQuarantineIntegrationTest {

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
    StaffRepository staffRepository;

    @Autowired
    ClassRepository classRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Student studentA;
    private Student studentB;
    private Staff staff;
    private ClassEntity schoolClass;
    private ClassEntity otherClass;
    private Enrollment enrollmentA;
    private Enrollment enrollmentB;

    @BeforeEach
    void enrollTwoStudents() {
        studentA = studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();

        Student s = new Student();
        s.setRole(UserRole.STUDENT);
        s.setEmail("student-b-" + UUID.randomUUID() + "@fixtures.school.test");
        s.setFirstName("Bea");
        s.setLastName("Other");
        s.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        s.setStatus(Status.ACTIVE);
        s.setIsEmailVerified(true);
        studentB = studentRepository.save(s);

        Staff st = new Staff();
        st.setRole(UserRole.STAFF);
        st.setEmail("staff-" + UUID.randomUUID() + "@fixtures.school.test");
        st.setFirstName("Sara");
        st.setLastName("Staff");
        st.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        st.setStatus(Status.ACTIVE);
        st.setIsEmailVerified(true);
        staff = staffRepository.save(st);

        schoolClass = classRepository.save(schoolClass("Administration"));
        otherClass = classRepository.save(schoolClass("Administration target"));
        enrollmentA = enrollmentRepository.save(activeEnrollment(studentA));
        enrollmentB = enrollmentRepository.save(activeEnrollment(studentB));
    }

    @AfterEach
    void removeEnrollments() {
        enrollmentRepository.deleteAll(enrollmentRepository.findAllById(List.of(enrollmentA.getId(), enrollmentB.getId())));
        classRepository.deleteAllById(List.of(schoolClass.getId(), otherClass.getId()));
        academicYears.deleteAllById(List.of(schoolClass.getAcademicYear().getId(), otherClass.getAcademicYear().getId()));
        studentRepository.deleteById(studentB.getId());
        staffRepository.deleteById(staff.getId());
    }

    @Test
    void aTeacherCannotAdministerEnrollment() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);
        long classId = schoolClass.getId();

        expectForbidden(mockMvc.perform(post("/api/v1/enrollments/enroll").header(HttpHeaders.AUTHORIZATION, teacher)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("studentId", studentB.getId(), "classId", otherClass.getId())))));
        expectForbidden(mockMvc.perform(put("/api/v1/enrollments/{id}/transfer", enrollmentB.getId())
                .header(HttpHeaders.AUTHORIZATION, teacher)
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newClassId", otherClass.getId())))));
        expectForbidden(mockMvc.perform(put("/api/v1/enrollments/{id}/status", enrollmentB.getId())
                .header(HttpHeaders.AUTHORIZATION, teacher)
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "WITHDRAWN")))));

        for (MockHttpServletRequestBuilder read : List.of(
                get("/api/v1/enrollments"),
                get("/api/v1/enrollments/class/{id}", classId),
                get("/api/v1/enrollments/status/ACTIVE"),
                get("/api/v1/enrollments/date-range").param("startDate", "2020-01-01T00:00:00").param("endDate", "2040-01-01T00:00:00"),
                get("/api/v1/enrollments/stats/class/{id}", classId),
                get("/api/v1/enrollments/can-enroll").param("studentId", String.valueOf(studentB.getId()))
                        .param("classId", String.valueOf(otherClass.getId())),
                get("/api/v1/enrollments/{id}", enrollmentB.getId()),
                get("/api/v1/enrollments/student/{id}", studentB.getId()),
                get("/api/v1/enrollments/stats/student/{id}", studentB.getId()))) {
            expectForbidden(mockMvc.perform(read.header(HttpHeaders.AUTHORIZATION, teacher)));
        }

        Enrollment unchanged = enrollmentRepository.findById(enrollmentB.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(unchanged.getClassEntity().getId()).isEqualTo(classId);
        assertThat(enrollmentRepository.findAllByStudentId(studentB.getId())).extracting(e -> e.getClassEntity().getId()).containsExactly(classId);
    }

    @Test
    void staffAndAdministratorsKeepEnrollmentAdministration() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/class/{id}", schoolClass.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(staff.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/enrollments/student/{id}", studentB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(staff.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(enrollmentB.getId()));

        mockMvc.perform(put("/api/v1/enrollments/{id}/status", enrollmentB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "WITHDRAWN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
    }

    @Test
    void aParentHasNoStudentScopedEnrollmentOrClassAccess() throws Exception {
        String parent = bearer(DevFixtureLoader.PARENT_EMAIL);

        for (long studentId : List.of(studentA.getId(), studentB.getId())) {
            expectForbidden(mockMvc.perform(get("/api/v1/enrollments/student/{id}", studentId).header(HttpHeaders.AUTHORIZATION, parent)));
            expectForbidden(mockMvc.perform(get("/api/v1/enrollments/stats/student/{id}", studentId).header(HttpHeaders.AUTHORIZATION, parent)));
            expectForbidden(mockMvc.perform(get("/api/v1/classes/student/{id}", studentId).header(HttpHeaders.AUTHORIZATION, parent)));
        }
        expectForbidden(mockMvc.perform(get("/api/v1/enrollments/{id}", enrollmentA.getId()).header(HttpHeaders.AUTHORIZATION, parent)));
    }

    @Test
    void enrollmentPagesSortOnlyByApprovedProperties() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        mockMvc.perform(get("/api/v1/enrollments/student/{id}", studentA.getId()).param("sort", "enrolledAt,desc")
                        .header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(enrollmentA.getId()));
        mockMvc.perform(get("/api/v1/enrollments").param("sort", "status").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());

        expectBadRequest(mockMvc.perform(get("/api/v1/enrollments/student/{id}", studentA.getId())
                        .param("sort", "student.password,asc").header(HttpHeaders.AUTHORIZATION, student)),
                "Unsupported sort field 'student.password'");
        expectBadRequest(mockMvc.perform(get("/api/v1/enrollments").param("sort", "student.email")
                        .header(HttpHeaders.AUTHORIZATION, admin)),
                "Unsupported sort field 'student.email'");
        expectBadRequest(mockMvc.perform(get("/api/v1/enrollments/class/{id}", schoolClass.getId())
                        .param("sort", "student.otpCode").header(HttpHeaders.AUTHORIZATION, admin)),
                "Unsupported sort field 'student.otpCode'");
    }

    @Test
    void aTeacherCannotAdministerStudentAccounts() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);
        long id = studentB.getId();
        String ids = json(List.of(id));

        for (MockHttpServletRequestBuilder change : List.of(
                post("/api/v1/students").content(json(Map.of(
                        "profile", Map.of("firstName", "New", "lastName", "Student",
                                "email", "student-" + UUID.randomUUID() + "@fixtures.school.test",
                                "birthday", "2011-05-09", "gender", "F"),
                        "gradeLevel", "HIGH", "enrollmentYear", 2024))),
                patch("/api/v1/students/{id}", id).content(json(Map.of(
                        "firstName", "Changed", "lastName", "Other", "gradeLevel", "HIGH", "enrollmentYear", 2024))),
                delete("/api/v1/students/{id}", id),
                delete("/api/v1/students/bulk").content(ids),
                patch("/api/v1/students/bulk/status").content(json(Map.of("ids", List.of(id), "status", "SUSPENDED"))),
                post("/api/v1/students/export/csv").content(json(Map.of("ids", List.of(id)))),
                post("/api/v1/students/export/excel").content(json(Map.of("ids", List.of(id)))),
                post("/api/v1/students/bulk/email").content(json(Map.of("ids", List.of(id), "subject", "Hi", "message", "Hi"))))) {
            expectForbidden(mockMvc.perform(change.header(HttpHeaders.AUTHORIZATION, teacher)
                    .contentType(MediaType.APPLICATION_JSON)));
        }

        Student unchanged = studentRepository.findById(id).orElseThrow();
        assertThat(unchanged.getFirstName()).isEqualTo("Bea");
        assertThat(unchanged.getStatus()).isEqualTo(Status.ACTIVE);

        // The read-only directory stays available to teachers.
        mockMvc.perform(get("/api/v1/students/{id}", id).header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void administratorsKeepStudentAccountAdministration() throws Exception {
        mockMvc.perform(patch("/api/v1/students/bulk/status").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ids", List.of(studentB.getId()), "status", "SUSPENDED"))))
                .andExpect(status().isOk());

        assertThat(studentRepository.findById(studentB.getId()).orElseThrow().getStatus()).isEqualTo(Status.SUSPENDED);
    }

    @Test
    void theClassesOfATeacherAreSelfOnlyForTeachers() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);
        long self = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow().getId();

        mockMvc.perform(get("/api/v1/classes/teacher/{id}", self).header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isOk());
        expectForbidden(mockMvc.perform(get("/api/v1/classes/teacher/{id}", staff.getId()).header(HttpHeaders.AUTHORIZATION, teacher)));
    }

    private ClassEntity schoolClass(String name) {
        ClassEntity c = new ClassEntity();
        c.setAcademicYear(AcademicYearTestFixtures.create(academicYears, currentSchool));
        c.setName(name + " " + UUID.randomUUID());
        return c;
    }

    private Enrollment activeEnrollment(Student student) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassEntity(schoolClass);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
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
                            request.setRemoteAddr("10.0.28." + clientAddress.incrementAndGet());
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
