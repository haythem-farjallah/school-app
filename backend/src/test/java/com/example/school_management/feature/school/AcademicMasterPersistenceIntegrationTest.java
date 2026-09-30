package com.example.school_management.feature.school;

import com.example.school_management.IntegrationTest;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.enums.RoomType;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.school.entity.School;
import com.example.school_management.feature.school.repository.SchoolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class AcademicMasterPersistenceIntegrationTest {
    @Autowired
    SchoolRepository schools;

    @Autowired
    CourseRepository courses;

    @Autowired
    RoomRepository rooms;

    @Autowired
    PeriodRepository periods;

    @Autowired
    EntityManager em;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void coursesPersistAndLookupsUseSchoolOwnership() {
        School first = school();
        School second = school();
        Course a = courses.save(course(first));
        Course b = courses.saveAndFlush(course(second));
        em.clear();

        Course reloaded = courses.findById(a.getId()).orElseThrow();
        assertThat(Hibernate.isInitialized(reloaded.getSchool())).isFalse();
        assertThat(reloaded.getSchool().getId()).isEqualTo(first.getId());
        assertThat(courses.findById(b.getId()).orElseThrow().getSchool().getId()).isEqualTo(second.getId());
        assertThat(courses.existsBySchoolIdAndCode(first.getId(), "MATH101")).isTrue();
        assertThat(courses.existsBySchoolIdAndCode(school().getId(), "MATH101")).isFalse();
        assertThat(courses.existsBySchoolIdAndNameIgnoreCase(first.getId(), "mathematics")).isTrue();
        assertThat(courses.existsBySchoolIdAndNameIgnoreCase(school().getId(), "mathematics")).isFalse();
    }

    @Test
    void periodsPersistAndIndexLookupUsesSchoolOwnership() {
        School first = school();
        School second = school();
        Period a = periods.save(period(first));
        Period b = periods.saveAndFlush(period(second));
        em.clear();

        Period reloaded = periods.findById(a.getId()).orElseThrow();
        assertThat(Hibernate.isInitialized(reloaded.getSchool())).isFalse();
        assertThat(reloaded.getSchool().getId()).isEqualTo(first.getId());
        assertThat(periods.findBySchoolIdAndIndex(first.getId(), 1)).map(Period::getId).contains(a.getId());
        assertThat(periods.findBySchoolIdAndIndex(second.getId(), 1)).map(Period::getId).contains(b.getId());
        assertThat(periods.findBySchoolIdAndIndex(school().getId(), 1)).isEmpty();
    }

    @Test
    void roomsPersistIndependentlyAndKeepOwnershipOutOfDetachedJson() throws Exception {
        School first = school();
        School second = school();
        Room a = rooms.save(room(first));
        Room b = rooms.saveAndFlush(room(second));
        em.clear();

        Room reloaded = rooms.findById(a.getId()).orElseThrow();
        assertThat(Hibernate.isInitialized(reloaded.getSchool())).isFalse();
        assertThat(reloaded.getSchool().getId()).isEqualTo(first.getId());
        assertThat(rooms.findById(b.getId()).orElseThrow().getSchool().getId()).isEqualTo(second.getId());
        em.clear();

        var json = objectMapper.readTree(objectMapper.writeValueAsString(reloaded));
        assertThat(json.has("school")).isFalse();
        assertThat(json.has("schoolId")).isFalse();
        assertThat(json.path("name").asText()).isEqualTo("Room A");
        assertThat(Hibernate.isInitialized(reloaded.getSchool())).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"course", "room", "period"})
    void jpaRejectsMissingSchool(String type) {
        assertThatThrownBy(() -> {
            Object entity = switch (type) {
                case "course" -> course(null);
                case "room" -> room(null);
                default -> period(null);
            };
            em.persist(entity);
            em.flush();
        }).isInstanceOf(RuntimeException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"course", "room", "period"})
    void removingMasterDoesNotCascadeToItsSchool(String type) {
        School school = school();
        Object entity = switch (type) {
            case "course" -> course(school);
            case "room" -> room(school);
            default -> period(school);
        };
        em.persist(entity);
        em.flush();
        em.remove(entity);
        em.flush();
        em.clear();
        assertThat(schools.findById(school.getId())).isPresent();
    }

    private School school() {
        School school = new School();
        school.setName("Test School");
        return schools.save(school);
    }

    private Course course(School school) {
        Course course = new Course();
        course.setSchool(school);
        course.setName("Mathematics");
        course.setCode("MATH101");
        return course;
    }

    private Room room(School school) {
        Room room = new Room();
        room.setSchool(school);
        room.setName("Room A");
        room.setRoomType(RoomType.CLASSROOM);
        return room;
    }

    private Period period(School school) {
        Period period = new Period();
        period.setSchool(school);
        period.setIndex(1);
        period.setStartTime(LocalTime.of(8, 0));
        period.setEndTime(LocalTime.of(8, 50));
        return period;
    }
}
