package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Teacher;

import java.util.Optional;

public interface TeacherRepository extends BaseUserRepository<Teacher> {
    Optional<Teacher> findByEmail(String email);

}
