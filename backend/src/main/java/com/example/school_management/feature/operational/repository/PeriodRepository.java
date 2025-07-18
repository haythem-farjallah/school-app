package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long>, JpaSpecificationExecutor<Period> {

    @Query("SELECT p FROM Period p ORDER BY p.index")
    List<Period> findAllOrderByIndex();

    @Query("SELECT p FROM Period p WHERE p.index = :index")
    Optional<Period> findByIndex(@Param("index") Integer index);

    @Query("SELECT p FROM Period p WHERE p.startTime <= :time AND p.endTime > :time")
    Optional<Period> findCurrentPeriod(@Param("time") LocalTime time);

    @Query("SELECT p FROM Period p WHERE p.startTime >= :startTime ORDER BY p.startTime")
    List<Period> findByStartTimeGreaterThanEqual(@Param("startTime") LocalTime startTime);

    @Query("SELECT p FROM Period p WHERE p.endTime <= :endTime ORDER BY p.endTime")
    List<Period> findByEndTimeLessThanEqual(@Param("endTime") LocalTime endTime);

    @Query("SELECT p FROM Period p WHERE p.startTime >= :startTime AND p.endTime <= :endTime ORDER BY p.index")
    List<Period> findByTimeRange(@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);
} 