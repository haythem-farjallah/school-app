package com.example.school_management.feature.operational.service.impl;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
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
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

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
    private final CurrentSchoolResolver currentSchoolResolver;

    @Override
    public TimetableSlot createSlot(TimetableSlotRequest request) {
        TimetableSlot slot = new TimetableSlot();
        applyRequest(slot, request, currentSchoolResolver.resolve().getId());
        return timetableSlotRepository.save(slot);
    }

    @Override
    public TimetableSlot updateSlot(Long slotId, TimetableSlotRequest request) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        TimetableSlot slot = requireSchoolSlot(slotId, schoolId);
        applyRequest(slot, request, schoolId);
        return timetableSlotRepository.save(slot);
    }

    @Override
    public void deleteSlot(Long slotId) {
        timetableSlotRepository.delete(requireSchoolSlot(slotId, currentSchoolResolver.resolve().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public TimetableSlot getSlot(Long slotId) {
        return requireSchoolSlot(slotId, currentSchoolResolver.resolve().getId());
    }

    private void applyRequest(TimetableSlot slot, TimetableSlotRequest request, Long schoolId) {
        // Validate every supplied reference before changing the managed slot.
        Period period = requireSchoolPeriod(request.getPeriodId(), schoolId);
        ClassEntity clazz = request.getForClassId() == null ? null : requireSchoolClass(request.getForClassId(), schoolId);
        Course course = request.getForCourseId() == null ? null : requireSchoolCourse(request.getForCourseId(), schoolId);
        Teacher teacher = request.getTeacherId() == null ? null : requireSchoolTeacher(request.getTeacherId(), schoolId);
        Room room = request.getRoomId() == null ? null : requireSchoolRoom(request.getRoomId(), schoolId);
        slot.setDayOfWeek(request.getDayOfWeek());
        slot.setDescription(request.getDescription());
        slot.setPeriod(period);
        slot.setForClass(clazz);
        slot.setForCourse(course);
        slot.setTeacher(teacher);
        slot.setRoom(room);
    }

    private TimetableSlot requireSchoolSlot(Long id, Long schoolId) {
        TimetableSlot slot = timetableSlotRepository.findByIdAndPeriodSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found with id: " + id));
        if (slot.getForClass() != null) requireSchoolClass(slot.getForClass().getId(), schoolId);
        if (slot.getForCourse() != null) requireSchoolCourse(slot.getForCourse().getId(), schoolId);
        if (slot.getTeacher() != null) requireSchoolTeacher(slot.getTeacher().getId(), schoolId);
        if (slot.getRoom() != null) requireSchoolRoom(slot.getRoom().getId(), schoolId);
        if (slot.getTimetable() != null && !schoolId.equals(slot.getTimetable().getSchool().getId())) {
            throw new ResourceNotFoundException("Timetable not found with id: " + slot.getTimetable().getId());
        }
        return slot;
    }

    private Period requireSchoolPeriod(Long id, Long schoolId) {
        return periodRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found with id: " + id));
    }
    private ClassEntity requireSchoolClass(Long id, Long schoolId) {
        ClassEntity clazz = classRepository.findByIdAndAcademicYearSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
        if (clazz.getAssignedRoom() != null && !schoolId.equals(clazz.getAssignedRoom().getSchool().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has a Room from a different School");
        }
        return clazz;
    }
    private Course requireSchoolCourse(Long id, Long schoolId) {
        Course course = courseRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        if (course.getTeacher() != null && teacherRepository.findByIdAndSchoolId(course.getTeacher().getId(), schoolId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course has an incompatible Teacher");
        }
        return course;
    }
    private Teacher requireSchoolTeacher(Long id, Long schoolId) {
        return teacherRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
    }
    private Room requireSchoolRoom(Long id, Long schoolId) {
        return roomRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }
}
