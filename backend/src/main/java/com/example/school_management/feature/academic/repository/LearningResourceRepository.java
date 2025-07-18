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
import java.util.Set;

@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long>, JpaSpecificationExecutor<LearningResource> {

    @Query("SELECT lr FROM LearningResource lr WHERE lr.isPublic = true")
    Page<LearningResource> findPublicResources(Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr WHERE lr.type = :type")
    Page<LearningResource> findByType(@Param("type") ResourceType type, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.createdBy t WHERE t.id = :teacherId")
    Page<LearningResource> findByTeacherId(@Param("teacherId") Long teacherId, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.targetClasses c WHERE c.id = :classId")
    Page<LearningResource> findByClassId(@Param("classId") Long classId, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.targetCourses c WHERE c.id = :courseId")
    Page<LearningResource> findByCourseId(@Param("courseId") Long courseId, Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr WHERE lr.title LIKE %:title% OR lr.description LIKE %:description%")
    Page<LearningResource> searchByTitleOrDescription(@Param("title") String title, 
                                                     @Param("description") String description, 
                                                     Pageable pageable);

    @Query("SELECT lr FROM LearningResource lr WHERE lr.status = :status")
    List<LearningResource> findByStatus(@Param("status") String status);

    @Query("SELECT lr FROM LearningResource lr JOIN lr.createdBy t WHERE t.id IN :teacherIds")
    List<LearningResource> findByTeacherIds(@Param("teacherIds") Set<Long> teacherIds);
} 