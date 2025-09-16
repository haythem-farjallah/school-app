package com.example.school_management.feature.operational.domain;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.List;

@PlanningSolution
@ConditionalOnProperty(name = "optaplanner.enabled", havingValue = "true", matchIfMissing = false)
public class TimetableSolution {

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "dayRange")
    private List<DayOfWeek> days;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "periodRange")
    private List<Period> periods;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private List<Room> rooms;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "teacherRange")
    private List<Teacher> teachers;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "classRange")
    private List<ClassEntity> classes;

    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "courseRange")
    private List<Course> courses;

    @PlanningEntityCollectionProperty
    private List<TimetableLesson> lessons;

    @PlanningScore
    private HardSoftScore score;

    // Constructors
    public TimetableSolution() {
    }

    public TimetableSolution(List<DayOfWeek> days, List<Period> periods, List<Room> rooms,
                           List<Teacher> teachers, List<ClassEntity> classes, List<Course> courses,
                           List<TimetableLesson> lessons) {
        this.days = days;
        this.periods = periods;
        this.rooms = rooms;
        this.teachers = teachers;
        this.classes = classes;
        this.courses = courses;
        this.lessons = lessons;
    }

    // Getters and Setters
    public List<DayOfWeek> getDays() {
        return days;
    }

    public void setDays(List<DayOfWeek> days) {
        this.days = days;
    }

    public List<Period> getPeriods() {
        return periods;
    }

    public void setPeriods(List<Period> periods) {
        this.periods = periods;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public List<Teacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<Teacher> teachers) {
        this.teachers = teachers;
    }

    public List<ClassEntity> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassEntity> classes) {
        this.classes = classes;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public List<TimetableLesson> getLessons() {
        return lessons;
    }

    public void setLessons(List<TimetableLesson> lessons) {
        this.lessons = lessons;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }
} 