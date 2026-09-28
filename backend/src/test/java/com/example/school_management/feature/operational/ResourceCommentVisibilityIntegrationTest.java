package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.operational.entity.ResourceComment;
import com.example.school_management.feature.operational.repository.ResourceCommentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comments follow their learning resource's visibility: students see public resources only, so
 * they neither add, read nor list comments on a private one, and never learn its title from a
 * comment. Teachers and administrators see both. The fixture teacher created both resources and
 * wrote one comment on each.
 *
 * <p>resource_comments.on_resource_id still references the legacy resources table, so each
 * learning resource gets a legacy row with the same id; otherwise no comment could be stored.
 */
@IntegrationTest
class ResourceCommentVisibilityIntegrationTest {

    private static final String COMMENTS = "/api/v1/resource-comments";
    private static final String NOT_AVAILABLE = "This learning resource is not available to you";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LearningResourceRepository resourceRepository;

    @Autowired
    ResourceCommentRepository commentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbc;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Teacher teacher;
    private String privateTitle;
    private LearningResource publicResource;
    private LearningResource privateResource;
    private ResourceComment publicComment;
    private ResourceComment privateComment;

    @BeforeEach
    void createResourcesWithComments() {
        teacher = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        privateTitle = "Private " + UUID.randomUUID();
        publicResource = resourceRepository.save(resource("Public " + UUID.randomUUID(), true));
        privateResource = resourceRepository.save(resource(privateTitle, false));
        for (LearningResource r : List.of(publicResource, privateResource)) {
            jdbc.update("INSERT INTO resources (id) VALUES (?)", r.getId());
        }
        publicComment = comment(publicResource, teacher);
        privateComment = comment(privateResource, teacher);
    }

    @AfterEach
    void deleteResources() {
        List<Long> resourceIds = List.of(publicResource.getId(), privateResource.getId());
        jdbc.update("DELETE FROM resource_comments WHERE on_resource_id IN (?, ?)", resourceIds.toArray());
        jdbc.update("DELETE FROM resources WHERE id IN (?, ?)", resourceIds.toArray());
        resourceRepository.deleteAllById(resourceIds);
    }

    @Test
    void aStudentCommentsOnAPublicResource() throws Exception {
        String body = mockMvc.perform(post(COMMENTS).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("resourceId", publicResource.getId(), "content", "Thanks for the notes"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceId").value(publicResource.getId()))
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(body).path("data").path("id").asLong();
        assertThat(commentRepository.findById(id)).hasValueSatisfying(stored ->
                assertThat(stored.getOnResource().getId()).isEqualTo(publicResource.getId()));
    }

    @Test
    void aStudentCannotCommentOnAPrivateResource() throws Exception {
        long before = commentRepository.count();

        expectForbidden(mockMvc.perform(post(COMMENTS).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("resourceId", privateResource.getId(), "content", "Let me in")))), NOT_AVAILABLE);

        assertThat(commentRepository.count()).isEqualTo(before);
    }

    @Test
    void aStudentReadsCommentsOnAPublicResourceOnly() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        mockMvc.perform(get(COMMENTS + "/{id}", publicComment.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(publicComment.getId()));
        mockMvc.perform(get(COMMENTS + "/resource/{id}", publicResource.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id").value(hasItem(publicComment.getId().intValue())));

        expectForbidden(mockMvc.perform(get(COMMENTS + "/{id}", privateComment.getId())
                .header(HttpHeaders.AUTHORIZATION, student)), NOT_AVAILABLE);
        mockMvc.perform(get(COMMENTS + "/resource/{id}", privateResource.getId()).header(HttpHeaders.AUTHORIZATION, student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(content().string(not(containsString(privateTitle))));
    }

    @Test
    void userAndGlobalListingsLeaveCommentsOnPrivateResourcesOutForStudents() throws Exception {
        String student = bearer(DevFixtureLoader.STUDENT_EMAIL);

        for (String uri : List.of(COMMENTS + "/user/" + teacher.getId(), COMMENTS)) {
            mockMvc.perform(get(uri).param("size", "1000").header(HttpHeaders.AUTHORIZATION, student))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].id").value(hasItem(publicComment.getId().intValue())))
                    .andExpect(jsonPath("$.data.content[*].id").value(not(hasItem(privateComment.getId().intValue()))))
                    .andExpect(content().string(not(containsString(privateTitle))));
        }
    }

    @Test
    void teachersAndAdministratorsSeeCommentsOnPrivateResources() throws Exception {
        for (String caller : List.of(DevFixtureLoader.TEACHER_EMAIL, DevFixtureLoader.ADMIN_EMAIL)) {
            String bearer = bearer(caller);

            mockMvc.perform(get(COMMENTS + "/{id}", privateComment.getId()).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.resourceTitle").value(privateTitle));
            for (String uri : List.of(COMMENTS + "/resource/" + privateResource.getId(), COMMENTS + "/user/" + teacher.getId(), COMMENTS)) {
                mockMvc.perform(get(uri).param("size", "1000").header(HttpHeaders.AUTHORIZATION, bearer))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.content[*].id").value(hasItem(privateComment.getId().intValue())));
            }
        }

        mockMvc.perform(post(COMMENTS).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("resourceId", privateResource.getId(), "content", "Staff note"))))
                .andExpect(status().isOk());
    }

    @Test
    void deletingSomeoneElsesCommentRevealsNothingAndKeepsIt() throws Exception {
        mockMvc.perform(delete(COMMENTS + "/{id}", privateComment.getId()).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only delete your own comments"))
                .andExpect(content().string(not(containsString(privateTitle))));

        assertThat(commentRepository.existsById(privateComment.getId())).isTrue();
    }

    @Test
    void theAuthorAndAnAdministratorStillDeleteComments() throws Exception {
        BaseUser student = userRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        ResourceComment own = comment(publicResource, student);

        mockMvc.perform(delete(COMMENTS + "/{id}", own.getId()).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.STUDENT_EMAIL)))
                .andExpect(status().isOk());
        mockMvc.perform(delete(COMMENTS + "/{id}", privateComment.getId()).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isOk());

        assertThat(commentRepository.existsById(own.getId())).isFalse();
        assertThat(commentRepository.existsById(privateComment.getId())).isFalse();
    }

    private LearningResource resource(String title, boolean isPublic) {
        LearningResource r = new LearningResource();
        r.setTitle(title);
        r.setUrl("https://example.org/" + UUID.randomUUID());
        r.setType(ResourceType.LINK);
        r.setPublic(isPublic);
        r.getCreatedBy().add(teacher);
        return r;
    }

    private ResourceComment comment(LearningResource resource, BaseUser author) {
        ResourceComment c = new ResourceComment();
        c.setContent("Comment by " + author.getEmail());
        c.setOnResource(resource);
        c.setCommentedBy(author);
        return commentRepository.save(c);
    }

    private void expectForbidden(ResultActions result, String detail) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(content().string(not(containsString(privateTitle))));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .with(request -> {
                            request.setRemoteAddr("10.0.34." + clientAddress.incrementAndGet());
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
