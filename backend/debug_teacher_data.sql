-- Debug script to check teacher data for testTeacher@yopmail.com (ID: 322)

-- 1. Check the teacher record
SELECT 
    t.id as teacher_id,
    u.first_name,
    u.last_name,
    u.email,
    u.role
FROM teacher t
JOIN users u ON t.id = u.id
WHERE u.email = 'testTeacher@yopmail.com';

-- 2. Check teaching assignments for this teacher
SELECT 
    ta.id,
    ta.teacher_id,
    ta.class_id,
    ta.course_id,
    ta.weekly_hours,
    c.name as class_name,
    co.name as course_name
FROM teaching_assignments ta
JOIN classes c ON ta.class_id = c.id
JOIN courses co ON ta.course_id = co.id
WHERE ta.teacher_id = 322;

-- 3. Check direct class assignments (class_teachers junction table)
SELECT 
    ct.teacher_id,
    ct.class_id,
    c.name as class_name,
    c.grade_level
FROM class_teachers ct
JOIN classes c ON ct.class_id = c.id
WHERE ct.teacher_id = 322;

-- 4. Check if there are any classes with this teacher in the classes.teachers many-to-many
-- This would be through the class_teachers junction table
SELECT 
    'Direct assignment' as assignment_type,
    c.id as class_id,
    c.name as class_name,
    c.grade_level,
    c.capacity,
    (SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id) as enrolled_count
FROM classes c
JOIN class_teachers ct ON c.id = ct.class_id
WHERE ct.teacher_id = 322

UNION ALL

SELECT 
    'Teaching assignment' as assignment_type,
    c.id as class_id,
    c.name as class_name,
    c.grade_level,
    c.capacity,
    (SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id) as enrolled_count
FROM classes c
JOIN teaching_assignments ta ON c.id = ta.class_id
WHERE ta.teacher_id = 322;

-- 5. Check all possible ways this teacher could be linked to classes
SELECT 'Summary' as info, COUNT(*) as count FROM (
    SELECT DISTINCT c.id
    FROM classes c
    JOIN class_teachers ct ON c.id = ct.class_id
    WHERE ct.teacher_id = 322
    
    UNION
    
    SELECT DISTINCT c.id
    FROM classes c
    JOIN teaching_assignments ta ON c.id = ta.class_id
    WHERE ta.teacher_id = 322
) all_classes;
