package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Teacher;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TeacherRepository extends BaseUserRepository<Teacher>, JpaSpecificationExecutor<Teacher> {
    Optional<Teacher> findByEmail(String email);

}
