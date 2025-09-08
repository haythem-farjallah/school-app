-- Add new columns to courses table for enhanced scheduling
ALTER TABLE courses 
ADD COLUMN duration_periods INTEGER DEFAULT 1,
ADD COLUMN weekly_frequency INTEGER DEFAULT 3,
ADD COLUMN can_split BOOLEAN DEFAULT false,
ADD COLUMN preferred_periods TEXT;

-- Update existing courses with realistic scheduling data
-- Mathematics courses (2 periods, 3 times per week)
UPDATE courses SET 
    duration_periods = 2, 
    weekly_frequency = 3,
    preferred_periods = '["1,2", "3,4", "5,6"]'
WHERE LOWER(name) LIKE '%math%' OR LOWER(name) LIKE '%algebra%' OR LOWER(name) LIKE '%geometry%';

-- Science courses (2 periods, 2 times per week)
UPDATE courses SET 
    duration_periods = 2, 
    weekly_frequency = 2,
    preferred_periods = '["1,2", "3,4"]'
WHERE LOWER(name) LIKE '%physics%' OR LOWER(name) LIKE '%chemistry%' OR LOWER(name) LIKE '%biology%';

-- Language courses (1 period, 4 times per week)
UPDATE courses SET 
    duration_periods = 1, 
    weekly_frequency = 4,
    can_split = true
WHERE LOWER(name) LIKE '%english%' OR LOWER(name) LIKE '%arabic%' OR LOWER(name) LIKE '%french%';

-- History and Geography (1 period, 2 times per week)
UPDATE courses SET 
    duration_periods = 1, 
    weekly_frequency = 2
WHERE LOWER(name) LIKE '%history%' OR LOWER(name) LIKE '%geography%';

-- Physical Education (1 period, 2 times per week)
UPDATE courses SET 
    duration_periods = 1, 
    weekly_frequency = 2
WHERE LOWER(name) LIKE '%sport%' OR LOWER(name) LIKE '%physical%' OR LOWER(name) LIKE '%gym%';

-- Art and Music (1 period, 1 time per week)
UPDATE courses SET 
    duration_periods = 1, 
    weekly_frequency = 1
WHERE LOWER(name) LIKE '%art%' OR LOWER(name) LIKE '%music%' OR LOWER(name) LIKE '%drawing%';

-- Set default subjects for all teachers to enable smart assignment
UPDATE teacher SET subjects_taught = 'General Education, Multiple Subjects' 
WHERE subjects_taught IS NULL OR subjects_taught = '';

-- Add some comments for clarity
COMMENT ON COLUMN courses.duration_periods IS 'Number of consecutive periods for this course (1=single, 2=double, etc.)';
COMMENT ON COLUMN courses.weekly_frequency IS 'How many times per week this course should be scheduled';
COMMENT ON COLUMN courses.can_split IS 'Whether this course can be split across different days';
COMMENT ON COLUMN courses.preferred_periods IS 'JSON array of preferred period combinations';