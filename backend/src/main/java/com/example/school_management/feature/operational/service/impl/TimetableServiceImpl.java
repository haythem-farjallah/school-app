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
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
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

}
