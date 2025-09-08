-- Fix test teacher issue by creating the teacher and assigning classes

-- 1. Create the test teacher user if it doesn't exist
INSERT INTO users (first_name, last_name, email, password, role, status, is_email_verified)
SELECT 'Test', 'Teacher', 'testTeacher@yopmail.com', 
       '$2a$10$1L9o3M5l3tQBvFecBiqzZ.C0esXSP5rq4NQ1xUPLcibm3flmYYA6K', -- password: ChangeMe123
       'TEACHER', 'ACTIVE', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'testTeacher@yopmail.com');

-- 2. Create the teacher record
INSERT INTO teacher (id, qualifications, subjects_taught, weekly_capacity, schedule_preferences)
SELECT u.id, 'Master in Education', 'Mathematics, Physics', 20, 'Morning preferred'
FROM users u 
WHERE u.email = 'testTeacher@yopmail.com'
AND NOT EXISTS (SELECT 1 FROM teacher WHERE id = u.id);

-- 3. Create some sample classes if they don't exist
INSERT INTO classes (name, grade_level, capacity, weekly_hours)
SELECT 'Test Class 1A', 'First Year', 30, 25
WHERE NOT EXISTS (SELECT 1 FROM classes WHERE name = 'Test Class 1A');

INSERT INTO classes (name, grade_level, capacity, weekly_hours)
SELECT 'Test Class 2B', 'Second Year', 28, 25
WHERE NOT EXISTS (SELECT 1 FROM classes WHERE name = 'Test Class 2B');

-- 4. Create some sample courses if they don't exist
INSERT INTO courses (name, code, credit, teacher_id)
SELECT 'Mathematics', 'MATH101', 3.0, t.id
FROM teacher t 
JOIN users u ON t.id = u.id
WHERE u.email = 'testTeacher@yopmail.com'
AND NOT EXISTS (SELECT 1 FROM courses WHERE code = 'MATH101');

INSERT INTO courses (name, code, credit, teacher_id)
SELECT 'Physics', 'PHYS101', 3.0, t.id
FROM teacher t 
JOIN users u ON t.id = u.id
WHERE u.email = 'testTeacher@yopmail.com'
AND NOT EXISTS (SELECT 1 FROM courses WHERE code = 'PHYS101');

-- 5. Assign courses to classes (many-to-many)
INSERT INTO class_courses (class_id, course_id)
SELECT c.id, co.id
FROM classes c, courses co
WHERE c.name = 'Test Class 1A' 
AND co.code = 'MATH101'
AND NOT EXISTS (
    SELECT 1 FROM class_courses cc 
    WHERE cc.class_id = c.id AND cc.course_id = co.id
);

INSERT INTO class_courses (class_id, course_id)
SELECT c.id, co.id
FROM classes c, courses co
WHERE c.name = 'Test Class 2B' 
AND co.code = 'PHYS101'
AND NOT EXISTS (
    SELECT 1 FROM class_courses cc 
    WHERE cc.class_id = c.id AND cc.course_id = co.id
);

-- 6. Create teaching assignments
INSERT INTO teaching_assignments (class_id, course_id, teacher_id, weekly_hours)
SELECT c.id, co.id, t.id, 4
FROM classes c, courses co, teacher t
JOIN users u ON t.id = u.id
WHERE c.name = 'Test Class 1A' 
AND co.code = 'MATH101'
AND u.email = 'testTeacher@yopmail.com'
AND NOT EXISTS (
    SELECT 1 FROM teaching_assignments ta 
    WHERE ta.class_id = c.id AND ta.course_id = co.id AND ta.teacher_id = t.id
);

INSERT INTO teaching_assignments (class_id, course_id, teacher_id, weekly_hours)
SELECT c.id, co.id, t.id, 3
FROM classes c, courses co, teacher t
JOIN users u ON t.id = u.id
WHERE c.name = 'Test Class 2B' 
AND co.code = 'PHYS101'
AND u.email = 'testTeacher@yopmail.com'
AND NOT EXISTS (
    SELECT 1 FROM teaching_assignments ta 
    WHERE ta.class_id = c.id AND ta.course_id = co.id AND ta.teacher_id = t.id
);

-- 7. Also add direct class-teacher relationships (alternative approach)
INSERT INTO class_teachers (class_id, teacher_id)
SELECT c.id, t.id
FROM classes c, teacher t
JOIN users u ON t.id = u.id
WHERE c.name IN ('Test Class 1A', 'Test Class 2B')
AND u.email = 'testTeacher@yopmail.com'
AND NOT EXISTS (
    SELECT 1 FROM class_teachers ct 
    WHERE ct.class_id = c.id AND ct.teacher_id = t.id
);

-- 8. Add some students to the classes to make them more realistic
INSERT INTO users (first_name, last_name, email, password, role, status, is_email_verified)
SELECT 'Student', '1', 'student1@test.com', 
       '$2a$10$1L9o3M5l3tQBvFecBiqzZ.C0esXSP5rq4NQ1xUPLcibm3flmYYA6K',
       'STUDENT', 'ACTIVE', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'student1@test.com');

INSERT INTO student (id, grade_level, enrolled_at)
SELECT u.id, 'First Year', CURRENT_TIMESTAMP
FROM users u 
WHERE u.email = 'student1@test.com'
AND NOT EXISTS (SELECT 1 FROM student WHERE id = u.id);

-- Link students to classes
INSERT INTO class_students (class_id, student_id)
SELECT c.id, s.id
FROM classes c, student s
JOIN users u ON s.id = u.id
WHERE c.name = 'Test Class 1A'
AND u.email = 'student1@test.com'
AND NOT EXISTS (
    SELECT 1 FROM class_students cs 
    WHERE cs.class_id = c.id AND cs.student_id = s.id
);

COMMIT;
