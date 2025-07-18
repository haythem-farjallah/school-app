package com.example.school_management.feature.academic.entity;

import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.Enrollment;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@Entity
@Table(name = "classes")
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private Integer yearOfStudy;
    @EqualsAndHashCode.Include
    private Integer maxStudents;

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
    private Set<Student> students = new HashSet<>();

    // Many-to-Many relationship with Course
    @ManyToMany
    @JoinTable(
            name = "class_courses",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<Course> courses = new HashSet<>();

    // Many-to-Many relationship with Teacher
    @ManyToMany
    @JoinTable(
            name = "class_teachers",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    private Set<Teacher> teachers = new HashSet<>();

    // One-to-One relationship with Schedule
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @OneToMany(mappedBy = "forClass")
    private Set<TimetableSlot> timetableSlots = new HashSet<>();

    @OneToMany(mappedBy = "classEntity")
    private Set<Enrollment> enrollments = new HashSet<>();

    @ManyToMany(mappedBy = "targetClasses")
    private Set<LearningResource> learningResources = new HashSet<>();

    @ManyToMany(mappedBy = "classes")
    private Set<Timetable> timetables = new HashSet<>();

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