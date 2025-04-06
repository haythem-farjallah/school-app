package com.example.school_management.feature.academic.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "resources")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;
    private String title;

    @Lob
    private String description;

    private boolean isPublic;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "resource_allowed_roles", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "role")
    private Set<String> allowedRoles = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne
    @JoinColumn(name = "level_id")
    private Level level;
}