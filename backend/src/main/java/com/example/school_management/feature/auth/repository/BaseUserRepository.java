package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.BaseUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

@NoRepositoryBean
public interface BaseUserRepository<E extends BaseUser>
        extends JpaRepository<E, Long>, JpaSpecificationExecutor<E> {

    Optional<E> findByEmail(String email);
    boolean existsByEmail(String email);
}
