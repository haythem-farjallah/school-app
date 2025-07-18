package com.example.school_management.feature.operational.repository;

import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.enums.RoomType;
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
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType ORDER BY r.name")
    List<Room> findByRoomType(@Param("roomType") RoomType roomType);

    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity ORDER BY r.capacity")
    List<Room> findByCapacityGreaterThanEqual(@Param("minCapacity") Integer minCapacity);

    @Query("SELECT r FROM Room r WHERE r.name LIKE %:name% ORDER BY r.name")
    Page<Room> findByNameContaining(@Param("name") String name, Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.capacity >= :minCapacity ORDER BY r.capacity")
    List<Room> findByRoomTypeAndCapacityGreaterThanEqual(@Param("roomType") RoomType roomType, 
                                                        @Param("minCapacity") Integer minCapacity);

    Optional<Room> findByName(String name);

    @Query("SELECT r FROM Room r WHERE r.id NOT IN (SELECT DISTINCT c.assignedRoom.id FROM ClassEntity c WHERE c.assignedRoom IS NOT NULL)")
    List<Room> findAvailableRooms();
} 