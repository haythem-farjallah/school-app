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
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
    private final CurrentSchoolResolver currentSchoolResolver;
    
    @Override
    @Transactional
    public TimetableDto create(CreateTimetableRequest request) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        Set<ClassEntity> classes = schoolClasses(request.getClassIds(), schoolId);
        Set<Teacher> teachers = schoolTeachers(request.getTeacherIds(), schoolId);
        Set<Room> rooms = schoolRooms(request.getRoomIds(), schoolId);
        Timetable timetable = new Timetable();
        timetable.setSchool(currentSchoolResolver.resolve());
        timetable.setName(request.getName());
        timetable.setDescription(request.getDescription());
        timetable.setAcademicYear(request.getAcademicYear());
        timetable.setSemester(request.getSemester());
        timetable.setClasses(classes);
        timetable.setTeachers(teachers);
        timetable.setRooms(rooms);
        return mapper.toTimetableDto(timetableRepository.save(timetable));
    }

    @Override
    @Transactional
    public TimetableDto update(Long id, UpdateTimetableRequest request) {
        Timetable timetable = requireSchoolTimetable(id);
        Long schoolId = timetable.getSchool().getId();
        Set<ClassEntity> classes = request.getClassIds() == null ? null : schoolClasses(request.getClassIds(), schoolId);
        Set<Teacher> teachers = request.getTeacherIds() == null ? null : schoolTeachers(request.getTeacherIds(), schoolId);
        Set<Room> rooms = request.getRoomIds() == null ? null : schoolRooms(request.getRoomIds(), schoolId);
        if (request.getName() != null) timetable.setName(request.getName());
        if (request.getDescription() != null) timetable.setDescription(request.getDescription());
        if (request.getAcademicYear() != null) timetable.setAcademicYear(request.getAcademicYear());
        if (request.getSemester() != null) timetable.setSemester(request.getSemester());
        if (classes != null) timetable.setClasses(classes);
        if (teachers != null) timetable.setTeachers(teachers);
        if (rooms != null) timetable.setRooms(rooms);
        return mapper.toTimetableDto(timetableRepository.save(timetable));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        timetableRepository.delete(requireSchoolTimetable(id));
    }

    @Override
    public TimetableDto get(Long id) {
        return mapper.toTimetableDto(requireSchoolTimetable(id));
    }

    @Override
    public Timetable requireSchoolTimetable(Long id) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        Timetable timetable = timetableRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable not found with id: " + id));
        validateTimetableResources(timetable, schoolId);
        return timetable;
    }

    @Override
    public Page<TimetableDto> list(Pageable pageable, String academicYear, String semester) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        Page<Timetable> result;
        if (academicYear != null && semester != null) {
            result = timetableRepository.findBySchoolIdAndAcademicYearAndSemester(schoolId, academicYear, semester, pageable);
        } else if (academicYear != null) {
            result = timetableRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear, pageable);
        } else {
            result = timetableRepository.findBySchoolId(schoolId, pageable);
        }
        return result.map(timetable -> {
            validateTimetableResources(timetable, schoolId);
            return mapper.toTimetableDto(timetable);
        });
    }

    @Override
    public List<TimetableDto> findByAcademicYear(String academicYear) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        return timetableRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear).stream().map(timetable -> {
            validateTimetableResources(timetable, schoolId);
            return mapper.toTimetableDto(timetable);
        }).toList();
    }

    @Override
    public List<TimetableDto> findByAcademicYearAndSemester(String academicYear, String semester) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        return timetableRepository.findBySchoolIdAndAcademicYearAndSemester(schoolId, academicYear, semester).stream().map(timetable -> {
            validateTimetableResources(timetable, schoolId);
            return mapper.toTimetableDto(timetable);
        }).toList();
    }

    @Override
    public List<TimetableSlot> getSlotsByClassId(Long classId) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        requireSchoolClass(classId, schoolId);
        List<TimetableSlot> result = timetableSlotRepository.findByClassIdAndPeriodSchoolId(classId, schoolId);
        result.forEach(slot -> validateSlotResources(slot, schoolId));
        return result;
    }

    @Override
    public List<TimetableSlot> getSlotsByTeacherId(Long teacherId) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        requireSchoolTeacher(teacherId, schoolId);
        List<TimetableSlot> result = timetableSlotRepository.findByTeacherIdAndPeriodSchoolId(teacherId, schoolId);
        result.forEach(slot -> validateSlotResources(slot, schoolId));
        return result;
    }

    @Override
    @Transactional
    public TimetableSlot updateSlot(Long slotId, TimetableSlot updatedSlot) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        TimetableSlot slot = requireSchoolSlot(slotId, schoolId);
        TimetableSlot canonical = canonicalSlot(updatedSlot, schoolId);
        copySlotFields(canonical, slot);
        // These legacy duplicate update methods do not change the slot's Class or parent.
        validateSlotResources(slot, schoolId);
        return timetableSlotRepository.save(slot);
    }

    @Override
    @Transactional
    public TimetableSlot createSlot(TimetableSlot newSlot) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        if (newSlot.getId() != null) requireSchoolSlot(newSlot.getId(), schoolId);
        TimetableSlot canonical = canonicalSlot(newSlot, schoolId);
        canonical.setId(newSlot.getId());
        return timetableSlotRepository.save(canonical);
    }

    @Override
    @Transactional
    public void deleteSlot(Long slotId) {
        timetableSlotRepository.delete(requireSchoolSlot(slotId, currentSchoolResolver.resolve().getId()));
    }

    @Override
    @Transactional
    public void saveSlotsForClass(Long classId, List<TimetableSlot> submitted) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        ClassEntity classEntity = requireSchoolClass(classId, schoolId);
        List<TimetableSlot> canonical = new ArrayList<>();
        Map<Long, TimetableSlot> retained = new HashMap<>();
        // Resolve the whole request before changing any persisted entity or deleting existing slots.
        for (TimetableSlot input : submitted) {
            if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot is required");
            if (input.getId() != null) {
                TimetableSlot existing = requireSchoolSlot(input.getId(), schoolId);
                if (existing.getForClass() != null && !existing.getForClass().getId().equals(classId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot belongs to a different Class");
                }
                if (retained.put(input.getId(), existing) != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot ID is repeated");
                }
            }
            TimetableSlot candidate = canonicalSlot(input, schoolId);
            if (candidate.getForClass() != null && !candidate.getForClass().getId().equals(classId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot Class must match the route Class");
            }
            candidate.setForClass(classEntity);
            candidate.setId(input.getId());
            canonical.add(candidate);
        }
        Timetable timetable = findOrCreateTimetable(classEntity, schoolId);
        List<TimetableSlot> existing = timetableSlotRepository.findByClassIdAndPeriodSchoolId(classId, schoolId);
        existing.forEach(slot -> validateSlotResources(slot, schoolId));
        timetableSlotRepository.deleteAll(existing.stream().filter(slot -> !retained.containsKey(slot.getId())).toList());
        for (TimetableSlot candidate : canonical) {
            TimetableSlot target = candidate.getId() == null ? candidate : retained.get(candidate.getId());
            copySlotFields(candidate, target);
            target.setForClass(classEntity);
            target.setTimetable(timetable);
            timetableSlotRepository.save(target);
        }
    }

    @Override
    @Transactional
    public void optimizeTimetableForClass(Long classId) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        ClassEntity classEntity = requireSchoolClass(classId, schoolId);
        for (Course course : classEntity.getCourses()) {
            if (!schoolId.equals(course.getSchool().getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has a Course from a different School");
            }
            requireSchoolCourse(course.getId(), schoolId);
        }
        Timetable timetable = findOrCreateTimetable(classEntity, schoolId);
        var periods = periodRepository.findBySchoolIdOrderByIndex(schoolId);
        var teachers = teacherRepository.findBySchoolId(schoolId);
        var rooms = roomRepository.findBySchoolId(schoolId);
        if (teachers.isEmpty() || periods.isEmpty() || rooms.isEmpty() || classEntity.getCourses().isEmpty()) {
            log.warn("Missing required data for timetable optimization");
            return;
        }
        List<TimetableSlot> existing = timetableSlotRepository.findByClassIdAndPeriodSchoolId(classId, schoolId);
        existing.forEach(slot -> validateSlotResources(slot, schoolId));
        timetableSlotRepository.deleteAll(existing);
        timetableSlotRepository.saveAll(generateEnhancedSchedule(timetable, classEntity, classEntity.getCourses(), periods, teachers, rooms));
    }

    private Timetable findOrCreateTimetable(ClassEntity classEntity, Long schoolId) {
        List<Timetable> existing = timetableRepository.findBySchoolIdAndClassId(schoolId, classEntity.getId());
        if (!existing.isEmpty()) {
            Timetable timetable = existing.get(0);
            validateTimetableResources(timetable, schoolId);
            return timetable;
        }
        Timetable timetable = new Timetable();
        timetable.setSchool(currentSchoolResolver.resolve());
        timetable.setName("Timetable for " + classEntity.getName());
        timetable.setDescription("Auto-generated timetable for " + classEntity.getName());
        timetable.setAcademicYear(classEntity.getAcademicYear().getName());
        timetable.setSemester("Fall");
        timetable.getClasses().add(classEntity);
        timetable.setTeachers(new HashSet<>(teacherRepository.findBySchoolId(schoolId)));
        timetable.setRooms(new HashSet<>(roomRepository.findBySchoolId(schoolId)));
        return timetableRepository.save(timetable);
    }

    private Set<ClassEntity> schoolClasses(Set<Long> ids, Long schoolId) {
        return ids == null ? new HashSet<>() : ids.stream().map(id -> requireSchoolClass(id, schoolId)).collect(Collectors.toSet());
    }
    private Set<Teacher> schoolTeachers(Set<Long> ids, Long schoolId) {
        return ids == null ? new HashSet<>() : ids.stream().map(id -> requireSchoolTeacher(id, schoolId)).collect(Collectors.toSet());
    }
    private Set<Room> schoolRooms(Set<Long> ids, Long schoolId) {
        return ids == null ? new HashSet<>() : ids.stream().map(id -> requireSchoolRoom(id, schoolId)).collect(Collectors.toSet());
    }
    private ClassEntity requireSchoolClass(Long id, Long schoolId) {
        ClassEntity clazz = classRepository.findByIdAndAcademicYearSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
        if (clazz.getAssignedRoom() != null && !schoolId.equals(clazz.getAssignedRoom().getSchool().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has a Room from a different School");
        }
        return clazz;
    }
    private Teacher requireSchoolTeacher(Long id, Long schoolId) {
        return teacherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
    }
    private Course requireSchoolCourse(Long id, Long schoolId) {
        Course course = courseRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        if (course.getTeacher() != null && teacherRepository.findByIdAndSchoolId(course.getTeacher().getId(), schoolId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course has an incompatible Teacher");
        }
        return course;
    }
    private Room requireSchoolRoom(Long id, Long schoolId) {
        return roomRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }
    private Period requireSchoolPeriod(Long id, Long schoolId) {
        if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period ID is required");
        return periodRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + id));
    }
    private TimetableSlot requireSchoolSlot(Long id, Long schoolId) {
        TimetableSlot slot = timetableSlotRepository.findByIdAndPeriodSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found with id: " + id));
        validateSlotResources(slot, schoolId);
        return slot;
    }
    private void validateTimetableResources(Timetable timetable, Long schoolId) {
        timetable.getClasses().forEach(clazz -> requireSchoolClass(clazz.getId(), schoolId));
        timetable.getTeachers().forEach(teacher -> requireSchoolTeacher(teacher.getId(), schoolId));
        timetable.getRooms().forEach(room -> requireSchoolRoom(room.getId(), schoolId));
        timetable.getSlots().forEach(slot -> validateSlotResources(slot, schoolId));
    }
    private void validateSlotResources(TimetableSlot slot, Long schoolId) {
        requireSchoolPeriod(slot.getPeriod() == null ? null : slot.getPeriod().getId(), schoolId);
        if (slot.getForClass() != null) requireSchoolClass(slot.getForClass().getId(), schoolId);
        if (slot.getForCourse() != null) requireSchoolCourse(slot.getForCourse().getId(), schoolId);
        if (slot.getTeacher() != null) requireSchoolTeacher(slot.getTeacher().getId(), schoolId);
        if (slot.getRoom() != null) requireSchoolRoom(slot.getRoom().getId(), schoolId);
        if (slot.getTimetable() != null && !schoolId.equals(slot.getTimetable().getSchool().getId())) {
            throw new ResourceNotFoundException("Timetable not found with id: " + slot.getTimetable().getId());
        }
    }
    private TimetableSlot canonicalSlot(TimetableSlot input, Long schoolId) {
        TimetableSlot slot = new TimetableSlot();
        slot.setDayOfWeek(input.getDayOfWeek());
        slot.setDescription(input.getDescription());
        slot.setPeriod(requireSchoolPeriod(input.getPeriod() == null ? null : input.getPeriod().getId(), schoolId));
        if (input.getForClass() != null) slot.setForClass(requireSchoolClass(input.getForClass().getId(), schoolId));
        if (input.getForCourse() != null) slot.setForCourse(requireSchoolCourse(input.getForCourse().getId(), schoolId));
        if (input.getTeacher() != null) slot.setTeacher(requireSchoolTeacher(input.getTeacher().getId(), schoolId));
        if (input.getRoom() != null) slot.setRoom(requireSchoolRoom(input.getRoom().getId(), schoolId));
        if (input.getTimetable() != null) slot.setTimetable(requireSchoolTimetable(input.getTimetable().getId()));
        return slot;
    }
    private void copySlotFields(TimetableSlot source, TimetableSlot target) {
        target.setDayOfWeek(source.getDayOfWeek());
        target.setDescription(source.getDescription());
        target.setPeriod(source.getPeriod());
        target.setForCourse(source.getForCourse());
        target.setTeacher(source.getTeacher());
        target.setRoom(source.getRoom());
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