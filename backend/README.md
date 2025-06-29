# School Management System - Entity Class Diagram

This document provides a comprehensive overview of the entity relationships in the School Management System.

## Authentication and User Management

### Base User Entity
```mermaid
classDiagram
    class BaseUser {
        +Long id
        +String email
        +String password
        +String firstName
        +String lastName
        +String phoneNumber
        +String address
        +String profilePicture
        +Boolean enabled
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }
```

### User Types
```mermaid
classDiagram
    BaseUser <|-- Student
    BaseUser <|-- Teacher
    BaseUser <|-- Parent
    BaseUser <|-- Worker
    BaseUser <|-- Administration
    
    class Student {
        +String studentId
        +LocalDate dateOfBirth
        +String gender
        +Parent parent
    }
    
    class Teacher {
        +String teacherId
        +String specialization
    }
    
    class Parent {
        +String parentId
        +List<Student> children
    }
    
    class Worker {
        +String workerId
        +String position
    }
    
    class Administration {
        +String adminId
        +String role
    }
```

### Role and Permission Management
```mermaid
classDiagram
    class Permission {
        +Long id
        +String name
        +String description
    }
    
    class RolePermission {
        +Long id
        +String roleName
        +List<Permission> permissions
    }
```

## Academic Management

### Academic Structure
```mermaid
classDiagram
    class Level {
        +Long id
        +String name
        +String description
        +List<ClassEntity> classes
    }
    
    class ClassEntity {
        +Long id
        +String name
        +Level level
        +List<Student> students
        +List<TeachingAssignment> teachingAssignments
    }
    
    class Course {
        +Long id
        +String name
        +String description
        +Level level
    }
    
    class TeachingAssignment {
        +Long id
        +Teacher teacher
        +ClassEntity classEntity
        +Course course
    }
```

### Schedule and Resources
```mermaid
classDiagram
    class Schedule {
        +Long id
        +LocalDateTime startTime
        +LocalDateTime endTime
        +TeachingAssignment teachingAssignment
    }
    
    class Resource {
        +Long id
        +String name
        +String type
        +String url
        +Course course
    }
```

## Operational Management

### Academic Operations
```mermaid
classDiagram
    class Semester {
        +Long id
        +String name
        +LocalDate startDate
        +LocalDate endDate
    }
    
    class SubjectGrade {
        +Long id
        +Student student
        +Course course
        +Double grade
        +Semester semester
    }
    
    class Bulletin {
        +Long id
        +Student student
        +Semester semester
        +List<SubjectGrade> grades
    }
```

### Attendance and Notes
```mermaid
classDiagram
    class Attendance {
        +Long id
        +Student student
        +LocalDate date
        +Boolean present
        +String remarks
    }
    
    class Note {
        +Long id
        +Student student
        +Teacher teacher
        +String content
        +LocalDateTime createdAt
    }
```

### Communication
```mermaid
classDiagram
    class Notification {
        +Long id
        +String title
        +String message
        +LocalDateTime createdAt
        +List<BaseUser> recipients
    }
    
    class Timetable {
        +Long id
        +ClassEntity classEntity
        +LocalDate date
        +List<Schedule> schedules
    }
```

### User Settings
```mermaid
classDiagram
    class ProfileSettings {
        +Long id
        +BaseUser user
        +String theme
        +String language
        +Boolean emailNotifications
    }
```

## Relationships Overview

1. **User Hierarchy**:
   - All user types (Student, Teacher, Parent, Worker, Administration) inherit from BaseUser
   - Parent has a one-to-many relationship with Student
   - Teacher has many-to-many relationship with ClassEntity through TeachingAssignment

2. **Academic Structure**:
   - Level contains multiple ClassEntity
   - ClassEntity contains multiple Students
   - Course is associated with Level
   - TeachingAssignment connects Teacher, ClassEntity, and Course

3. **Operational Structure**:
   - Semester organizes academic periods
   - SubjectGrade connects Student, Course, and Semester
   - Bulletin aggregates SubjectGrades for a Student in a Semester
   - Attendance tracks Student presence
   - Note allows Teacher to Student communication
   - Notification can be sent to multiple users
   - Timetable organizes Schedules for a ClassEntity

4. **Settings and Permissions**:
   - ProfileSettings is associated with BaseUser
   - RolePermission manages user access through Permissions 