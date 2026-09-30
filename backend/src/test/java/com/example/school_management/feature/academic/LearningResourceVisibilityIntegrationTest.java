package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.AcademicYearTestFixtures;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.academic.service.LearningResourceService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Students see public learning resources only, by id, by any list filter and by file
 * name; a file is served only through a resource that references it. Both resources
 * are created by the fixture teacher and target the same class and course.
 */
@IntegrationTest
class LearningResourceVisibilityIntegrationTest {

    private static final String FILES = "/api/v1/learning-resources/files/";
    private static final String NOT_AVAILABLE = "This learning resource is not available to you";

    @Autowired
    AcademicYearRepository academicYears;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LearningResourceRepository resourceRepository;

    @Autowired
    LearningResourceService resourceService;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    ClassRepository classRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    CurrentSchoolResolver currentSchool;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${app.file.upload.path:uploads/learning-resources}")
    String uploadPath;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private final List<Path> files = new ArrayList<>();
    private String title;
    private Teacher creator;
    private Teacher otherTeacher;
    private ClassEntity schoolClass;
    private Course course;
    private LearningResource publicResource;
    private LearningResource privateResource;
    private String publicFile;
    private String privateFile;

    @BeforeEach
    void createResources() throws Exception {
        creator = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();

        Teacher t = new Teacher();
        t.setRole(UserRole.TEACHER);
        t.setEmail("teacher-b-" + UUID.randomUUID() + "@fixtures.school.test");
        t.setFirstName("Tina");
        t.setLastName("Other");
        t.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        t.setStatus(Status.ACTIVE);
        t.setIsEmailVerified(true);
        otherTeacher = teacherRepository.save(t);

        ClassEntity c = new ClassEntity();
        c.setAcademicYear(AcademicYearTestFixtures.create(academicYears, currentSchool));
        c.setName("Visibility " + UUID.randomUUID());
        schoolClass = classRepository.save(c);
        Course k = new Course();
        k.setSchool(currentSchool.resolve());
        k.setName("Visibility course");
        k.setCode("VI-" + UUID.randomUUID().toString().substring(0, 8));
        course = courseRepository.save(k);

        title = "Visibility " + UUID.randomUUID();
        publicFile = UUID.randomUUID() + ".pdf";
        privateFile = UUID.randomUUID() + ".pdf";
        publicResource = resourceRepository.save(resource(publicFile, true));
        privateResource = resourceRepository.save(resource(privateFile, false));
        writeUpload(publicFile, "public");
        writeUpload(privateFile, "private");
    }

    @AfterEach
    void deleteResources() throws Exception {
        for (LearningResource r : List.of(publicResource, privateResource)) {
            resourceRepository.findById(r.getId()).ifPresent(resourceRepository::delete);
        }
        classRepository.deleteById(schoolClass.getId());
        academicYears.deleteById(schoolClass.getAcademicYear().getId());
        courseRepository.deleteById(course.getId());
        teacherRepository.deleteById(otherTeacher.getId());
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void aStudentReadsAPublicResourceButNotAPrivateOneById() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get("/api/v1/learning-resources/{id}", publicResource.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(publicResource.getId()));
        expectForbidden(mockMvc.perform(get("/api/v1/learning-resources/{id}", privateResource.getId())
                .header(HttpHeaders.AUTHORIZATION, student)), NOT_AVAILABLE);

        mockMvc.perform(get("/api/v1/learning-resources/{id}", privateResource.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    void everyStudentListingLeavesPrivateResourcesOut() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        // The type filter is left out: it fails for every caller on the PostgreSQL enum comparison.
        for (Map<String, String> filter : List.of(
                Map.<String, String>of(),
                Map.of("teacherId", String.valueOf(creator.getId())),
                Map.of("classId", String.valueOf(schoolClass.getId())),
                Map.of("courseId", String.valueOf(course.getId())),
                Map.of("search", title))) {
            list(student, filter)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].id").value(hasItem(publicResource.getId().intValue())))
                    .andExpect(jsonPath("$.data.content[*].id").value(not(hasItem(privateResource.getId().intValue()))));
            list(teacher, filter)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].id").value(hasItem(privateResource.getId().intValue())));
        }
    }

