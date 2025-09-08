package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.entity.*;
import com.example.school_management.feature.operational.mapper.OperationalMapper;
import com.example.school_management.feature.operational.repository.*;
import com.example.school_management.feature.operational.service.TimetableService;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableServiceImpl implements TimetableService {
    
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final PeriodRepository periodRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;
    private final OperationalMapper mapper;
    
    @Override
    @Transactional
    public TimetableDto create(CreateTimetableRequest request) {
        log.debug("Creating new timetable: {}", request.getName());
        
        Timetable timetable = new Timetable();
        timetable.setName(request.getName());
        timetable.setDescription(request.getDescription());
        timetable.setAcademicYear(request.getAcademicYear());
        timetable.setSemester(request.getSemester());
        
        // Set classes
        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            Set<ClassEntity> classes = new HashSet<>(classRepository.findAllById(request.getClassIds()));
            timetable.setClasses(classes);
        }
        
        // Set teachers
        if (request.getTeacherIds() != null && !request.getTeacherIds().isEmpty()) {
            Set<Teacher> teachers = new HashSet<>(teacherRepository.findAllById(request.getTeacherIds()));
            timetable.setTeachers(teachers);
        }
        
        // Set rooms
        if (request.getRoomIds() != null && !request.getRoomIds().isEmpty()) {
            Set<Room> rooms = new HashSet<>(roomRepository.findAllById(request.getRoomIds()));
            timetable.setRooms(rooms);
        }
        
        Timetable saved = timetableRepository.save(timetable);
        log.info("Created timetable with ID: {}", saved.getId());
        
        return mapper.toTimetableDto(saved);
    }
    
    @Override
    @Transactional
    public TimetableDto update(Long id, UpdateTimetableRequest request) {
        log.debug("Updating timetable ID: {}", id);
        
        Timetable timetable = timetableRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Timetable not found with id: " + id));
        
        if (request.getName() != null) {
            timetable.setName(request.getName());
        }
        if (request.getDescription() != null) {
            timetable.setDescription(request.getDescription());
        }
        if (request.getAcademicYear() != null) {
            timetable.setAcademicYear(request.getAcademicYear());
        }
        if (request.getSemester() != null) {
            timetable.setSemester(request.getSemester());
        }
        
        // Update classes
        if (request.getClassIds() != null) {
            Set<ClassEntity> classes = new HashSet<>(classRepository.findAllById(request.getClassIds()));
            timetable.setClasses(classes);
        }
        
        // Update teachers
        if (request.getTeacherIds() != null) {
            Set<Teacher> teachers = new HashSet<>(teacherRepository.findAllById(request.getTeacherIds()));
            timetable.setTeachers(teachers);
        }
        
        // Update rooms
        if (request.getRoomIds() != null) {
            Set<Room> rooms = new HashSet<>(roomRepository.findAllById(request.getRoomIds()));
            timetable.setRooms(rooms);
        }
        
        Timetable updated = timetableRepository.save(timetable);
        log.info("Updated timetable ID: {}", id);
        
        return mapper.toTimetableDto(updated);
    }
    
    @Override
    @Transactional
    public void delete(Long id) {
        log.debug("Deleting timetable ID: {}", id);
        
        if (!timetableRepository.existsById(id)) {
            throw new EntityNotFoundException("Timetable not found with id: " + id);
        }
        
        timetableRepository.deleteById(id);
        log.info("Deleted timetable ID: {}", id);
    }
    
    @Override
    public TimetableDto get(Long id) {
        log.debug("Getting timetable ID: {}", id);
        
        Timetable timetable = timetableRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Timetable not found with id: " + id));
        
        return mapper.toTimetableDto(timetable);
    }
    
    @Override
    public Page<TimetableDto> list(Pageable pageable, String academicYear, String semester) {
        log.debug("Listing timetables - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        
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
        log.debug("Finding timetables by academic year: {}", academicYear);
        
        List<Timetable> timetables = timetableRepository.findByAcademicYear(academicYear);
        return timetables.stream()
            .map(mapper::toTimetableDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TimetableDto> findByAcademicYearAndSemester(String academicYear, String semester) {
        log.debug("Finding timetables by academic year: {} and semester: {}", academicYear, semester);
        
        List<Timetable> timetables = timetableRepository.findByAcademicYearAndSemester(academicYear, semester);
        return timetables.stream()
            .map(mapper::toTimetableDto)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<TimetableSlot> getSlotsByClassId(Long classId) {
        log.debug("Getting timetable slots for class ID: {}", classId);
        return timetableSlotRepository.findByClassId(classId);
    }
    
    @Override
    public List<TimetableSlot> getSlotsByTeacherId(Long teacherId) {
        log.debug("Getting timetable slots for teacher ID: {}", teacherId);
        return timetableSlotRepository.findByTeacherId(teacherId);
    }
    
    @Override
    @Transactional
    public TimetableSlot updateSlot(Long slotId, TimetableSlot updatedSlot) {
        log.debug("Updating timetable slot ID: {}", slotId);
        
        TimetableSlot slot = timetableSlotRepository.findById(slotId)
            .orElseThrow(() -> new EntityNotFoundException("Timetable slot not found with id: " + slotId));
        
        slot.setDayOfWeek(updatedSlot.getDayOfWeek());
        slot.setPeriod(updatedSlot.getPeriod());
        slot.setTeacher(updatedSlot.getTeacher());
        slot.setRoom(updatedSlot.getRoom());
        slot.setForCourse(updatedSlot.getForCourse());
        slot.setDescription(updatedSlot.getDescription());
        
        return timetableSlotRepository.save(slot);
    }
    
    @Override
    @Transactional
    public TimetableSlot createSlot(TimetableSlot newSlot) {
        log.debug("Creating new timetable slot");
        return timetableSlotRepository.save(newSlot);
    }
    
    @Override
    @Transactional
    public void deleteSlot(Long slotId) {
        log.debug("Deleting timetable slot ID: {}", slotId);
        
        if (!timetableSlotRepository.existsById(slotId)) {
            throw new EntityNotFoundException("Timetable slot not found with id: " + slotId);
        }
        
        timetableSlotRepository.deleteById(slotId);
    }
    
    @Override
    @Transactional
    public void saveSlotsForClass(Long classId, List<TimetableSlot> slots) {
        log.info("Saving {} slots for class ID: {}", slots.size(), classId);
        
        // Get the class entity first
        var classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + classId));
        log.info("Found class: {} with name: {}", classId, classEntity.getName());
        
        // Find or create a timetable for this class
        List<Timetable> timetables = timetableRepository.findByClassId(classId);
        Timetable timetable;
        
        if (timetables.isEmpty()) {
            log.info("No existing timetable found for class {}, creating new one", classId);
            // Create a new timetable for this class
            timetable = new Timetable();
            timetable.setName("Timetable for " + classEntity.getName());
            timetable.setDescription("Auto-generated timetable for " + classEntity.getName());
            timetable.setAcademicYear("2024-2025");
            timetable.setSemester("Fall");
            
            // Link the class to the timetable
            timetable.getClasses().add(classEntity);
            
            // Add all teachers and rooms to the timetable
            var teachers = teacherRepository.findAll();
            var rooms = roomRepository.findAll();
            log.info("Found {} rooms and {} teachers for timetable", rooms.size(), teachers.size());
            
            timetable.setRooms(rooms.stream().collect(Collectors.toSet()));
            timetable.setTeachers(teachers.stream().collect(Collectors.toSet()));
            
            timetable = timetableRepository.save(timetable);
            log.info("Created new timetable with ID: {}", timetable.getId());
        } else {
            timetable = timetables.get(0);
            log.info("Using existing timetable with ID: {}", timetable.getId());
        }
        
        // Delete existing slots for this class
        List<TimetableSlot> existingSlots = timetableSlotRepository.findByClassId(classId);
        timetableSlotRepository.deleteAll(existingSlots);
        log.info("Deleted {} existing slots for class {}", existingSlots.size(), classId);
        
        // Save new slots and associate them with the timetable
        if (!slots.isEmpty()) {
            for (TimetableSlot slot : slots) {
                // Ensure the slot is properly associated with the timetable and class
                slot.setTimetable(timetable);
                slot.setForClass(classEntity);
                
                // Ensure all references are properly set
                if (slot.getPeriod() != null && slot.getPeriod().getId() != null) {
                    var period = periodRepository.findById(slot.getPeriod().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Period not found: " + slot.getPeriod().getId()));
                    slot.setPeriod(period);
                }
                
                if (slot.getTeacher() != null && slot.getTeacher().getId() != null) {
                    var teacher = teacherRepository.findById(slot.getTeacher().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Teacher not found: " + slot.getTeacher().getId()));
                    slot.setTeacher(teacher);
                }
                
                if (slot.getForCourse() != null && slot.getForCourse().getId() != null) {
                    var course = courseRepository.findById(slot.getForCourse().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Course not found: " + slot.getForCourse().getId()));
                    slot.setForCourse(course);
                }
                
                if (slot.getRoom() != null && slot.getRoom().getId() != null) {
                    var room = roomRepository.findById(slot.getRoom().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Room not found: " + slot.getRoom().getId()));
                    slot.setRoom(room);
                }
            }
            
            List<TimetableSlot> savedSlots = timetableSlotRepository.saveAll(slots);
            log.info("Saved {} new slots for class {} with timetable ID {}", savedSlots.size(), classId, timetable.getId());
        }
        
        // Debug: Verify save
        List<TimetableSlot> verifySlots = timetableSlotRepository.findByClassId(classId);
        log.info("Verification: Found {} slots for class {} after save, {} have teachers, {} have timetable", 
            verifySlots.size(), classId, 
            slots.stream().filter(s -> s.getTeacher() != null).count(),
            verifySlots.stream().filter(s -> s.getTimetable() != null).count()
        );
        
        // Also verify through timetable relationship
        Timetable finalTimetable = timetableRepository.findById(timetable.getId()).orElse(null);
        if (finalTimetable != null) {
            log.info("Timetable {} now has {} associated slots", finalTimetable.getId(), finalTimetable.getSlots().size());
        }
    }

    @Override
    @Transactional
    public void optimizeTimetableForClass(Long classId) {
        try {
            log.info("Starting optimization for class ID: {}", classId);
            
            // Get the class entity first
            var classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + classId));
            log.info("Found class: {} with name: {}", classId, classEntity.getName());
            
            // Find or create a timetable for this class
            List<Timetable> timetables = timetableRepository.findByClassId(classId);
            Timetable timetable;
            
            if (timetables.isEmpty()) {
                log.info("No existing timetable found for class {}, creating new one", classId);
                // Create a new timetable for this class
                timetable = new Timetable();
                timetable.setName("Timetable for " + classEntity.getName());
                timetable.setDescription("Auto-generated timetable for " + classEntity.getName());
                timetable.setAcademicYear("2024-2025");
                timetable.setSemester("Fall");
                
                // Link the class to the timetable
                timetable.getClasses().add(classEntity);
                
                // Add all teachers and rooms to the timetable
                var teachers = teacherRepository.findAll();
                var rooms = roomRepository.findAll();
                log.info("Found {} rooms and {} teachers for timetable", rooms.size(), teachers.size());
                
                timetable.setRooms(rooms.stream().collect(Collectors.toSet()));
                timetable.setTeachers(teachers.stream().collect(Collectors.toSet()));
                
                timetable = timetableRepository.save(timetable);
                log.info("Created new timetable with ID: {}", timetable.getId());
            } else {
                timetable = timetables.get(0);
                log.info("Using existing timetable with ID: {} for class {}", timetable.getId(), classId);
                
                // Ensure the class is linked to this timetable
                if (!timetable.getClasses().contains(classEntity)) {
                    timetable.getClasses().add(classEntity);
                    timetable = timetableRepository.save(timetable);
                    log.info("Linked class {} to existing timetable {}", classId, timetable.getId());
                }
            }
            
            // Perform enhanced optimization
            log.info("Performing enhanced optimization for timetable ID: {}", timetable.getId());
            performEnhancedOptimization(timetable, classId);
            log.info("Optimization completed successfully for class ID: {}", classId);
            
        } catch (Exception e) {
            log.error("Error during optimization for class ID: {}", classId, e);
            throw new RuntimeException("Failed to optimize timetable for class " + classId + ": " + e.getMessage(), e);
        }
    }
    
    private void performEnhancedOptimization(Timetable timetable, Long classId) {
        try {
            log.info("Starting enhanced optimization for class: {}", classId);
            
            var periods = periodRepository.findAllOrderByIndex();
            var teachers = teacherRepository.findAll();
            var rooms = roomRepository.findAll();
            var classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found with id: " + classId));
            
            if (teachers.isEmpty() || periods.isEmpty() || rooms.isEmpty()) {
                log.warn("Missing required data for optimization");
                return;
            }
            
            var courses = classEntity.getCourses();
            if (courses.isEmpty()) {
                log.warn("No courses found for class {}", classId);
                return;
            }
            
            // Clear existing slots
            List<TimetableSlot> existingSlots = timetableSlotRepository.findByClassId(classId);
            if (!existingSlots.isEmpty()) {
                timetableSlotRepository.deleteAll(existingSlots);
            }
            
            // Enhanced course scheduling
            List<TimetableSlot> newSlots = generateEnhancedSchedule(
                timetable, classEntity, courses, periods, teachers, rooms
            );
            
            if (!newSlots.isEmpty()) {
                timetableSlotRepository.saveAll(newSlots);
                log.info("Successfully created {} enhanced slots for class {}", newSlots.size(), classId);
            }
            
        } catch (Exception e) {
            log.error("Error during enhanced optimization", e);
            throw new RuntimeException("Enhanced optimization failed: " + e.getMessage(), e);
        }
    }

    private List<TimetableSlot> generateEnhancedSchedule(
            Timetable timetable,
            ClassEntity classEntity,
            Set<Course> courses,
            List<Period> periods,
            List<Teacher> teachers,
            List<Room> rooms
    ) {
        List<TimetableSlot> slots = new ArrayList<>();
        Map<String, Integer> courseWeeklyCount = new HashMap<>();
        Map<Long, Integer> teacherWeeklyCount = new HashMap<>();
        
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        Random random = new Random();
        
        // Initialize course weekly count
        for (Course course : courses) {
            courseWeeklyCount.put(course.getCode(), 0);
        }
        
        // Try to schedule each course according to its weekly frequency
        for (Course course : courses) {
            int targetFrequency = course.getWeeklyFrequency() != null ? course.getWeeklyFrequency() : 3;
            int scheduledCount = 0;
            
            // Find a teacher who can teach this course
            Teacher assignedTeacher = findTeacherForCourse(course, teachers);
            if (assignedTeacher == null) {
                log.warn("No teacher found for course: {}", course.getName());
                continue;
            }
            
            // Try to schedule the course
            for (int attempt = 0; attempt < targetFrequency * 3 && scheduledCount < targetFrequency; attempt++) {
                DayOfWeek day = days[random.nextInt(days.length)];
                Period period = periods.get(random.nextInt(Math.min(periods.size(), 6))); // Prefer earlier periods
                
                // Check if slot is available
                boolean slotAvailable = slots.stream().noneMatch(s -> 
                    s.getDayOfWeek().equals(day) && 
                    s.getPeriod().getId().equals(period.getId())
                );
                
                if (slotAvailable) {
                    // Check teacher availability
                    long teacherSlotsCount = slots.stream()
                        .filter(s -> s.getTeacher() != null && s.getTeacher().getId().equals(assignedTeacher.getId()))
                        .count();
                        
                    if (teacherSlotsCount < (assignedTeacher.getWeeklyCapacity() != null ? assignedTeacher.getWeeklyCapacity() : 20)) {
                        // Check if teacher is free at this time
                        boolean teacherFree = slots.stream().noneMatch(s ->
                            s.getDayOfWeek().equals(day) &&
                            s.getPeriod().getId().equals(period.getId()) &&
                            s.getTeacher() != null &&
                            s.getTeacher().getId().equals(assignedTeacher.getId())
                        );
                        
                        if (teacherFree) {
                            // Create slot
                            TimetableSlot slot = new TimetableSlot();
                            slot.setTimetable(timetable);
                            slot.setForClass(classEntity);
                            slot.setDayOfWeek(day);
                            slot.setPeriod(period);
                            slot.setForCourse(course);
                            slot.setTeacher(assignedTeacher);
                            
                            // Assign a suitable room
                            Room assignedRoom = findSuitableRoom(course, rooms, slots, day, period);
                            slot.setRoom(assignedRoom);
                            
                            slot.setDescription(course.getName() + " - " + assignedTeacher.getFirstName() + " " + assignedTeacher.getLastName());
                            
                            slots.add(slot);
                            scheduledCount++;
                            
                            // Handle multi-period courses
                            if (course.getDurationPeriods() != null && course.getDurationPeriods() > 1) {
                                int periodIndex = periods.indexOf(period);
                                for (int i = 1; i < course.getDurationPeriods() && periodIndex + i < periods.size(); i++) {
                                    Period nextPeriod = periods.get(periodIndex + i);
                                    
                                    // Check if next slot is available
                                    boolean nextSlotAvailable = slots.stream().noneMatch(s -> 
                                        s.getDayOfWeek().equals(day) && 
                                        s.getPeriod().getId().equals(nextPeriod.getId())
                                    );
                                    
                                    if (nextSlotAvailable) {
                                        TimetableSlot continuationSlot = new TimetableSlot();
                                        continuationSlot.setTimetable(timetable);
                                        continuationSlot.setForClass(classEntity);
                                        continuationSlot.setDayOfWeek(day);
                                        continuationSlot.setPeriod(nextPeriod);
                                        continuationSlot.setForCourse(course);
                                        continuationSlot.setTeacher(assignedTeacher);
                                        continuationSlot.setRoom(assignedRoom);
                                        continuationSlot.setDescription(course.getName() + " (cont.)");
                                        
                                        slots.add(continuationSlot);
                                    } else {
                                        // Can't schedule multi-period course, remove the first slot
                                        slots.remove(slots.size() - 1);
                                        scheduledCount--;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            courseWeeklyCount.put(course.getCode(), scheduledCount);
            log.info("Scheduled {} sessions for course {} (target: {})", scheduledCount, course.getName(), targetFrequency);
        }
        
        return slots;
    }
    
    private Teacher findTeacherForCourse(Course course, List<Teacher> teachers) {
        // Try to find a teacher who teaches this subject
        return teachers.stream()
            .filter(t -> t.getSubjectsTaught() != null && 
                        (t.getSubjectsTaught().toLowerCase().contains(course.getName().toLowerCase()) ||
                         course.getName().toLowerCase().contains(t.getSubjectsTaught().toLowerCase())))
            .findFirst()
            .orElse(teachers.isEmpty() ? null : teachers.get(new Random().nextInt(teachers.size())));
    }
    
    private Room findSuitableRoom(Course course, List<Room> rooms, List<TimetableSlot> existingSlots, DayOfWeek day, Period period) {
        // Find available rooms
        List<Room> availableRooms = rooms.stream()
            .filter(room -> existingSlots.stream().noneMatch(slot ->
                slot.getDayOfWeek().equals(day) &&
                slot.getPeriod().getId().equals(period.getId()) &&
                slot.getRoom() != null &&
                slot.getRoom().getId().equals(room.getId())
            ))
            .collect(Collectors.toList());
        
        if (availableRooms.isEmpty()) {
            return rooms.isEmpty() ? null : rooms.get(0); // Fallback
        }
        
        // Prefer labs for science courses (identified by name)
        if (course.getName().toLowerCase().contains("computer") || 
            course.getName().toLowerCase().contains("science") ||
            course.getName().toLowerCase().contains("chemistry")) {
            Room lab = availableRooms.stream()
                .filter(r -> r.getName() != null && r.getName().toLowerCase().contains("lab"))
                .findFirst()
                .orElse(null);
            if (lab != null) return lab;
        }
        
        // Return a random available room
        return availableRooms.get(new Random().nextInt(availableRooms.size()));
    }
}