package com.example.school_management.feature.auth.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class BaseUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String telephone;
    private LocalDateTime birthday;
    private String gender;
    private String address;
    private String password;
    private String image;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    private String otpCode;
    private LocalDateTime otpExpiry;
    private boolean passwordChangeRequired = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private ProfileSettings profileSettings;
}
