package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.operational.entity.ResourceComment;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional
class ResourceCommentSchoolAccessIntegrationTest {
    private static final String BASE = "/api/v1/resource-comments";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;

    private School school;
    private School foreignSchool;
    private BaseUser teacher;
    private BaseUser foreignUser;
    private BaseUser wrongRoleUser;
    private LearningResource publicResource;
    private LearningResource privateResource;
    private LearningResource foreignResource;
    private ResourceComment publicComment;
    private ResourceComment privateComment;
    private ResourceComment foreignComment;

    @BeforeEach
    void setUp() {
        school = school("Comment current");
        foreignSchool = school("Comment foreign");
        doReturn(school).when(currentSchool).resolve();
        teacher = users.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        membership(teacher, school, MembershipRole.TEACHER);
        membership(teacher, foreignSchool, MembershipRole.TEACHER);
        membership(users.findByEmail(DevFixtureLoader.ADMIN_EMAIL).orElseThrow(), school, MembershipRole.ADMIN);
        membership(users.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow(), school, MembershipRole.STUDENT);
        foreignUser = student();
        membership(foreignUser, foreignSchool, MembershipRole.STUDENT);
        wrongRoleUser = student();
        membership(wrongRoleUser, school, MembershipRole.GUARDIAN);
        publicResource = resource(school, true);
        privateResource = resource(school, false);
        foreignResource = resource(foreignSchool, true);
        publicComment = comment(publicResource, teacher);
        privateComment = comment(privateResource, teacher);
        foreignComment = comment(foreignResource, teacher);
        em.flush();
    }

    @Test
    void foreignCommentAndResourceIdsAre404ForAllCallers() throws Exception {
        for (String role : List.of("STUDENT", "TEACHER", "ADMIN")) {
            perform(get(BASE + "/" + foreignComment.getId()), role).andExpect(status().isNotFound());
            perform(delete(BASE + "/" + foreignComment.getId()), role).andExpect(status().isNotFound());
            perform(get(BASE + "/resource/" + foreignResource.getId()), role).andExpect(status().isNotFound());
            perform(post(BASE).content(body(foreignResource)), role).andExpect(status().isNotFound());
        }
        assertThat(em.find(ResourceComment.class, foreignComment.getId())).isNotNull();
    }

    @Test
    void globalAndUserPagingCountOnlyCurrentSchoolVisibleComments() throws Exception {
        for (String role : List.of("TEACHER", "ADMIN", "STUDENT")) {
            for (String path : List.of(BASE, BASE + "/user/" + teacher.getId())) {
                JsonNode page = data(get(path).param("size", "1"), role);
                assertThat(page.path("totalElements").asLong()).isEqualTo(role.equals("STUDENT") ? 1 : 2);
                assertThat(page.path("content")).hasSize(1);
                assertThat(page.path("content").get(0).path("id").asLong())
                        .isIn(publicComment.getId(), privateComment.getId());
                JsonNode full = data(get(path).param("size", "10"), role);
                assertThat(full.path("content").findValuesAsText("id"))
                        .doesNotContain(foreignComment.getId().toString());
                if (role.equals("STUDENT")) {
                    assertThat(full.path("content").findValuesAsText("id"))
                            .containsExactly(publicComment.getId().toString());
                }
            }
        }
    }

    @Test
    void userRouteRequiresAnApplicableCurrentSchoolMembership() throws Exception {
        for (long id : List.of(foreignUser.getId(), wrongRoleUser.getId(), Long.MAX_VALUE)) {
            perform(get(BASE + "/user/" + id), "ADMIN").andExpect(status().isNotFound());
        }
    }

    @Test
    void studentCommentsOnPublicResourcesOnly() throws Exception {
        perform(post(BASE).content(body(privateResource)), "STUDENT").andExpect(status().isForbidden());
        perform(get(BASE + "/" + privateComment.getId()), "STUDENT").andExpect(status().isForbidden());
        perform(post(BASE).content(body(publicResource)), "STUDENT").andExpect(status().isOk());
    }

