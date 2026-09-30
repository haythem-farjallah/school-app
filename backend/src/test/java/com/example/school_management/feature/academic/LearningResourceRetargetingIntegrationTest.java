package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.AcademicYearTestFixtures;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Changing a learning resource's target classes and courses is limited to the teacher
 * who created it, and stays open to administrators. The fixture teacher is teacher B
 * for the resource created by another teacher.
 */
@IntegrationTest
class LearningResourceRetargetingIntegrationTest {

    private static final String DENIED = "You can only change the targets of resources you created";

    @Autowired
    AcademicYearRepository academicYears;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LearningResourceRepository resourceRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    ClassRepository classRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    CurrentSchoolResolver currentSchool;

    @Autowired
    TransactionTemplate transaction;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Teacher teacherA;
    private LearningResource resourceOfTeacherA;
    private LearningResource resourceOfFixtureTeacher;
    private ClassEntity schoolClass;
    private Course course;

    @BeforeEach
    void createResources() {
        Teacher t = new Teacher();
        t.setRole(UserRole.TEACHER);
        t.setEmail("teacher-a-" + UUID.randomUUID() + "@fixtures.school.test");
        t.setFirstName("Tara");
        t.setLastName("Owner");
        t.setPassword("not-used-for-login");
        t.setStatus(Status.ACTIVE);
        teacherA = teacherRepository.save(t);

        resourceOfTeacherA = resourceRepository.save(resource(teacherA));
        resourceOfFixtureTeacher = resourceRepository.save(
                resource(teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow()));

        ClassEntity c = new ClassEntity();
        c.setAcademicYear(AcademicYearTestFixtures.create(academicYears, currentSchool));
        c.setName("Retargeting " + UUID.randomUUID());
        schoolClass = classRepository.save(c);

        Course k = new Course();
        k.setSchool(currentSchool.resolve());
        k.setName("Retargeting course");
        k.setCode("RT-" + UUID.randomUUID().toString().substring(0, 8));
        course = courseRepository.save(k);
    }

    @AfterEach
    void deleteResources() {
        resourceRepository.deleteAll(resourceRepository.findAllById(
                List.of(resourceOfTeacherA.getId(), resourceOfFixtureTeacher.getId())));
        classRepository.deleteById(schoolClass.getId());
        academicYears.deleteById(schoolClass.getAcademicYear().getId());
        courseRepository.deleteById(course.getId());
        teacherRepository.deleteById(teacherA.getId());
    }

    @Test
    void anotherTeacherCannotRetargetTheResource() throws Exception {
        String teacherB = bearer(DevFixtureLoader.TEACHER_EMAIL);
        long id = resourceOfTeacherA.getId();

        for (MockHttpServletRequestBuilder request : List.of(
                post("/api/v1/learning-resources/{id}/classes", id).content(ids(schoolClass.getId())),
                delete("/api/v1/learning-resources/{id}/classes", id).content(ids(schoolClass.getId())),
                post("/api/v1/learning-resources/{id}/courses", id).content(ids(course.getId())),
                delete("/api/v1/learning-resources/{id}/courses", id).content(ids(course.getId())))) {
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, teacherB).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value(DENIED));
        }

        assertThat(targetClassIds(id)).isEmpty();
        assertThat(targetCourseIds(id)).isEmpty();
    }

    @Test
    void theCreatorCanRetargetTheResource() throws Exception {
        long id = resourceOfFixtureTeacher.getId();

        mockMvc.perform(post("/api/v1/learning-resources/{id}/classes", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ids(schoolClass.getId())))
                .andExpect(status().isOk());

        assertThat(targetClassIds(id)).containsExactly(schoolClass.getId());
    }

    @Test
    void anAdministratorCanRetargetAnyResource() throws Exception {
        long id = resourceOfTeacherA.getId();

        mockMvc.perform(post("/api/v1/learning-resources/{id}/courses", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ids(course.getId())))
                .andExpect(status().isOk());

        assertThat(targetCourseIds(id)).containsExactly(course.getId());
    }

    private List<Long> targetClassIds(long resourceId) {
        return transaction.execute(tx -> resourceRepository.findById(resourceId).orElseThrow()
                .getTargetClasses().stream().map(ClassEntity::getId).toList());
    }

    private List<Long> targetCourseIds(long resourceId) {
        return transaction.execute(tx -> resourceRepository.findById(resourceId).orElseThrow()
                .getTargetCourses().stream().map(Course::getId).toList());
    }

    private static LearningResource resource(Teacher creator) {
        LearningResource r = new LearningResource();
        r.setTitle("Retargeting test resource");
        r.setUrl("https://example.test/resource.pdf");
        r.setType(ResourceType.DOCUMENT);
        r.getCreatedBy().add(creator);
        return r;
    }

    private String ids(long id) {
        return "[" + id + "]";
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.25." + clientAddress.incrementAndGet());
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
