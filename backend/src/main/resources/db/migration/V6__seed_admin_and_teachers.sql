/* V6__seed_admin_and_teachers.sql
   BCrypt hash for password = ChangeMe123 */
INSERT INTO users
(first_name, last_name, email, telephone,
 birthday, gender, address,
 password, role, password_change_required, status)
VALUES
    ('Alice','Admin','admin@school.test','+886-900-000-001',
     NULL,NULL,'1 Admin St',
     '$2a$10$1L9o3M5l3tQBvFecBiqzZ.C0esXSP5rq4NQ1xUPLcibm3flmYYA6K',
     'ADMIN',TRUE,'ACTIVE'),

    ('Tom','Teacher','tom.teacher@school.test','+886-900-000-002',
     '1985-05-05','M','2 Teacher Rd',
     '$2a$10$1L9o3M5l3tQBvFecBiqzZ.C0esXSP5rq4NQ1xUPLcibm3flmYYA6K',
     'TEACHER',TRUE,'ACTIVE'),

    ('Sara','Teacher','sara.teacher@school.test','+886-900-000-003',
     '1990-08-14','F','3 Teacher Rd',
     '$2a$10$1L9o3M5l3tQBvFecBiqzZ.C0esXSP5rq4NQ1xUPLcibm3flmYYA6K',
     'TEACHER',TRUE,'ACTIVE'),

    ('John','Teacher','john.teacher@school.test','+886-900-000-004',
     '1988-12-12','M','4 Teacher Rd',
     '$2a$10$1L9o3M5l3tQBvFecBiqzZ.C0esXSP5rq4NQ1xUPLcibm3flmYYA6K',
     'TEACHER',TRUE,'ACTIVE');

/* profile_settings */
INSERT INTO profile_settings(language,theme,notifications_enabled,dark_mode,user_id)
SELECT 'en','light',TRUE,FALSE,u.id
FROM users u
WHERE u.email IN (
                  'admin@school.test',
                  'tom.teacher@school.test',
                  'sara.teacher@school.test',
                  'john.teacher@school.test'
    );
