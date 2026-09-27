package com.example.school_management.feature.auth.repository;

import com.example.school_management.feature.auth.entity.BaseUser;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Primary
@Repository
public interface UserRepository extends BaseUserRepository<BaseUser> {

    /**
     * Loads the account with its row locked until the transaction ends, so concurrent
     * password-reset requests for one account are handled one at a time.
     */
    default Optional<BaseUser> findByEmailForUpdate(String email) {
        return lockIdByEmail(email).flatMap(this::findById);
    }

    // Locks with plain SQL: a JPA lock on the polymorphic BaseUser query becomes a follow-on
    // version-checked lock, which fails under contention instead of waiting.
    @Query(value = "SELECT id FROM users WHERE email = :email AND status <> 'DELETED' FOR UPDATE", nativeQuery = true)
    Optional<Long> lockIdByEmail(String email);
}
