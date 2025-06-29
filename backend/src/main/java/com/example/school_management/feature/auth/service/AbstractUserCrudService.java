package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.BaseUserDtoMarker; // marker interface "Dto"
import com.example.school_management.feature.auth.dto.UserCreatedEvent;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.mapper.BaseUserMapper;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generic CRUD service for every user-subtype (Teacher, Worker, Administration, …).
 *
 * @param <E> entity subtype (extends {@link BaseUser})
 * @param <C> DTO used to create a new instance
 * @param <U> DTO used to patch/update an existing instance
 */
@Slf4j
@Transactional
public abstract class AbstractUserCrudService<
        E extends BaseUser,
        C extends BaseUserDtoMarker,
        U extends BaseUserDtoMarker,
        R  > {

    private final BaseUserRepository<E> repo;
    private final BaseUserMapper<E, C, U,R> mapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordUtil passwordUtil;
    private final ApplicationEventPublisher events;

    protected AbstractUserCrudService(BaseUserRepository<E> repo,
                                      BaseUserMapper<E, C, U,R> mapper,
                                      PasswordEncoder passwordEncoder,
                                      PasswordUtil passwordUtil,
                                      ApplicationEventPublisher events) {
        this.repo             = repo;
        this.mapper           = mapper;
        this.passwordEncoder  = passwordEncoder;
        this.passwordUtil     = passwordUtil;
        this.events           = events;
    }

    /* ------------------------------------------------------------------ *
     *  CREATE                                                             *
     * ------------------------------------------------------------------ */
    public E create(C dto) {

        E entity = mapper.toEntity(dto);

        /* generate & encode first password */
        String rawPw = passwordUtil.generate();
        entity.setPassword(passwordEncoder.encode(rawPw));
        entity.setPasswordChangeRequired(true);
        entity.setStatus(Status.ACTIVE);
        entity = repo.save(entity);
        log.info("Created {} id={} email={}", entity.getRole(), entity.getId(), entity.getEmail());

        /* domain event (async email, audit, etc.) */
        events.publishEvent(new UserCreatedEvent(entity, rawPw));
        return entity;
    }

    /* ------------------------------------------------------------------ *
     *  READ                                                               *
     * ------------------------------------------------------------------ */
    public Page<E> findAll(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public E find(long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User id " + id + " not found"));
    }

    /* ------------------------------------------------------------------ *
     *  UPDATE / PATCH                                                     *
     * ------------------------------------------------------------------ */
    public E patch(long id, U dto) {
        E entity = find(id);
        mapper.patch(dto, entity);          // MapStruct null-aware merge
        log.info("Patched {} id={}", entity.getRole(), id);
        return entity;
    }

    /* ------------------------------------------------------------------ *
     *  SOFT DELETE                                                        *
     * ------------------------------------------------------------------ */
    public void delete(long id) {
        E entity = find(id);
        entity.setStatus(Status.DELETED);   // hidden by @SQLRestriction
        log.info("Soft-deleted user id={}", id);
    }
}
