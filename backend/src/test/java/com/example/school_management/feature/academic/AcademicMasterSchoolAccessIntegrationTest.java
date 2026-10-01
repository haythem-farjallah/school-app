package com.example.school_management.feature.academic;

import com.example.school_management.IntegrationTest;
import com.example.school_management.commons.exceptions.ConflictException;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.commons.utils.QueryParams;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.dto.*;
import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.academic.repository.*;
import com.example.school_management.feature.academic.service.ClassService;
import com.example.school_management.feature.academic.service.CourseService;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.RoomType;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.school_management.feature.academic.dto.BatchIdsRequest.Operation.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
@Transactional
class AcademicMasterSchoolAccessIntegrationTest {
    @Autowired CourseService courseService;
    @Autowired ClassService classService;
    @Autowired CourseRepository courses;
    @Autowired ClassRepository classes;
    @Autowired AcademicYearRepository years;
    @Autowired SchoolRepository schools;
    @Autowired RoomRepository rooms;
    @Autowired PeriodRepository periods;
    @Autowired TeacherRepository teachers;
    @Autowired StudentRepository students;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;

    private School school;
    private School otherSchool;
    private AcademicYear year;
    private AcademicYear otherYear;

    @BeforeEach
    void setUp() {
        school = school("Current school");
        otherSchool = school("Other school");
        year = year(school, "Current", true);
        otherYear = year(otherSchool, "Current", true);
        doReturn(school).when(currentSchool).resolve();
    }

    @ParameterizedTest
    @ValueSource(strings = {"get", "update", "delete"})
    void foreignCourseIsNotFoundAndUnchanged(String operation) {
        var foreign = course(otherSchool, "Foreign math");
        assertThatThrownBy(() -> {
            switch (operation) {
                case "get" -> courseService.get(foreign.getId());
                case "update" -> courseService.update(foreign.getId(), courseUpdate("Changed"));
                case "delete" -> courseService.delete(foreign.getId());
            }
        }).isInstanceOf(ResourceNotFoundException.class);
        em.flush();
        em.clear();
        assertThat(courses.findById(foreign.getId()).orElseThrow().getName()).isEqualTo("Foreign math");
    }

    @Test
    void courseCrudPreservesOwnershipAndAllowsOwnNameAndOtherSchoolNames() {
        course(otherSchool, "Math");
        var created = courseService.create(new CreateCourseRequest("Math", "#123456", 1f, 3, null));
        assertThat(courseService.get(created.id()).name()).isEqualTo("Math");
        assertThat(courseService.update(created.id(), courseUpdate("math")).name()).isEqualTo("math");
        assertThat(courseService.update(created.id(), courseUpdate(null)).name()).isEqualTo("math");
        em.flush();
        em.clear();
        assertThat(courses.findById(created.id()).orElseThrow().getSchool().getId()).isEqualTo(school.getId());
        courseService.delete(created.id());
        em.flush();
        assertThat(courses.existsById(created.id())).isFalse();
    }

    @Test
    void duplicateCourseUpdateIsCaseInsensitiveInsideSchool() {
        course(school, "Math");
        var second = course(school, "Science");
        assertThatThrownBy(() -> courseService.update(second.getId(), courseUpdate("math")))
                .isInstanceOf(ConflictException.class);
        assertThat(second.getName()).isEqualTo("Science");
    }

    @Test
    void bothCourseListsKeepSchoolConstraintWithFiltersIncludesSortAndPagination() {
        var teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        var first = course(school, "Math A");
        first.setTeacher(teacher);
        var second = course(school, "Math B");
        second.setTeacher(teacher);
        course(school, "Science");
        course(otherSchool, "Math foreign").setTeacher(teacher);
        em.flush();
        var simple = courseService.list(PageRequest.of(0, 1, Sort.by("name")), teacher.getId(), "math");
        assertThat(simple.getContent()).extracting(CourseDto::id).containsExactly(first.getId());
        assertThat(simple.getTotalElements()).isEqualTo(2);
        var qp = query("name", "Math");
        qp.setInclude(List.of("teacher"));
        qp.setSize(1);
        qp.setSort(List.of(Sort.Order.desc("name")));
        var dynamic = courseService.listCourses(qp);
        assertThat(dynamic.getContent()).extracting(CourseDto::id).containsExactly(second.getId());
        assertThat(dynamic.getTotalElements()).isEqualTo(2);
        assertThat(courseService.listCourses(query("school.id", otherSchool.getId().toString()))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"get", "update", "delete", "details", "students", "courses"})
    void foreignClassIsNotFoundAndUnchanged(String operation) {
        var foreign = clazz(otherYear, "Foreign 7-A");
        assertThatThrownBy(() -> {
            switch (operation) {
                case "get" -> classService.get(foreign.getId());
                case "update" -> classService.update(foreign.getId(), new UpdateClassRequest("Changed"));
                case "delete" -> classService.delete(foreign.getId());
                case "details" -> classService.getDetails(foreign.getId());
                case "students" -> classService.mutateStudents(foreign.getId(), new BatchIdsRequest(ADD, Set.of()));
                case "courses" -> classService.mutateCourses(foreign.getId(), new BatchIdsRequest(ADD, Set.of()));
            }
        }).isInstanceOf(ResourceNotFoundException.class);
        em.flush();
        em.clear();
        assertThat(classes.findById(foreign.getId()).orElseThrow().getName()).isEqualTo("Foreign 7-A");
    }

