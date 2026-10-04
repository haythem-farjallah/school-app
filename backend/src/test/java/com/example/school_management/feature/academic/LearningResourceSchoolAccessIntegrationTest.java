package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.repository.AcademicYearRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
class LearningResourceSchoolAccessIntegrationTest {
    private static final String ROOT = "/api/v1/learning-resources";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SchoolRepository schools;
    @Autowired AcademicYearRepository years;
    @Autowired ClassRepository classes;
    @Autowired CourseRepository courses;
    @Autowired TeacherRepository teachers;
    @Autowired UserRepository users;
    @Autowired SchoolMembershipRepository memberships;
    @Autowired LearningResourceRepository resources;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    @Value("${app.file.upload.path:uploads/learning-resources}") String uploadPath;

    private School school;
    private School otherSchool;
    private ClassEntity ownClass;
    private ClassEntity otherClass;
    private Course ownCourse;
    private Course otherCourse;
    private Teacher teacher;
    private Teacher foreignTeacher;
    private LearningResource publicResource;
    private LearningResource privateResource;
    private LearningResource foreignPublic;
    private LearningResource foreignPrivate;

    @BeforeEach
    void setUp() {
        school = school("Resource school A");
        otherSchool = school("Resource school B");
        doReturn(school).when(currentSchool).resolve();
        teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        membership(teacher, school, MembershipRole.TEACHER);
        membership(teacher, otherSchool, MembershipRole.TEACHER);
        for (var entry : Map.of(DevFixtureLoader.ADMIN_EMAIL, MembershipRole.ADMIN,
                DevFixtureLoader.STUDENT_EMAIL, MembershipRole.STUDENT).entrySet()) {
            var account = users.findByEmail(entry.getKey()).orElseThrow();
            membership(account, school, entry.getValue());
            membership(account, otherSchool, entry.getValue());
        }
        foreignTeacher = teacher("Foreign");
        membership(foreignTeacher, otherSchool, MembershipRole.TEACHER);
        ownClass = clazz(school, "7-A");
        otherClass = clazz(otherSchool, "7-B");
        ownCourse = course(school, "Math A");
        otherCourse = course(otherSchool, "Math B");
        publicResource = resource(school, true, "School lesson public", ownClass, ownCourse);
        privateResource = resource(school, false, "School lesson private", ownClass, ownCourse);
        foreignPublic = resource(otherSchool, true, "School lesson foreign public", otherClass, otherCourse);
        foreignPrivate = resource(otherSchool, false, "School lesson foreign private", otherClass, otherCourse);
        em.flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"get", "update", "delete", "classes", "courses", "teachers"})
    void foreignResourceOperationsAreNotFound(String operation) throws Exception {
        for (var resource : List.of(foreignPublic, foreignPrivate)) {
            MockHttpServletRequestBuilder request = switch (operation) {
                case "get" -> get(ROOT + "/{id}", resource.getId());
                case "update" -> put(ROOT + "/{id}", resource.getId()).content("{\"title\":\"Changed\"}");
                case "delete" -> delete(ROOT + "/{id}", resource.getId());
                default -> post(ROOT + "/{id}/{relation}", resource.getId(), operation).content("[]");
            };
            mvc.perform(request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")).contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
            assertThat(resources.existsById(resource.getId())).isTrue();
            assertThat(resource.getTitle()).contains("foreign");
        }
    }

    @ParameterizedTest
    @CsvSource({"ADMIN,admin@fixtures.school.test,2", "TEACHER,teacher@fixtures.school.test,2", "STUDENT,student@fixtures.school.test,1"})
    void everyListModeScopesSchoolAndVisibilityBeforePagination(String role, String email, int total) throws Exception {
        for (var filter : List.of(Map.<String, String>of(), Map.of("type", "DOCUMENT"),
                Map.of("teacherId", teacher.getId().toString()), Map.of("classId", ownClass.getId().toString()),
                Map.of("courseId", ownCourse.getId().toString()), Map.of("search", "School lesson"))) {
            var request = get(ROOT).param("size", "1").with(user(email).roles(role));
            filter.forEach(request::param);
            String body = mvc.perform(request).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(total))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andReturn().getResponse().getContentAsString();
            long id = json.readTree(body).path("data").path("content").get(0).path("id").asLong();
            assertThat(id).isIn(publicResource.getId(), privateResource.getId());
            if (role.equals("STUDENT")) assertThat(id).isEqualTo(publicResource.getId());
        }
    }

    @Test
    void multiSchoolTeacherSeesOnlyTheSelectedSchool() throws Exception {
        doReturn(otherSchool).when(currentSchool).resolve();
        String body = mvc.perform(get(ROOT).with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andReturn().getResponse().getContentAsString();
        assertThat(java.util.stream.StreamSupport.stream(json.readTree(body).path("data").path("content").spliterator(), false)
                .map(node -> node.path("id").asLong()).toList())
                .containsExactlyInAnyOrder(foreignPublic.getId(), foreignPrivate.getId());
        mvc.perform(get(ROOT + "/{id}", publicResource.getId()).with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"TEACHER,teacher@fixtures.school.test", "ADMIN,admin@fixtures.school.test", "STUDENT,student@fixtures.school.test"})
    void resourceVisibilityCannotRevealForeignPublicOrPrivateResources(String role, String email) throws Exception {
        for (var foreign : List.of(foreignPublic, foreignPrivate)) {
            mvc.perform(get(ROOT + "/{id}", foreign.getId()).with(user(email).roles(role))).andExpect(status().isNotFound());
        }
        mvc.perform(get(ROOT + "/{id}", publicResource.getId()).with(user(email).roles(role))).andExpect(status().isOk());
        mvc.perform(get(ROOT + "/{id}", privateResource.getId()).with(user(email).roles(role)))
                .andExpect(role.equals("STUDENT") ? status().isForbidden() : status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacherId", "classId", "courseId"})
    void foreignAndMissingFilterIdsAreNotFound(String filter) throws Exception {
        long foreignId = switch (filter) {
            case "teacherId" -> foreignTeacher.getId();
            case "classId" -> otherClass.getId();
            default -> otherCourse.getId();
        };
        for (long id : List.of(foreignId, Long.MAX_VALUE)) {
            mvc.perform(get(ROOT).param(filter, Long.toString(id)).with(user(teacher.getEmail()).roles("TEACHER")))
                    .andExpect(status().isNotFound());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"classIds", "courseIds"})
    void createRejectsForeignAndMixedTargetsWithoutPersistingAResource(String target) throws Exception {
        long ownId = target.equals("classIds") ? ownClass.getId() : ownCourse.getId();
        long foreignId = target.equals("classIds") ? otherClass.getId() : otherCourse.getId();
        long before = resources.count();
        for (var ids : List.of(List.of(foreignId), List.of(ownId, foreignId))) {
            mvc.perform(post(ROOT).with(user(teacher.getEmail()).roles("TEACHER"))
                            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                    "title", "Rejected target", "url", "https://example.test/rejected", "type", "LINK", target, ids))))
                    .andExpect(status().isNotFound());
            assertThat(resources.count()).isEqualTo(before);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"classIds", "courseIds"})
    void uploadRejectsForeignTargetsBeforeWritingAFile(String target) throws Exception {
        long foreignId = target.equals("classIds") ? otherClass.getId() : otherCourse.getId();
        long before = resources.count();
        Set<String> filesBefore = uploadedFiles();
        mvc.perform(multipart(ROOT + "/upload").file(new MockMultipartFile("file", "lesson.pdf", "application/pdf",
                        "%PDF-1.4\n%school test\n".getBytes(StandardCharsets.US_ASCII)))
                        .param("title", "Rejected upload").param("description", "Rejected foreign targets").param("type", "DOCUMENT").param(target, "[" + foreignId + "]")
                        .with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isNotFound());
        assertThat(resources.count()).isEqualTo(before);
        assertThat(uploadedFiles()).isEqualTo(filesBefore);
    }

    @Test
    void createValidatesAllTargetsBeforeMutatingAnyAssociation() throws Exception {
        var teacherResources = Set.copyOf(teacher.getLearningResources());
        var classResources = Set.copyOf(ownClass.getLearningResources());
        long before = resources.count();

        mvc.perform(post(ROOT).with(user(teacher.getEmail()).roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "title", "Rejected cross-target set", "url", "https://example.test/rejected",
                                "type", "LINK", "classIds", List.of(ownClass.getId()),
                                "courseIds", List.of(otherCourse.getId())))))
                .andExpect(status().isNotFound());

        assertThat(teacher.getLearningResources()).containsExactlyInAnyOrderElementsOf(teacherResources);
        assertThat(ownClass.getLearningResources()).containsExactlyInAnyOrderElementsOf(classResources);
        assertThat(resources.count()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"classIds", "courseIds"})
    void updateRejectsMixedForeignTargetsWithoutChangingExistingTargets(String target) throws Exception {
        long ownId = target.equals("classIds") ? ownClass.getId() : ownCourse.getId();
        long foreignId = target.equals("classIds") ? otherClass.getId() : otherCourse.getId();
        mvc.perform(put(ROOT + "/{id}", publicResource.getId()).with(user(teacher.getEmail()).roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                                "title", "Rejected update", target, List.of(ownId, foreignId)))))
                .andExpect(status().isNotFound());
        assertThat(publicResource.getTitle()).isEqualTo("School lesson public");
        assertThat(publicResource.getTargetClasses()).containsExactly(ownClass);
        assertThat(publicResource.getTargetCourses()).containsExactly(ownCourse);
    }

    @ParameterizedTest
    @CsvSource({"classes,ADD", "classes,REMOVE", "courses,ADD", "courses,REMOVE", "teachers,ADD", "teachers,REMOVE"})
    void relationMutationsRejectForeignMembers(String relation, String operation) throws Exception {
        long id = switch (relation) {
            case "classes" -> otherClass.getId();
            case "courses" -> otherCourse.getId();
            default -> foreignTeacher.getId();
        };
        var request = operation.equals("ADD") ? post(ROOT + "/{id}/{relation}", publicResource.getId(), relation)
                : delete(ROOT + "/{id}/{relation}", publicResource.getId(), relation);
        mvc.perform(request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("[" + id + "]"))
                .andExpect(status().isNotFound());
        assertThat(publicResource.getTargetClasses()).containsExactly(ownClass);
        assertThat(publicResource.getTargetCourses()).containsExactly(ownCourse);
        assertThat(publicResource.getCreatedBy()).containsExactly(teacher);
    }

    @Test
    void teacherLinksAndFiltersRequireTeacherMembershipRatherThanAccountRole() throws Exception {
        membership(foreignTeacher, school, MembershipRole.STUDENT);
        mvc.perform(get(ROOT).param("teacherId", foreignTeacher.getId().toString())
                        .with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isNotFound());
        for (var request : List.of(post(ROOT + "/{id}/teachers", publicResource.getId()),
                delete(ROOT + "/{id}/teachers", publicResource.getId()))) {
            mvc.perform(request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content("[" + foreignTeacher.getId() + "]"))
                    .andExpect(status().isNotFound());
        }
        assertThat(publicResource.getCreatedBy()).containsExactly(teacher);
    }

    @Test
    void currentSchoolTargetsAndTeacherLinksCanBeAddedAndRemoved() throws Exception {
        Teacher colleague = teacher("Current");
        membership(colleague, school, MembershipRole.TEACHER);
        for (String relation : List.of("classes", "courses", "teachers")) {
            long id = switch (relation) {
                case "classes" -> ownClass.getId();
                case "courses" -> ownCourse.getId();
                default -> colleague.getId();
            };
            for (var request : List.of(post(ROOT + "/{id}/{relation}", publicResource.getId(), relation),
                    delete(ROOT + "/{id}/{relation}", publicResource.getId(), relation))) {
                mvc.perform(request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))
                                .contentType(MediaType.APPLICATION_JSON).content("[" + id + "]"))
                        .andExpect(status().isOk());
            }
        }
        assertThat(publicResource.getTargetClasses()).isEmpty();
        assertThat(publicResource.getTargetCourses()).isEmpty();
        assertThat(publicResource.getCreatedBy()).containsExactly(teacher);
    }

    @ParameterizedTest
    @CsvSource({"TEACHER,teacher@fixtures.school.test", "ADMIN,admin@fixtures.school.test"})
    void createUsesCurrentSchoolAndPreservesAdminCreatorCompatibility(String role, String email) throws Exception {
        String body = mvc.perform(post(ROOT).with(user(email).roles(role)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Created here", "url", "https://example.test/created",
                                "type", "LINK", "schoolId", otherSchool.getId()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.schoolId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).path("data").path("id").asLong();
        LearningResource created = resources.findById(id).orElseThrow();
        assertThat(created.getSchool().getId()).isEqualTo(school.getId());
        if (role.equals("ADMIN")) assertThat(created.getCreatedBy()).isEmpty();
        else assertThat(created.getCreatedBy()).containsExactly(teacher);
        assertThat(jdbc.queryForObject("SELECT acted_by_id FROM audit_events WHERE entity_type = 'LearningResource' AND entity_id = ?",
                Long.class, id)).isEqualTo(users.findByEmail(email).orElseThrow().getId());
        mvc.perform(put(ROOT + "/{id}", id).with(user(email).roles(role)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Updated here", "schoolId", otherSchool.getId()))))
                .andExpect(status().isOk());
        assertThat(created.getSchool().getId()).isEqualTo(school.getId());
    }

    @Test
    void adminUploadUsesCurrentSchoolWithoutInventingATeacherCreator() throws Exception {
        String body = mvc.perform(multipart(ROOT + "/upload")
                        .file(new MockMultipartFile("file", "admin.pdf", "application/pdf",
                                "%PDF-1.4\n%admin school test\n".getBytes(StandardCharsets.US_ASCII)))
                        .param("title", "Admin upload").param("description", "Uploaded by an Admin").param("type", "DOCUMENT")
                        .param("schoolId", otherSchool.getId().toString())
                        .with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode dto = json.readTree(body).path("data");
        Path uploaded = Path.of(uploadPath).resolve(dto.path("url").asText().substring((ROOT + "/files/").length()));
        try {
            LearningResource created = resources.findById(dto.path("id").asLong()).orElseThrow();
            assertThat(created.getSchool().getId()).isEqualTo(school.getId());
            assertThat(created.getCreatedBy()).isEmpty();
            assertThat(uploaded).exists();
            assertThat(jdbc.queryForObject("SELECT acted_by_id FROM audit_events WHERE entity_type = 'LearningResource' AND entity_id = ?",
                    Long.class, created.getId())).isEqualTo(users.findByEmail(DevFixtureLoader.ADMIN_EMAIL).orElseThrow().getId());
        } finally {
            Files.deleteIfExists(uploaded);
        }
    }

    @ParameterizedTest
    @CsvSource({"TEACHER,teacher@fixtures.school.test", "ADMIN,admin@fixtures.school.test"})
    void accountRoleCannotReplaceCurrentSchoolMembershipForCreateOrMutation(String role, String email) throws Exception {
        var account = users.findByEmail(email).orElseThrow();
        var membership = memberships.findByUserIdAndSchoolId(account.getId(), school.getId()).orElseThrow();
        membership.setRoles(Set.of(MembershipRole.STUDENT));
        em.flush();
        long before = resources.count();
        mvc.perform(post(ROOT).with(user(email).roles(role)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Denied\",\"url\":\"https://example.test/denied\",\"type\":\"LINK\"}"))
                .andExpect(status().isForbidden());
        assertThat(resources.count()).isEqualTo(before);
        Set<String> filesBefore = uploadedFiles();
        mvc.perform(multipart(ROOT + "/upload")
                        .file(new MockMultipartFile("file", "denied.pdf", "application/pdf",
                                "%PDF-1.4\n%denied school test\n".getBytes(StandardCharsets.US_ASCII)))
                        .param("title", "Denied upload").param("description", "Denied membership").param("type", "DOCUMENT").with(user(email).roles(role)))
                .andExpect(status().isForbidden());
        assertThat(resources.count()).isEqualTo(before);
        assertThat(uploadedFiles()).isEqualTo(filesBefore);
        mvc.perform(put(ROOT + "/{id}", publicResource.getId()).with(user(email).roles(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Denied\"}"))
                .andExpect(status().isForbidden());
        assertThat(publicResource.getTitle()).isEqualTo("School lesson public");
    }

    @ParameterizedTest
    @CsvSource({"TEACHER,teacher@fixtures.school.test,absent", "TEACHER,teacher@fixtures.school.test,STUDENT",
            "ADMIN,admin@fixtures.school.test,absent", "ADMIN,admin@fixtures.school.test,STUDENT",
            "STUDENT,student@fixtures.school.test,absent", "STUDENT,student@fixtures.school.test,GUARDIAN"})
    void accountRoleAloneDoesNotGrantAnySchoolResourceVisibility(String role, String email, String membershipState) throws Exception {
        var account = users.findByEmail(email).orElseThrow();
        var membership = memberships.findByUserIdAndSchoolId(account.getId(), school.getId()).orElseThrow();
        if (membershipState.equals("absent")) memberships.delete(membership);
        else membership.setRoles(Set.of(MembershipRole.valueOf(membershipState)));
        em.flush();

        mvc.perform(get(ROOT + "/{id}", privateResource.getId()).with(user(email).roles(role)))
                .andExpect(status().isForbidden());
        mvc.perform(get(ROOT + "/{id}", publicResource.getId()).with(user(email).roles(role)))
                .andExpect(status().isForbidden());
        for (var filter : List.of(Map.<String, String>of(), Map.of("type", "DOCUMENT"),
                Map.of("teacherId", teacher.getId().toString()), Map.of("classId", ownClass.getId().toString()),
                Map.of("courseId", ownCourse.getId().toString()), Map.of("search", "School lesson"),
                Map.of("teacherId", foreignTeacher.getId().toString()), Map.of("classId", otherClass.getId().toString()),
                Map.of("courseId", otherCourse.getId().toString()))) {
            var request = get(ROOT).with(user(email).roles(role));
            filter.forEach(request::param);
            mvc.perform(request).andExpect(status().isForbidden());
        }
        mvc.perform(get(ROOT + "/{id}", foreignPublic.getId()).with(user(email).roles(role)))
                .andExpect(status().isForbidden());
    }

    private Set<String> uploadedFiles() throws Exception {
        Path root = Path.of(uploadPath);
        if (!Files.exists(root)) return Set.of();
        try (var paths = Files.list(root)) {
            return paths.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
        }
    }

    private School school(String name) {
        var school = new School();
        school.setName(name);
        return schools.saveAndFlush(school);
    }

    private void membership(BaseUser user, School school, MembershipRole role) {
        var membership = new SchoolMembership();
        membership.setUser(user);
        membership.setSchool(school);
        membership.setRoles(Set.of(role));
        membership.setStatus(MembershipStatus.ACTIVE);
        memberships.saveAndFlush(membership);
    }

    private Teacher teacher(String name) {
        var teacher = new Teacher();
        teacher.setRole(UserRole.TEACHER);
        teacher.setFirstName(name);
        teacher.setLastName("Teacher");
        teacher.setEmail(UUID.randomUUID() + "@fixtures.school.test");
        teacher.setPassword("unused");
        teacher.setStatus(Status.ACTIVE);
        return teachers.saveAndFlush(teacher);
    }

    private ClassEntity clazz(School school, String name) {
        var year = new AcademicYear();
        year.setSchool(school);
        year.setName("2026-2027");
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 6, 30));
        year.setActive(true);
        var clazz = new ClassEntity();
        clazz.setName(name);
        clazz.setAcademicYear(years.saveAndFlush(year));
        return classes.saveAndFlush(clazz);
    }

    private Course course(School school, String name) {
        var course = new Course();
        course.setSchool(school);
        course.setName(name);
        course.setCode(name);
        return courses.saveAndFlush(course);
    }

    private LearningResource resource(School owner, boolean visible, String title, ClassEntity clazz, Course course) {
        var resource = new LearningResource();
        resource.setSchool(owner);
        resource.setPublic(visible);
        resource.setTitle(title);
        resource.setUrl("https://example.test/" + UUID.randomUUID());
        resource.setType(ResourceType.DOCUMENT);
        resource.getCreatedBy().add(teacher);
        resource.getTargetClasses().add(clazz);
        resource.getTargetCourses().add(course);
        return resources.saveAndFlush(resource);
    }
}
