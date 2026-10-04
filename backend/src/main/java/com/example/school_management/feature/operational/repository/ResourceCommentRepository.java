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
import java.util.Optional;

@Repository
public interface ResourceCommentRepository extends JpaRepository<ResourceComment, Long>, JpaSpecificationExecutor<ResourceComment> {

    Optional<ResourceComment> findByIdAndOnResourceSchoolId(Long id, Long schoolId);

    // The School and visibility predicates apply before pagination and counting.
    @Query("SELECT rc FROM ResourceComment rc WHERE rc.onResource.school.id = :schoolId AND rc.onResource.id = :resourceId AND (:publicOnly = false OR rc.onResource.isPublic = true) ORDER BY rc.createdAt DESC")
    Page<ResourceComment> findByResourceIdAndSchoolId(@Param("resourceId") Long resourceId, @Param("schoolId") Long schoolId,
                                                   @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.onResource.school.id = :schoolId AND rc.commentedBy.id = :userId AND (:publicOnly = false OR rc.onResource.isPublic = true) ORDER BY rc.createdAt DESC")
    Page<ResourceComment> findByCommentedByUserIdAndSchoolId(@Param("userId") Long userId, @Param("schoolId") Long schoolId,
                                                          @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.onResource.school.id = :schoolId AND (:publicOnly = false OR rc.onResource.isPublic = true)")
    Page<ResourceComment> findAllVisibleBySchoolId(@Param("schoolId") Long schoolId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT rc FROM ResourceComment rc WHERE rc.onResource.school.id = :schoolId AND rc.onResource.id = :resourceId ORDER BY rc.createdAt ASC")
    List<ResourceComment> findAllByResourceIdAndSchoolIdOrderByCreatedAtAsc(@Param("resourceId") Long resourceId, @Param("schoolId") Long schoolId);

    @Query("SELECT COUNT(rc) FROM ResourceComment rc WHERE rc.onResource.school.id = :schoolId AND rc.onResource.id = :resourceId")
    Long countByResourceIdAndSchoolId(@Param("resourceId") Long resourceId, @Param("schoolId") Long schoolId);
}
