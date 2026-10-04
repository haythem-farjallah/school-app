package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.academic.repository.LearningResourceRepository;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.membership.repository.SchoolMembershipRepository;
import com.example.school_management.feature.membership.service.SchoolMembershipProvisioningService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uploaded files through the real filter chain: they are served with defensive headers, and the
 * managed URL naming one is assigned by the upload endpoint only, so no other resource can alias
 * it and then delete it. Teacher A is the fixture teacher; teacher B uploads the victim file.
 */
@IntegrationTest
class LearningResourceManagedFileIntegrationTest {

    private static final String RESOURCES = "/api/v1/learning-resources";
    private static final String FILES = RESOURCES + "/files/";
    private static final String MANAGED_URL_REFUSED = "url: uploaded file URLs are assigned by the upload endpoint";
    private static final String FILE_CSP = "default-src 'none'; sandbox";
    private static final byte[] PDF = "%PDF-1.4\n%managed file\n".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    LearningResourceRepository resourceRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    CurrentSchoolResolver currentSchool;

    @Autowired
    SchoolRepository schools;

    @Value("${app.file.upload.path:uploads/learning-resources}")
    String uploadPath;

    @Autowired
    SchoolMembershipProvisioningService membershipProvisioner;

    @Autowired
    SchoolMembershipRepository memberships;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private final List<Long> createdResources = new ArrayList<>();
    private final List<Path> files = new ArrayList<>();
    private final List<School> createdSchools = new ArrayList<>();
    private Teacher teacherB;
    private String teacherA;
    private String teacherBBearer;
    private JsonNode victim;
    private Path victimFile;

    @BeforeEach
    void uploadVictimFile() throws Exception {
        School uploadSchool = currentSchool.resolve();
        doReturn(uploadSchool).when(currentSchool).resolve();
        Teacher t = new Teacher();
        t.setRole(UserRole.TEACHER);
        t.setEmail("teacher-b-" + UUID.randomUUID() + "@fixtures.school.test");
        t.setFirstName("Tina");
        t.setLastName("Other");
        t.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        t.setStatus(Status.ACTIVE);
        t.setIsEmailVerified(true);
        teacherB = teacherRepository.save(t);
        membershipProvisioner.provisionFor(teacherB);

        teacherA = bearer(DevFixtureLoader.TEACHER_EMAIL);
        teacherBBearer = bearer(teacherB.getEmail());
        victim = upload(teacherBBearer, new MockMultipartFile("file", "notes.pdf", "application/pdf", PDF), "DOCUMENT");
        victimFile = managedFile(victim.path("url").asText());
    }

