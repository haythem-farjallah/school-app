package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LevelRepository  extends JpaRepository<Level,  Long>, JpaSpecificationExecutor<Level> { }