package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.LearningResource;
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
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long>, JpaSpecificationExecutor<LearningResource> {

    // publicOnly = true limits every listing below to public resources.

    @Query("SELECT lr FROM LearningResource lr WHERE lr.school.id = :schoolId AND cast(lr.type as string) = :type AND (:publicOnly = false OR lr.isPublic = true) ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findByTypeAndSchoolId(@Param("type") String type, @Param("schoolId") Long schoolId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.createdBy t WHERE lr.school.id = :schoolId AND t.id = :teacherId AND (:publicOnly = false OR lr.isPublic = true) ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findByTeacherIdAndSchoolId(@Param("teacherId") Long teacherId, @Param("schoolId") Long schoolId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.targetClasses c WHERE lr.school.id = :schoolId AND c.id = :classId AND (:publicOnly = false OR lr.isPublic = true) ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findByClassIdAndSchoolId(@Param("classId") Long classId, @Param("schoolId") Long schoolId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.targetCourses c WHERE lr.school.id = :schoolId AND c.id = :courseId AND (:publicOnly = false OR lr.isPublic = true) ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findByCourseIdAndSchoolId(@Param("courseId") Long courseId, @Param("schoolId") Long schoolId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr WHERE lr.school.id = :schoolId AND (LOWER(lr.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR lr.description LIKE CONCAT('%', :searchTerm, '%')) AND (:publicOnly = false OR lr.isPublic = true) ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> searchByTitleOrDescriptionAndSchoolId(@Param("searchTerm") String searchTerm,
                                                     @Param("schoolId") Long schoolId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr WHERE lr.school.id = :schoolId AND (:publicOnly = false OR lr.isPublic = true) ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findAllSortedBySchoolId(@Param("schoolId") Long schoolId, @Param("publicOnly") boolean publicOnly, Pageable pageable);

    Optional<LearningResource> findByIdAndSchoolId(Long id, Long schoolId);

    /** Resources in the current School referencing exactly this managed URL. */
    List<LearningResource> findByUrlAndSchoolId(String url, Long schoolId);

    /** Global storage-integrity check only: another School may still reference a shared file. */
    long countByUrl(String url);
}
