package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long>, JpaSpecificationExecutor<TimetableSlot> {

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.forClass.id = :classId ORDER BY ts.dayOfWeek, ts.period.index")
    List<TimetableSlot> findByClassId(@Param("classId") Long classId);

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.forCourse.id = :courseId ORDER BY ts.dayOfWeek, ts.period.index")
    List<TimetableSlot> findByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.dayOfWeek = :dayOfWeek ORDER BY ts.period.index")
    List<TimetableSlot> findByDayOfWeek(@Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.period.id = :periodId ORDER BY ts.dayOfWeek")
    List<TimetableSlot> findByPeriodId(@Param("periodId") Long periodId);

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.forClass.id = :classId AND ts.dayOfWeek = :dayOfWeek ORDER BY ts.period.index")
    List<TimetableSlot> findByClassIdAndDayOfWeek(@Param("classId") Long classId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.forCourse.id = :courseId AND ts.dayOfWeek = :dayOfWeek ORDER BY ts.period.index")
    List<TimetableSlot> findByCourseIdAndDayOfWeek(@Param("courseId") Long courseId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.period.id = :periodId AND ts.dayOfWeek = :dayOfWeek")
    List<TimetableSlot> findByPeriodIdAndDayOfWeek(@Param("periodId") Long periodId, @Param("dayOfWeek") DayOfWeek dayOfWeek);

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.timetable.id = :timetableId ORDER BY ts.dayOfWeek, ts.period.index")
    List<TimetableSlot> findByTimetableId(@Param("timetableId") Long timetableId);

    @Query("SELECT ts FROM TimetableSlot ts WHERE ts.teacher.id = :teacherId ORDER BY ts.dayOfWeek, ts.period.index")
    List<TimetableSlot> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT COUNT(ts) FROM TimetableSlot ts WHERE ts.teacher.id = :teacherId")
    long countByTeacherId(@Param("teacherId") Long teacherId);

    @Modifying
    @Query("DELETE FROM TimetableSlot ts WHERE ts.timetable.id = :timetableId")
    void deleteByTimetableId(@Param("timetableId") Long timetableId);
} 