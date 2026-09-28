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

    // publicOnly = true limits each listing below to comments on public learning resources.

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.onResource.id = :resourceId AND (:publicOnly = false OR rc.onResource.isPublic = true) ORDER BY rc.createdAt DESC")
    Page<ResourceComment> findByResourceId(@Param("resourceId") Long resourceId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.commentedBy.id = :userId AND (:publicOnly = false OR rc.onResource.isPublic = true) ORDER BY rc.createdAt DESC")
    Page<ResourceComment> findByCommentedByUserId(@Param("userId") Long userId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT rc FROM ResourceComment rc WHERE :publicOnly = false OR rc.onResource.isPublic = true")
    Page<ResourceComment> findAllVisible(@Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.onResource.id = :resourceId ORDER BY rc.createdAt ASC")
    List<ResourceComment> findAllByResourceIdOrderByCreatedAtAsc(@Param("resourceId") Long resourceId);

    @Query("SELECT COUNT(rc) FROM ResourceComment rc WHERE rc.onResource.id = :resourceId")
    Long countByResourceId(@Param("resourceId") Long resourceId);
} 