-- Modify users table
ALTER TABLE users ADD COLUMN is_email_verified BOOLEAN DEFAULT FALSE;

-- Modify student table
ALTER TABLE student DROP COLUMN IF EXISTS enrollment_year;
ALTER TABLE student ADD COLUMN enrolled_at TIMESTAMP;

-- Fix grade_level data before converting to enum using PostgreSQL CASE statement
UPDATE student SET grade_level = 
    CASE 
        WHEN grade_level IN ('G-4', 'G-5', 'G-6') THEN 'ELEMENTARY'
        WHEN grade_level IN ('G-7', 'G-8') THEN 'MIDDLE'
        WHEN grade_level IN ('G-9', 'G-10', 'G-11', 'G-12') THEN 'HIGH'
        ELSE 'ELEMENTARY'  -- Default fallback
    END
WHERE grade_level IS NOT NULL;

-- Now convert to enum type using PostgreSQL casting
ALTER TABLE student ALTER COLUMN grade_level TYPE grade_level USING grade_level::text::grade_level;

-- Modify teacher table
ALTER TABLE teacher RENAME COLUMN available_hours TO weekly_capacity;

-- Modify parent table
ALTER TABLE parent ALTER COLUMN preferred_contact_method TYPE contact_method USING preferred_contact_method::text::contact_method;
ALTER TABLE parent ADD COLUMN relation relation;

-- Rename worker to staff and modify
ALTER TABLE worker RENAME TO staff;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS staff_type staff_type;

-- Modify courses table
ALTER TABLE courses RENAME COLUMN coefficient TO credit;
ALTER TABLE courses ALTER COLUMN credit TYPE FLOAT;

-- Modify classes table
ALTER TABLE classes ADD COLUMN year_of_study INTEGER;
ALTER TABLE classes ADD COLUMN max_students INTEGER;

-- Modify resources table
ALTER TABLE resources ADD COLUMN type resource_type;
ALTER TABLE resources ADD COLUMN thumbnail_url VARCHAR(500);
ALTER TABLE resources ADD COLUMN duration INTEGER;
ALTER TABLE resources ADD COLUMN status VARCHAR(50);
ALTER TABLE resources ADD COLUMN target_grade_level grade_level;

-- Modify notifications table
ALTER TABLE notifications ADD COLUMN read BOOLEAN DEFAULT FALSE; 