package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.membership.entity.MembershipRole;
import com.example.school_management.feature.membership.entity.MembershipStatus;
import com.example.school_management.feature.membership.entity.SchoolMembership;
import com.example.school_management.feature.operational.dto.CreateTimetableRequest;
import com.example.school_management.feature.operational.dto.UpdateTimetableRequest;
import com.example.school_management.feature.operational.entity.*;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.example.school_management.feature.operational.entity.enums.RoomType;
import com.example.school_management.feature.operational.repository.TimetableRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.operational.service.TimetableService;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.school_management.feature.operational.service.TimetablePdfService;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@Transactional
class TimetableSchoolAccessIntegrationTest {
    private static final String BASE = "/api/v1/timetables";
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TimetableRepository timetables;
    @Autowired TimetableSlotRepository slots;
    @Autowired TimetableService service;
    @MockitoBean TimetablePdfService pdfService;
    @MockitoSpyBean CurrentSchoolResolver currentSchool;
    private School ownSchool, foreignSchool;
    private ClassEntity ownClass, foreignClass;
    private Course ownCourse, foreignCourse;
    private Teacher teacher, foreignTeacher;
    private Period ownPeriod, foreignPeriod;
    private Room ownRoom, foreignRoom;
    private Timetable ownTimetable, foreignTimetable;
    private TimetableSlot ownSlot, foreignSlot;

    @BeforeEach
    void setUp() {
        ownSchool = school("Schedule current"); foreignSchool = school("Schedule foreign");
        doReturn(ownSchool).when(currentSchool).resolve();
        ownClass = clazz(ownSchool, "Current class"); foreignClass = clazz(foreignSchool, "Foreign class");
        ownCourse = course(ownSchool); foreignCourse = course(foreignSchool);
        ownClass.getCourses().add(ownCourse); foreignClass.getCourses().add(foreignCourse);
        teacher = teacher(); foreignTeacher = teacher();
        membership(teacher, ownSchool); membership(teacher, foreignSchool); membership(foreignTeacher, foreignSchool);
        ownPeriod = period(ownSchool); foreignPeriod = period(foreignSchool);
        ownRoom = room(ownSchool); foreignRoom = room(foreignSchool);
        ownTimetable = timetable(ownSchool, ownClass, ownRoom); foreignTimetable = timetable(foreignSchool, foreignClass, foreignRoom);
        ownSlot = slot(ownTimetable, ownClass, ownCourse, ownPeriod, ownRoom);
        foreignSlot = slot(foreignTimetable, foreignClass, foreignCourse, foreignPeriod, foreignRoom);
        em.flush();
    }

    @Test
    void foreignTimetableCannotBeReadDeletedExportedOrOptimized() throws Exception {
        for (var request : List.of(get(BASE + "/{id}", foreignTimetable.getId()),
                delete(BASE + "/{id}", foreignTimetable.getId()),
                get(BASE + "/{id}/export/pdf", foreignTimetable.getId()),
                post(BASE + "/{id}/optimize", foreignTimetable.getId()))) response(request, 404);
        assertThat(timetables.existsById(foreignTimetable.getId())).isTrue();
    }

    @Test
    void ordinaryPdfReceivesOnlyValidatedCurrentSchoolTimetableAndSlots() throws Exception {
        Long ownId = ownTimetable.getId(), ownSlotId = ownSlot.getId();
        em.clear();
        when(pdfService.generateTimetablePdf(any(Timetable.class))).thenReturn(new byte[]{1});
        response(get(BASE + "/{id}/export/pdf", ownId), 200);
        ArgumentCaptor<Timetable> exported = ArgumentCaptor.forClass(Timetable.class);
        verify(pdfService).generateTimetablePdf(exported.capture());
        assertThat(exported.getValue().getSchool().getId()).isEqualTo(ownSchool.getId());
        assertThat(exported.getValue().getSlots()).extracting(TimetableSlot::getId).containsExactly(ownSlotId);
    }

