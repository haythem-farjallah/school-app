-- Add missing columns to classes table to match ClassEntity
-- V30__add_missing_class_columns.sql

-- Add section column (A, B, C, etc.)
ALTER TABLE classes ADD COLUMN IF NOT EXISTS section VARCHAR(10);

-- Add academic_year column (e.g., "2024-2025")
ALTER TABLE classes ADD COLUMN IF NOT EXISTS academic_year VARCHAR(20);

-- Add capacity column with default value
ALTER TABLE classes ADD COLUMN IF NOT EXISTS capacity INTEGER DEFAULT 30;

-- Add grade_level column for the class
ALTER TABLE classes ADD COLUMN IF NOT EXISTS grade_level VARCHAR(50);

-- Add weekly_hours column with default value
ALTER TABLE classes ADD COLUMN IF NOT EXISTS weekly_hours INTEGER DEFAULT 30;

-- Update existing records with default values where null
UPDATE classes SET 
    section = 'A',
    academic_year = '2024-2025',
    capacity = 30,
    grade_level = 'First Year',
    weekly_hours = 30
WHERE section IS NULL OR academic_year IS NULL OR capacity IS NULL OR grade_level IS NULL OR weekly_hours IS NULL;