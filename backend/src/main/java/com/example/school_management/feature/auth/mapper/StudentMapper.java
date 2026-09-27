package com.example.school_management.feature.auth.mapper;

import com.example.school_management.feature.auth.dto.StudentCreateDto;
import com.example.school_management.feature.auth.dto.StudentDto;
import com.example.school_management.feature.auth.dto.StudentUpdateDto;
import com.example.school_management.feature.auth.entity.Student;
import org.mapstruct.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface StudentMapper
        extends BaseUserMapper<
        Student,          // entity subtype
        StudentCreateDto, // C
        StudentUpdateDto, // U
        StudentDto> {     // R

    /* Extra field mappings that don't match 1-to-1 names */

    @Override
    @Mappings({
            @Mapping(target = "firstName",  source = "profile.firstName"),
            @Mapping(target = "lastName",   source = "profile.lastName"),
            @Mapping(target = "email",      source = "profile.email"),
            @Mapping(target = "telephone",  source = "profile.telephone"),
            @Mapping(target = "birthday",   source = "profile.birthday", qualifiedByName = "birthdayAtStartOfDay"),
            @Mapping(target = "gender",     source = "profile.gender"),
            @Mapping(target = "address",    source = "profile.address"),
            @Mapping(target = "role",       constant = "STUDENT"),
            @Mapping(target = "enrolledAt", expression = "java(dto.enrollmentYear() != null ? LocalDateTime.of(dto.enrollmentYear(), 1, 1, 0, 0) : null)")
    })
    Student toEntity(StudentCreateDto dto);

    @Override
    @Mappings({
        @Mapping(target = "enrollmentYear", expression = "java(entity.getEnrolledAt() != null ? entity.getEnrolledAt().getYear() : null)")
    })
    StudentDto toDto(Student entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "enrolledAt", expression = "java(dto.getEnrollmentYear() != null ? LocalDateTime.of(dto.getEnrollmentYear(), 1, 1, 0, 0) : entity.getEnrolledAt())")
    void patch(StudentUpdateDto dto, @MappingTarget Student entity);

    // The generic LocalDate -> LocalDateTime conversion returns null for a date-only value.
    @Named("birthdayAtStartOfDay")
    default LocalDateTime birthdayAtStartOfDay(LocalDate birthday) {
        return birthday == null ? null : birthday.atStartOfDay();
    }

}