    @Test
    void allListBranchesScopeRowsAndPaginationTotals() throws Exception {
        timetable(ownSchool, ownClass, ownRoom);
        timetable(ownSchool, ownClass, ownRoom);
        for (int i = 0; i < 3; i++) timetable(foreignSchool, foreignClass, foreignRoom);
        em.flush();
        for (var request : List.of(get(BASE), get(BASE).param("academicYear", "2030-2031"),
                get(BASE).param("academicYear", "2030-2031").param("semester", "Fall"))) {
            JsonNode data = response(request.param("size", "1"), 200);
            assertThat(data.path("totalElements").asInt()).isEqualTo(3);
            assertThat(data.path("content")).hasSize(1);
        }
        for (var page : List.of(service.list(PageRequest.of(0, 1), null, null),
                service.list(PageRequest.of(0, 1), "2030-2031", null),
                service.list(PageRequest.of(0, 1), "2030-2031", "Fall"))) {
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(3);
        }
        for (var request : List.of(get(BASE + "/academic-year/2030-2031"),
                get(BASE + "/academic-year/2030-2031/semester/Fall"))) assertThat(response(request, 200)).hasSize(3);
        for (int page = 0; page < 3; page++) {
            assertThat(service.list(PageRequest.of(page, 1), "2030-2031", "Fall").getContent())
                    .allSatisfy(dto -> {
                        assertThat(dto.getClassIds()).containsExactly(ownClass.getId());
                        assertThat(dto.getRoomIds()).containsExactly(ownRoom.getId());
                    });
        }
    }

