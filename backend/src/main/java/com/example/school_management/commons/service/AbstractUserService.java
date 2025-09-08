package com.example.school_management.commons.service;

import com.example.school_management.feature.auth.entity.BaseUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Abstract service interface defining common operations for user entities.
 * 
 * @param <T> Entity type (Teacher, Student, Staff, etc.)
 */
public interface AbstractUserService<T extends BaseUser> {

    /* =================== STANDARD CRUD OPERATIONS =================== */

    /**
     * Create a new entity
     */
    T create(Object createDto);

    /**
     * Find entity by ID
     */
    T find(long id);

    /**
     * Update entity by ID
     */
    T patch(long id, Object updateDto);

    /**
     * Delete entity by ID (soft delete)
     */
    void delete(long id);

    /* =================== LISTING AND FILTERING =================== */

    /**
     * Find all entities with pagination
     */
    Page<T> findAll(Pageable pageable);

    /**
     * Find all entities (without pagination)
     */
    List<T> findAll();

    /**
     * Search entities by query string
     */
    Page<T> search(Pageable pageable, String query);

    /**
     * Find entities with advanced filtering
     */
    Page<T> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap);

    /* =================== BULK OPERATIONS =================== */

    /**
     * Bulk delete entities by IDs
     */
    void bulkDelete(List<Long> ids);

    /**
     * Bulk update entity status
     */
    void bulkUpdateStatus(List<Long> ids, String status);

    /**
     * Find entities by IDs
     */
    List<T> findByIds(List<Long> ids);
}
