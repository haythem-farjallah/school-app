package com.example.school_management.feature.auth;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.entity.enums.GradeLevel;
import com.example.school_management.feature.auth.service.StudentService;
import com.example.school_management.feature.auth.service.TeacherService;
import com.example.school_management.feature.auth.service.ParentService;
import com.example.school_management.feature.membership.entity.*;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.commons.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;

@IntegrationTest
@Transactional
class PeopleDirectorySchoolAccessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired StudentService studentService;
    @Autowired TeacherService teacherService;
    @Autowired ParentService parentService;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    @MockitoBean EmailService emailService;

    private School school;
    private School foreignSchool;
    private final Map<String, List<BaseUser>> own = new HashMap<>();
    private final Map<String, BaseUser> foreign = new HashMap<>();
    private final Map<String, BaseUser> withoutMembership = new HashMap<>();
    private final Map<String, BaseUser> wrongRole = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(emailService.sendBulkEmails(anyList(), anyString(), anyString(), anyMap()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(new EmailService.BulkEmailResult(0, 0, 0, List.of(), List.of())));
        school = school("People current");
        foreignSchool = school("People foreign");
        doReturn(school).when(currentSchool).resolve();
        for (String type : List.of("students", "teachers", "parents")) {
            BaseUser first = account(type, "Directory match");
            BaseUser second = account(type, "Directory match");
            own.put(type, List.of(first, second));
            membership(first, school, role(type), MembershipStatus.SUSPENDED);
            membership(first, foreignSchool, role(type), MembershipStatus.ACTIVE);
            membership(second, school, role(type), MembershipStatus.INACTIVE);
            foreign.put(type, account(type, "Directory match"));
            membership(foreign.get(type), foreignSchool, role(type), MembershipStatus.ACTIVE);
            withoutMembership.put(type, account(type, "Directory match"));
            wrongRole.put(type, account(type, "Directory match"));
            membership(wrongRole.get(type), school, MembershipRole.ADMIN, MembershipStatus.ACTIVE);
        }
        em.flush();
    }

    @ParameterizedTest
    @CsvSource({"students,GET", "students,PATCH", "students,DELETE",
            "teachers,GET", "teachers,PATCH", "teachers,DELETE",
            "parents,GET", "parents,PATCH", "parents,DELETE"})
    void foreignMissingAndWrongRoleResourcesAre404(String type, String method) throws Exception {
        for (Long id : List.of(foreign.get(type).getId(), withoutMembership.get(type).getId(),
                wrongRole.get(type).getId(), Long.MAX_VALUE)) {
            String path = base(type) + "/" + id;
            MockHttpServletRequestBuilder request = switch (method) {
                case "GET" -> get(path);
                case "DELETE" -> delete(path);
                default -> patch(path).content(type.equals("students")
                        ? "{\"firstName\":\"Changed\",\"lastName\":\"Name\",\"gradeLevel\":\"HIGH\",\"enrollmentYear\":2024}"
                        : "{\"telephone\":\"changed\"}");
            };
            perform(request).andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"students", "teachers", "parents"})
    void collectionsScopeBeforePaginationAndPreserveMembershipHistory(String type) throws Exception {
        for (MockHttpServletRequestBuilder request : List.of(
                get(base(type)), get(base(type) + "/search").param("q", "Directory"),
                get(base(type) + "/search").param("q", " "),
                get(base(type) + "/filter").param("firstName_like", "Directory"),
                get(base(type) + "/filter").param("search", "Directory"))) {
            JsonNode page = data(request.param("size", "1"));
            assertThat(page.path("totalElements").asInt()).isEqualTo(2);
            assertThat(page.path("content")).hasSize(1);
            assertThat(page.path("content").get(0).path("id").asLong())
                    .isIn(own.get(type).get(0).getId(), own.get(type).get(1).getId());
        }
        JsonNode secondPage = data(get(base(type)).param("size", "1").param("page", "1"));
        assertThat(secondPage.path("content")).hasSize(1);
        assertThat(secondPage.path("totalElements").asInt()).isEqualTo(2);
        JsonNode all = data(get(base(type)).param("size", "50"));
        assertThat(all.path("content").findValuesAsText("id")).containsExactlyInAnyOrder(
                own.get(type).get(0).getId().toString(), own.get(type).get(1).getId().toString());
        for (BaseUser account : own.get(type)) {
            perform(get(base(type) + "/" + account.getId())).andExpect(status().isOk());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"students", "teachers", "parents"})
    void clientFiltersCannotExposeForeignUsers(String type) throws Exception {
        for (MockHttpServletRequestBuilder request : List.of(
                get(base(type)).param("emailLike", foreign.get(type).getEmail()),
                get(base(type) + "/search").param("q", foreign.get(type).getEmail()),
                get(base(type) + "/filter").param("email_eq", foreign.get(type).getEmail()))) {
            JsonNode page = data(request);
            assertThat(page.path("content")).isEmpty();
            assertThat(page.path("totalElements").asInt()).isZero();
        }
    }

    @Test
    void studentStatsUseOnlyCurrentSchoolPopulation() throws Exception {
        Student first = (Student) own.get("students").get(0);
        Student second = (Student) own.get("students").get(1);
        first.setGradeLevel(GradeLevel.HIGH);
        first.setEnrolledAt(LocalDateTime.of(2023, 9, 1, 0, 0));
        second.setStatus(Status.SUSPENDED);
        second.setGradeLevel(GradeLevel.MIDDLE);
        second.setEnrolledAt(LocalDateTime.of(2024, 9, 1, 0, 0));
        Student outsider = (Student) foreign.get("students");
        outsider.setGradeLevel(GradeLevel.UNIVERSITY);
        outsider.setEnrolledAt(LocalDateTime.of(2020, 9, 1, 0, 0));
        em.flush();
        JsonNode stats = data(get(base("students") + "/stats"));
        assertThat(stats.path("totalStudents").asInt()).isEqualTo(2);
        assertThat(stats.path("activeStudents").asInt()).isEqualTo(1);
        assertThat(stats.path("suspendedStudents").asInt()).isEqualTo(1);
        assertThat(stats.path("studentsByGradeLevel")).isEqualTo(json.readTree("{\"HIGH\":1,\"MIDDLE\":1}"));
        assertThat(stats.path("studentsByEnrollmentYear")).isEqualTo(json.readTree("{\"2023\":1,\"2024\":1}"));
    }

    @ParameterizedTest
    @CsvSource({"students,csv", "students,excel", "teachers,csv", "teachers,excel"})
    void allAndExplicitExportsAreSchoolScoped(String type, String format) throws Exception {
        byte[] bytes = perform(post(base(type) + "/export/" + format)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        String exported;
        if (format.equals("csv")) {
            exported = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } else {
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                StringBuilder cells = new StringBuilder();
                workbook.getSheetAt(0).forEach(row -> row.forEach(cell -> cells.append(cell.toString()).append('\n')));
                exported = cells.toString();
            }
        }
        for (BaseUser account : own.get(type)) assertThat(exported).contains(account.getEmail());
        for (BaseUser outsider : List.of(foreign.get(type), withoutMembership.get(type), wrongRole.get(type))) {
            assertThat(exported).doesNotContain(outsider.getEmail());
        }
        for (Long invalid : List.of(foreign.get(type).getId(), withoutMembership.get(type).getId(),
                wrongRole.get(type).getId(), Long.MAX_VALUE)) {
            perform(post(base(type) + "/export/" + format)
                    .content(json.writeValueAsString(Map.of("ids", List.of(own.get(type).get(0).getId(), invalid)))))
                    .andExpect(status().isNotFound());
        }
        perform(post(base(type) + "/export/" + format).content(json.writeValueAsString(
                Map.of("ids", List.of(own.get(type).get(0).getId(), own.get(type).get(0).getId())))))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"students,status", "students,delete", "teachers,status", "teachers,delete"})
    void bulkMutationRejectsCompleteMixedSetBeforeChangingAnyAccount(String type, String operation) throws Exception {
        for (Long invalid : List.of(foreign.get(type).getId(), withoutMembership.get(type).getId(),
                wrongRole.get(type).getId(), Long.MAX_VALUE)) {
            List<Long> ids = List.of(own.get(type).get(0).getId(), invalid, own.get(type).get(1).getId());
            MockHttpServletRequestBuilder request = operation.equals("status")
                    ? patch(base(type) + "/bulk/status").content(json.writeValueAsString(Map.of("ids", ids, "status", "SUSPENDED")))
                    : delete(base(type) + "/bulk").content(json.writeValueAsString(ids));
            perform(request).andExpect(status().isNotFound());
            assertThat(own.get(type)).allSatisfy(account -> assertThat(account.getStatus()).isEqualTo(Status.ACTIVE));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"students", "teachers"})
    void bulkEmailRejectsInvalidTargetsBeforeSending(String type) throws Exception {
        for (Long invalid : List.of(foreign.get(type).getId(), withoutMembership.get(type).getId(),
                wrongRole.get(type).getId(), Long.MAX_VALUE)) {
            perform(post(base(type) + "/bulk/email").content(json.writeValueAsString(Map.of(
                    "ids", List.of(own.get(type).get(0).getId(), invalid), "subject", "Notice", "message", "Hello"))))
                    .andExpect(status().isNotFound());
            verify(emailService, never()).sendBulkEmails(anyList(), anyString(), anyString(), anyMap());
        }
    }

    @Test
    void inheritedPagedReadsAndExplicitIdHelpersUseSchoolMembership() {
        var pageable = PageRequest.of(0, 1);
        Map<String, String[]> filters = Map.of("firstName_like", new String[]{"Directory"});
        for (var page : List.of(
                studentService.findAll(pageable), teacherService.findAll(pageable), parentService.findAll(pageable),
                studentService.search(pageable, "Directory"), teacherService.search(pageable, "Directory"), parentService.search(pageable, "Directory"),
                studentService.findWithAdvancedFilters(pageable, filters), teacherService.findWithAdvancedFilters(pageable, filters),
                parentService.findWithAdvancedFilters(pageable, filters),
                studentService.findAllWithFilters(pageable, null, null, null, null, null, null),
                teacherService.findAllWithFilters(pageable, null, null, null, null, null, null, null),
                parentService.findAllWithFilters(pageable, null, null, null, null, null))) {
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.getContent()).hasSize(1);
        }
        assertThatThrownBy(() -> studentService.findByIds(List.of(foreign.get("students").getId())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> teacherService.findByIds(List.of(foreign.get("teachers").getId())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private org.springframework.test.web.servlet.ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")).contentType(MediaType.APPLICATION_JSON));
    }

    private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
        return json.readTree(perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    }

    private String base(String type) {
        return switch (type) {
            case "students" -> "/api/v1/students";
            case "teachers" -> "/api/admin/teachers";
            default -> "/api/admin/parent-management";
        };
    }

    private MembershipRole role(String type) {
        return switch (type) {
            case "students" -> MembershipRole.STUDENT;
            case "teachers" -> MembershipRole.TEACHER;
            default -> MembershipRole.GUARDIAN;
        };
    }

    private School school(String name) {
        School value = new School();
        value.setName(name);
        em.persist(value);
        return value;
    }

    private BaseUser account(String type, String name) {
        BaseUser value = switch (type) {
            case "students" -> new Student();
            case "teachers" -> new Teacher();
            default -> new Parent();
        };
        value.setRole(switch (type) {
            case "students" -> UserRole.STUDENT;
            case "teachers" -> UserRole.TEACHER;
            default -> UserRole.PARENT;
        });
        value.setFirstName(name);
        value.setLastName("Person");
        value.setEmail(UUID.randomUUID() + "@people.test");
        value.setStatus(Status.ACTIVE);
        em.persist(value);
        return value;
    }

    private void membership(BaseUser account, School owner, MembershipRole role, MembershipStatus status) {
        SchoolMembership value = new SchoolMembership();
        value.setUser(account);
        value.setSchool(owner);
        value.setRoles(new HashSet<>(Set.of(role)));
        value.setStatus(status);
        em.persist(value);
    }
}
