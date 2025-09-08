-- Debug script to check teacher classes data
-- Run this to see if there are teaching assignments for teachers

-- 1. Check all teachers
SELECT 
    t.id as teacher_id,
    u.first_name,
    u.last_name,
    u.email,
    u.role
FROM teacher t
JOIN users u ON t.id = u.id
WHERE u.role = 'TEACHER';

-- 2. Check teaching assignments
SELECT 
    ta.id,
    ta.teacher_id,
    ta.class_id,
    ta.course_id,
    ta.weekly_hours,
    u.email as teacher_email,
    c.name as class_name,
    co.name as course_name
FROM teaching_assignments ta
JOIN teacher t ON ta.teacher_id = t.id
JOIN users u ON t.id = u.id
JOIN classes c ON ta.class_id = c.id
JOIN courses co ON ta.course_id = co.id
ORDER BY u.email, c.name;

-- 3. Check direct class assignments (many-to-many)
SELECT 
    ct.teachers_id as teacher_id,
    ct.classes_id as class_id,
    u.email as teacher_email,
    c.name as class_name
FROM classes_teachers ct
JOIN teacher t ON ct.teachers_id = t.id
JOIN users u ON t.id = u.id
JOIN classes c ON ct.classes_id = c.id
ORDER BY u.email, c.name;

-- 4. Count assignments per teacher
SELECT 
    u.email,
    COUNT(ta.id) as teaching_assignments_count,
    COUNT(DISTINCT ta.class_id) as unique_classes_count
FROM teacher t
JOIN users u ON t.id = u.id
LEFT JOIN teaching_assignments ta ON t.id = ta.teacher_id
WHERE u.role = 'TEACHER'
GROUP BY t.id, u.email
ORDER BY u.email;
