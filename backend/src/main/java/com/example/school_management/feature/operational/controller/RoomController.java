package com.example.school_management.feature.operational.controller;

import com.example.school_management.commons.dtos.ApiSuccessResponse;
import com.example.school_management.commons.dtos.PageDto;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.operational.dto.RoomDto;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.enums.RoomType;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Endpoints for managing rooms")
public class RoomController {

    private final RoomRepository roomRepository;
    private final CurrentSchoolResolver currentSchool;

    @Operation(summary = "Create a new room")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Room>> create(@Valid @RequestBody RoomDto roomDto) {
        log.debug("Creating room: {}", roomDto);
        
        Room room = new Room();
        room.setSchool(currentSchool.resolve());
        room.setName(roomDto.getName());
        room.setCapacity(roomDto.getCapacity());
        room.setRoomType(roomDto.getRoomType());
        
        Room savedRoom = roomRepository.save(room);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Room created successfully", savedRoom));
    }

    @Operation(summary = "Get room by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Room>> get(@PathVariable Long id) {
        log.debug("Getting room: {}", id);
        Room room = findRoom(id);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Room retrieved successfully", room));
    }

    @Operation(summary = "Update room by ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Room>> update(@PathVariable Long id, @Valid @RequestBody RoomDto roomDto) {
        log.debug("Updating room: {}", id);
        
        Room room = findRoom(id);
        
        room.setName(roomDto.getName());
        room.setCapacity(roomDto.getCapacity());
        room.setRoomType(roomDto.getRoomType());
        
        Room updatedRoom = roomRepository.save(room);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Room updated successfully", updatedRoom));
    }

    @Operation(summary = "Delete room by ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiSuccessResponse<Void>> delete(@PathVariable Long id) {
        log.debug("Deleting room: {}", id);
        
        Room room = findRoom(id);
        roomRepository.delete(room);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Room deleted successfully", null));
    }

    @Operation(summary = "List all rooms with pagination and filtering")
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageDto<Room>>> list(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by room type") @RequestParam(required = false) RoomType roomType,
            @Parameter(description = "Filter by minimum capacity") @RequestParam(required = false) Integer minCapacity,
            @Parameter(description = "Search by room name") @RequestParam(required = false) String name) {
        
        log.debug("Listing rooms - page: {}, size: {}, roomType: {}, minCapacity: {}, name: {}", 
                page, size, roomType, minCapacity, name);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms;
        Long schoolId = currentSchool.resolve().getId();
        
        if (name != null && !name.trim().isEmpty()) {
            rooms = roomRepository.findBySchoolIdAndNameContaining(schoolId, name, pageable);
        } else {
            rooms = roomRepository.findAllBySchoolId(schoolId, pageable);
        }
        
        var dto = new PageDto<>(rooms);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Rooms retrieved successfully", dto));
    }

    @Operation(summary = "Get available rooms (not assigned to any class)")
    @GetMapping("/available")
    public ResponseEntity<ApiSuccessResponse<List<Room>>> getAvailableRooms() {
        log.debug("Getting available rooms");
        List<Room> availableRooms = roomRepository.findAvailableRoomsBySchoolId(currentSchool.resolve().getId());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Available rooms retrieved successfully", availableRooms));
    }

    @Operation(summary = "Get rooms by type")
    @GetMapping("/by-type/{roomType}")
    public ResponseEntity<ApiSuccessResponse<List<Room>>> getRoomsByType(@PathVariable RoomType roomType) {
        log.debug("Getting rooms by type: {}", roomType);
        List<Room> rooms = roomRepository.findBySchoolIdAndRoomTypeOrderByName(
                currentSchool.resolve().getId(), roomType.name());
        return ResponseEntity.ok(new ApiSuccessResponse<>("Rooms retrieved successfully", rooms));
    }

    @Operation(summary = "Get rooms by minimum capacity")
    @GetMapping("/by-capacity/{minCapacity}")
    public ResponseEntity<ApiSuccessResponse<List<Room>>> getRoomsByCapacity(@PathVariable Integer minCapacity) {
        log.debug("Getting rooms by capacity >= {}", minCapacity);
        List<Room> rooms = roomRepository.findBySchoolIdAndCapacityGreaterThanEqualOrderByCapacity(
                currentSchool.resolve().getId(), minCapacity);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Rooms retrieved successfully", rooms));
    }

    private Room findRoom(Long id) {
        return roomRepository.findByIdAndSchoolId(id, currentSchool.resolve().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }
}
