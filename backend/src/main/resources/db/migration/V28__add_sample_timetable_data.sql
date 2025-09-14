-- Add sample data for timetable testing
-- This ensures we have the necessary data to test timetable generation

-- Add sample periods if they don't exist
INSERT INTO periods (index_number, start_time, end_time) 
SELECT 1, '08:00:00', '09:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 1);

INSERT INTO periods (index_number, start_time, end_time) 
SELECT 2, '09:00:00', '10:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 2);

INSERT INTO periods (index_number, start_time, end_time) 
SELECT 3, '10:00:00', '11:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 3);

INSERT INTO periods (index_number, start_time, end_time) 
SELECT 4, '11:00:00', '12:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 4);

INSERT INTO periods (index_number, start_time, end_time) 
SELECT 5, '14:00:00', '15:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 5);

INSERT INTO periods (index_number, start_time, end_time) 
SELECT 6, '15:00:00', '16:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 6);

-- Add sample rooms if they don't exist
INSERT INTO rooms (name, capacity, room_type) 
SELECT 'Room 101', 30, 'CLASSROOM' WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE name = 'Room 101');

INSERT INTO rooms (name, capacity, room_type) 
SELECT 'Room 102', 25, 'CLASSROOM' WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE name = 'Room 102');

INSERT INTO rooms (name, capacity, room_type) 
SELECT 'Lab 201', 20, 'LABORATORY' WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE name = 'Lab 201');

INSERT INTO rooms (name, capacity, room_type) 
SELECT 'Auditorium A', 100, 'AUDITORIUM' WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE name = 'Auditorium A');

-- Add sample teachers if they don't exist (assuming some users exist)
-- First, ensure we have some users with TEACHER role
INSERT INTO users (first_name, last_name, email, password, role, status, is_email_verified)
SELECT 'John', 'Doe', 'john.doe@school.com', '$2a$10$dummy.hash.for.testing', 'TEACHER', 'ACTIVE', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'john.doe@school.com');

INSERT INTO users (first_name, last_name, email, password, role, status, is_email_verified)
SELECT 'Jane', 'Smith', 'jane.smith@school.com', '$2a$10$dummy.hash.for.testing', 'TEACHER', 'ACTIVE', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'jane.smith@school.com');

INSERT INTO users (first_name, last_name, email, password, role, status, is_email_verified)
SELECT 'Bob', 'Johnson', 'bob.johnson@school.com', '$2a$10$dummy.hash.for.testing', 'TEACHER', 'ACTIVE', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'bob.johnson@school.com');

-- Now add teacher records for these users
INSERT INTO teacher (id, qualifications, weekly_capacity)
SELECT u.id, 'Bachelor in Education', 20
FROM users u 
WHERE u.role = 'TEACHER' 
AND NOT EXISTS (SELECT 1 FROM teacher WHERE id = u.id)
LIMIT 3;

-- Add sample classes if they don't exist (removed level_id dependency)
INSERT INTO classes (name, year_of_study, max_students)
SELECT 'Class 10A', 10, 30 WHERE NOT EXISTS (SELECT 1 FROM classes WHERE name = 'Class 10A');

INSERT INTO classes (name, year_of_study, max_students)
SELECT 'Class 9B', 9, 25 WHERE NOT EXISTS (SELECT 1 FROM classes WHERE name = 'Class 9B');

INSERT INTO classes (name, year_of_study, max_students)
SELECT 'Class 8C', 8, 28 WHERE NOT EXISTS (SELECT 1 FROM classes WHERE name = 'Class 8C');

-- Add sample courses if they don't exist
INSERT INTO courses (name, color, credit, teacher_id)
SELECT 'Mathematics', '#FF6B6B', 4.0, t.id
FROM teacher t 
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = 'Mathematics')
LIMIT 1;

INSERT INTO courses (name, color, credit, teacher_id)
SELECT 'Physics', '#4ECDC4', 3.5, t.id
FROM teacher t 
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = 'Physics')
LIMIT 1;

INSERT INTO courses (name, color, credit, teacher_id)
SELECT 'English', '#45B7D1', 3.0, t.id
FROM teacher t 
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = 'English')
LIMIT 1;

-- Link courses to classes
INSERT INTO class_courses (class_id, course_id)
SELECT c.id, co.id
FROM classes c, courses co
WHERE c.name = 'Class 10A' AND co.name IN ('Mathematics', 'Physics', 'English')
AND NOT EXISTS (SELECT 1 FROM class_courses WHERE class_id = c.id AND course_id = co.id);

INSERT INTO class_courses (class_id, course_id)
SELECT c.id, co.id
FROM classes c, courses co
WHERE c.name = 'Class 9B' AND co.name IN ('Mathematics', 'English')
AND NOT EXISTS (SELECT 1 FROM class_courses WHERE class_id = c.id AND course_id = co.id);

INSERT INTO class_courses (class_id, course_id)
SELECT c.id, co.id
FROM classes c, courses co
WHERE c.name = 'Class 8C' AND co.name IN ('Mathematics')
AND NOT EXISTS (SELECT 1 FROM class_courses WHERE class_id = c.id AND course_id = co.id);