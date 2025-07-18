package com.example.school_management.feature.operational.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "periods")
public class Period {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "index_number")
    @EqualsAndHashCode.Include
    private Integer index;
    
    @EqualsAndHashCode.Include
    private LocalTime startTime;
    @EqualsAndHashCode.Include
    private LocalTime endTime;

    @OneToMany(mappedBy = "period")
    private Set<TimetableSlot> timetableSlots = new HashSet<>();
} 