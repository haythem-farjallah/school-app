package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.AuditEvent;
import com.example.school_management.feature.operational.entity.enums.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.actedBy.id = :userId ORDER BY ae.createdAt DESC")
    Page<AuditEvent> findByActedByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.eventType = :eventType ORDER BY ae.createdAt DESC")
    Page<AuditEvent> findByEventType(@Param("eventType") AuditEventType eventType, Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.entityType = :entityType AND ae.entityId = :entityId ORDER BY ae.createdAt DESC")
    List<AuditEvent> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.entityType = :entityType AND ae.entityId = :entityId ORDER BY ae.createdAt DESC")
    List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.actedBy.id = :userId ORDER BY ae.createdAt DESC")
    List<AuditEvent> findByActedByIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.createdAt BETWEEN :startDate AND :endDate ORDER BY ae.createdAt DESC")
    Page<AuditEvent> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate, 
                                           Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.actedBy.email LIKE %:email% ORDER BY ae.createdAt DESC")
    Page<AuditEvent> findByActedByEmailContaining(@Param("email") String email, Pageable pageable);
} 