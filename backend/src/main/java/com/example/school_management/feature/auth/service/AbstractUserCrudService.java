package com.example.school_management.feature.auth.service;

import com.example.school_management.feature.auth.dto.BaseUserDtoMarker; // marker interface "Dto"
import com.example.school_management.feature.auth.dto.UserCreatedEvent;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.mapper.BaseUserMapper;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.auth.repository.UserRepository;
import com.example.school_management.feature.auth.util.PasswordUtil;
import com.example.school_management.feature.operational.service.AuditService;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AuditService auditService;
    private final UserRepository userRepository;

    protected AbstractUserCrudService(BaseUserRepository<E> repo,
                                      BaseUserMapper<E, C, U,R> mapper,
                                      PasswordEncoder passwordEncoder,
                                      PasswordUtil passwordUtil,
                                      ApplicationEventPublisher events,
                                      AuditService auditService,
                                      UserRepository userRepository) {
        this.repo             = repo;
        this.mapper           = mapper;
        this.passwordEncoder  = passwordEncoder;
        this.passwordUtil     = passwordUtil;
        this.events           = events;
        this.auditService     = auditService;
        this.userRepository   = userRepository;
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

        // Create audit event for user creation
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = String.format("New %s created", entity.getRole().toString().toLowerCase());
            String details = String.format("User created: %s %s (%s), Role: %s", 
                entity.getFirstName(), entity.getLastName(), entity.getEmail(), entity.getRole());
            
            auditService.createAuditEvent(
                AuditEventType.USER_CREATED, 
                entity.getRole().toString(), 
                entity.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for user creation: {}", e.getMessage());
        }

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
        
        // Log BEFORE values for updatable fields
        log.info("🔄 PATCH REQUEST - Teacher ID: {}, BEFORE update:", id);
        log.info("   📞 Telephone: '{}'", entity.getTelephone());
        log.info("   🏠 Address: '{}'", entity.getAddress());
        
        // For Teacher-specific fields, we need to cast to access them
        if (entity instanceof com.example.school_management.feature.auth.entity.Teacher) {
            com.example.school_management.feature.auth.entity.Teacher teacher = (com.example.school_management.feature.auth.entity.Teacher) entity;
            log.info("   🎓 Qualifications: '{}'", teacher.getQualifications());
            log.info("   📚 Subjects Taught: '{}'", teacher.getSubjectsTaught());
            log.info("   ⏰ Available Hours: {}", teacher.getWeeklyCapacity());
            log.info("   📅 Schedule Preferences: '{}'", teacher.getSchedulePreferences());
        }
        
        // Store old values for audit
        String oldValues = String.format("Name: %s %s, Email: %s, Status: %s", 
            entity.getFirstName(), entity.getLastName(), entity.getEmail(), entity.getStatus());
        
        mapper.patch(dto, entity);          // MapStruct null-aware merge
        
        // Log AFTER values for updatable fields
        log.info("✅ PATCH COMPLETED - Teacher ID: {}, AFTER update:", id);
        log.info("   📞 Telephone: '{}'", entity.getTelephone());
        log.info("   🏠 Address: '{}'", entity.getAddress());
        
        // For Teacher-specific fields, we need to cast to access them
        if (entity instanceof com.example.school_management.feature.auth.entity.Teacher) {
            com.example.school_management.feature.auth.entity.Teacher teacher = (com.example.school_management.feature.auth.entity.Teacher) entity;
            log.info("   🎓 Qualifications: '{}'", teacher.getQualifications());
            log.info("   📚 Subjects Taught: '{}'", teacher.getSubjectsTaught());
            log.info("   ⏰ Available Hours: {}", teacher.getWeeklyCapacity());
            log.info("   📅 Schedule Preferences: '{}'", teacher.getSchedulePreferences());
        }
        
        log.info("Patched {} id={}", entity.getRole(), id);
        
        // Create audit event for user update
        try {
            BaseUser currentUser = getCurrentUser();
            String newValues = String.format("Name: %s %s, Email: %s, Status: %s", 
                entity.getFirstName(), entity.getLastName(), entity.getEmail(), entity.getStatus());
            String summary = String.format("%s updated", entity.getRole().toString());
            String details = String.format("User updated. Old values: %s. New values: %s", oldValues, newValues);
            
            auditService.createAuditEvent(
                AuditEventType.USER_UPDATED, 
                entity.getRole().toString(), 
                entity.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for user update: {}", e.getMessage());
        }
        
        return entity;
    }

    /* ------------------------------------------------------------------ *
     *  SOFT DELETE                                                        *
     * ------------------------------------------------------------------ */
    public void delete(long id) {
        E entity = find(id);
        String userDetails = String.format("%s %s (%s)", entity.getFirstName(), entity.getLastName(), entity.getEmail());
        
        entity.setStatus(Status.DELETED);   // hidden by @SQLRestriction
        log.info("Soft-deleted user id={}", id);
        
        // Create audit event for user deletion
        try {
            BaseUser currentUser = getCurrentUser();
            String summary = String.format("%s deleted", entity.getRole().toString());
            String details = String.format("User soft-deleted: %s, Role: %s", userDetails, entity.getRole());
            
            auditService.createAuditEvent(
                AuditEventType.USER_DELETED, 
                entity.getRole().toString(), 
                entity.getId(),
                summary,
                details,
                currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to create audit event for user deletion: {}", e.getMessage());
        }
    }
    
    /**
     * Get the current authenticated user
     */
    private BaseUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + email));
    }
}
