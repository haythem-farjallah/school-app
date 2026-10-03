package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.dto.CreateTeachingAssignmentDto;
import com.example.school_management.feature.academic.dto.UpdateTeachingAssignmentDto;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.academic.service.TeachingAssignmentService;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.membership.entity.*;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional
class TeachingAssignmentSchoolAccessIntegrationTest {
    private static final String BASE = "/admin/teaching-assignments";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired TeachingAssignmentService service;
    @Autowired TeachingAssignmentRepository assignments;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    private School ownSchool, foreignSchool;
    private ClassEntity ownClass, foreignClass;
    private Course ownCourse, foreignCourse;
    private Teacher teacher, foreignTeacher;
    private TeachingAssignment ownAssignment, foreignAssignment;
    private SchoolMembership teacherMembership;

    @BeforeEach
    void setUp() {
        ownSchool = school("Assignment current");
        foreignSchool = school("Assignment foreign");
        doReturn(ownSchool).when(currentSchool).resolve();
        ownClass = clazz(ownSchool, "Matching assignment class");
        foreignClass = clazz(foreignSchool, "Matching assignment foreign class");
        ownCourse = course(ownSchool, "Matching assignment course");
        foreignCourse = course(foreignSchool, "Matching assignment foreign course");
        teacher = teacher();
        foreignTeacher = teacher();
        teacherMembership = membership(teacher, ownSchool);
        membership(teacher, foreignSchool);
        membership(foreignTeacher, foreignSchool);
        ownAssignment = assignment(teacher, ownClass, ownCourse);
        foreignAssignment = assignment(teacher, foreignClass, foreignCourse);
        em.flush();
    }

