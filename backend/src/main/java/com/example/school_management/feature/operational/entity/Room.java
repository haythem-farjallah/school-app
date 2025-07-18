package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.operational.entity.enums.RoomType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnTransformer;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", columnDefinition = "room_type")
    @ColumnTransformer(write = "?::room_type")
    @EqualsAndHashCode.Include
    private RoomType roomType;

    @OneToMany(mappedBy = "assignedRoom")
    private Set<ClassEntity> classes = new HashSet<>();

    @ManyToMany(mappedBy = "rooms")
    private Set<Timetable> timetables = new HashSet<>();
} 