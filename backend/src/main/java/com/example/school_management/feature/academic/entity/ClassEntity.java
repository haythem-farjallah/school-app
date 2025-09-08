package com.example.school_management.feature.academic.entity;

import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "classes")
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String name;
    
    @Column(name = "grade_level")
    private String gradeLevel; // First Year, Second Year, Third Year
    
    @Column(name = "section")
    private String section; // A, B, C, etc.
    
    @Column(name = "academic_year")
    private String academicYear; // 2024-2025
    
    @EqualsAndHashCode.Include
    private Integer yearOfStudy;
    
    @EqualsAndHashCode.Include
    private Integer maxStudents;
    
    @Column(name = "capacity")
    private Integer capacity = 30;
    
    @Column(name = "weekly_hours")
    private Integer weeklyHours = 30; // Total weekly hours for this class/grade

    @ManyToOne
    @JoinColumn(name = "assigned_room_id")
    private Room assignedRoom;

    // Many-to-Many relationship with Student (from userauth module)
    @ManyToMany
    @JoinTable(
            name = "class_students",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    @Fetch(FetchMode.SUBSELECT)
    @JsonIgnore
    private Set<Student> students = new HashSet<>();

    // Many-to-Many relationship with Course
    @ManyToMany
    @JoinTable(
            name = "class_courses",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Fetch(FetchMode.SUBSELECT)
    @JsonIgnore
    private Set<Course> courses = new HashSet<>();

    // Many-to-Many relationship with Teacher
    @ManyToMany
    @JoinTable(
            name = "class_teachers",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    @JsonIgnore
    private Set<Teacher> teachers = new HashSet<>();

    // One-to-One relationship with Schedule
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "schedule_id")
    @JsonIgnore
    private Schedule schedule;

    @OneToMany(mappedBy = "forClass")
    @JsonIgnore
    private Set<TimetableSlot> timetableSlots = new HashSet<>();

    @OneToMany(mappedBy = "classEntity")
    @JsonIgnore
    private Set<Enrollment> enrollments = new HashSet<>();

    @ManyToMany(mappedBy = "targetClasses")
    @JsonIgnore
    private Set<LearningResource> learningResources = new HashSet<>();

    @ManyToMany(mappedBy = "classes")
    @JsonIgnore
    private Set<Timetable> timetables = new HashSet<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    
    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    
    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }
    
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    
    public Integer getWeeklyHours() { return weeklyHours; }
    public void setWeeklyHours(Integer weeklyHours) { this.weeklyHours = weeklyHours; }
    
    public Room getAssignedRoom() { return assignedRoom; }
    public void setAssignedRoom(Room assignedRoom) { this.assignedRoom = assignedRoom; }
    
    public Set<Student> getStudents() { return students; }
    public void setStudents(Set<Student> students) { this.students = students; }
    
    public Set<Course> getCourses() { return courses; }
    public void setCourses(Set<Course> courses) { this.courses = courses; }
    
    public Set<Teacher> getTeachers() { return teachers; }
    public void setTeachers(Set<Teacher> teachers) { this.teachers = teachers; }
    
    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }
    
    public Set<TimetableSlot> getTimetableSlots() { return timetableSlots; }
    public void setTimetableSlots(Set<TimetableSlot> timetableSlots) { this.timetableSlots = timetableSlots; }
    
    public Set<Enrollment> getEnrollments() { return enrollments; }
    public void setEnrollments(Set<Enrollment> enrollments) { this.enrollments = enrollments; }
    
    public Set<LearningResource> getLearningResources() { return learningResources; }
    public void setLearningResources(Set<LearningResource> learningResources) { this.learningResources = learningResources; }
    
    public Set<Timetable> getTimetables() { return timetables; }
    public void setTimetables(Set<Timetable> timetables) { this.timetables = timetables; }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ClassEntity that = (ClassEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}