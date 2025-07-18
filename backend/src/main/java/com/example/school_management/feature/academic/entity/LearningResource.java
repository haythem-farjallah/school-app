package com.example.school_management.feature.academic.entity;

import com.example.school_management.feature.academic.entity.enums.ResourceType;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.entity.ResourceComment;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@Table(name = "learning_resources")
@EntityListeners(AuditingEntityListener.class)
public class LearningResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @EqualsAndHashCode.Include
    private String url;
    @EqualsAndHashCode.Include
    private String title;

    @Lob
    @EqualsAndHashCode.Include
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "resource_type")
    @ColumnTransformer(write = "?::resource_type")
    @EqualsAndHashCode.Include
    private ResourceType type;

    @EqualsAndHashCode.Include
    private String thumbnailUrl;
    @EqualsAndHashCode.Include
    private Integer duration; // in minutes

    @EqualsAndHashCode.Include
    private boolean isPublic = true;
    @EqualsAndHashCode.Include
    private String status = "ACTIVE";

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Many-to-Many relationship with Teacher (creator)
    @ManyToMany
    @JoinTable(
        name = "learning_resource_teachers",
        joinColumns = @JoinColumn(name = "resource_id"),
        inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    private Set<Teacher> createdBy = new HashSet<>();

    // Many-to-Many relationship with ClassEntity
    @ManyToMany
    @JoinTable(
        name = "learning_resource_classes",
        joinColumns = @JoinColumn(name = "resource_id"),
        inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    private Set<ClassEntity> targetClasses = new HashSet<>();

    // Many-to-Many relationship with Course
    @ManyToMany
    @JoinTable(
        name = "learning_resource_courses",
        joinColumns = @JoinColumn(name = "resource_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> targetCourses = new HashSet<>();

    @OneToMany(mappedBy = "onResource", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ResourceComment> comments = new HashSet<>();

    // Helper methods
    public void addTeacher(Teacher teacher) {
        this.createdBy.add(teacher);
        teacher.getLearningResources().add(this);
    }

    public void removeTeacher(Teacher teacher) {
        this.createdBy.remove(teacher);
        teacher.getLearningResources().remove(this);
    }

    public void addTargetClass(ClassEntity classEntity) {
        this.targetClasses.add(classEntity);
        classEntity.getLearningResources().add(this);
    }

    public void removeTargetClass(ClassEntity classEntity) {
        this.targetClasses.remove(classEntity);
        classEntity.getLearningResources().remove(this);
    }

    public void addTargetCourse(Course course) {
        this.targetCourses.add(course);
        course.getLearningResources().add(this);
    }

    public void removeTargetCourse(Course course) {
        this.targetCourses.remove(course);
        course.getLearningResources().remove(this);
    }
} 