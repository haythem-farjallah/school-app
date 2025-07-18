package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.ResourceComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceCommentRepository extends JpaRepository<ResourceComment, Long>, JpaSpecificationExecutor<ResourceComment> {

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.onResource.id = :resourceId ORDER BY rc.createdAt DESC")
    Page<ResourceComment> findByResourceId(@Param("resourceId") Long resourceId, Pageable pageable);

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.commentedBy.id = :userId ORDER BY rc.createdAt DESC")
    Page<ResourceComment> findByCommentedByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.onResource.id = :resourceId ORDER BY rc.createdAt ASC")
    List<ResourceComment> findAllByResourceIdOrderByCreatedAtAsc(@Param("resourceId") Long resourceId);

    @Query("SELECT COUNT(rc) FROM ResourceComment rc WHERE rc.onResource.id = :resourceId")
    Long countByResourceId(@Param("resourceId") Long resourceId);
} 