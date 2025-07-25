package com.example.school_management.feature.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email")
        })
@SQLRestriction("status <> 'DELETED'")
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Version
    private Long version;

    @EqualsAndHashCode.Include
    private String firstName;
    @EqualsAndHashCode.Include
    private String lastName;

    @Column(unique = true)
    @EqualsAndHashCode.Include
    private String email;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
    private String telephone;
    private LocalDateTime birthday;
    private String gender;
    private String address;
    @JsonIgnore
    private String password;
    private String image;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "user_role")
    @ColumnTransformer(write = "?::user_role")
    private UserRole role;
    private String otpCode;
    private LocalDateTime otpExpiry;
    private boolean passwordChangeRequired = false;
    private Boolean isEmailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "status")
    @ColumnTransformer(write = "?::status")
    private Status status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @OneToOne(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private ProfileSettings profileSettings;
}
