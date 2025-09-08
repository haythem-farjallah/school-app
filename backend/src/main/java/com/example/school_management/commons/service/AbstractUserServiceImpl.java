package com.example.school_management.commons.service;

import com.example.school_management.commons.dto.FilterCriteria;
import com.example.school_management.commons.utils.DynamicSpecificationBuilder;
import com.example.school_management.commons.utils.FilterCriteriaParser;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Abstract implementation providing common functionality for user services.
 * Extends the existing AbstractUserCrudService and implements AbstractUserService interface.
 * 
 * @param <T> Entity type (Teacher, Student, Staff, etc.)
 */
@Slf4j
@Transactional
public abstract class AbstractUserServiceImpl<T extends BaseUser> implements AbstractUserService<T> {

    protected final BaseUserRepository<T> repository;

    protected AbstractUserServiceImpl(BaseUserRepository<T> repository) {
        this.repository = repository;
    }

    /* =================== STANDARD CRUD OPERATIONS =================== */
    // These will be delegated to the concrete service that extends AbstractUserCrudService

    /* =================== LISTING AND FILTERING =================== */

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> search(Pageable pageable, String query) {
        if (query == null || query.isBlank()) {
            return repository.findAll(pageable);
        }
        
        String searchTerm = "%" + query.toLowerCase() + "%";
        Specification<T> spec = createSearchSpecification(searchTerm);
        return repository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findWithAdvancedFilters(Pageable pageable, Map<String, String[]> parameterMap) {
        FilterCriteria criteria = FilterCriteriaParser.parseRequestParams(parameterMap);
        Specification<T> spec = DynamicSpecificationBuilder.build(criteria);
        return repository.findAll(spec, pageable);
    }

    /* =================== BULK OPERATIONS =================== */

    @Override
    @Transactional
    public void bulkDelete(List<Long> ids) {
        log.debug("Bulk deleting {} entities", ids.size());
        List<T> entities = repository.findAllById(ids);
        for (T entity : entities) {
            entity.setStatus(Status.DELETED); // Soft delete
        }
        repository.saveAll(entities);
        log.info("Bulk deleted {} entities", entities.size());
    }

    @Override
    @Transactional
    public void bulkUpdateStatus(List<Long> ids, String status) {
        log.debug("Bulk updating status for {} entities to {}", ids.size(), status);
        List<T> entities = repository.findAllById(ids);
        for (T entity : entities) {
            entity.setStatus(Status.valueOf(status.toUpperCase()));
        }
        repository.saveAll(entities);
        log.info("Bulk updated status for {} entities", entities.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findByIds(List<Long> ids) {
        return repository.findAllById(ids);
    }

    /* =================== ABSTRACT METHODS =================== */

    /**
     * Create search specification for the specific entity type.
     * Subclasses should implement this to define which fields to search.
     * 
     * @param searchTerm The term to search for (already lowercased with wildcards)
     * @return Specification for searching
     */
    protected abstract Specification<T> createSearchSpecification(String searchTerm);

    /**
     * Get the entity type name for logging purposes
     */
    protected abstract String getEntityTypeName();
}