    @Test
    void foreignAssignmentReadPatchDeleteAndBulkDeleteReturn404WithoutMutation() throws Exception {
        response(get(BASE + "/{id}", foreignAssignment.getId()), 404);
        response(patch(BASE + "/{id}", foreignAssignment.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"weeklyHours\":8}"), 404);
        response(delete(BASE + "/{id}", foreignAssignment.getId()), 404);
        response(delete(BASE + "/bulk").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(List.of(ownAssignment.getId(), foreignAssignment.getId()))), 404);
        assertThat(assignments.existsById(ownAssignment.getId())).isTrue();
        assertThat(assignments.existsById(foreignAssignment.getId())).isTrue();
        assertThat(foreignAssignment.getWeeklyHours()).isEqualTo(2);
    }

    @Test
    void listSearchFilterAndResourcePagingExcludeForeignAndInconsistentAssignments() throws Exception {
        assignment(teacher, ownClass, foreignCourse);
        assignment(teacher, foreignClass, ownCourse);
        assignment(foreignTeacher, clazz(ownSchool, "Matching assignment invalid teacher class"), ownCourse);
        em.flush();
        for (MockHttpServletRequestBuilder request : List.of(get(BASE), get(BASE + "/search").param("q", "Matching assignment"),
                get(BASE + "/filter").param("weeklyHours_eq", "2"), get(BASE + "/teacher/{id}", teacher.getId()),
                get(BASE + "/course/{id}", ownCourse.getId()), get(BASE + "/class/{id}", ownClass.getId()))) {
            JsonNode data = response(request.param("size", "1"), 200);
            assertThat(data.path("totalElements").asLong()).isEqualTo(1);
            assertThat(data.path("content")).hasSize(1);
            assertThat(data.path("content").get(0).path("id").asLong()).isEqualTo(ownAssignment.getId());
        }
        PageRequest pageable = PageRequest.of(0, 1);
        for (var page : List.of(service.findAll(pageable), service.search(pageable, "Matching assignment"),
                service.findWithAdvancedFilters(pageable, Map.of("weeklyHours_eq", new String[]{"2"})),
                service.findByTeacherId(teacher.getId(), pageable), service.findByCourseId(ownCourse.getId(), pageable),
                service.findByClassId(ownClass.getId(), pageable))) {
            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getTotalPages()).isEqualTo(1);
            assertThat(page.getContent()).extracting(TeachingAssignment::getId).containsExactly(ownAssignment.getId());
        }
        assertThat(service.findAll()).extracting(TeachingAssignment::getId).containsExactly(ownAssignment.getId());
        assertThat(service.findByTeacherId(teacher.getId())).extracting(TeachingAssignment::getId).containsExactly(ownAssignment.getId());
        assertThat(service.findByCourseId(ownCourse.getId())).extracting(TeachingAssignment::getId).containsExactly(ownAssignment.getId());
        assertThat(service.findByClassId(ownClass.getId())).extracting(TeachingAssignment::getId).containsExactly(ownAssignment.getId());
        for (MockHttpServletRequestBuilder request : List.of(get(BASE + "/teacher/{id}", foreignTeacher.getId()),
                get(BASE + "/course/{id}", foreignCourse.getId()), get(BASE + "/class/{id}", foreignClass.getId()))) response(request, 404);
        assertThat(service.search(PageRequest.of(0, 1), null).getTotalElements()).isEqualTo(1);
    }

    @Test
    void classDetailAndAssignmentCountsExcludeForeignCourseAndTeacherLinks() throws Exception {
        assignment(teacher, ownClass, foreignCourse);
        Course anotherOwnCourse = course(ownSchool, "Invalid teacher course");
        assignment(foreignTeacher, ownClass, anotherOwnCourse);
        em.flush();
        JsonNode data = response(get("/api/v1/classes/{id}/details", ownClass.getId()), 200);
        assertThat(data.path("courses")).hasSize(1);
        assertThat(data.path("courses").get(0).path("courseId").asLong()).isEqualTo(ownCourse.getId());
        assertThat(data.path("courses").get(0).path("teacherId").asLong()).isEqualTo(teacher.getId());
        assertThat(assignments.aggregateForClasses(List.of(ownClass.getId())))
                .singleElement().satisfies(row -> assertThat(row.getTeacherCnt()).isEqualTo(1));
        response(get("/api/v1/classes/{id}/details", foreignClass.getId()), 404);
    }

    @Test
    void inconsistentAssignmentIdsAreInvisible() throws Exception {
        TeachingAssignment foreignCourseLink = assignment(teacher, ownClass, foreignCourse);
        TeachingAssignment foreignClassLink = assignment(teacher, foreignClass, ownCourse);
        TeachingAssignment foreignTeacherLink = assignment(foreignTeacher, clazz(ownSchool, "Invalid teacher class"), ownCourse);
        em.flush();
        for (TeachingAssignment invalid : List.of(foreignCourseLink, foreignClassLink, foreignTeacherLink)) {
            response(get(BASE + "/{id}", invalid.getId()), 404);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher", "class", "course"})
    void createAndPatchRejectForeignReplacementResources(String reference) throws Exception {
        Long teacherId = reference.equals("teacher") ? foreignTeacher.getId() : teacher.getId();
        Long courseId = reference.equals("course") ? foreignCourse.getId() : ownCourse.getId();
        Long classId = reference.equals("class") ? foreignClass.getId() : ownClass.getId();
        long count = assignments.count();
        response(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(new CreateTeachingAssignmentDto(teacherId, courseId, classId, 2))), 404);
        response(patch(BASE + "/{id}", ownAssignment.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new UpdateTeachingAssignmentDto(teacherId, courseId, classId, 3))), 404);
        assertThat(assignments.count()).isEqualTo(count);
        assertThat(ownAssignment.getTeacher().getId()).isEqualTo(teacher.getId());
        assertThat(ownAssignment.getClazz().getId()).isEqualTo(ownClass.getId());
        assertThat(ownAssignment.getCourse().getId()).isEqualTo(ownCourse.getId());
    }

    @Test
    void validCreatePatchAndDeletePreserveAssignmentModel() throws Exception {
        Course another = course(ownSchool, "Another course");
        JsonNode created = response(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new CreateTeachingAssignmentDto(teacher.getId(), another.getId(), ownClass.getId(), 3))), 201);
        Long id = created.path("id").asLong();
        response(patch(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{\"weeklyHours\":4}"), 200);
        assertThat(service.find(id).getWeeklyHours()).isEqualTo(4);
        response(delete(BASE + "/{id}", id), 200);
        assertThat(assignments.existsById(id)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"teacher", "class", "course"})
    void bulkCreatePrevalidatesEntireRequestBeforeCreatingAnyAssignment(String reference) throws Exception {
        Course newCourse = course(ownSchool, "New bulk course");
        CreateTeachingAssignmentDto valid = new CreateTeachingAssignmentDto(teacher.getId(), newCourse.getId(), ownClass.getId(), 2);
        CreateTeachingAssignmentDto invalid = new CreateTeachingAssignmentDto(reference.equals("teacher") ? foreignTeacher.getId() : teacher.getId(),
                reference.equals("course") ? foreignCourse.getId() : newCourse.getId(), reference.equals("class") ? foreignClass.getId() : ownClass.getId(), 2);
        long count = assignments.count();
        response(post(BASE + "/bulk/create").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(List.of(valid, invalid, valid))), 404);
        assertThat(assignments.count()).isEqualTo(count);
    }

    @Test
    void validBulkCreateKeepsBestEffortDuplicateBehaviorAndBulkDeleteRemovesOnlyValidatedRows() throws Exception {
        Course first = course(ownSchool, "First bulk course");
        Course second = course(ownSchool, "Second bulk course");
        long count = assignments.count();
        CreateTeachingAssignmentDto existing = new CreateTeachingAssignmentDto(teacher.getId(), ownCourse.getId(), ownClass.getId(), 2);
        CreateTeachingAssignmentDto newFirst = new CreateTeachingAssignmentDto(teacher.getId(), first.getId(), ownClass.getId(), 2);
        CreateTeachingAssignmentDto newSecond = new CreateTeachingAssignmentDto(teacher.getId(), second.getId(), ownClass.getId(), 2);
        response(post(BASE + "/bulk/create").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(List.of(newFirst, existing, newSecond))), 200);
        assertThat(assignments.count()).isEqualTo(count + 2);
        List<Long> ids = service.findByClassId(ownClass.getId()).stream().map(TeachingAssignment::getId)
                .filter(id -> !id.equals(ownAssignment.getId())).toList();
        response(delete(BASE + "/bulk").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(ids)), 200);
        assertThat(assignments.count()).isEqualTo(count);
        assertThat(assignments.existsById(foreignAssignment.getId())).isTrue();
    }

    @Test
    void assignTeacherToCoursesAndTeachersToCoursePrevalidateEveryReference() throws Exception {
        Course newCourse = course(ownSchool, "New assign course");
        long count = assignments.count();
        response(post(BASE + "/assign/teacher-to-courses").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "teacherId", teacher.getId(), "classId", ownClass.getId(), "courseIds", List.of(newCourse.getId(), foreignCourse.getId())))), 404);
        response(post(BASE + "/assign/teachers-to-course").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "teacherIds", List.of(teacher.getId(), foreignTeacher.getId()), "classId", ownClass.getId(), "courseId", newCourse.getId()))), 404);
        assertThat(assignments.count()).isEqualTo(count);
        response(post(BASE + "/assign/teacher-to-courses").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "teacherId", teacher.getId(), "classId", ownClass.getId(), "courseIds", List.of(newCourse.getId())))), 200);
        assertThat(assignments.count()).isEqualTo(count + 1);
        Course last = course(ownSchool, "Last assign course");
        response(post(BASE + "/assign/teachers-to-course").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "teacherIds", List.of(teacher.getId()), "classId", ownClass.getId(), "courseId", last.getId()))), 200);
        assertThat(assignments.count()).isEqualTo(count + 2);
    }

    @Test
    void explicitIdAndExistenceLookupsRejectForeignResources() {
        assertThatThrownBy(() -> service.findByIds(List.of(ownAssignment.getId(), foreignAssignment.getId()))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.find(Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.existsByTeacherAndCourseAndClass(teacher.getId(), foreignCourse.getId(), ownClass.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.existsByTeacherAndCourseAndClass(foreignTeacher.getId(), ownCourse.getId(), ownClass.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.hasConflictingAssignment(teacher.getId(), ownCourse.getId(), foreignClass.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.existsByTeacherAndCourseAndClass(teacher.getId(), ownCourse.getId(), ownClass.getId())).isTrue();
        assertThat(service.hasConflictingAssignment(teacher.getId(), ownCourse.getId(), ownClass.getId())).isTrue();
        assertThat(service.findByIds(List.of(ownAssignment.getId()))).extracting(TeachingAssignment::getId).containsExactly(ownAssignment.getId());
    }

    @Test
    void teacherMembershipRoleRatherThanAccountRoleControlsAssignmentResource() throws Exception {
        teacherMembership.setRoles(Set.of(MembershipRole.STUDENT));
        em.flush();
        response(get(BASE + "/{id}", ownAssignment.getId()), 404);
        response(get(BASE + "/teacher/{id}", teacher.getId()), 404);
        assertThat(service.findAll()).isEmpty();
        assertThat(assignments.findByTeacherIdAndSchoolId(teacher.getId(), ownSchool.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVE", "SUSPENDED"})
    void administrativeSchedulingPreservesMembershipStatusCompatibility(MembershipStatus membershipStatus) throws Exception {
        teacherMembership.setStatus(membershipStatus);
        em.flush();
        response(get(BASE + "/{id}", ownAssignment.getId()), 200);
        assertThat(service.findByTeacherId(teacher.getId())).hasSize(1);
    }

    private JsonNode response(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        String body = mvc.perform(request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN"))).andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("data");
    }
    private School school(String name) { School school = new School(); school.setName(name); em.persist(school); return school; }
    private ClassEntity clazz(School school, String name) {
        AcademicYear year = new AcademicYear(); year.setSchool(school); year.setName(name + " year"); year.setStartDate(LocalDate.of(2031, 9, 1));
        year.setEndDate(LocalDate.of(2032, 6, 30)); em.persist(year);
        ClassEntity clazz = new ClassEntity(); clazz.setAcademicYear(year); clazz.setName(name); em.persist(clazz); return clazz;
    }
    private Course course(School school, String name) {
        Course course = new Course(); course.setSchool(school); course.setName(name); course.setCode(UUID.randomUUID().toString().substring(0, 8)); em.persist(course); return course;
    }
    private Teacher teacher() {
        Teacher teacher = new Teacher(); teacher.setEmail(UUID.randomUUID() + "@assignment.school.test"); teacher.setFirstName("Assignment"); teacher.setLastName("Teacher");
        teacher.setPassword("unused"); teacher.setRole(UserRole.TEACHER); teacher.setStatus(Status.ACTIVE); teacher.setIsEmailVerified(true); em.persist(teacher); return teacher;
    }
    private SchoolMembership membership(Teacher teacher, School school) {
        SchoolMembership membership = new SchoolMembership(); membership.setUser(teacher); membership.setSchool(school); membership.setRoles(Set.of(MembershipRole.TEACHER));
        membership.setStatus(MembershipStatus.ACTIVE); em.persist(membership); return membership;
    }
    private TeachingAssignment assignment(Teacher teacher, ClassEntity clazz, Course course) {
        TeachingAssignment assignment = new TeachingAssignment(); assignment.setTeacher(teacher); assignment.setClazz(clazz); assignment.setCourse(course); assignment.setWeeklyHours(2);
        em.persist(assignment); return assignment;
    }
}
