-- Fix: Add STUDENT_READ permission to STUDENT role
-- This allows students to access their own dashboard

INSERT INTO role_permissions (role, permission_id)
SELECT 'STUDENT', id FROM permissions
WHERE code = 'STUDENT_READ'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role = 'STUDENT' AND rp.permission_id = (
        SELECT id FROM permissions WHERE code = 'STUDENT_READ'
    )
);