    @Test
    void teachersAndAdminsKeepPrivateResourceAccessAndCreation() throws Exception {
        for (String role : List.of("TEACHER", "ADMIN")) {
            perform(get(BASE + "/" + privateComment.getId()), role).andExpect(status().isOk());
            perform(post(BASE).content(body(privateResource)), role).andExpect(status().isOk());
            JsonNode page = data(get(BASE + "/resource/" + privateResource.getId()), role);
            assertThat(page.path("content").findValuesAsText("id")).contains(privateComment.getId().toString());
        }
    }

    @Test
    void commentDeletionPreservesAuthorOrCurrentSchoolAdminAuthorization() throws Exception {
        perform(delete(BASE + "/" + publicComment.getId()), "STUDENT").andExpect(status().isForbidden());
        perform(delete(BASE + "/" + publicComment.getId()), "TEACHER").andExpect(status().isOk());
        perform(delete(BASE + "/" + privateComment.getId()), "ADMIN").andExpect(status().isOk());
    }

    @Test
    void deletingCurrentSchoolLearningResourceRemovesItsCommentsOnly() throws Exception {
        long deletedComment = publicComment.getId();
        long retainedComment = foreignComment.getId();
        long deletedResource = publicResource.getId();
        // Read the aggregate from the database, as an HTTP request outside this
        // test transaction would, rather than reuse the fixture's empty inverse collection.
        em.clear();
        perform(delete("/api/v1/learning-resources/" + deletedResource), "ADMIN")
                .andExpect(status().isOk());
        em.flush();
        em.clear();
        assertThat(em.find(ResourceComment.class, deletedComment)).isNull();
        assertThat(em.find(ResourceComment.class, retainedComment)).isNotNull();
    }

    private org.springframework.test.web.servlet.ResultActions perform(MockHttpServletRequestBuilder request, String role) throws Exception {
        String email = switch (role) {
            case "ADMIN" -> DevFixtureLoader.ADMIN_EMAIL;
            case "TEACHER" -> DevFixtureLoader.TEACHER_EMAIL;
            default -> DevFixtureLoader.STUDENT_EMAIL;
        };
        return mvc.perform(request.with(user(email).roles(role)).contentType(MediaType.APPLICATION_JSON));
    }

    private JsonNode data(MockHttpServletRequestBuilder request, String role) throws Exception {
        return json.readTree(perform(request, role).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
    }

    private String body(LearningResource resource) throws Exception {
        return json.writeValueAsString(Map.of("resourceId", resource.getId(), "content", "School comment"));
    }

    private School school(String name) {
        School value = new School();
        value.setName(name);
        em.persist(value);
        return value;
    }

    private BaseUser student() {
        Student value = new Student();
        value.setFirstName("Comment");
        value.setLastName("Student");
        value.setEmail(UUID.randomUUID() + "@comments.test");
        value.setRole(UserRole.STUDENT);
        value.setStatus(Status.ACTIVE);
        em.persist(value);
        return value;
    }

    private void membership(BaseUser account, School owner, MembershipRole role) {
        SchoolMembership value = new SchoolMembership();
        value.setUser(account);
        value.setSchool(owner);
        value.setRoles(new HashSet<>(Set.of(role)));
        value.setStatus(MembershipStatus.ACTIVE);
        em.persist(value);
    }

    private LearningResource resource(School owner, boolean visible) {
        LearningResource value = new LearningResource();
        value.setSchool(owner);
        value.setTitle("School comment resource " + UUID.randomUUID());
        value.setUrl("https://example.test/" + UUID.randomUUID());
        value.setType(ResourceType.LINK);
        value.setPublic(visible);
        em.persist(value);
        em.flush();
        return value;
    }

    private ResourceComment comment(LearningResource resource, BaseUser author) {
        ResourceComment value = new ResourceComment();
        value.setContent("School comment");
        value.setOnResource(resource);
        value.setCommentedBy(author);
        resource.getComments().add(value);
        em.persist(value);
        return value;
    }
}
