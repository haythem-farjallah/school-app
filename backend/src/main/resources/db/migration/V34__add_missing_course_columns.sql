-- Add missing columns to courses table to match Course entity
-- V31__add_missing_course_columns.sql

-- Add code column (unique, not null)
ALTER TABLE courses ADD COLUMN IF NOT EXISTS code VARCHAR(20) UNIQUE;

-- Add description column  
ALTER TABLE courses ADD COLUMN IF NOT EXISTS description TEXT;

-- Add credit column (weight for final grade computation)
ALTER TABLE courses ADD COLUMN IF NOT EXISTS credit FLOAT;

-- Add credits column (credit hours) with default value
ALTER TABLE courses ADD COLUMN IF NOT EXISTS credits INTEGER DEFAULT 3;

-- Add duration_periods column with default value  
ALTER TABLE courses ADD COLUMN IF NOT EXISTS duration_periods INTEGER DEFAULT 1;

-- Add weekly_frequency column with default value
ALTER TABLE courses ADD COLUMN IF NOT EXISTS weekly_frequency INTEGER DEFAULT 3;

-- Add can_split column with default value
ALTER TABLE courses ADD COLUMN IF NOT EXISTS can_split BOOLEAN DEFAULT false;

-- Add preferred_periods column for JSON data
ALTER TABLE courses ADD COLUMN IF NOT EXISTS preferred_periods VARCHAR(500);

-- Update existing records with default values where null
UPDATE courses SET 
    code = 'COURSE_' || id,
    description = 'Course description for ' || COALESCE(name, 'Unknown Course'),
    credit = 1.0,
    credits = 3,
    duration_periods = 1,
    weekly_frequency = 3,
    can_split = false,
    preferred_periods = '[]'
WHERE code IS NULL OR description IS NULL OR credit IS NULL 
   OR credits IS NULL OR duration_periods IS NULL 
   OR weekly_frequency IS NULL OR can_split IS NULL 
   OR preferred_periods IS NULL;

-- Make code NOT NULL after setting default values
ALTER TABLE courses ALTER COLUMN code SET NOT NULL;