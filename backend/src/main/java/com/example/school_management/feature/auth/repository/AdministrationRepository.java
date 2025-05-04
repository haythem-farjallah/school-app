package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Administration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministrationRepository extends JpaRepository<Administration, Long> {
    Optional<Administration> findByEmail(String email);
}
