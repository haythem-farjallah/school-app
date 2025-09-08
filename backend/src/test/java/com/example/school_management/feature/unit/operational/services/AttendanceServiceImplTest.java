package com.example.school_management.feature.unit.operational.services;


import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.auth.entity.BaseUser;
import com.example.school_management.feature.auth.entity.Student;
import com.example.school_management.feature.auth.repository.BaseUserRepository;
import com.example.school_management.feature.operational.dto.AttendanceDto;
import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.mapper.OperationalMapper;
import com.example.school_management.feature.operational.repository.AttendanceRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.operational.repository.NotificationRepository;
import com.example.school_management.feature.operational.service.impl.RealTimeNotificationService;
import com.example.school_management.feature.operational.service.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private BaseUserRepository<BaseUser> userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private TeachingAssignmentRepository teachingAssignmentRepository;
    @Mock
    private TimetableSlotRepository timetableSlotRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private ParentRepository parentRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private RealTimeNotificationService realTimeNotificationService;

    private OperationalMapper mapper = Mappers.getMapper(OperationalMapper.class);
    private AttendanceServiceImpl attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceServiceImpl(
                attendanceRepository,
                userRepository,
                courseRepository,
                classRepository,
                teachingAssignmentRepository,
                timetableSlotRepository,
                studentRepository,
                teacherRepository,
                parentRepository,
                notificationRepository,
                realTimeNotificationService,
                mapper
        );
    }

    @Test
    void recordAttendance_success() {
        // given
        Student user = new Student();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");

        AttendanceDto attendanceDto = new AttendanceDto();
        attendanceDto.setUserId(1L);
        attendanceDto.setDate(LocalDate.of(2024, 1, 15));
        attendanceDto.setStatus(AttendanceStatus.PRESENT);
        attendanceDto.setUserType(UserType.STUDENT);
        attendanceDto.setRemarks("On time");

        Attendance savedAttendance = new Attendance();
        savedAttendance.setId(1L);
        savedAttendance.setUser(user);
        savedAttendance.setDate(LocalDate.of(2024, 1, 15));
        savedAttendance.setStatus(AttendanceStatus.PRESENT);
        savedAttendance.setUserType(UserType.STUDENT);
        savedAttendance.setRemarks("On time");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserIdAndCourseIdAndDate(1L, null, LocalDate.of(2024, 1, 15)))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(savedAttendance);

        // when
        AttendanceDto result = attendanceService.recordAttendance(attendanceDto);

        // then
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getUserType()).isEqualTo(UserType.STUDENT);
        assertThat(result.getRemarks()).isEqualTo("On time");
    }
} 