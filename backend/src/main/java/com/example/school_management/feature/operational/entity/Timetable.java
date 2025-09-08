package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "timetables")
@EntityListeners(AuditingEntityListener.class)
public class Timetable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private String description;
    @EqualsAndHashCode.Include
    private String academicYear;
    @EqualsAndHashCode.Include
    private String semester;

    @CreatedDate
    @JsonIgnore
    private LocalDateTime createdAt;

    @LastModifiedDate
    @JsonIgnore
    private LocalDateTime updatedAt;

    // One-to-Many relationship with TimetableSlot
    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<TimetableSlot> slots = new HashSet<>();

    // Many-to-Many relationship with ClassEntity
    @ManyToMany
    @JoinTable(
        name = "timetable_classes",
        joinColumns = @JoinColumn(name = "timetable_id"),
        inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    @JsonIgnore
    private Set<ClassEntity> classes = new HashSet<>();

    // Many-to-Many relationship with Teacher
    @ManyToMany
    @JoinTable(
        name = "timetable_teachers",
        joinColumns = @JoinColumn(name = "timetable_id"),
        inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    @JsonIgnore
    private Set<Teacher> teachers = new HashSet<>();

    // Many-to-Many relationship with Room
    @ManyToMany
    @JoinTable(
        name = "timetable_rooms",
        joinColumns = @JoinColumn(name = "timetable_id"),
        inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    @JsonIgnore
    private Set<Room> rooms = new HashSet<>();

    // Helper methods
    public void addSlot(TimetableSlot slot) {
        this.slots.add(slot);
        slot.setTimetable(this);
    }

    public void removeSlot(TimetableSlot slot) {
        this.slots.remove(slot);
        slot.setTimetable(null);
    }

    public void addClass(ClassEntity classEntity) {
        this.classes.add(classEntity);
        classEntity.getTimetables().add(this);
    }

    public void removeClass(ClassEntity classEntity) {
        this.classes.remove(classEntity);
        classEntity.getTimetables().remove(this);
    }

    public void addTeacher(Teacher teacher) {
        this.teachers.add(teacher);
        teacher.getTimetables().add(this);
    }

    public void removeTeacher(Teacher teacher) {
        this.teachers.remove(teacher);
        teacher.getTimetables().remove(this);
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
        room.getTimetables().add(this);
    }

    public void removeRoom(Room room) {
        this.rooms.remove(room);
        room.getTimetables().remove(this);
    }
}