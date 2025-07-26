package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.dto.CreateTimetableRequest;
import com.example.school_management.feature.operational.dto.TimetableDto;
import com.example.school_management.feature.operational.dto.UpdateTimetableRequest;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.mapper.OperationalMapper;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.operational.repository.TimetableRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.operational.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.example.school_management.feature.operational.service.TimetableOptimizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {
    
    private static final Logger log = LoggerFactory.getLogger(TimetableServiceImpl.class);

    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final OperationalMapper mapper;
    private final TimetableOptimizationService optimizationService;
    private final PeriodRepository periodRepository;

    // Timetable CRUD operations
    @Override
    @Transactional
    public TimetableDto create(CreateTimetableRequest request) {
        Timetable timetable = mapper.toTimetable(request);
        
        // Set relationships
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            timetable.setClasses(classRepository.findAllById(request.getClassIds()).stream().collect(Collectors.toSet()));
        }
        if (request.getTeacherIds() != null && !request.getTeacherIds().isEmpty()) {
            timetable.setTeachers(teacherRepository.findAllById(request.getTeacherIds()).stream().collect(Collectors.toSet()));
        }
        if (request.getRoomIds() != null && !request.getRoomIds().isEmpty()) {
            timetable.setRooms(roomRepository.findAllById(request.getRoomIds()).stream().collect(Collectors.toSet()));
        }
        
        Timetable saved = timetableRepository.save(timetable);
        return mapper.toTimetableDto(saved);
    }

    @Override
    @Transactional
    public TimetableDto update(Long id, UpdateTimetableRequest request) {
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable not found with id: " + id));
        
        mapper.updateTimetable(request, timetable);
        
        // Update relationships
        if (request.getClassIds() != null) {
            timetable.getClasses().clear();
            if (!request.getClassIds().isEmpty()) {
                timetable.setClasses(classRepository.findAllById(request.getClassIds()).stream().collect(Collectors.toSet()));
            }
        }
        if (request.getTeacherIds() != null) {
            timetable.getTeachers().clear();
            if (!request.getTeacherIds().isEmpty()) {
                timetable.setTeachers(teacherRepository.findAllById(request.getTeacherIds()).stream().collect(Collectors.toSet()));
            }
        }
        if (request.getRoomIds() != null) {
            timetable.getRooms().clear();
            if (!request.getRoomIds().isEmpty()) {
                timetable.setRooms(roomRepository.findAllById(request.getRoomIds()).stream().collect(Collectors.toSet()));
            }
        }
        
        Timetable updated = timetableRepository.save(timetable);
        return mapper.toTimetableDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!timetableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Timetable not found with id: " + id);
        }
        timetableRepository.deleteById(id);
    }

    @Override
    public TimetableDto get(Long id) {
        Timetable timetable = timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable not found with id: " + id));
        return mapper.toTimetableDto(timetable);
    }

    @Override
    public Page<TimetableDto> list(Pageable pageable, String academicYear, String semester) {
        Page<Timetable> timetables;
        
        if (academicYear != null && semester != null) {
            timetables = timetableRepository.findByAcademicYearAndSemester(academicYear, semester, pageable);
        } else if (academicYear != null) {
            timetables = timetableRepository.findByAcademicYear(academicYear, pageable);
        } else {
            timetables = timetableRepository.findAll(pageable);
        }
        
        return timetables.map(mapper::toTimetableDto);
    }

    @Override
    public List<TimetableDto> findByAcademicYear(String academicYear) {
        return timetableRepository.findByAcademicYear(academicYear)
                .stream()
                .map(mapper::toTimetableDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TimetableDto> findByAcademicYearAndSemester(String academicYear, String semester) {
        return timetableRepository.findByAcademicYearAndSemester(academicYear, semester)
                .stream()
                .map(mapper::toTimetableDto)
                .collect(Collectors.toList());
    }

    // TimetableSlot operations
    @Override
    public List<TimetableSlot> getSlotsByClassId(Long classId) {
        return timetableSlotRepository.findByClassId(classId);
    }

    @Override
    public List<TimetableSlot> getSlotsByTeacherId(Long teacherId) {
        return timetableSlotRepository.findByTeacherId(teacherId);
    }

    @Override
    public TimetableSlot updateSlot(Long slotId, TimetableSlot updatedSlot) {
        TimetableSlot slot = timetableSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with id: " + slotId));
        
        // Update fields
        slot.setDayOfWeek(updatedSlot.getDayOfWeek());
        slot.setPeriod(updatedSlot.getPeriod());
        slot.setRoom(updatedSlot.getRoom());
        slot.setTeacher(updatedSlot.getTeacher());
        slot.setForClass(updatedSlot.getForClass());
        slot.setForCourse(updatedSlot.getForCourse());
        slot.setDescription(updatedSlot.getDescription());
        
        return timetableSlotRepository.save(slot);
    }

    @Override
    public TimetableSlot createSlot(TimetableSlot newSlot) {
        return timetableSlotRepository.save(newSlot);
    }

    @Override
    public void deleteSlot(Long slotId) {
        if (!timetableSlotRepository.existsById(slotId)) {
            throw new ResourceNotFoundException("Slot not found with id: " + slotId);
        }
        timetableSlotRepository.deleteById(slotId);
    }

    @Override
    @Transactional
    public void saveSlotsForClass(Long classId, List<TimetableSlot> slots) {
        // First, delete existing slots for this class
        List<TimetableSlot> existingSlots = timetableSlotRepository.findByClassId(classId);
        if (!existingSlots.isEmpty()) {
            timetableSlotRepository.deleteAll(existingSlots);
        }
        
        // Save new slots
        for (TimetableSlot slot : slots) {
            if (slot.getTeacher() != null) { // Only save slots with assigned teachers
                slot.setForClass(classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId)));
                timetableSlotRepository.save(slot);
            }
        }
    }

    @Override
    @Transactional
    public void optimizeTimetableForClass(Long classId) {
        try {
            log.info("Starting optimization for class ID: {}", classId);
            
            // Find or create a timetable for this class
            List<Timetable> timetables = timetableRepository.findByClassId(classId);
            Timetable timetable;
            
            if (timetables.isEmpty()) {
                log.info("No existing timetable found for class {}, creating new one", classId);
                // Create a new timetable for this class
                timetable = new Timetable();
                timetable.setName("Timetable for Class " + classId);
                timetable.setAcademicYear("2024-2025"); // Default academic year
                timetable.setSemester("Fall"); // Default semester
                
                var classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
                timetable.setClasses(Set.of(classEntity));
                
                // Set default rooms and teachers
                var rooms = roomRepository.findAll();
                var teachers = teacherRepository.findAll();
                timetable.setRooms(rooms.stream().collect(Collectors.toSet()));
                timetable.setTeachers(teachers.stream().collect(Collectors.toSet()));
                
                timetable = timetableRepository.save(timetable);
                log.info("Created new timetable with ID: {}", timetable.getId());
            } else {
                timetable = timetables.get(0);
                log.info("Using existing timetable with ID: {}", timetable.getId());
            }
            
            // Simple optimization: assign teachers randomly to slots
            log.info("Performing simple optimization for timetable ID: {}", timetable.getId());
            performSimpleOptimization(timetable, classId);
            log.info("Optimization completed successfully for class ID: {}", classId);
            
        } catch (Exception e) {
            log.error("Error during optimization for class ID: {}", classId, e);
            throw new RuntimeException("Failed to optimize timetable for class " + classId + ": " + e.getMessage(), e);
        }
    }
    
    private void performSimpleOptimization(Timetable timetable, Long classId) {
        // Get all periods and teachers
        var periods = periodRepository.findAllOrderByIndex();
        var teachers = teacherRepository.findAll();
        var classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));
        
        if (teachers.isEmpty()) {
            log.warn("No teachers available for optimization");
            return;
        }
        
        // Clear existing slots for this class
        List<TimetableSlot> existingSlots = timetableSlotRepository.findByClassId(classId);
        if (!existingSlots.isEmpty()) {
            timetableSlotRepository.deleteAll(existingSlots);
        }
        
        // Create new slots with random teacher assignments
        List<TimetableSlot> newSlots = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        
        for (String day : days) {
            for (var period : periods) {
                // Skip some slots randomly to avoid overloading
                if (Math.random() > 0.7) continue;
                
                // Pick a random teacher
                var randomTeacher = teachers.get((int) (Math.random() * teachers.size()));
                
                TimetableSlot slot = new TimetableSlot();
                slot.setTimetable(timetable);
                slot.setDayOfWeek(DayOfWeek.valueOf(day.toUpperCase()));
                slot.setPeriod(period);
                slot.setTeacher(randomTeacher);
                slot.setForClass(classEntity);
                slot.setDescription("Auto-generated slot");
                
                newSlots.add(slot);
            }
        }
        
        // Save all new slots
        timetableSlotRepository.saveAll(newSlots);
        log.info("Created {} optimized slots for class {}", newSlots.size(), classId);
    }
} 