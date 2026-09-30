package com.example.school_management.feature.operational;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import com.example.school_management.dev.DevFixtureLoader;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.auth.entity.Staff;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StaffRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.dto.TeacherAttendanceRequest;
import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.TeacherAttendance;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.example.school_management.feature.operational.repository.AttendanceRepository;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.repository.TeacherAttendanceRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teachers keep the slot attendance workflow for their own slots only; broad attendance
 * reads and writes are administrative, and teacher-id routes are self-only. The fixture
 * teacher is teacher A and teaches Monday slot A; teacher B teaches Monday slot B. Both
 * slots are for a class whose only student is the fixture student; student B is outside it.
 */
@IntegrationTest
class AttendanceQuarantineIntegrationTest {

    private static final LocalDate MONDAY = LocalDate.of(2030, 1, 7);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final String SCHEDULED_DAY_ONLY = "Attendance for this slot can only be taken on its scheduled day";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    ClassRepository classRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    PeriodRepository periodRepository;

    @Autowired
    CurrentSchoolResolver currentSchool;

    @Autowired
    TimetableSlotRepository slotRepository;

    @Autowired
    AttendanceRepository attendanceRepository;

    @Autowired
    TeacherAttendanceRepository teacherAttendanceRepository;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    StaffRepository staffRepository;

    private static final AtomicInteger clientAddress = new AtomicInteger();

    private Student studentA;
    private Student studentB;
    private Teacher teacherA;
    private Teacher teacherB;
    private ClassEntity schoolClass;
    private Course course;
    private Period period;
    private TimetableSlot slotA;
    private TimetableSlot slotB;
    private TeacherAttendance hrRecordA;
    private TeacherAttendance hrRecordB;

    @BeforeEach
    void scheduleTwoTeachers() {
        studentA = studentRepository.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        teacherA = teacherRepository.findByEmail(DevFixtureLoader.TEACHER_EMAIL).orElseThrow();

        Student s = new Student();
        s.setRole(UserRole.STUDENT);
        s.setEmail("student-b-" + UUID.randomUUID() + "@fixtures.school.test");
        s.setFirstName("Bea");
        s.setLastName("Outside");
        s.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        s.setStatus(Status.ACTIVE);
        studentB = studentRepository.save(s);

        Teacher t = new Teacher();
        t.setRole(UserRole.TEACHER);
        t.setEmail("teacher-b-" + UUID.randomUUID() + "@fixtures.school.test");
        t.setFirstName("Tina");
        t.setLastName("Other");
        t.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        t.setStatus(Status.ACTIVE);
        t.setIsEmailVerified(true);
        teacherB = teacherRepository.save(t);

        ClassEntity c = new ClassEntity();
        c.setName("Attendance " + UUID.randomUUID());
        c.getStudents().add(studentA);
        schoolClass = classRepository.save(c);

        Course k = new Course();
        k.setSchool(currentSchool.resolve());
        k.setName("Attendance course");
        k.setCode("AT-" + UUID.randomUUID().toString().substring(0, 8));
        course = courseRepository.save(k);

        Period p = new Period();
        p.setSchool(currentSchool.resolve());
        p.setIndex(99);
        p.setStartTime(LocalTime.of(8, 0));
        p.setEndTime(LocalTime.of(9, 0));
        period = periodRepository.save(p);

        slotA = slotRepository.save(mondaySlot(teacherA));
        slotB = slotRepository.save(mondaySlot(teacherB));

        hrRecordA = teacherAttendanceRepository.save(hrRecord(teacherA));
        hrRecordB = teacherAttendanceRepository.save(hrRecord(teacherB));
    }

