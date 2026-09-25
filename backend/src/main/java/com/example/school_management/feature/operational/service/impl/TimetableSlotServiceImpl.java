package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.operational.dto.TimetableSlotRequest;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.operational.service.TimetableSlotService;
import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TimetableSlotServiceImpl implements TimetableSlotService {

    private final TimetableSlotRepository timetableSlotRepository;
    private final PeriodRepository periodRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;

    @Override
    public TimetableSlot createSlot(TimetableSlotRequest request) {
        log.debug("Creating timetable slot from request: {}", request);
        
        TimetableSlot slot = new TimetableSlot();
        slot.setDayOfWeek(request.getDayOfWeek());
        slot.setDescription(request.getDescription());
        
        // Set period
        Period period = periodRepository.findById(request.getPeriodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.getPeriodId()));
        slot.setPeriod(period);
        
        // Set class if provided
        if (request.getForClassId() != null) {
            ClassEntity classEntity = classRepository.findById(request.getForClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + request.getForClassId()));
            slot.setForClass(classEntity);
        }
        
        // Set course if provided
        if (request.getForCourseId() != null) {
            Course course = courseRepository.findById(request.getForCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getForCourseId()));
            slot.setForCourse(course);
        }
        
        // Set teacher if provided
        if (request.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));
            slot.setTeacher(teacher);
        }
        
        // Set room if provided
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.getRoomId()));
            slot.setRoom(room);
        }
        
        return timetableSlotRepository.save(slot);
    }

    @Override
    public TimetableSlot updateSlot(Long slotId, TimetableSlotRequest request) {
        log.debug("Updating timetable slot {} with request: {}", slotId, request);
        
        TimetableSlot slot = timetableSlotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found with id: " + slotId));
        
        slot.setDayOfWeek(request.getDayOfWeek());
        slot.setDescription(request.getDescription());
        
        // Update period
        Period period = periodRepository.findById(request.getPeriodId())
            .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + request.getPeriodId()));
        slot.setPeriod(period);
        
        // Update class if provided
        if (request.getForClassId() != null) {
            ClassEntity classEntity = classRepository.findById(request.getForClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + request.getForClassId()));
            slot.setForClass(classEntity);
        } else {
            slot.setForClass(null);
        }
        
        // Update course if provided
        if (request.getForCourseId() != null) {
            Course course = courseRepository.findById(request.getForCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getForCourseId()));
            slot.setForCourse(course);
        } else {
            slot.setForCourse(null);
        }
        
        // Update teacher if provided
        if (request.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + request.getTeacherId()));
            slot.setTeacher(teacher);
        } else {
            slot.setTeacher(null);
        }
        
        // Update room if provided
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.getRoomId()));
            slot.setRoom(room);
        } else {
            slot.setRoom(null);
        }
        
        return timetableSlotRepository.save(slot);
    }

    @Override
    public void deleteSlot(Long slotId) {
        log.debug("Deleting timetable slot: {}", slotId);
        
        if (!timetableSlotRepository.existsById(slotId)) {
            throw new ResourceNotFoundException("Timetable slot not found with id: " + slotId);
        }
        
        timetableSlotRepository.deleteById(slotId);
    }

    @Override
    public TimetableSlot getSlot(Long slotId) {
        return timetableSlotRepository.findById(slotId)
            .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found with id: " + slotId));
    }
}
