# Teacher Attendance System Demo Guide

## Overview
This guide demonstrates the comprehensive teacher attendance system that allows teachers to mark student attendance based on their timetable slots.

## System Features

### Backend Features
1. **Teacher Schedule Integration**: Teachers can only mark attendance for classes they are assigned to teach
2. **Timetable-based Attendance**: Attendance is linked to specific timetable slots
3. **Date Validation**: Teachers can only mark attendance for the correct day of the week
4. **Bulk Attendance Marking**: Mark attendance for all students in a class at once
5. **Attendance Statistics**: Weekly and daily attendance summaries
6. **Absent Student Tracking**: Quick view of absent students

### Frontend Features
1. **Teacher Dashboard**: Overview of attendance statistics and trends
2. **Daily Schedule View**: See today's classes and attendance status
3. **Student List Management**: Mark attendance for individual students
4. **Bulk Actions**: Mark all students present/absent with one click
5. **Absent Student Alerts**: Quick view of students who are absent
6. **Weekly Overview**: Visual representation of weekly attendance patterns

## API Endpoints

### Teacher-Specific Endpoints
```
GET /api/v1/attendance/teacher/{teacherId}/today?date={date}
- Get teacher's schedule for today with attendance status

GET /api/v1/attendance/teacher/{teacherId}/absent-students?date={date}
- Get list of absent students for teacher on specific date

GET /api/v1/attendance/teacher/{teacherId}/weekly-summary?startOfWeek={date}
- Get weekly attendance summary for teacher

GET /api/v1/attendance/slot/{slotId}/students?date={date}
- Get all students for a timetable slot with their attendance status

POST /api/v1/attendance/slot/{slotId}/mark?date={date}
- Mark attendance for all students in a timetable slot

GET /api/v1/attendance/teacher/{teacherId}/can-mark/{slotId}?date={date}
- Check if teacher can mark attendance for specific slot
```

## Demo Scenarios

### Scenario 1: Teacher Morning Routine
1. Teacher logs in and navigates to Attendance page
2. Views today's schedule showing all assigned classes
3. Sees which classes have attendance marked and which don't
4. Clicks on first period class to mark attendance

### Scenario 2: Marking Attendance for a Class
1. Teacher selects a timetable slot from their schedule
2. System loads all students enrolled in that class
3. Teacher can:
   - Mark individual students as Present/Absent/Late/Excused
   - Use bulk actions to mark all present or all absent
   - Add remarks for specific students
   - Save attendance for the entire class

### Scenario 3: Viewing Absent Students
1. Teacher switches to "Absent Students" tab
2. Sees a consolidated list of all absent students across all their classes
3. Can see which class each absent student belongs to
4. Can add remarks or change status if needed

### Scenario 4: Weekly Overview
1. Teacher switches to Dashboard tab
2. Views weekly attendance statistics
3. Sees attendance trends and patterns
4. Can navigate between different weeks
5. Views attendance rate percentages

## Testing the System

### Prerequisites
1. Ensure you have teachers, students, classes, and timetable slots set up
2. Run the database migration V49 to enhance the attendance system
3. Ensure teachers are assigned to timetable slots

### Test Data Setup
1. Create a teacher user
2. Create a class with enrolled students
3. Create timetable slots assigned to the teacher for that class
4. Ensure the timetable slots have the correct day of week

### Frontend Testing
1. Login as a teacher
2. Navigate to `/teacher/attendance`
3. Test the following flows:
   - View today's schedule
   - Mark attendance for a class
   - View absent students
   - Check weekly dashboard

### API Testing
Use the provided Postman collection or test with curl:

```bash
# Get teacher's today schedule
curl -X GET "http://localhost:8080/api/v1/attendance/teacher/1/today" \
  -H "Authorization: Bearer {token}"

# Get students for a slot
curl -X GET "http://localhost:8080/api/v1/attendance/slot/1/students" \
  -H "Authorization: Bearer {token}"

# Mark attendance for a slot
curl -X POST "http://localhost:8080/api/v1/attendance/slot/1/mark" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "userId": 1,
      "timetableSlotId": 1,
      "date": "2024-01-15",
      "status": "PRESENT",
      "userType": "STUDENT"
    }
  ]'
```

## Business Logic

### Attendance Marking Rules
1. Teachers can only mark attendance for their assigned classes
2. Attendance can only be marked for the correct day of the week
3. Existing attendance records are updated, not duplicated
4. All attendance changes are logged with timestamp and recorder

### Data Validation
1. Teacher must be assigned to the timetable slot
2. Date must match the slot's day of week
3. Students must be enrolled in the class
4. Status must be a valid AttendanceStatus enum value

## Troubleshooting

### Common Issues
1. **Teacher can't mark attendance**: Check if teacher is assigned to the timetable slot
2. **Students not showing**: Verify students are enrolled in the class
3. **Date mismatch**: Ensure the date matches the timetable slot's day of week
4. **Permission denied**: Check user roles and authentication

### Database Queries for Debugging
```sql
-- Check teacher's timetable slots
SELECT ts.*, t.first_name, t.last_name, c.name as class_name, co.name as course_name
FROM timetable_slots ts
JOIN teacher t ON ts.teacher_id = t.id
LEFT JOIN classes c ON ts.for_class_id = c.id
LEFT JOIN courses co ON ts.for_course_id = co.id
WHERE t.id = {teacherId};

-- Check attendance records for a slot
SELECT a.*, u.first_name, u.last_name
FROM attendance a
JOIN users u ON a.user_id = u.id
WHERE a.timetable_slot_id = {slotId}
AND a.date = '{date}';
```

## Future Enhancements
1. **Mobile App**: Native mobile app for quick attendance marking
2. **QR Code Scanning**: Students scan QR codes to mark their own attendance
3. **Geolocation**: Verify teacher is in the classroom before marking attendance
4. **Parent Notifications**: Automatic notifications to parents for absent students
5. **Analytics Dashboard**: Advanced analytics and reporting for administrators
6. **Integration**: Integration with school management systems and parent portals