    @Test
    void classCrudPreservesYearAndAllowsOwnNameAndNamesInOtherYears() {
        clazz(otherYear, "7-A");
        clazz(year(school, "Previous", false), "7-A");
        var created = classService.create(new CreateClassRequest("7-A"));
        assertThat(classService.get(created.id()).name()).isEqualTo("7-A");
        assertThat(classService.update(created.id(), new UpdateClassRequest("7-a")).name()).isEqualTo("7-a");
        assertThat(classService.update(created.id(), new UpdateClassRequest(null)).name()).isEqualTo("7-a");
        em.flush();
        em.clear();
        assertThat(classes.findById(created.id()).orElseThrow().getAcademicYear().getId()).isEqualTo(year.getId());
        classService.delete(created.id());
        em.flush();
        assertThat(classes.existsById(created.id())).isFalse();
        assertThat(years.existsById(year.getId())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void duplicateClassUpdateIsCaseInsensitiveInsideItsOwnYear(boolean activeYear) {
        var ownYear = activeYear ? year : year(school, "Previous", false);
        clazz(ownYear, "7-A");
        var second = clazz(ownYear, "7-B");
        assertThatThrownBy(() -> classService.update(second.getId(), new UpdateClassRequest("7-a")))
                .isInstanceOf(ConflictException.class);
        assertThat(second.getName()).isEqualTo("7-B");
    }

    @Test
    void classListsAndCardsKeepSchoolConstraintBeforeCounting() {
        var own = clazz(year, "7-A");
        var previous = clazz(year(school, "Previous", false), "7-B");
        var foreign = clazz(otherYear, "7-C");
        var math = course(school, "Math");
        own.getCourses().add(math);
        assignment(own, math);
        var foreignMath = course(otherSchool, "Foreign math");
        foreign.getCourses().add(foreignMath);
        assignment(foreign, foreignMath);
        em.flush();
        var broad = classService.list(PageRequest.of(0, 1, Sort.by("name")), "7-");
        assertThat(broad.getContent()).extracting(ClassDto::id).containsExactly(own.getId());
        assertThat(broad.getTotalElements()).isEqualTo(2);
        var qp = query("name", "7-");
        qp.setInclude(List.of("courses"));
        qp.setSort(List.of(Sort.Order.desc("name")));
        qp.setSize(1);
        var dynamic = classService.listClasses(qp);
        assertThat(dynamic.getContent()).extracting(ClassDto::id).containsExactly(previous.getId());
        assertThat(dynamic.getTotalElements()).isEqualTo(2);
        var cards = classService.listCards(new QueryParams());
        assertThat(cards.getContent()).extracting(ClassCardDto::id).containsExactly(own.getId(), previous.getId());
        assertThat(cards.getContent().get(0).teacherCount()).isEqualTo(1);
        assertThat(cards.getContent().get(0).courseCount()).isEqualTo(1);
        assertThat(classService.getDetails(own.getId()).courses()).hasSize(1);
        var foreignFilter = query("academicYear.school.id", otherSchool.getId().toString());
        assertThat(classService.listClasses(foreignFilter)).isEmpty();
        assertThat(classService.listCards(foreignFilter)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"assignment", "timetable", "legacy"})
    void teacherClassesAreSchoolScopedAcrossEveryRelationship(String relation) {
        var own = clazz(year, "7-A");
        var foreign = clazz(otherYear, "7-B");
        teacherLink(own, relation);
        teacherLink(foreign, relation);
        em.flush();
        var teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        var result = classService.getClassesByTeacherId(teacher.getId(), PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(ClassDto::id).containsExactly(own.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void studentClassesUseEnrollmentAndCurrentSchool() {
        var own = clazz(year, "7-A");
        var foreign = clazz(otherYear, "7-B");
        var student = students.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        for (var clazz : List.of(own, foreign)) {
            var enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setClassEntity(clazz);
            em.persist(enrollment);
        }
        em.flush();
        assertThat(classService.getClassesByStudentId(student.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(ClassDto::id).containsExactly(own.getId());
        classService.addStudent(own.getId(), student.getId());
        assertThat(own.getStudents()).contains(student);
        classService.removeStudent(own.getId(), student.getId());
        assertThat(own.getStudents()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADD", "REMOVE"})
    void courseBatchRejectsForeignIdsBeforeAnyMutation(String operation) {
        var own = clazz(year, "7-A");
        var linked = course(school, "Linked");
        var valid = course(school, "Valid");
        var foreign = course(otherSchool, "Foreign");
        own.getCourses().add(linked);
        em.flush();
        var ids = new LinkedHashSet<>(List.of(linked.getId(), valid.getId(), foreign.getId()));
        assertThatThrownBy(() -> classService.mutateCourses(own.getId(), new BatchIdsRequest(
                BatchIdsRequest.Operation.valueOf(operation), ids)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(own.getCourses()).containsExactly(linked);
        em.flush();
        em.clear();
        assertThat(classes.findById(own.getId()).orElseThrow().getCourses()).extracting(Course::getId)
                .containsExactly(linked.getId());
    }

    @Test
    void singleCourseWrappersEnforceSchoolAndAllowSameSchoolLinks() {
        var own = clazz(year, "7-A");
        var valid = course(school, "Math");
        var foreign = course(otherSchool, "Foreign");
        assertThat(classService.addCourse(own.getId(), valid.getId()).courseIds()).containsExactly(valid.getId());
        assertThat(classService.removeCourse(own.getId(), valid.getId()).courseIds()).isEmpty();
        assertThatThrownBy(() -> classService.addCourse(own.getId(), foreign.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> classService.removeCourse(own.getId(), foreign.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(own.getCourses()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"courses", "classes", "rooms"})
    void foreignResourceHttpCrudMatchesMissingResource(String resource) throws Exception {
        Long id = switch (resource) {
            case "courses" -> course(otherSchool, "Foreign").getId();
            case "classes" -> clazz(otherYear, "Foreign").getId();
            case "rooms" -> room(otherSchool, "Foreign", 20, RoomType.CLASSROOM).getId();
            default -> throw new AssertionError(resource);
        };
        String body = resource.equals("rooms") ? "{\"name\":\"Changed\",\"capacity\":30,\"roomType\":\"LABORATORY\"}" : "{\"name\":\"Changed\"}";
        String path = "/api/v1/" + resource + "/";
        var foreign = mvc.perform(get(path + id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();
        var missing = mvc.perform(get(path + Long.MAX_VALUE).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        // Room's existing error includes the requested ID; compare after normalizing that ID.
        assertThat(json.readTree(foreign).path("detail").asText().replace(id.toString(), "ID"))
                .isEqualTo(json.readTree(missing).path("detail").asText().replace(Long.toString(Long.MAX_VALUE), "ID"));
        mvc.perform(put(path + id).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mvc.perform(delete(path + id).with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        em.clear();
        String name = switch (resource) {
            case "courses" -> courses.findById(id).orElseThrow().getName();
            case "classes" -> classes.findById(id).orElseThrow().getName();
            case "rooms" -> rooms.findById(id).orElseThrow().getName();
            default -> throw new AssertionError(resource);
        };
        assertThat(name).isEqualTo("Foreign");
    }

    @Test
    void roomListsPreserveFiltersAndOrderingInsideCurrentSchool() throws Exception {
        var first = room(school, "Studio A", 20, RoomType.CLASSROOM);
        var second = room(school, "Studio B", 40, RoomType.LABORATORY);
        room(otherSchool, "Studio foreign", 60, RoomType.LABORATORY);
        clazz(year, "7-A").setAssignedRoom(first);
        em.flush();
        mvc.perform(get("/api/v1/rooms").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2));
        mvc.perform(get("/api/v1/rooms").param("name", "Studio").param("size", "1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(first.getId()));
        mvc.perform(get("/api/v1/rooms").param("roomType", "LABORATORY").param("minCapacity", "40")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2));
        for (String path : List.of("/available", "/by-type/LABORATORY", "/by-capacity/30")) {
            mvc.perform(get("/api/v1/rooms" + path).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(second.getId()));
        }
    }

    @Test
    void roomCrudPreservesSchoolAndResponseShape() throws Exception {
        var own = room(school, "Studio", 20, RoomType.CLASSROOM);
        mvc.perform(get("/api/v1/rooms/{id}", own.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(own.getId()))
                .andExpect(jsonPath("$.data.school").doesNotExist());
        mvc.perform(put("/api/v1/rooms/{id}", own.getId()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"capacity\":40,\"roomType\":\"LABORATORY\",\"schoolId\":999999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Updated"))
                .andExpect(jsonPath("$.data.capacity").value(40)).andExpect(jsonPath("$.data.roomType").value("LABORATORY"));
        em.flush();
        em.clear();
        assertThat(rooms.findById(own.getId()).orElseThrow().getSchool().getId()).isEqualTo(school.getId());
        mvc.perform(delete("/api/v1/rooms/{id}", own.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        em.flush();
        assertThat(rooms.existsById(own.getId())).isFalse();
        mvc.perform(post("/api/v1/rooms").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Created\",\"capacity\":20,\"roomType\":\"CLASSROOM\"}"))
                .andExpect(status().isOk());
        assertThat(rooms.findByName("Created").orElseThrow().getSchool().getId()).isEqualTo(school.getId());
    }

    @Test
    void periodsReturnOnlyCurrentSchoolInIndexOrder() throws Exception {
        var later = period(school, 2);
        var earlier = period(school, 1);
        period(otherSchool, 1);
        mvc.perform(get("/api/v1/periods").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(earlier.getId()))
                .andExpect(jsonPath("$[1].id").value(later.getId()));
    }

    @Test
    void teacherRoutesRemainSelfOnlyAndReturnCurrentSchoolClasses() throws Exception {
        var teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        var own = clazz(year, "7-A");
        teacherLink(own, "legacy");
        teacherLink(clazz(otherYear, "7-B"), "legacy");
        em.flush();
        for (String path : List.of("me", teacher.getId().toString())) {
            mvc.perform(get("/api/v1/classes/teacher/" + path).with(user(teacher.getEmail()).roles("TEACHER")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(own.getId()));
        }
        for (String role : List.of("ADMIN", "STAFF")) {
            mvc.perform(get("/api/v1/classes/teacher/{id}", teacher.getId()).with(user("reader").roles(role)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        }
        mvc.perform(get("/api/v1/classes/teacher/{id}", Long.MAX_VALUE).with(user(teacher.getEmail()).roles("TEACHER")))
                .andExpect(status().isForbidden());
    }

    private School school(String name) {
        var school = new School();
        school.setName(name);
        return schools.saveAndFlush(school);
    }

    private AcademicYear year(School school, String name, boolean active) {
        var year = new AcademicYear();
        year.setSchool(school);
        year.setName(name);
        year.setStartDate(LocalDate.of(2026, 9, 1));
        year.setEndDate(LocalDate.of(2027, 6, 30));
        year.setActive(active);
        return years.saveAndFlush(year);
    }

    private Course course(School school, String name) {
        var course = new Course();
        course.setSchool(school);
        course.setName(name);
        course.setCode("C-" + name);
        course.setCredit(1f);
        return courses.saveAndFlush(course);
    }

    private ClassEntity clazz(AcademicYear year, String name) {
        var clazz = new ClassEntity();
        clazz.setAcademicYear(year);
        clazz.setName(name);
        return classes.saveAndFlush(clazz);
    }

    private Room room(School school, String name, int capacity, RoomType type) {
        var room = new Room();
        room.setSchool(school);
        room.setName(name);
        room.setCapacity(capacity);
        room.setRoomType(type);
        return rooms.saveAndFlush(room);
    }

    private Period period(School school, int index) {
        var period = new Period();
        period.setSchool(school);
        period.setIndex(index);
        period.setStartTime(LocalTime.of(8 + index, 0));
        period.setEndTime(LocalTime.of(9 + index, 0));
        return periods.saveAndFlush(period);
    }

    private void assignment(ClassEntity clazz, Course course) {
        var assignment = new TeachingAssignment();
        assignment.setClazz(clazz);
        assignment.setCourse(course);
        assignment.setTeacher(teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow());
        em.persist(assignment);
    }

    private void teacherLink(ClassEntity clazz, String relation) {
        var teacher = teachers.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();
        var school = clazz.getAcademicYear().getSchool();
        switch (relation) {
            case "assignment" -> assignment(clazz, course(school, "Math"));
            case "legacy" -> clazz.getTeachers().add(teacher);
            case "timetable" -> {
                var slot = new TimetableSlot();
                slot.setDayOfWeek(DayOfWeek.MONDAY);
                slot.setForClass(clazz);
                slot.setTeacher(teacher);
                slot.setPeriod(period(school, 1));
                em.persist(slot);
            }
            default -> throw new AssertionError(relation);
        }
    }

    private static UpdateCourseRequest courseUpdate(String name) {
        return new UpdateCourseRequest(name, null, null, null, null);
    }

    private static QueryParams query(String attribute, String value) {
        var qp = new QueryParams();
        qp.setFilters(Map.of(attribute, List.of(value)));
        return qp;
    }
}
