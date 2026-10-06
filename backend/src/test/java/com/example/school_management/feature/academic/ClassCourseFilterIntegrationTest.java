package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.academic.repository.*;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Transactional
class ClassCourseFilterIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired SchoolRepository schools;
    @Autowired AcademicYearRepository years;
    @Autowired ClassRepository classes;
    @Autowired CourseRepository courses;
    @Autowired TeacherRepository teachers;
    @Autowired StudentRepository students;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;

    private ClassEntity firstClass;
    private ClassEntity foreignClass;
    private Course firstCourse;
    private List<Course> ownCourses;

    @BeforeEach
    void setUp() {
        School school = school("Filter School A");
        School other = school("Filter School B");
        doReturn(school).when(currentSchool).resolve();
        AcademicYear year = year(school);
        firstClass = clazz(year, "Room A", 1, 20);
        clazz(year, "Room B", 2, 30);
        clazz(year, "Room C", 3, 40);
        foreignClass = clazz(year(other), "Room foreign", 1, 20);
        clazz(foreignClass.getAcademicYear(), "Room foreign second", 2, 30);
        firstCourse = course(school, "Math A", 3f, 4);
        ownCourses = List.of(firstCourse, course(school, "Math B", 3f, 5), course(school, "Math C", 3.5f, 4));
        course(other, "Math foreign", 3f, 4);
        course(other, "Math foreign second", 3.5f, 4);
    }

    @ParameterizedTest
    @CsvSource({"name_like,room a,1", "yearOfStudy_eq,1,1", "yearOfStudy_in,'1,2',2",
            "maxStudents_eq,20,1", "maxStudents_in,'20,30',2", "search,room a,1"})
    void approvedClassFiltersMatchRows(String parameter, String value, int total) throws Exception {
        for (String path : new String[]{"/api/v1/classes/filter", "/api/v1/classes/cards"}) {
            request(get(path).param(parameter, value).param("sort", "name:asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(total))
                    .andExpect(jsonPath("$.data.content[0].id").value(firstClass.getId()));
        }
    }

    @ParameterizedTest
    @CsvSource({"name_like,math a,1", "credit_eq,3,2", "credit_eq,3.0,2", "credit_eq,3.5,1",
            "weeklyCapacity_eq,4,2", "search,math a,1"})
    void approvedCourseFiltersIncludingFloatEqualityMatchRows(String parameter, String value, int total) throws Exception {
        request(get("/api/v1/courses/filter").param(parameter, value).param("sort", "name:asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(total))
                .andExpect(jsonPath("$.data.content[0].name").value(value.equals("3.5") ? "Math C" : "Math A"));
    }

    @Test
    void approvedTeacherIdFilterUsesOnlyThatExactPath() throws Exception {
        var teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        firstCourse.setTeacher(teacher);
        em.flush();
        request(get("/api/v1/courses/filter").param("teacher.id_eq", teacher.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(firstCourse.getId()));
    }

    @Test
    void filtersAreAndedAcrossFields() throws Exception {
        request(get("/api/v1/classes/filter").param("yearOfStudy_eq", "1").param("maxStudents_eq", "30"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        request(get("/api/v1/courses/filter").param("credit_eq", "3.5").param("weeklyCapacity_eq", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/classes/filter", "/api/v1/classes/cards", "/api/v1/courses/filter"})
    void schoolScopeIsAppliedBeforePaginationAndCounting(String path) throws Exception {
        String name = path.contains("courses") ? "Math" : "Room";
        for (int page = 0; page < 3; page++) {
            request(get(path).param("name_like", name).param("size", "1").param("page", String.valueOf(page))
                            .param("sort", "name:asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(3))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].name").value(name + " " + (char) ('A' + page)))
                    .andExpect(jsonPath("$.data.page").value(page))
                    .andExpect(jsonPath("$.data.size").value(1))
                    .andExpect(jsonPath("$.data.totalPages").doesNotExist());
        }
        request(get(path).param("name_like", name).param("size", "1").param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @ParameterizedTest
    @CsvSource({"name:asc,Room A", "yearOfStudy:desc,Room C", "maxStudents:desc,Room C"})
    void approvedClassSortsOrderBothLists(String sort, String first) throws Exception {
        for (String path : new String[]{"/api/v1/classes/filter", "/api/v1/classes/cards"}) {
            request(get(path).param("sort", sort).param("size", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].name").value(first));
        }
    }

    @ParameterizedTest
    @CsvSource({"name:asc,Math A", "credit:desc,Math C", "weeklyCapacity:desc,Math B"})
    void approvedCourseSortsOrderRows(String sort, String first) throws Exception {
        request(get("/api/v1/courses/filter").param("sort", sort).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].name").value(first));
    }

    @ParameterizedTest
    @ValueSource(strings = {"academicYear.school.id_eq", "academicYear.name_like", "schedule.id_eq",
            "teachers.id_eq", "enrollments.id_eq", "capacity_eq", "school_eq", "studentIds_in",
            "courses.id_eq", "learningResources.id_eq", "timetables.id_eq", "password_like", "name_regex"})
    void unsafeClassFieldsAndOperationsAreRejectedByBothLists(String parameter) throws Exception {
        for (String path : new String[]{"/api/v1/classes/filter", "/api/v1/classes/cards"}) {
            expectProblem(request(get(path).param(parameter, "1")), 400, path)
                    .andExpect(jsonPath("$.detail").value(parameter.equals("name_regex")
                            ? "Unsupported filter parameter 'name_regex'"
                            : "Unsupported filter field '" + parameter.split("_", 2)[0] + "'"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"school.id_eq", "teacher.password_like", "teacher.otpCode_notnull",
            "classes.id_eq", "learningResources.id_eq", "timetableSlots.id_eq", "preferredPeriods_like", "name_regex"})
    void unsafeCourseFieldsAndOperationsAreRejected(String parameter) throws Exception {
        expectProblem(request(get("/api/v1/courses/filter").param(parameter, "true")), 400, "/api/v1/courses/filter")
                .andExpect(jsonPath("$.detail").value(parameter.equals("name_regex")
                        ? "Unsupported filter parameter 'name_regex'"
                        : "Unsupported filter field '" + parameter.split("_", 2)[0] + "'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"academicYear.school.id:asc", "teachers.id:desc", "capacity:asc"})
    void unsafeClassSortsAreRejectedByBothLists(String sort) throws Exception {
        for (String path : new String[]{"/api/v1/classes/filter", "/api/v1/classes/cards"}) {
            expectProblem(request(get(path).param("sort", sort)), 400, path)
                    .andExpect(jsonPath("$.detail").value("Unsupported sort field '" + sort.split(":", 2)[0] + "'"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"school.id:asc", "teacher.password:desc", "teacher.id:asc"})
    void unsafeCourseSortsAreRejected(String sort) throws Exception {
        expectProblem(request(get("/api/v1/courses/filter").param("sort", sort)), 400, "/api/v1/courses/filter")
                .andExpect(jsonPath("$.detail").value("Unsupported sort field '" + sort.split(":", 2)[0] + "'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/classes/new", "/api/v1/courses/new"})
    void legacyEndpointsNoLongerMatchARoute(String path) throws Exception {
        expectProblem(request(get(path)), 404, path)
                .andExpect(jsonPath("$.detail").value("No endpoint matches this request"));
    }

    @Test
    void cardsKeepActiveRosterAndExistingCourseTeacherAggregatesAndIgnoreLegacyParams() throws Exception {
        assignFirstCourse();
        firstClass.getCourses().addAll(ownCourses);
        var student = students.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        for (EnrollmentStatus status : new EnrollmentStatus[]{EnrollmentStatus.ACTIVE, EnrollmentStatus.WITHDRAWN}) {
            var enrollment = new Enrollment();
            enrollment.setClassEntity(firstClass);
            enrollment.setStudent(student);
            enrollment.setStatus(status);
            em.persist(enrollment);
        }
        em.flush();
        request(get("/api/v1/classes/cards").param("name_like", "Room A")
                        .param("filter[name]", "foreign").param("include", "doesNotExist")
                        .param("fields[classCard]", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Room A"))
                .andExpect(jsonPath("$.data.content[0].studentCount").value(1))
                .andExpect(jsonPath("$.data.content[0].courseCount").value(3))
                .andExpect(jsonPath("$.data.content[0].teacherCount").value(1));
        request(get("/api/v1/classes/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Room A"))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void detailsReturnCompleteStableDtoAndKeepSchoolLookup() throws Exception {
        var assignment = assignFirstCourse();
        assignment.setWeeklyHours(4);
        em.flush();
        request(get("/api/v1/classes/{id}/details", firstClass.getId()).param("fields[classView]", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstClass.getId()))
                .andExpect(jsonPath("$.data.name").value("Room A"))
                .andExpect(jsonPath("$.data.courses.length()").value(1))
                .andExpect(jsonPath("$.data.courses[0].courseId").value(firstCourse.getId()))
                .andExpect(jsonPath("$.data.courses[0].courseName").value("Math A"))
                .andExpect(jsonPath("$.data.courses[0].teacherId").value(assignment.getTeacher().getId()))
                .andExpect(jsonPath("$.data.courses[0].teacherName").value("Theo Teacher"))
                .andExpect(jsonPath("$.data.courses[0].weeklyHours").value(4));
        String path = "/api/v1/classes/" + foreignClass.getId() + "/details";
        expectProblem(request(get(path)), 404, path);
    }

    private TeachingAssignment assignFirstCourse() {
        var teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        var membership = new SchoolMembership();
        membership.setUser(teacher);
        membership.setSchool(firstCourse.getSchool());
        membership.setRoles(Set.of(MembershipRole.TEACHER));
        membership.setStatus(MembershipStatus.ACTIVE);
        em.persist(membership);
        firstClass.getCourses().add(firstCourse);
        var assignment = new TeachingAssignment();
        assignment.setClazz(firstClass);
        assignment.setCourse(firstCourse);
        assignment.setTeacher(teacher);
        em.persist(assignment);
        return assignment;
    }

    private ResultActions request(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(user("filter-reader").roles("ADMIN")));
    }

    private ResultActions expectProblem(ResultActions response, int status, String path) throws Exception {
        return response.andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").isString())
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").value(path));
    }

    private School school(String name) {
        var school = new School();
        school.setName(name);
        return schools.saveAndFlush(school);
    }

    private AcademicYear year(School school) {
        var year = new AcademicYear();
        year.setSchool(school);
        year.setName("2026-2027");
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 6, 30));
        year.setActive(true);
        return years.saveAndFlush(year);
    }

    private ClassEntity clazz(AcademicYear year, String name, int level, int maximum) {
        var clazz = new ClassEntity();
        clazz.setAcademicYear(year);
        clazz.setName(name);
        clazz.setYearOfStudy(level);
        clazz.setMaxStudents(maximum);
        return classes.saveAndFlush(clazz);
    }

    private Course course(School school, String name, float credit, int capacity) {
        var course = new Course();
        course.setSchool(school);
        course.setName(name);
        course.setCode(name.replace(" ", "-"));
        course.setCredit(credit);
        course.setWeeklyCapacity(capacity);
        return courses.saveAndFlush(course);
    }
}
