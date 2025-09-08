package com.example.school_management.feature.academic.repository;

import com.example.school_management.feature.academic.entity.LearningResource;
import com.example.school_management.feature.academic.entity.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long>, JpaSpecificationExecutor<LearningResource> {

    @Query("SELECT lr FROM LearningResource lr WHERE lr.isPublic = true ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findPublicResources(Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr WHERE lr.type = :type ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findByType(@Param("type") ResourceType type, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.createdBy t WHERE t.id = :teacherId ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findByTeacherId(@Param("teacherId") Long teacherId, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.targetClasses c WHERE c.id = :classId ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findByClassId(@Param("classId") Long classId, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.targetCourses c WHERE c.id = :courseId ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findByCourseId(@Param("courseId") Long courseId, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr WHERE LOWER(lr.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR lr.description LIKE CONCAT('%', :searchTerm, '%') ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> searchByTitleOrDescription(@Param("searchTerm") String searchTerm, 
                                                     Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr WHERE lr.status = :status ORDER BY lr.createdAt DESC, lr.id DESC")
    List<LearningResource> findByStatus(@Param("status") String status);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.createdBy t WHERE t.id IN :teacherIds ORDER BY lr.createdAt DESC, lr.id DESC")
    List<LearningResource> findByTeacherIds(@Param("teacherIds") Set<Long> teacherIds);
    
    @Query("SELECT lr FROM LearningResource lr WHERE lr.url LIKE CONCAT('%', :filename, '%')")
    Optional<LearningResource> findByFilename(@Param("filename") String filename);
    
    @Query("SELECT lr FROM LearningResource lr ORDER BY lr.createdAt DESC, lr.id DESC")
    Page<LearningResource> findAllSorted(Pageable pageable);
} 