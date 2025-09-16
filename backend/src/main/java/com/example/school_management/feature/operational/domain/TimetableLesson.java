package com.example.school_management.feature.operational.domain;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@PlanningEntity
@ConditionalOnProperty(name = "optaplanner.enabled", havingValue = "true", matchIfMissing = false)
public class TimetableLesson {

    @PlanningId
    private Long id;
    private String subject;
    private Integer weeklyHours;

    // Planning variables - these will be assigned by OptaPlanner
    @PlanningVariable(valueRangeProviderRefs = {"dayRange"})
    private DayOfWeek day;

    @PlanningVariable(valueRangeProviderRefs = {"periodRange"})
    private Period period;

    @PlanningVariable(valueRangeProviderRefs = {"roomRange"})
    private Room room;

    @PlanningVariable(valueRangeProviderRefs = {"teacherRange"})
    private Teacher teacher;

    @PlanningVariable(valueRangeProviderRefs = {"classRange"})
    private ClassEntity classEntity;

    @PlanningVariable(valueRangeProviderRefs = {"courseRange"})
    private Course course;

    // Constructors
    public TimetableLesson() {
    }

    public TimetableLesson(Long id, String subject, Integer weeklyHours) {
        this.id = id;
        this.subject = subject;
        this.weeklyHours = weeklyHours;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(Integer weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public ClassEntity getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    // Helper methods
    public boolean isAssigned() {
        return day != null && period != null && room != null && 
               teacher != null && classEntity != null && course != null;
    }

    public String getSlotInfo() {
        if (!isAssigned()) {
            return "Unassigned";
        }
        return String.format("%s - Period %d - %s - %s - %s", 
            day, 
            period.getIndex(),
            classEntity.getName(),
            course.getName(),
            teacher.getFirstName() + " " + teacher.getLastName()
        );
    }
} 