    @AfterEach
    void cleanUp() throws Exception {
        for (Long id : createdResources) {
            resourceRepository.findById(id).ifPresent(resourceRepository::delete);
        }
        for (School school : createdSchools) {
            memberships.deleteAll(memberships.findAllBySchoolId(school.getId()));
            schools.deleteById(school.getId());
        }
        // Uploads record audit events acted by the uploader.
        jdbc.update("DELETE FROM audit_events WHERE acted_by_id = ?", teacherB.getId());
        memberships.deleteAll(memberships.findAllByUserId(teacherB.getId()));
        teacherRepository.deleteById(teacherB.getId());
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void anUploadedPdfIsStoredAndServedWithDefensiveHeaders() throws Exception {
        assertThat(victim.path("url").asText()).startsWith(FILES);
        assertThat(Files.readAllBytes(victimFile)).isEqualTo(PDF);
        String name = victimFile.getFileName().toString();

        for (String route : List.of("files", "preview")) {
            mockMvc.perform(get(RESOURCES + "/{route}/{name}", route, name).with(nextAddress()).header(HttpHeaders.AUTHORIZATION, teacherA))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/pdf"))
                    .andExpect(content().bytes(PDF))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Content-Security-Policy", FILE_CSP));
        }
    }

    @Test
    void anUploadedImageIsServedAsThatImage() throws Exception {
        JsonNode image = upload(teacherA, new MockMultipartFile("file", "diagram.png", "image/png", PNG), "IMAGE");
        String name = managedFile(image.path("url").asText()).getFileName().toString();

        assertThat(image.path("type").asText()).isEqualTo("IMAGE");
        mockMvc.perform(get(RESOURCES + "/preview/{name}", name).with(nextAddress()).header(HttpHeaders.AUTHORIZATION, teacherA))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", FILE_CSP));
    }

    @Test
    void anSvgStoredBeforeUploadsRefusedItIsServedAsOpaqueBytes() throws Exception {
        String name = UUID.randomUUID() + ".svg";
        Path svg = Paths.get(uploadPath).resolve(name);
        Files.writeString(svg, "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>");
        files.add(svg);
        createdResources.add(resourceRepository.save(storedResource(FILES + name)).getId());

        mockMvc.perform(get(RESOURCES + "/preview/{name}", name).with(nextAddress()).header(HttpHeaders.AUTHORIZATION, teacherA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", FILE_CSP));
    }

    @Test
    void aTeacherCannotPointTheirResourceAtAnotherTeachersUploadedFile() throws Exception {
        JsonNode own = createLink(teacherA, "https://example.org/lesson");

        expectManagedUrlRefused(mockMvc.perform(put(RESOURCES + "/{id}", own.path("id").asLong()).with(nextAddress())
                .header(HttpHeaders.AUTHORIZATION, teacherA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("url", victim.path("url").asText())))));

        assertThat(resourceRepository.findById(own.path("id").asLong()).orElseThrow().getUrl()).isEqualTo("https://example.org/lesson");

        // Deleting the resource afterwards leaves the other teacher's file and resource untouched.
        mockMvc.perform(delete(RESOURCES + "/{id}", own.path("id").asLong()).with(nextAddress()).header(HttpHeaders.AUTHORIZATION, teacherA))
                .andExpect(status().isOk());
        expectVictimIntact();
    }

    @Test
    void aTeacherCannotCreateAResourceAliasingAnUploadedFile() throws Exception {
        long before = resourceRepository.count();

        for (String url : List.of(victim.path("url").asText(), FILES + "../files/" + victimFile.getFileName())) {
            expectManagedUrlRefused(mockMvc.perform(post(RESOURCES).with(nextAddress())
                    .header(HttpHeaders.AUTHORIZATION, teacherA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(linkRequest(url)))));
        }

        assertThat(resourceRepository.count()).isEqualTo(before);
        expectVictimIntact();
    }

    @Test
    void anUploadedResourceCannotBeRetargetedToAnotherUploadedFile() throws Exception {
        JsonNode own = upload(teacherA, new MockMultipartFile("file", "own.pdf", "application/pdf", PDF), "DOCUMENT");

        expectManagedUrlRefused(mockMvc.perform(put(RESOURCES + "/{id}", own.path("id").asLong()).with(nextAddress())
                .header(HttpHeaders.AUTHORIZATION, teacherA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("url", victim.path("url").asText())))));

        assertThat(resourceRepository.findById(own.path("id").asLong()).orElseThrow().getUrl()).isEqualTo(own.path("url").asText());
        expectVictimIntact();
    }

    @Test
    void theCreatorEditsAnUploadedResourceResendingItsOwnUrl() throws Exception {
        mockMvc.perform(put(RESOURCES + "/{id}", victim.path("id").asLong()).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, teacherBBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Renamed notes", "url", victim.path("url").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Renamed notes"))
                .andExpect(jsonPath("$.data.url").value(victim.path("url").asText()));
    }

