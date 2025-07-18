package com.example.school_management.feature.auth.entity;

import com.example.school_management.feature.auth.entity.enums.ContactMethod;
import com.example.school_management.feature.auth.entity.enums.Relation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnTransformer;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "parent")
public class Parent extends BaseUser {
    @ManyToMany
    @JoinTable(
            name = "parent_students",
            joinColumns = @JoinColumn(name = "parent_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<Student> children = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact_method", columnDefinition = "contact_method")
    @ColumnTransformer(write = "?::contact_method")
    private ContactMethod preferredContactMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "relation", columnDefinition = "relation")
    @ColumnTransformer(write = "?::relation")
    private Relation relation;
}