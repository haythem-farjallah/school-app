-- Enhance attendance system for teacher functionality
-- This migration ensures the attendance system is properly configured for teachers

-- Ensure UserType enum includes all necessary values
DO $$
BEGIN
    -- Check if the user_type enum exists and has the required values
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_type') THEN
        CREATE TYPE user_type AS ENUM ('STUDENT', 'TEACHER', 'STAFF', 'PARENT', 'ADMIN');
    ELSE
        -- Add missing enum values if they don't exist
        BEGIN
            ALTER TYPE user_type ADD VALUE IF NOT EXISTS 'STUDENT';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE user_type ADD VALUE IF NOT EXISTS 'TEACHER';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE user_type ADD VALUE IF NOT EXISTS 'STAFF';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE user_type ADD VALUE IF NOT EXISTS 'PARENT';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE user_type ADD VALUE IF NOT EXISTS 'ADMIN';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
    END IF;
END$$;

-- Ensure AttendanceStatus enum includes all necessary values
DO $$
BEGIN
    -- Check if the attendance_status enum exists and has the required values
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'attendance_status') THEN
        CREATE TYPE attendance_status AS ENUM ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED', 'SICK_LEAVE', 'MEDICAL_LEAVE', 'OTHER');
    ELSE
        -- Add missing enum values if they don't exist
        BEGIN
            ALTER TYPE attendance_status ADD VALUE IF NOT EXISTS 'PRESENT';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE attendance_status ADD VALUE IF NOT EXISTS 'ABSENT';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE attendance_status ADD VALUE IF NOT EXISTS 'LATE';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE attendance_status ADD VALUE IF NOT EXISTS 'EXCUSED';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE attendance_status ADD VALUE IF NOT EXISTS 'SICK_LEAVE';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE attendance_status ADD VALUE IF NOT EXISTS 'MEDICAL_LEAVE';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
        
        BEGIN
            ALTER TYPE attendance_status ADD VALUE IF NOT EXISTS 'OTHER';
        EXCEPTION WHEN duplicate_object THEN NULL;
        END;
    END IF;
END$$;

-- Add indexes for better performance on teacher-specific queries
CREATE INDEX IF NOT EXISTS idx_attendance_teacher_date 
ON attendance(recorded_by_id, date) 
WHERE user_type = 'STUDENT';

CREATE INDEX IF NOT EXISTS idx_attendance_slot_teacher_date 
ON attendance(timetable_slot_id, date) 
WHERE user_type = 'STUDENT';

-- Add a composite index for teacher attendance queries
CREATE INDEX IF NOT EXISTS idx_attendance_teacher_slot_date_status 
ON attendance(timetable_slot_id, date, status) 
WHERE user_type = 'STUDENT';

-- Ensure the attendance table has all necessary columns
ALTER TABLE attendance 
ADD COLUMN IF NOT EXISTS user_type user_type DEFAULT 'STUDENT',
ADD COLUMN IF NOT EXISTS status attendance_status DEFAULT 'PRESENT';

-- Update existing records to have proper user_type 
-- Set default user_type to STUDENT for existing records
UPDATE attendance 
SET user_type = 'STUDENT' 
WHERE user_type IS NULL;

-- Ensure status has default values for existing records
UPDATE attendance 
SET status = 'PRESENT' 
WHERE status IS NULL;

-- Add a comment to document the teacher attendance functionality
COMMENT ON TABLE attendance IS 'Attendance records for students, teachers, and staff. Enhanced for teacher-specific attendance marking functionality.';
COMMENT ON COLUMN attendance.timetable_slot_id IS 'Links attendance to specific timetable slots for teacher-based marking';
COMMENT ON COLUMN attendance.recorded_by_id IS 'Teacher or staff member who recorded the attendance';
COMMENT ON COLUMN attendance.user_type IS 'Type of user for whom attendance is being recorded';