    @Test
    void externalLinkResourcesAreCreatedAndUpdated() throws Exception {
        JsonNode link = createLink(teacherA, "https://example.org/video");

        mockMvc.perform(put(RESOURCES + "/{id}", link.path("id").asLong()).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, teacherA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("url", "https://example.org/other-video"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://example.org/other-video"));
    }

    @Test
    void deletingAnAliasStoredEarlierKeepsTheFileItShares() throws Exception {
        // A row written before managed URLs were reserved to uploads, by teacher A.
        LearningResource alias = storedResource(victim.path("url").asText());
        alias.getCreatedBy().add(teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow());
        long aliasId = resourceRepository.save(alias).getId();
        createdResources.add(aliasId);

        mockMvc.perform(delete(RESOURCES + "/{id}", aliasId).with(nextAddress()).header(HttpHeaders.AUTHORIZATION, teacherA))
                .andExpect(status().isOk());

        assertThat(resourceRepository.existsById(aliasId)).isFalse();
        expectVictimIntact();
    }

    @Test
    void onlyTheCreatorOrAnAdministratorDeletesAnUploadedResourceAndItsFile() throws Exception {
        mockMvc.perform(delete(RESOURCES + "/{id}", victim.path("id").asLong()).with(nextAddress()).header(HttpHeaders.AUTHORIZATION, teacherA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You can only delete resources you created"));
        expectVictimIntact();

        mockMvc.perform(delete(RESOURCES + "/{id}", victim.path("id").asLong()).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isOk());
        assertThat(resourceRepository.existsById(victim.path("id").asLong())).isFalse();
        assertThat(victimFile).doesNotExist();

        JsonNode own = upload(teacherBBearer, new MockMultipartFile("file", "again.pdf", "application/pdf", PDF), "DOCUMENT");
        Path ownFile = managedFile(own.path("url").asText());
        mockMvc.perform(delete(RESOURCES + "/{id}", own.path("id").asLong()).with(nextAddress()).header(HttpHeaders.AUTHORIZATION, teacherBBearer))
                .andExpect(status().isOk());
        assertThat(ownFile).doesNotExist();
    }

    @Test
    void anotherSchoolCannotReadAFileOrIncrementItsCounters() throws Exception {
        School other = otherSchoolWithTeacherMembership();
        doReturn(other).when(currentSchool).resolve();
        String name = victimFile.getFileName().toString();
        for (String route : List.of("files", "preview", "stream")) {
            mockMvc.perform(get(RESOURCES + "/{route}/{name}", route, name).with(nextAddress())
                            .header(HttpHeaders.AUTHORIZATION, teacherA))
                    .andExpect(status().isNotFound());
        }
        LearningResource unchanged = resourceRepository.findById(victim.path("id").asLong()).orElseThrow();
        assertThat(unchanged.getViewCount()).isZero();
        assertThat(unchanged.getDownloadCount()).isZero();
        assertThat(victimFile).exists();
    }

    @Test
    void sharedFilenameCountersChangeOnlyInTheCurrentSchool() throws Exception {
        School other = otherSchoolWithTeacherMembership();
        LearningResource foreign = storedResource(victim.path("url").asText());
        foreign.setSchool(other);
        foreign = resourceRepository.save(foreign);
        createdResources.add(foreign.getId());
        String name = victimFile.getFileName().toString();
        for (String route : List.of("files", "preview")) {
            mockMvc.perform(get(RESOURCES + "/{route}/{name}", route, name).with(nextAddress())
                            .header(HttpHeaders.AUTHORIZATION, teacherA))
                    .andExpect(status().isOk()).andExpect(content().bytes(PDF));
        }
        LearningResource own = resourceRepository.findById(victim.path("id").asLong()).orElseThrow();
        LearningResource untouched = resourceRepository.findById(foreign.getId()).orElseThrow();
        assertThat(own.getViewCount()).isEqualTo(1L);
        assertThat(own.getDownloadCount()).isEqualTo(1L);
        assertThat(untouched.getViewCount()).isZero();
        assertThat(untouched.getDownloadCount()).isZero();
    }

    @Test
    void sameSchoolCanStreamVideoWithExistingDefensiveHeaders() throws Exception {
        String name = UUID.randomUUID() + ".mp4";
        Path video = Paths.get(uploadPath).resolve(name);
        byte[] bytes = "stored video bytes".getBytes(StandardCharsets.US_ASCII);
        Files.write(video, bytes);
        files.add(video);
        LearningResource resource = storedResource(FILES + name);
        resource.setType(ResourceType.VIDEO);
        createdResources.add(resourceRepository.save(resource).getId());
        mockMvc.perform(get(RESOURCES + "/stream/{name}", name).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, teacherA))
                .andExpect(status().isOk()).andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes(bytes)).andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", FILE_CSP));
    }

    @Test
    void deletingAResourceKeepsThePhysicalFileReferencedByAnotherSchool() throws Exception {
        School other = otherSchoolWithTeacherMembership();
        LearningResource foreign = storedResource(victim.path("url").asText());
        foreign.setSchool(other);
        foreign = resourceRepository.save(foreign);
        createdResources.add(foreign.getId());
        mockMvc.perform(delete(RESOURCES + "/{id}", victim.path("id").asLong()).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, teacherBBearer))
                .andExpect(status().isOk());
        assertThat(resourceRepository.existsById(victim.path("id").asLong())).isFalse();
        assertThat(resourceRepository.existsById(foreign.getId())).isTrue();
        assertThat(Files.readAllBytes(victimFile)).isEqualTo(PDF);
        doReturn(other).when(currentSchool).resolve();
        mockMvc.perform(get(RESOURCES + "/files/{name}", victimFile.getFileName().toString()).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, teacherA))
                .andExpect(status().isOk()).andExpect(content().bytes(PDF));
    }