    @AfterEach
    void removeSchedule() {
        transaction.executeWithoutResult(status -> attendanceRepository.deleteAll(attendanceRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(studentB.getId())
                        || (a.getClassEntity() != null && a.getClassEntity().getId().equals(schoolClass.getId())))
                .toList()));
        teacherAttendanceRepository.deleteAllById(List.of(hrRecordA.getId(), hrRecordB.getId()));
        slotRepository.deleteAllById(List.of(slotA.getId(), slotB.getId()));
        periodRepository.deleteById(period.getId());
        classRepository.deleteById(schoolClass.getId());
        courseRepository.deleteById(course.getId());
        studentRepository.deleteById(studentB.getId());
        teacherRepository.deleteById(teacherB.getId());
    }

    @Test
    void teacherIdAttendanceRoutesAreSelfOnly() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        for (long teacherId : List.of(teacherA.getId(), teacherB.getId())) {
            List<MockHttpServletRequestBuilder> routes = List.of(
                    get("/api/v1/attendance/teacher/{id}/today", teacherId).param("date", MONDAY.toString()),
                    get("/api/v1/attendance/teacher/{id}/absent-students", teacherId).param("date", MONDAY.toString()),
                    get("/api/v1/attendance/teacher/{id}/weekly-summary", teacherId).param("startOfWeek", MONDAY.toString()),
                    get("/api/v1/attendance/teacher/{id}/can-mark/{slot}", teacherId, slotA.getId()).param("date", MONDAY.toString()),
                    get("/api/v1/attendance/user/{id}", teacherId).params(dateRange()),
                    get("/api/v1/attendance/statistics/user/{id}", teacherId).params(dateRange()),
                    get("/api/v1/teacher-attendance/statistics/{id}", teacherId),
                    get("/api/v1/teacher-attendance").param("teacherId", String.valueOf(teacherId)));
            for (MockHttpServletRequestBuilder route : routes) {
                ResultActions result = mockMvc.perform(route.header(HttpHeaders.AUTHORIZATION, teacher));
                if (teacherId == teacherA.getId()) {
                    result.andExpect(status().isOk());
                } else {
                    expectForbidden(result, "ACCESS_DENIED");
                }
            }
        }
        expectForbidden(mockMvc.perform(get("/api/v1/attendance/teacher/{id}/class/{c}/course/{k}", teacherB.getId(), schoolClass.getId(), 1L)
                .header(HttpHeaders.AUTHORIZATION, teacher)), "ACCESS_DENIED");
        expectForbidden(mockMvc.perform(get("/api/v1/attendance/user/{id}", studentA.getId()).params(dateRange())
                .header(HttpHeaders.AUTHORIZATION, teacher)), "ACCESS_DENIED");
    }

    @Test
    void aTeacherWithoutATeacherIdReadsOnlyTheirOwnHrAttendance() throws Exception {
        mockMvc.perform(get("/api/v1/teacher-attendance").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id").value(hasItem(hrRecordA.getId().intValue())))
                .andExpect(jsonPath("$.data[*].teacherId").value(everyItem(is(teacherA.getId().intValue()))));

        mockMvc.perform(get("/api/v1/teacher-attendance").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id").value(hasItem(hrRecordB.getId().intValue())));
    }

    @Test
    void aTeacherReadsTheStudentsOfTheirOwnSlotOnly() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        mockMvc.perform(get("/api/v1/attendance/slot/{id}/students", slotA.getId()).param("date", MONDAY.toString())
                        .header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].userId").value(hasItem(studentA.getId().intValue())));
        expectForbidden(mockMvc.perform(get("/api/v1/attendance/slot/{id}/students", slotB.getId()).param("date", MONDAY.toString())
                .header(HttpHeaders.AUTHORIZATION, teacher)), "You can only take attendance for your own timetable slots");

        mockMvc.perform(get("/api/v1/attendance/slot/{id}/students", slotB.getId()).param("date", MONDAY.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    void aTeacherReadsTheirOwnSlotOnlyOnItsScheduledDay() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        mockMvc.perform(get("/api/v1/attendance/slot/{id}/students", slotA.getId()).param("date", MONDAY.toString())
                        .header(HttpHeaders.AUTHORIZATION, teacher))
                .andExpect(status().isOk());
        expectForbidden(mockMvc.perform(get("/api/v1/attendance/slot/{id}/students", slotA.getId()).param("date", TUESDAY.toString())
                .header(HttpHeaders.AUTHORIZATION, teacher)), SCHEDULED_DAY_ONLY);
        expectForbidden(mockMvc.perform(get("/api/v1/attendance/slot/{id}/students", slotB.getId()).param("date", TUESDAY.toString())
                .header(HttpHeaders.AUTHORIZATION, teacher)), "You can only take attendance for your own timetable slots");

        assertThat(slotAttendance()).isEmpty();
    }

    @Test
    void administratorsAndStaffReadAnySlotOnAnyDay() throws Exception {
        Staff st = new Staff();
        st.setRole(UserRole.STAFF);
        st.setEmail("staff-" + UUID.randomUUID() + "@fixtures.school.test");
        st.setFirstName("Sara");
        st.setLastName("Staff");
        st.setPassword(passwordEncoder.encode(DevFixtureLoader.PASSWORD));
        st.setStatus(Status.ACTIVE);
        st.setIsEmailVerified(true);
        Staff staff = staffRepository.save(st);
        try {
            for (String caller : List.of(DevFixtureLoader.ADMIN_EMAIL, staff.getEmail())) {
                mockMvc.perform(get("/api/v1/attendance/slot/{id}/students", slotB.getId()).param("date", TUESDAY.toString())
                                .header(HttpHeaders.AUTHORIZATION, bearer(caller)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data[*].userId").value(hasItem(studentA.getId().intValue())));
            }
        } finally {
            staffRepository.deleteById(staff.getId());
        }
    }

    @Test
    void aTeacherMarksTheirOwnSlotOnItsDay() throws Exception {
        mockMvc.perform(markSlot(slotA, MONDAY, studentA).header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].userId").value(studentA.getId()))
                .andExpect(jsonPath("$.data[0].status").value("LATE"));

        assertThat(slotAttendance()).singleElement().satisfies(attendance -> {
            assertThat(attendance.getUser().getId()).isEqualTo(studentA.getId());
            assertThat(attendance.getRecordedBy().getId()).isEqualTo(teacherA.getId());
        });
    }

    @Test
    void aTeacherCannotMarkAnotherTeachersSlotOrAnotherDay() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);

        expectForbidden(mockMvc.perform(markSlot(slotB, MONDAY, studentA).header(HttpHeaders.AUTHORIZATION, teacher)),
                "You can only take attendance for your own timetable slots");
        expectForbidden(mockMvc.perform(markSlot(slotA, TUESDAY, studentA).header(HttpHeaders.AUTHORIZATION, teacher)),
                SCHEDULED_DAY_ONLY);

        assertThat(slotAttendance()).isEmpty();
    }

    @Test
    void aTeacherCannotMarkAStudentOutsideTheSlotsClass() throws Exception {
        expectForbidden(mockMvc.perform(markSlot(slotA, MONDAY, studentA, studentB)
                        .header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.TEACHER_EMAIL))),
                "Attendance can only be marked for students of this slot's class");

        assertThat(slotAttendance()).isEmpty();
        assertThat(attendanceRepository.findByUserIdAndDateBetween(studentB.getId(), MONDAY, MONDAY)).isEmpty();
    }

    @Test
    void broadAttendanceRoutesAreAdministrative() throws Exception {
        String teacher = bearer(DevFixtureLoader.TEACHER_EMAIL);
        long classId = schoolClass.getId();
        String record = json(Map.of("userId", studentB.getId(), "classId", classId, "courseId", course.getId(), "date", MONDAY.toString(),
                "status", "PRESENT", "userType", "STUDENT"));
        String records = "[" + record + "]";

        for (MockHttpServletRequestBuilder route : List.of(
                post("/api/v1/attendance").contentType(MediaType.APPLICATION_JSON).content(record),
                post("/api/v1/attendance/batch").contentType(MediaType.APPLICATION_JSON).content(records),
                put("/api/v1/attendance/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(record),
                patch("/api/v1/attendance/{id}/excuse", 1L).param("excuse", "Ill"),
                patch("/api/v1/attendance/{id}/late", 1L).param("remarks", "Bus"),
                post("/api/v1/attendance/class/{id}/mark", classId).param("date", MONDAY.toString())
                        .contentType(MediaType.APPLICATION_JSON).content(records),
                get("/api/v1/attendance/class/{id}", classId).param("date", MONDAY.toString()),
                get("/api/v1/attendance/course/{id}", course.getId()).param("date", MONDAY.toString()),
                get("/api/v1/attendance/statistics"),
                get("/api/v1/attendance/statistics/class/{id}", classId).params(dateRange()),
                get("/api/v1/attendance/type/STUDENT").params(dateRange()),
                get("/api/v1/attendance/filter"),
                get("/api/v1/attendance/class/{id}/students", classId),
                get("/api/v1/attendance/class/{id}/students-simple", classId))) {
            expectForbidden(mockMvc.perform(route.header(HttpHeaders.AUTHORIZATION, teacher)), "ACCESS_DENIED");
        }
        assertThat(attendanceRepository.findByUserIdAndDateBetween(studentB.getId(), MONDAY, MONDAY)).isEmpty();

        // Administrators keep recording attendance for any user.
        mockMvc.perform(post("/api/v1/attendance").header(HttpHeaders.AUTHORIZATION, bearer(DevFixtureLoader.ADMIN_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON).content(record))
                .andExpect(status().isCreated());
        assertThat(attendanceRepository.findByUserIdAndDateBetween(studentB.getId(), MONDAY, MONDAY)).hasSize(1);
    }

    @Test
    void attendanceByUserTypeSortsOnlyByApprovedProperties() throws Exception {
        String admin = bearer(DevFixtureLoader.ADMIN_EMAIL);

        mockMvc.perform(get("/api/v1/attendance/type/STUDENT").params(dateRange()).param("sort", "date,desc")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/attendance/type/STUDENT").params(dateRange()).param("sort", "user.password,asc")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unsupported sort field 'user.password'"));
    }

    private MockHttpServletRequestBuilder markSlot(TimetableSlot slot, LocalDate date, Student... students) throws Exception {
        List<Map<String, Object>> body = Arrays.stream(students)
                .map(student -> Map.<String, Object>of("userId", student.getId(), "timetableSlotId", slot.getId(),
                        "date", date.toString(), "status", "LATE", "userType", "STUDENT"))
                .toList();
        return post("/api/v1/attendance/slot/{id}/mark", slot.getId()).param("date", date.toString())
                .contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    private List<Attendance> slotAttendance() {
        return transaction.execute(status -> attendanceRepository.findAll().stream()
                .filter(a -> a.getTimetableSlot() != null
                        && List.of(slotA.getId(), slotB.getId()).contains(a.getTimetableSlot().getId()))
                .peek(a -> a.getRecordedBy().getId())
                .toList());
    }

    private TimetableSlot mondaySlot(Teacher teacher) {
        TimetableSlot slot = new TimetableSlot();
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setPeriod(period);
        slot.setForClass(schoolClass);
        slot.setForCourse(course);
        slot.setTeacher(teacher);
        return slot;
    }

    private static TeacherAttendance hrRecord(Teacher teacher) {
        TeacherAttendance record = new TeacherAttendance();
        record.setTeacherId(teacher.getId());
        record.setDate(MONDAY);
        record.setStatus(TeacherAttendanceRequest.TeacherAttendanceStatus.PRESENT);
        record.setRecordedById(teacher.getId());
        record.setRecordedByName("Fixture");
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    private static MultiValueMap<String, String> dateRange() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("startDate", "2029-01-01");
        params.add("endDate", "2031-12-31");
        return params;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
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
                            request.setRemoteAddr("10.0.30." + clientAddress.incrementAndGet());
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
