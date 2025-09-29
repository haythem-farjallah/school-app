package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Administration;

import java.util.Optional;


public interface AdministrationRepository extends BaseUserRepository<Administration> {
    Optional<Administration> findByEmailIgnoreCase(String email);
}
