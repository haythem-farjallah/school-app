package com.example.school_management.feature.auth.entity;

import com.example.school_management.feature.auth.entity.enums.StaffType;
import com.example.school_management.feature.operational.entity.Announcement;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnTransformer;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "staff")
public class Staff extends BaseUser {
    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", columnDefinition = "staff_type")
    @ColumnTransformer(write = "?::staff_type")
    private StaffType staffType;
    private String department;

    @ManyToMany
    @JoinTable(
            name = "staff_announcements",
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "announcement_id")
    )
    private Set<Announcement> announcements = new HashSet<>();
} 