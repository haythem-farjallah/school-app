package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Timetable;
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
public interface TimetableRepository extends JpaRepository<Timetable, Long>, JpaSpecificationExecutor<Timetable> {

    @Query("SELECT t FROM Timetable t WHERE t.academicYear = :academicYear")
    List<Timetable> findByAcademicYear(@Param("academicYear") String academicYear);

    @Query("SELECT t FROM Timetable t WHERE t.academicYear = :academicYear")
    Page<Timetable> findByAcademicYear(@Param("academicYear") String academicYear, Pageable pageable);

    @Query("SELECT t FROM Timetable t WHERE t.academicYear = :academicYear AND t.semester = :semester")
    List<Timetable> findByAcademicYearAndSemester(@Param("academicYear") String academicYear, 
                                                  @Param("semester") String semester);

    @Query("SELECT t FROM Timetable t WHERE t.academicYear = :academicYear AND t.semester = :semester")
    Page<Timetable> findByAcademicYearAndSemester(@Param("academicYear") String academicYear, 
                                                  @Param("semester") String semester, Pageable pageable);

    @Query("SELECT t FROM Timetable t WHERE t.name LIKE %:name%")
    Page<Timetable> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT t FROM Timetable t JOIN t.classes c WHERE c.id = :classId")
    List<Timetable> findByClassId(@Param("classId") Long classId);

    @Query("SELECT t FROM Timetable t JOIN t.teachers teacher WHERE teacher.id = :teacherId")
    List<Timetable> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT t FROM Timetable t WHERE t.academicYear = :academicYear AND t.semester = :semester AND t.name = :name")
    Optional<Timetable> findByAcademicYearAndSemesterAndName(@Param("academicYear") String academicYear,
                                                             @Param("semester") String semester,
                                                             @Param("name") String name);

    @Query("SELECT DISTINCT t.academicYear FROM Timetable t ORDER BY t.academicYear DESC")
    List<String> findAllAcademicYears();

    @Query("SELECT DISTINCT t.semester FROM Timetable t WHERE t.academicYear = :academicYear ORDER BY t.semester")
    List<String> findSemestersByAcademicYear(@Param("academicYear") String academicYear);
} 