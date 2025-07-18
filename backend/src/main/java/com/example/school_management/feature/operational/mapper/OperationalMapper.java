package com.example.school_management.feature.operational.mapper;

import com.example.school_management.feature.operational.dto.*;
import com.example.school_management.feature.operational.entity.*;
import org.mapstruct.*;

import java.time.Duration;
import java.time.LocalTime;


@Mapper(componentModel = "spring")
public interface OperationalMapper {

    /* ─────────────────────── ENTITY ➜ DTO ─────────────────────── */

    /* ---------- AuditEvent ---------- */
    @Mapping(target = "actedById", source = "actedBy.id")
    @Mapping(target = "actedByEmail", source = "actedBy.email")
    AuditEventDto toAuditEventDto(AuditEvent entity);

    /* ---------- ResourceComment ---------- */
    @Mapping(target = "resourceId", source = "onResource.id")
    @Mapping(target = "resourceTitle", source = "onResource.title")
    @Mapping(target = "commentedById", source = "commentedBy.id")
    @Mapping(target = "commentedByName", expression = "java(entity.getCommentedBy().getFirstName() + \" \" + entity.getCommentedBy().getLastName())")
    ResourceCommentDto toResourceCommentDto(ResourceComment entity);

    /* ---------- Room ---------- */
    @Mapping(target = "isAssigned", expression = "java(entity.getClasses() != null && !entity.getClasses().isEmpty())")
    RoomDto toRoomDto(Room entity);

    /* ---------- Period ---------- */
    @Mapping(target = "duration", expression = "java(calculateDuration(entity.getStartTime(), entity.getEndTime()))")
    PeriodDto toPeriodDto(Period entity);

    /* ---------- Announcement ---------- */
    @Mapping(target = "publisherIds", ignore = true) // Avoid circular dependency
    AnnouncementDto toAnnouncementDto(Announcement entity);

    /* ---------- Timetable ---------- */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "totalSlots", expression = "java(entity.getSlots() != null ? entity.getSlots().size() : 0)")
    @Mapping(target = "classIds", expression = "java(entity.getClasses() != null ? entity.getClasses().stream().map(c -> c.getId()).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of())")
    @Mapping(target = "teacherIds", expression = "java(entity.getTeachers() != null ? entity.getTeachers().stream().map(t -> t.getId()).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of())")
    @Mapping(target = "roomIds", expression = "java(entity.getRooms() != null ? entity.getRooms().stream().map(r -> r.getId()).collect(java.util.stream.Collectors.toSet()) : java.util.Set.of())")
    TimetableDto toTimetableDto(Timetable entity);

    /* ---------- Attendance ---------- */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "courseName", ignore = true)
    @Mapping(target = "className", ignore = true)
    @Mapping(target = "recordedByName", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "classId", source = "classEntity.id")
    @Mapping(target = "timetableSlotId", source = "timetableSlot.id")
    @Mapping(target = "recordedById", source = "recordedBy.id")
    AttendanceDto toAttendanceDto(Attendance entity);

    /* ---------- Grade ---------- */
    @Mapping(target = "enrollmentId", source = "enrollment.id")
    @Mapping(target = "studentId", source = "enrollment.student.id")
    @Mapping(target = "studentName", expression = "java(entity.getEnrollment().getStudent().getFirstName() + \" \" + entity.getEnrollment().getStudent().getLastName())")
    @Mapping(target = "studentEmail", source = "enrollment.student.email")
    @Mapping(target = "classId", source = "enrollment.classEntity.id")
    @Mapping(target = "className", source = "enrollment.classEntity.name")
    @Mapping(target = "courseId", ignore = true) // Will be set in service layer from course context
    @Mapping(target = "courseName", ignore = true) // Will be set in service layer from course context
    @Mapping(target = "courseCode", ignore = true) // Will be set in service layer from course context
    @Mapping(target = "teacherId", source = "assignedBy.id")
    @Mapping(target = "teacherName", expression = "java(entity.getAssignedBy().getFirstName() + \" \" + entity.getAssignedBy().getLastName())")
    @Mapping(target = "teacherEmail", source = "assignedBy.email")
    @Mapping(target = "level", expression = "java(\"Year \" + entity.getEnrollment().getClassEntity().getYearOfStudy())")
    @Mapping(target = "subject", ignore = true) // Will be set in service layer from course context
    @Mapping(target = "canEdit", ignore = true) // Set in service layer based on permissions
    @Mapping(target = "canDelete", ignore = true) // Set in service layer based on permissions
    GradeResponse toGradeResponse(Grade entity);

    /* ─────────────────────── CREATE DTO ➜ ENTITY ─────────────────────── */

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "actedBy", ignore = true)
    @Mapping(target = "ipAddress", ignore = true)
    @Mapping(target = "userAgent", ignore = true)
    AuditEvent toAuditEvent(AuditEventDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "onResource", ignore = true)
    @Mapping(target = "commentedBy", ignore = true)
    ResourceComment toResourceComment(ResourceCommentDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classes", ignore = true)
    Room toRoom(RoomDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timetableSlots", ignore = true)
    Period toPeriod(PeriodDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishers", ignore = true)
    Announcement toAnnouncement(AnnouncementDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "teachers", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    Timetable toTimetable(CreateTimetableRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "classEntity", ignore = true)
    @Mapping(target = "timetableSlot", ignore = true)
    @Mapping(target = "recordedBy", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    Attendance toAttendance(AttendanceDto dto);

    /* ─────────────────────── UPDATE PATCHERS ───────────────────── */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "actedBy", ignore = true)
    @Mapping(target = "ipAddress", ignore = true) 
    @Mapping(target = "userAgent", ignore = true)
    void updateAuditEvent(AuditEventDto src, @MappingTarget AuditEvent target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "onResource", ignore = true)
    @Mapping(target = "commentedBy", ignore = true)
    void updateResourceComment(ResourceCommentDto src, @MappingTarget ResourceComment target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "classes", ignore = true)
    void updateRoom(RoomDto src, @MappingTarget Room target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "timetableSlots", ignore = true)
    void updatePeriod(PeriodDto src, @MappingTarget Period target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishers", ignore = true)
    void updateAnnouncement(AnnouncementDto src, @MappingTarget Announcement target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slots", ignore = true)
    @Mapping(target = "classes", ignore = true)
    @Mapping(target = "teachers", ignore = true)
    @Mapping(target = "rooms", ignore = true)
    void updateTimetable(UpdateTimetableRequest src, @MappingTarget Timetable target);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "classEntity", ignore = true)
    @Mapping(target = "timetableSlot", ignore = true)
    @Mapping(target = "recordedBy", ignore = true)
    @Mapping(target = "recordedAt", ignore = true)
    void updateAttendance(AttendanceDto src, @MappingTarget Attendance target);

    /* ─────────────────────── HELPERS ───────────────────────────── */

    default String calculateDuration(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) return "";
        Duration duration = Duration.between(startTime, endTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02d:%02d", hours, minutes);
    }
} 