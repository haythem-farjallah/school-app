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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {
    
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final OperationalMapper mapper;

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
} 