    private School otherSchoolWithTeacherMembership() {
        School other = new School();
        other.setName("Managed file school B " + UUID.randomUUID());
        other = schools.saveAndFlush(other);
        createdSchools.add(other);
        SchoolMembership membership = new SchoolMembership();
        membership.setSchool(other);
        membership.setUser(teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow());
        membership.setRoles(Set.of(MembershipRole.TEACHER));
        membership.setStatus(MembershipStatus.ACTIVE);
        memberships.saveAndFlush(membership);
        return other;
    }

    private void expectVictimIntact() throws Exception {
        LearningResource stored = resourceRepository.findById(victim.path("id").asLong()).orElseThrow();
        assertThat(stored.getUrl()).isEqualTo(victim.path("url").asText());
        assertThat(Files.readAllBytes(victimFile)).isEqualTo(PDF);
        mockMvc.perform(get(RESOURCES + "/files/{name}", victimFile.getFileName().toString()).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, teacherBBearer))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PDF));
    }

    private void expectManagedUrlRefused(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(MANAGED_URL_REFUSED));
    }

    private JsonNode upload(String bearer, MockMultipartFile file, String type) throws Exception {
        String body = mockMvc.perform(multipart(RESOURCES + "/upload").file(file).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .param("title", "Managed " + UUID.randomUUID())
                        .param("description", "Uploaded by a test")
                        .param("type", type))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode resource = objectMapper.readTree(body).path("data");
        createdResources.add(resource.path("id").asLong());
        files.add(managedFile(resource.path("url").asText()));
        return resource;
    }

    private JsonNode createLink(String bearer, String url) throws Exception {
        String body = mockMvc.perform(post(RESOURCES).with(nextAddress())
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(linkRequest(url))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(url))
                .andReturn().getResponse().getContentAsString();
        JsonNode resource = objectMapper.readTree(body).path("data");
        createdResources.add(resource.path("id").asLong());
        return resource;
    }

    private static Map<String, Object> linkRequest(String url) {
        return Map.of("title", "Link " + UUID.randomUUID(), "description", "A lesson link", "url", url, "type", "LINK");
    }

    private LearningResource storedResource(String url) {
        LearningResource r = new LearningResource();
        r.setSchool(currentSchool.resolve());
        r.setTitle("Stored " + UUID.randomUUID());
        r.setUrl(url);
        r.setType(ResourceType.DOCUMENT);
        r.setPublic(true);
        return r;
    }

    private Path managedFile(String url) {
        assertThat(url).startsWith(FILES);
        return Paths.get(uploadPath).resolve(url.substring(FILES.length()));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static RequestPostProcessor nextAddress() {
        return request -> {
            request.setRemoteAddr("10.0.33." + clientAddress.incrementAndGet());
            return request;
        };
    }

    private String bearer(String email) throws Exception {
        MockHttpServletRequestBuilder login = post("/api/auth/login").with(nextAddress())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", DevFixtureLoader.PASSWORD)));
        String body = mockMvc.perform(login)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode token = objectMapper.readTree(body).path("data").path("accessToken");
        assertThat(token.isTextual()).isTrue();
        return "Bearer " + token.asText();
    }
}
