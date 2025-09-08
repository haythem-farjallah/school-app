/*package com.example.school_management.feature.unit.operational.mapper;

import com.example.school_management.feature.operational.dto.AttendanceDto;
import com.example.school_management.feature.operational.entity.Attendance;
import com.example.school_management.feature.operational.entity.enums.AttendanceStatus;
import com.example.school_management.feature.operational.entity.enums.UserType;
import com.example.school_management.feature.operational.mapper.OperationalMapper;
import com.example.school_management.feature.auth.entity.Student;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalMapperTest {

    private final OperationalMapper mapper = Mappers.getMapper(OperationalMapper.class);

    @Test
    void attendanceEntity_isMapped_to_AttendanceDto() {
        // given
        Student user = new Student();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");

        Attendance attendance = new Attendance();
        attendance.setId(1L);
        attendance.setUser(user);
        attendance.setDate(LocalDate.of(2024, 1, 15));
        attendance.setStatus(AttendanceStatus.PRESENT);
        attendance.setUserType(UserType.STUDENT);
        attendance.setRemarks("On time");

        // when
        AttendanceDto dto = mapper.toAttendanceDto(attendance);

        // then
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(dto.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(dto.getUserType()).isEqualTo(UserType.STUDENT);
        assertThat(dto.getRemarks()).isEqualTo("On time");
    }

    @Test
    void attendanceDto_isMapped_to_AttendanceEntity() {
        // given
        AttendanceDto dto = new AttendanceDto();
        dto.setUserId(1L);
        dto.setDate(LocalDate.of(2024, 1, 15));
        dto.setStatus(AttendanceStatus.PRESENT);
        dto.setUserType(UserType.STUDENT);
        dto.setRemarks("On time");

        // when
        Attendance entity = mapper.toAttendance(dto);

        // then
        assertThat(entity.getDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(entity.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(entity.getUserType()).isEqualTo(UserType.STUDENT);
        assertThat(entity.getRemarks()).isEqualTo("On time");
        // User should be ignored as per mapping configuration
        assertThat(entity.getUser()).isNull();
    }
} */