    @Test
    void aStudentOpensAPublicFileButNotAPrivateOne() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        for (String route : List.of("files", "preview")) {
            mockMvc.perform(get("/api/v1/learning-resources/{route}/{name}", route, publicFile).header(HttpHeaders.AUTHORIZATION, student))
                    .andExpect(status().isOk())
                    .andExpect(content().string("public"));
        }
        for (String route : List.of("files", "preview", "stream")) {
            expectForbidden(mockMvc.perform(get("/api/v1/learning-resources/{route}/{name}", route, privateFile)
                    .header(HttpHeaders.AUTHORIZATION, student)), NOT_AVAILABLE);
        }

        mockMvc.perform(get("/api/v1/learning-resources/files/{name}", privateFile)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().string("private"));
    }

    @Test
    void aFileNoResourceReferencesIsNotServed() throws Exception {
        String orphan = UUID.randomUUID() + ".pdf";
        writeUpload(orphan, "orphan");

        for (String caller : List.of(DevFixtureLoader.STUDENT_EMAIL, DevFixtureLoader.TEACHER_EMAIL)) {
            String bearer = bearer(caller);
            for (String route : List.of("files", "preview", "stream")) {
                mockMvc.perform(get("/api/v1/learning-resources/{route}/{name}", route, orphan).header(HttpHeaders.AUTHORIZATION, bearer))
                        .andExpect(status().isNotFound());
            }
        }
    }

    @Test
    void aFileOutsideTheUploadDirectoryIsNeverServed() throws Exception {
        // A public resource whose stored URL points above the upload directory.
        String escaping = "../escape-" + UUID.randomUUID() + ".pdf";
        LearningResource escape = resourceRepository.save(resource(escaping, true));
        writeUpload(escaping, "outside");
        try {
            mockMvc.perform(get(URI.create(FILES + escaping.replace("/", "%2F")))
                            .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL)))
                    .andExpect(status().isBadRequest());

            asStudent(() -> assertThatThrownBy(() -> resourceService.resolveReadableFile(escaping))
                    .isInstanceOf(ResourceNotFoundException.class));
            asStudent(() -> assertThatThrownBy(() -> resourceService.resolveReadableFile(".."))
                    .isInstanceOf(ResourceNotFoundException.class));
        } finally {
            resourceRepository.delete(escape);
        }
    }

    @Test
    void anAdministratorUpdatesAndDeletesAResourceTheyDidNotCreate() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        mockMvc.perform(put("/api/v1/learning-resources/{id}", privateResource.getId()).header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Renamed by admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Renamed by admin"));
        mockMvc.perform(delete("/api/v1/learning-resources/{id}", privateResource.getId()).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());

        assertThat(resourceRepository.existsById(privateResource.getId())).isFalse();
    }

    @Test
    void onlyTheCreatingTeacherUpdatesOrDeletesTheResource() throws Exception {
        String other = bearer(otherTeacher.getEmail());
        String uri = "/api/v1/learning-resources/" + publicResource.getId();

        expectForbidden(mockMvc.perform(put(uri).header(HttpHeaders.AUTHORIZATION, other)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Taken over\"}")),
                "You can only update resources you created");
        expectForbidden(mockMvc.perform(delete(uri).header(HttpHeaders.AUTHORIZATION, other)),
                "You can only delete resources you created");

        mockMvc.perform(put(uri).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Renamed by creator\"}"))
                .andExpect(status().isOk());
        assertThat(resourceRepository.findById(publicResource.getId()).orElseThrow().getTitle()).isEqualTo("Renamed by creator");
    }

    private LearningResource resource(String filename, boolean isPublic) {
        LearningResource r = new LearningResource();
        r.setTitle(title);
        r.setUrl(FILES + filename);
        r.setType(ResourceType.DOCUMENT);
        r.setPublic(isPublic);
        r.getCreatedBy().add(creator);
        r.getTargetClasses().add(schoolClass);
        r.getTargetCourses().add(course);
        return r;
    }

    private void writeUpload(String filename, String body) throws Exception {
        Path file = Paths.get(uploadPath).resolve(filename).normalize();
        Files.createDirectories(file.getParent());
        Files.writeString(file, body, StandardCharsets.UTF_8);
        files.add(file);
    }

    private ResultActions list(String bearer, Map<String, String> filter) throws Exception {
        var request = get("/api/v1/learning-resources").param("size", "1000").header(HttpHeaders.AUTHORIZATION, bearer);
        filter.forEach(request::param);
        return mockMvc.perform(request);
    }

    private static void asStudent(Runnable action) {
        UserDetails student = User.withUsername(DevFixtureLoader.STUDENT_EMAIL).password("unused").roles("STUDENT").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student, null, student.getAuthorities()));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void expectForbidden(ResultActions result, String detail) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.29." + clientAddress.incrementAndGet());
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
