package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.school.entity.School;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "periods", uniqueConstraints =
        @UniqueConstraint(name = "uk_periods_school_index", columnNames = {"school_id", "index_number"}))
public class Period {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private School school;

    @Column(name = "index_number")
    @EqualsAndHashCode.Include
    private Integer index;
    
    @EqualsAndHashCode.Include
    private LocalTime startTime;
    @EqualsAndHashCode.Include
    private LocalTime endTime;

    @OneToMany(mappedBy = "period")
    @JsonIgnore
    private Set<TimetableSlot> timetableSlots = new HashSet<>();
}
