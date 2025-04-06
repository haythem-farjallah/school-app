package com.example.school_management.feature.operational.entity;

import com.example.school_management.feature.auth.entity.BaseUser;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "timetables")
public class Timetable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each timetable is linked to one user
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BaseUser user;

    @Lob
    private String timetableJson;
}