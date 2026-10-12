package com.example.school_management.feature.operational.service;

import com.example.school_management.commons.security.SecurityService;
import com.example.school_management.feature.academic.service.CurrentAcademicYearResolver;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.operational.dto.TimetableResponseDto;
import com.example.school_management.feature.operational.mapper.TimetableResponseMapper;
import com.example.school_management.feature.operational.repository.EnrollmentRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentTimetableService {
    private final SecurityService securityService;
    private final StudentRepository students;
    private final CurrentSchoolResolver currentSchool;
    private final CurrentAcademicYearResolver currentAcademicYear;
    private final EnrollmentRepository enrollments;
    private final TimetableService timetableService;
    private final TimetableResponseMapper mapper;

    @Transactional(readOnly = true)
    public TimetableResponseDto getMyTimetable() {
        Long userId = securityService.currentUserId()
                .orElseThrow(() -> new AccessDeniedException("ACCESS_DENIED"));
        Long schoolId = currentSchool.resolve().getId();
        var student = students.findByIdAndSchoolId(userId, schoolId)
                .filter(account -> account.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new AccessDeniedException("ACCESS_DENIED"));
        var academicYear = currentAcademicYear.resolve();
        var enrollment = enrollments.findActiveByStudentIdAndAcademicYearIdAndSchoolId(
                student.getId(), academicYear.getId(), schoolId);

        // This is a class slot view; there may be no single persisted timetable supplying metadata.
        TimetableResponseDto response = new TimetableResponseDto();
        response.setSlots(enrollment.map(active -> timetableService.getSlotsByClassId(active.getClassEntity().getId())
                .stream().map(mapper::toSlotDto).toList()).orElseGet(List::of));
        return response;
    }
}
