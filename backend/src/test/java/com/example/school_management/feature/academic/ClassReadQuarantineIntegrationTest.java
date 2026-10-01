package com.example.school_management.feature.academic;

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
import com.example.school_management.feature.auth.entity.Teacher;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Class listings and details carry every class's student, course and teacher ids, so they are
 * administrative. Teachers and students reach only their own classes through self-scoped routes.
 * The fixture teacher teaches the fixture student's class; student B is in another class.
 */
@IntegrationTest
class ClassReadQuarantineIntegrationTest {

    @Autowired
    CurrentSchoolResolver currentSchool;

    @Autowired
    AcademicYearRepository academicYears;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ClassRepository classRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    StaffRepository staffRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    TransactionTemplate transaction;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Student studentA;
    private Student studentB;
    private Teacher teacher;
    private Staff staff;
    private ClassEntity ownClass;
    private ClassEntity otherClass;
    private List<Enrollment> enrollments;

    @BeforeEach
    void createClasses() {
        studentA = studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        teacher = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();

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

        ownClass = classRepository.save(schoolClass("Own", teacher));
        otherClass = classRepository.save(schoolClass("Other", null));
        enrollments = enrollmentRepository.saveAll(List.of(activeEnrollment(studentA, ownClass), activeEnrollment(studentB, otherClass)));
    }

    @AfterEach
    void deleteClasses() {
        enrollmentRepository.deleteAll(enrollments);
        classRepository.deleteAllById(List.of(ownClass.getId(), otherClass.getId()));
        academicYears.deleteAllById(List.of(ownClass.getAcademicYear().getId(), otherClass.getAcademicYear().getId()));
        studentRepository.deleteById(studentB.getId());
        staffRepository.deleteById(staff.getId());
    }

    @Test
    void teachersStudentsAndParentsCannotReadBroadClassRoutes() throws Exception {
        for (String caller : List.of(DevFixtureLoader.TEACHER_EMAIL, DevFixtureLoader.STUDENT_EMAIL, DevFixtureLoader.PARENT_EMAIL)) {
            String bearer = bearer(caller);
            for (String uri : broadRoutes()) {
                expectForbidden(mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer)));
            }
        }
    }

    @Test
    void aTeacherCannotReadAStudentsClassesById() throws Exception {
        String teacherBearer = bearer(DevFixtureLoader.TEACHER_EMAIL);

        // Not even for a student in a class the teacher teaches.
        for (Student student : List.of(studentA, studentB)) {
            expectForbidden(mockMvc.perform(get("/api/v1/classes/student/{id}", student.getId())
                    .header(HttpHeaders.AUTHORIZATION, teacherBearer)));
        }
    }

    @Test
    void aStudentReadsOnlyTheirOwnClasses() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get("/api/v1/classes/student/{id}", studentA.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id").value(hasItem(ownClass.getId().intValue())))
                .andExpect(jsonPath("$.data.content[*].id").value(not(hasItem(otherClass.getId().intValue()))));
        expectForbidden(mockMvc.perform(get("/api/v1/classes/student/{id}", studentB.getId())
                .header(HttpHeaders.AUTHORIZATION, student)));
    }

    @Test
    void aTeacherReadsTheirOwnTeachingClasses() throws Exception {
        String teacherBearer = bearer(DevFixtureLoader.TEACHER_EMAIL);

        for (String uri : List.of("/api/v1/classes/teacher/me", "/api/v1/classes/teacher/" + teacher.getId())) {
            mockMvc.perform(get(uri).param("size", "100").header(HttpHeaders.AUTHORIZATION, teacherBearer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].id").value(hasItem(ownClass.getId().intValue())))
                    .andExpect(jsonPath("$.data.content[*].id").value(not(hasItem(otherClass.getId().intValue()))));
        }
    }

    @Test
    void administratorsAndStaffKeepBroadClassAccess() throws Exception {
        for (String caller : List.of(DevFixtureLoader.ADMIN_EMAIL, staff.getEmail())) {
            String bearer = bearer(caller);
            for (String uri : broadRoutes()) {
                mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isOk());
            }
            mockMvc.perform(get("/api/v1/classes/{id}", otherClass.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(jsonPath("$.data.studentIds[*]").value(hasItem(studentB.getId().intValue())));
            mockMvc.perform(get("/api/v1/classes/student/{id}", studentB.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].id").value(hasItem(otherClass.getId().intValue())));
        }
    }

    @Test
    void classMembershipCanOnlyBeChangedThroughEnrollments() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);
        String path = "/api/v1/classes/{classId}/students";

        for (var request : List.of(
                post(path + "/{studentId}", ownClass.getId(), studentB.getId()),
                delete(path + "/{studentId}", ownClass.getId(), studentA.getId()),
                patch(path, ownClass.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation\":\"ADD\",\"ids\":[" + studentB.getId() + "]}"))) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, admin))
                    .andExpect(result -> assertThat(result.getResponse().getStatus()).isBetween(400, 499));
        }

        assertThat(studentIdsOf(ownClass)).containsExactly(studentA.getId());
    }

    private List<String> broadRoutes() {
        return List.of(
                "/api/v1/classes",
                "/api/v1/classes/new",
                "/api/v1/classes/cards",
                "/api/v1/classes/" + otherClass.getId(),
                "/api/v1/classes/" + otherClass.getId() + "/details");
    }

    private Set<Long> studentIdsOf(ClassEntity schoolClass) {
        return new HashSet<>(enrollmentRepository.findActiveStudentIdsByClassId(schoolClass.getId()));
    }

    private ClassEntity schoolClass(String name, Teacher teacher) {
        ClassEntity c = new ClassEntity();
        c.setAcademicYear(AcademicYearTestFixtures.create(academicYears, currentSchool));
        c.setName(name + " " + UUID.randomUUID());
        if (teacher != null) {
            c.getTeachers().add(teacher);
        }
        return c;
    }

    private static Enrollment activeEnrollment(Student student, ClassEntity schoolClass) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassEntity(schoolClass);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
    }

    private void expectForbidden(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.35." + clientAddress.incrementAndGet());
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", DevFixtureLoader.PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
