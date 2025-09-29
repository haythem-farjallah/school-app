-- Fix teacher login issue by updating password_change_required flag
-- This allows teachers to login with their default password without being forced to change it

-- Update existing teacher accounts to not require password change
UPDATE users 
SET password_change_required = FALSE 
WHERE role = 'TEACHER' 
AND password_change_required = TRUE;

-- Also ensure teacher records exist for all teacher users
INSERT INTO teacher (id, qualifications, subjects_taught, weekly_capacity, schedule_preferences)
SELECT u.id, 'Master in Education', 'General Subjects', 20, 'Flexible'
FROM users u 
WHERE u.role = 'TEACHER' 
AND NOT EXISTS (SELECT 1 FROM teacher WHERE id = u.id);

-- Log the changes
SELECT 
    u.email,
    u.first_name,
    u.last_name,
    u.role,
    u.password_change_required,
    CASE WHEN t.id IS NOT NULL THEN 'Teacher record exists' ELSE 'No teacher record' END as teacher_status
FROM users u
LEFT JOIN teacher t ON u.id = t.id
WHERE u.role = 'TEACHER';