    @Test
    void teacherAndClassReadsAndPdfsUseOnlyCurrentSchoolSlots() throws Exception {
        JsonNode data = response(get(BASE + "/teacher/{id}", teacher.getId()), 200);
        assertThat(data).hasSize(1);
        assertThat(data.get(0).path("id").asLong()).isEqualTo(ownSlot.getId());
        assertThat(response(get(BASE + "/class/{id}", ownClass.getId()), 200).path("slots")).hasSize(1);
        // Capture the authorized data passed to rendering; legacy class/teacher templates are absent.
        when(pdfService.generateTeacherTimetablePdf(eq(teacher.getId()), anyList())).thenReturn(new byte[]{1});
        when(pdfService.generateClassTimetablePdf(eq(ownClass.getId()), anyList())).thenReturn(new byte[]{1});
        response(get(BASE + "/teacher/{id}/export/pdf", teacher.getId()), 200);
        response(get(BASE + "/class/{id}/export/pdf", ownClass.getId()), 200);
        ArgumentCaptor<List<TimetableSlot>> teacherSlots = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<TimetableSlot>> classSlots = ArgumentCaptor.forClass(List.class);
        verify(pdfService).generateTeacherTimetablePdf(eq(teacher.getId()), teacherSlots.capture());
        verify(pdfService).generateClassTimetablePdf(eq(ownClass.getId()), classSlots.capture());
        assertThat(teacherSlots.getValue()).extracting(TimetableSlot::getId).containsExactly(ownSlot.getId());
        assertThat(classSlots.getValue()).extracting(TimetableSlot::getId).containsExactly(ownSlot.getId());
        for (var request : List.of(get(BASE + "/teacher/{id}", foreignTeacher.getId()),
                get(BASE + "/class/{id}", foreignClass.getId()),
                get(BASE + "/teacher/{id}/export/pdf", foreignTeacher.getId()),
                get(BASE + "/class/{id}/export/pdf", foreignClass.getId()))) response(request, 404);
        verifyNoMoreInteractions(pdfService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"periodId", "forClassId", "forCourseId", "teacherId", "roomId"})
    void manualSlotCreateRejectsEveryForeignReference(String field) throws Exception {
        var payload = slotPayload(); payload.put(field, foreignId(field));
        long count = slots.count();
        response(post(BASE + "/slots").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload)), 404);
        assertThat(slots.count()).isEqualTo(count);
    }

    @ParameterizedTest
    @ValueSource(strings = {"periodId", "forClassId", "forCourseId", "teacherId", "roomId"})
    void manualSlotUpdateRejectsForeignReferencesWithoutChangingExistingSlot(String field) throws Exception {
        var payload = slotPayload(); payload.put(field, foreignId(field));
        response(put(BASE + "/slots/{id}", ownSlot.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(payload)), 404);
        assertThat(ownSlot.getPeriod().getId()).isEqualTo(ownPeriod.getId());
        assertThat(ownSlot.getForClass().getId()).isEqualTo(ownClass.getId());
        assertThat(ownSlot.getForCourse().getId()).isEqualTo(ownCourse.getId());
        assertThat(ownSlot.getTeacher().getId()).isEqualTo(teacher.getId());
        assertThat(ownSlot.getRoom().getId()).isEqualTo(ownRoom.getId());
    }

    @Test
    void foreignSlotUpdateAndDeleteReturnNotFound() throws Exception {
        response(put(BASE + "/slots/{id}", foreignSlot.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(slotPayload())), 404);
        response(delete(BASE + "/slots/{id}", foreignSlot.getId()), 404);
        assertThat(slots.existsById(foreignSlot.getId())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"periodId", "forClassId", "forCourseId", "teacherId", "roomId"})
    void rawEntitySaveRejectsForeignReferencesBeforeDeletingExistingSlots(String field) throws Exception {
        String entityField = Map.of("periodId", "period", "forClassId", "forClass", "forCourseId", "forCourse",
                "teacherId", "teacher", "roomId", "room").get(field);
        var payload = new java.util.HashMap<String, Object>();
        payload.put("dayOfWeek", "MONDAY"); payload.put("period", Map.of("id", ownPeriod.getId()));
        payload.put(entityField, Map.of("id", foreignId(field)));
        long count = slots.count();
        response(put(BASE + "/class/{id}/slots", ownClass.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(List.of(payload))), 404);
        assertThat(slots.count()).isEqualTo(count);
        assertThat(slots.existsById(ownSlot.getId())).isTrue();
    }

    @Test
    void rawSaveCanonicalizesForgedFieldsAndNormalizesClassWithoutRetargetingResources() throws Exception {
        var payload = Map.of("id", ownSlot.getId(), "dayOfWeek", "TUESDAY",
                "period", Map.of("id", ownPeriod.getId(), "index", 99),
                "forCourse", Map.of("id", ownCourse.getId(), "name", "Forged course"),
                "teacher", Map.of("id", teacher.getId(), "firstName", "Forged teacher"),
                "room", Map.of("id", ownRoom.getId(), "name", "Forged room"));
        Long slotId = ownSlot.getId();
        response(put(BASE + "/class/{id}/slots", ownClass.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(List.of(payload))), 200);
        em.flush(); em.clear();
        TimetableSlot saved = slots.findById(slotId).orElseThrow();
        assertThat(saved.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(saved.getForClass().getId()).isEqualTo(ownClass.getId());
        assertThat(saved.getForCourse().getName()).isEqualTo(ownCourse.getName());
        assertThat(saved.getTeacher().getFirstName()).isEqualTo("Schedule");
        assertThat(saved.getRoom().getName()).isEqualTo(ownRoom.getName());
        assertThat(saved.getPeriod().getIndex()).isEqualTo(1);
        assertThat(saved.getTimetable().getSchool().getId()).isEqualTo(ownSchool.getId());
    }

    @Test
    void rawSaveRejectsOtherSameSchoolClassAndForeignSlotId() throws Exception {
        ClassEntity other = clazz(ownSchool, "Other current class"); em.flush();
        var payload = Map.of("dayOfWeek", "MONDAY", "period", Map.of("id", ownPeriod.getId()), "forClass", Map.of("id", other.getId()));
        response(put(BASE + "/class/{id}/slots", ownClass.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(List.of(payload))), 409);
        var foreignIdentity = Map.of("id", foreignSlot.getId(), "dayOfWeek", "MONDAY", "period", Map.of("id", ownPeriod.getId()));
        response(put(BASE + "/class/{id}/slots", ownClass.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(List.of(foreignIdentity))), 404);
    }

    @Test
    void createAndInternalUpdateValidateEveryCollectionResource() {
        for (String kind : List.of("class", "teacher", "room")) {
            var classes = Set.of(kind.equals("class") ? foreignClass.getId() : ownClass.getId());
            var teachers = Set.of(kind.equals("teacher") ? foreignTeacher.getId() : teacher.getId());
            var rooms = Set.of(kind.equals("room") ? foreignRoom.getId() : ownRoom.getId());
            long count = timetables.count();
            assertThatThrownBy(() -> service.create(new CreateTimetableRequest("New", null, "2030-2031", "Fall", classes, teachers, rooms)))
                    .isInstanceOf(ResourceNotFoundException.class);
            assertThat(timetables.count()).isEqualTo(count);
            assertThatThrownBy(() -> service.update(ownTimetable.getId(), new UpdateTimetableRequest("Changed", null, "2030-2031", "Fall", classes, teachers, rooms)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Test
    void autoCreatedTimetableUsesClassYearAndOnlyCurrentSchoolResourcePools() throws Exception {
        ClassEntity unscheduled = clazz(ownSchool, "Unscheduled"); em.flush();
        var payload = Map.of("dayOfWeek", "MONDAY", "period", Map.of("id", ownPeriod.getId()), "forCourse", Map.of("id", ownCourse.getId()));
        response(put(BASE + "/class/{id}/slots", unscheduled.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(List.of(payload))), 200);
        Timetable created = timetables.findByClassId(unscheduled.getId()).get(0);
        assertThat(created.getAcademicYear()).isEqualTo(unscheduled.getAcademicYear().getName());
        assertThat(created.getRooms()).extracting(Room::getId).doesNotContain(foreignRoom.getId());
        assertThat(created.getTeachers()).extracting(Teacher::getId).doesNotContain(foreignTeacher.getId());
        assertThat(em.createNativeQuery("select school_id from timetables where id = :id").setParameter("id", created.getId()).getSingleResult())
                .isEqualTo(ownSchool.getId());
    }

    @Test
    void autoCreationDoesNotSelectForeignTimetableIncorrectlyLinkedToCurrentClass() throws Exception {
        ClassEntity unscheduled = clazz(ownSchool, "Unscheduled");
        foreignTimetable.getClasses().add(unscheduled); em.flush();
        var payload = Map.of("dayOfWeek", "MONDAY", "period", Map.of("id", ownPeriod.getId()));
        response(put(BASE + "/class/{id}/slots", unscheduled.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(List.of(payload))), 200);
        Timetable own = timetables.findBySchoolIdAndClassId(ownSchool.getId(), unscheduled.getId()).get(0);
        assertThat(own.getId()).isNotEqualTo(foreignTimetable.getId());
        assertThat(service.getSlotsByClassId(unscheduled.getId())).allSatisfy(slot ->
                assertThat(slot.getTimetable().getId()).isEqualTo(own.getId()));
    }

    @Test
    void optimizationRejectsForeignClassAndCorruptCourseContext() throws Exception {
        response(post(BASE + "/class/{id}/optimize", foreignClass.getId()), 404);
        ownClass.getCourses().add(foreignCourse); em.flush();
        response(post(BASE + "/class/{id}/optimize", ownClass.getId()), 409);
        assertThat(slots.existsById(ownSlot.getId())).isTrue();
    }

    @Test
    void newTimetableIgnoresBodySchoolAndScopesServerOwnedSchool() throws Exception {
        long count = timetables.count();
        response(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                "name", "Server owned", "academicYear", "2030-2031", "semester", "Fall",
                "schoolId", foreignSchool.getId(), "classIds", List.of(ownClass.getId())))), 201);
        assertThat(timetables.count()).isEqualTo(count + 1);
        assertThat(em.createNativeQuery("select school_id from timetables where name = 'Server owned'").getSingleResult())
                .isEqualTo(ownSchool.getId());
    }

    @Test
    void optimizationRejectsForeignNestedCourseTeacherBeforeCreatingTimetable() throws Exception {
        ClassEntity unscheduled = clazz(ownSchool, "Invalid context");
        Course course = course(ownSchool); course.setTeacher(foreignTeacher);
        unscheduled.getCourses().add(course); em.flush();
        long timetableCount = timetables.count(), slotCount = slots.count();
        response(post(BASE + "/class/{id}/optimize", unscheduled.getId()), 409);
        assertThat(timetables.count()).isEqualTo(timetableCount);
        assertThat(slots.count()).isEqualTo(slotCount);
    }

    @Test
    void optimizationCandidatePoolsCannotScheduleForeignResources() throws Exception {
        response(post(BASE + "/class/{id}/optimize", ownClass.getId()), 200);
        em.flush();
        em.clear();
        var result = service.getSlotsByClassId(ownClass.getId());
        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(slot -> {
            assertThat(slot.getPeriod().getId()).isEqualTo(ownPeriod.getId());
            assertThat(slot.getRoom().getId()).isEqualTo(ownRoom.getId());
            assertThat(slot.getTeacher().getId()).isEqualTo(teacher.getId());
            assertThat(slot.getForCourse().getId()).isEqualTo(ownCourse.getId());
        });
    }

    @Test
    void retainedInternalSlotMethodsRejectForeignParentAndForeignSlotIdentity() {
        TimetableSlot incoming = new TimetableSlot(); incoming.setDayOfWeek(DayOfWeek.MONDAY);
        incoming.setPeriod(ownPeriod); incoming.setTimetable(foreignTimetable);
        assertThatThrownBy(() -> service.createSlot(incoming)).isInstanceOf(ResourceNotFoundException.class);
        incoming.setTimetable(ownTimetable); incoming.setPeriod(foreignPeriod);
        assertThatThrownBy(() -> service.createSlot(incoming)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateSlot(foreignSlot.getId(), incoming)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.deleteSlot(foreignSlot.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    private Long foreignId(String field) { return switch (field) {
        case "periodId" -> foreignPeriod.getId(); case "forClassId" -> foreignClass.getId();
        case "forCourseId" -> foreignCourse.getId(); case "teacherId" -> foreignTeacher.getId(); default -> foreignRoom.getId();
    }; }
    private java.util.HashMap<String,Object> slotPayload() { return new java.util.HashMap<>(Map.of(
            "dayOfWeek", "MONDAY", "periodId", ownPeriod.getId(), "forClassId", ownClass.getId(),
            "forCourseId", ownCourse.getId(), "teacherId", teacher.getId(), "roomId", ownRoom.getId())); }
    private JsonNode response(MockHttpServletRequestBuilder request, int expected) throws Exception {
        var result = mvc.perform(request.with(user(DevFixtureLoader.ADMIN_EMAIL).roles("ADMIN")))
                .andExpect(status().is(expected)).andReturn().getResponse();
        if (result.getContentType() != null && result.getContentType().contains("pdf")) return json.nullNode();
        return json.readTree(result.getContentAsString()).path("data");
    }
    private School school(String name) { School s = new School(); s.setName(name); em.persist(s); return s; }
    private ClassEntity clazz(School school, String name) {
        AcademicYear y = new AcademicYear(); y.setSchool(school); y.setName(name + " year");
        y.setStartDate(LocalDate.of(2030,9,1)); y.setEndDate(LocalDate.of(2031,6,30)); em.persist(y);
        ClassEntity c = new ClassEntity(); c.setName(name); c.setAcademicYear(y); em.persist(c); return c;
    }
    private Course course(School school) { Course c = new Course(); c.setSchool(school); c.setName("Course " + school.getName()); c.setCode(UUID.randomUUID().toString().substring(0, 8)); em.persist(c); return c; }
    private Teacher teacher() { Teacher t = new Teacher(); t.setEmail(UUID.randomUUID() + "@schedule.test"); t.setFirstName("Schedule"); t.setLastName("Teacher"); t.setRole(UserRole.TEACHER); t.setPassword("unused"); t.setStatus(Status.ACTIVE); t.setIsEmailVerified(true); em.persist(t); return t; }
    private void membership(Teacher teacher, School school) { SchoolMembership m = new SchoolMembership(); m.setSchool(school); m.setUser(teacher); m.setStatus(MembershipStatus.ACTIVE); m.setRoles(Set.of(MembershipRole.TEACHER)); em.persist(m); }
    private Period period(School school) { Period p = new Period(); p.setSchool(school); p.setIndex(1); p.setStartTime(LocalTime.of(8,0)); p.setEndTime(LocalTime.of(9,0)); em.persist(p); return p; }
    private Room room(School school) { Room r = new Room(); r.setSchool(school); r.setName("Room " + school.getName()); r.setCapacity(30); r.setRoomType(RoomType.CLASSROOM); em.persist(r); return r; }
    private Timetable timetable(School school, ClassEntity clazz, Room room) {
        Timetable t = new Timetable(); t.setName("Matching timetable"); t.setAcademicYear("2030-2031"); t.setSemester("Fall");
        t.getClasses().add(clazz); t.getRooms().add(room); t.getTeachers().add(teacher);
        t.setSchool(school);
        em.persist(t);
        return t;
    }
    private TimetableSlot slot(Timetable timetable, ClassEntity clazz, Course course, Period period, Room room) {
        TimetableSlot s = new TimetableSlot(); s.setTimetable(timetable); s.setForClass(clazz); s.setForCourse(course);
        s.setPeriod(period); s.setRoom(room); s.setTeacher(teacher); s.setDayOfWeek(DayOfWeek.MONDAY); em.persist(s); return s;
    }
}
