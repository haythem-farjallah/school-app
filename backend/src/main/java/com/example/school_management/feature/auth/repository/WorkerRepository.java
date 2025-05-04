package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.Administration;
import com.example.school_management.feature.auth.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Optional<Worker> findByEmail(String email);

}
