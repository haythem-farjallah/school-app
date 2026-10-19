package com.example.school_management.dev;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.academic.entity.AcademicYear;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.EnrollmentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
@ActiveProfiles({"test", "fixtures", "dev"})
@TestPropertySource(properties = "school.dev.timetable-fixtures.enabled=true")
class DevTimetableFixtureLoaderIntegrationTest {
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ApplicationContext context;
    @Autowired StudentRepository students;

    @Test
    @Transactional
    void servesFourRealPersistedLessonsAndAnEmptyStudent() throws Exception {
        var rows = em.createQuery("select s from TimetableSlot s where s.forClass.name = 'Demo 7A' order by s.id", TimetableSlot.class)
                .getResultList();
        assertThat(rows).hasSize(4);
        JsonNode slots = timetable(DevFixtureLoader.STUDENT_EMAIL).path("slots");
        assertThat(slots.size()).isEqualTo(4);
        assertThat(rows).extracting(s -> s.getDayOfWeek().name() + " " + s.getPeriod().getStartTime()
                + " " + s.getForCourse().getName() + " " + s.getRoom().getName()).containsExactlyInAnyOrder(
                "MONDAY 08:15 Physics Demo Lab 201", "MONDAY 09:15 Mathematics Demo Room 101",
                "SATURDAY 09:15 Art Demo Room 101", "SUNDAY 09:15 History Demo Room 101");
        for (TimetableSlot row : rows) {
            JsonNode slot = StreamSupport.stream(slots.spliterator(), false)
                    .filter(node -> node.path("id").asLong() == row.getId()).findFirst().orElseThrow();
            assertThat(slot.path("forClassId").asLong()).isEqualTo(row.getForClass().getId());
            assertThat(slot.path("forClassName").asText()).isEqualTo("Demo 7A");
            assertThat(slot.path("forCourseId").asLong()).isEqualTo(row.getForCourse().getId());
            assertThat(slot.path("forCourseName").asText()).isEqualTo(row.getForCourse().getName());
            assertThat(slot.path("dayOfWeek").asText()).isEqualTo(row.getDayOfWeek().name());
            assertThat(slot.path("roomId").asLong()).isEqualTo(row.getRoom().getId());
            assertThat(slot.path("roomName").asText()).isEqualTo(row.getRoom().getName());
            assertThat(slot.path("teacherId").asLong()).isEqualTo(row.getTeacher().getId());
            assertThat(slot.path("teacherFirstName").asText()).isEqualTo("Theo");
            assertThat(slot.path("teacherLastName").asText()).isEqualTo("Teacher");
            assertThat(slot.path("periodId").asLong()).isEqualTo(row.getPeriod().getId());
            assertThat(slot.path("periodIndex").asInt()).isEqualTo(row.getPeriod().getIndex());
            assertThat(slot.path("periodStartTime").asText()).isEqualTo(row.getPeriod().getStartTime().toString() + ":00");
            assertThat(slot.path("periodEndTime").asText()).isEqualTo(row.getPeriod().getEndTime().toString() + ":00");
        }
        assertThat(jdbc.queryForObject("select count(*) from teaching_assignments ta join classes c on c.id=ta.class_id where c.name='Demo 7A'", Long.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("""
                select count(*) from timetable_slots s
                join teaching_assignments ta on ta.class_id=s.for_class_id and ta.course_id=s.for_course_id and ta.teacher_id=s.teacher_id
                join classes c on c.id=s.for_class_id where c.name='Demo 7A'
                """, Long.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from timetable_classes tc join timetables t on t.id=tc.timetable_id where t.name='Development timetable demo'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from timetable_teachers tt join timetables t on t.id=tt.timetable_id where t.name='Development timetable demo'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from timetable_rooms tr join timetables t on t.id=tr.timetable_id where t.name='Development timetable demo'", Long.class)).isEqualTo(2);
        assertThat(timetable("empty.student@fixtures.school.test").path("slots").size()).isZero();
    }

    @Test
    void repeatedRunsAcrossTransactionsPreserveCountsAndIds() throws Exception {
        var before = snapshot();
        var relationshipsBefore = relationshipSnapshot();
        runLoader();
        runLoader();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(relationshipSnapshot()).isEqualTo(relationshipsBefore);
    }

    @Test
    @Transactional
    void preservesEditedAndDeletedDemoRecords() throws Exception {
        var rows = em.createQuery("select s from TimetableSlot s where s.forClass.name='Demo 7A' order by s.id", TimetableSlot.class).getResultList();
        rows.get(0).setDescription("Local edit");
        Long removed = rows.get(1).getId();
        em.remove(rows.get(1));
        em.flush();
        runLoader();
        em.flush();
        em.clear();
        assertThat(em.find(TimetableSlot.class, rows.get(0).getId()).getDescription()).isEqualTo("Local edit");
        assertThat(em.find(TimetableSlot.class, removed)).isNull();
        assertThat(timetable(DevFixtureLoader.STUDENT_EMAIL).path("slots").size()).isEqualTo(3);
    }

    @Test
    @Transactional
    void createsDemoInExistingActiveYearWithoutChangingOtherYears() throws Exception {
        removeDemo();
        AcademicYear active = em.createQuery("select y from AcademicYear y where y.active=true", AcademicYear.class).getSingleResult();
        active.setName("Existing active year");
        AcademicYear inactive = new AcademicYear();
        inactive.setSchool(active.getSchool()); inactive.setName("Existing archived year");
        inactive.setStartDate(LocalDate.of(2018, 9, 1)); inactive.setEndDate(LocalDate.of(2019, 6, 30));
        inactive.setActive(false); em.persist(inactive); em.flush();
        runLoader();
        ClassEntity clazz = em.createQuery("select c from ClassEntity c where c.name='Demo 7A'", ClassEntity.class).getSingleResult();
        assertThat(clazz.getAcademicYear().getId()).isEqualTo(active.getId());
        assertThat(active.getName()).isEqualTo("Existing active year");
        assertThat(active.isActive()).isTrue(); assertThat(inactive.isActive()).isFalse();
    }

    @Test
    @Transactional
    void skipsWholeDemoWhenStudentAlreadyHasAnEnrollment() throws Exception {
        removeDemo();
        AcademicYear year = em.createQuery("select y from AcademicYear y where y.active=true", AcademicYear.class).getSingleResult();
        ClassEntity clazz = new ClassEntity(); clazz.setName("Existing class"); clazz.setAcademicYear(year); em.persist(clazz);
        Enrollment enrollment = new Enrollment(); enrollment.setStudent(students.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow());
        enrollment.setClassEntity(clazz); enrollment.setStatus(EnrollmentStatus.ACTIVE); em.persist(enrollment); em.flush();
        var before = snapshot();
        runLoader();
        em.flush();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(em.createQuery("select c from ClassEntity c where c.name='Demo 7A'", ClassEntity.class).getResultList()).isEmpty();
        assertThat(enrollment.getClassEntity().getId()).isEqualTo(clazz.getId());
    }

    @Test
    @Transactional
    void reservedPeriodIsPreservedAndPreventsPartialDemoCreation() throws Exception {
        removeDemo();
        jdbc.update("insert into periods(school_id,index_number,start_time,end_time) select id,101,'12:00','12:45' from schools");
        var before = snapshot();
        runLoader();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(jdbc.queryForObject("select start_time::text from periods where index_number=101", String.class)).isEqualTo("12:00:00");
    }

    @Test
    @Transactional
    void classNameCollisionUsesTheSameCaseInsensitiveIdentityAsPersistence() throws Exception {
        removeDemo();
        var year = em.createQuery("select y from AcademicYear y where y.active=true", AcademicYear.class).getSingleResult();
        ClassEntity existing = new ClassEntity(); existing.setAcademicYear(year); existing.setName("demo 7a");
        em.persist(existing); em.flush();
        var before = snapshot();
        runLoader();
        em.flush();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(existing.getName()).isEqualTo("demo 7a");
    }

    @Test
    @Transactional
    void createsAnActiveYearOnlyWhenNoneExists() throws Exception {
        removeDemo();
        jdbc.update("delete from academic_years where name='Development demo year'");
        runLoader();
        assertThat(jdbc.queryForObject("select count(*) from academic_years where active=true", Long.class)).isEqualTo(1);
        assertThat(timetable(DevFixtureLoader.STUDENT_EMAIL).path("slots").size()).isEqualTo(4);
    }

    @Test
    @Transactional
    void leavesExistingEmptyAccountCredentialsStatusAndMembershipUntouched() throws Exception {
        removeDemo();
        var empty = students.findByEmail("empty.student@fixtures.school.test").orElseThrow();
        String password = empty.getPassword();
        empty.setStatus(Status.SUSPENDED);
        jdbc.update("update school_memberships set status='SUSPENDED' where user_id=?", empty.getId());
        em.flush();
        runLoader();
        em.flush(); em.clear();
        var reloaded = students.findById(empty.getId()).orElseThrow();
        assertThat(reloaded.getPassword()).isEqualTo(password);
        assertThat(reloaded.getStatus()).isEqualTo(Status.SUSPENDED);
        assertThat(jdbc.queryForObject("select status from school_memberships where user_id=?", String.class, empty.getId())).isEqualTo("SUSPENDED");
    }

    @Test
    @Transactional
    void changedFixtureAccountPreventsSeedingWithoutResettingIt() throws Exception {
        removeDemo();
        var student = students.findByEmail(DevFixtureLoader.STUDENT_EMAIL).orElseThrow();
        student.setFirstName("Local name"); em.flush();
        var before = snapshot();
        runLoader();
        em.flush(); em.clear();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(students.findById(student.getId()).orElseThrow().getFirstName()).isEqualTo("Local name");
    }

    @Test
    @Transactional
    void reservedInactiveYearIsNeverReactivated() throws Exception {
        removeDemo();
        jdbc.update("update academic_years set active=false where name='Development demo year'");
        var before = snapshot();
        runLoader();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from academic_years where active=true", Long.class)).isZero();
    }

    private JsonNode timetable(String email) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/timetables/me").with(user(email).roles("STUDENT")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
    }

    private void runLoader() throws Exception {
        context.getBean(DevTimetableFixtureLoader.class).run(new DefaultApplicationArguments());
    }

    private List<List<Long>> snapshot() {
        return List.of("academic_years", "classes", "courses", "rooms", "periods", "enrollments", "teaching_assignments", "timetables", "timetable_slots", "users", "school_memberships")
                .stream().map(table -> jdbc.queryForList("select id from " + table + " order by id", Long.class)).toList();
    }

    private List<List<Map<String, Object>>> relationshipSnapshot() {
        return List.of("timetable_classes", "timetable_teachers", "timetable_rooms").stream()
                .map(table -> jdbc.queryForList("select * from " + table + " order by 1,2")).toList();
    }

    private void removeDemo() {
        jdbc.update("delete from timetable_slots where for_class_id in (select id from classes where name='Demo 7A')");
        jdbc.update("delete from timetable_classes where class_id in (select id from classes where name='Demo 7A')");
        jdbc.update("delete from timetable_teachers where timetable_id in (select id from timetables where name='Development timetable demo')");
        jdbc.update("delete from timetable_rooms where timetable_id in (select id from timetables where name='Development timetable demo')");
        jdbc.update("delete from timetables where name='Development timetable demo'");
        jdbc.update("delete from teaching_assignments where class_id in (select id from classes where name='Demo 7A')");
        jdbc.update("delete from enrollments where class_id in (select id from classes where name='Demo 7A')");
        jdbc.update("delete from classes where name='Demo 7A'");
        jdbc.update("delete from courses where code in ('DEMO-MATH','DEMO-PHYS','DEMO-ART','DEMO-HIST')");
        jdbc.update("delete from rooms where name in ('Demo Room 101','Demo Lab 201')");
        jdbc.update("delete from periods where index_number in (101,102)");
        em.clear();
    }
}
