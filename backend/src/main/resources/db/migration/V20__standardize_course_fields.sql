-- Standardize course table fields to match entity requirements
-- Change coefficient to credit and add weeklyCapacity

-- Add weeklyCapacity column
ALTER TABLE courses ADD COLUMN IF NOT EXISTS weekly_capacity INTEGER DEFAULT 3;

-- Rename coefficient to credit (if coefficient exists)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'courses' AND column_name = 'coefficient') THEN
        ALTER TABLE courses RENAME COLUMN coefficient TO credit;
    END IF;
END $$;

-- Update credit column type to FLOAT if it's not already
ALTER TABLE courses ALTER COLUMN credit TYPE FLOAT USING credit::FLOAT;

-- Add default value for credit if not exists
ALTER TABLE courses ALTER COLUMN credit SET DEFAULT 1.0;

-- Add indexes for better performance
CREATE INDEX IF NOT EXISTS idx_courses_credit ON courses(credit);
CREATE INDEX IF NOT EXISTS idx_courses_weekly_capacity ON courses(weekly_capacity);
CREATE INDEX IF NOT EXISTS idx_courses_teacher ON courses(teacher_id); 