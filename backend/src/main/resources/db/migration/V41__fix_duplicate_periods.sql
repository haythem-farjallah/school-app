-- Fix duplicate periods issue
-- This migration will clean up duplicate periods and establish a consistent schedule

-- First, backup any existing timetable slots that reference periods
CREATE TEMP TABLE temp_timetable_slots AS 
SELECT * FROM timetable_slots;

-- Delete all timetable slots temporarily to avoid foreign key constraints
DELETE FROM timetable_slots;

-- Delete all existing periods
DELETE FROM periods;

-- Create a clean, consistent set of periods for a school day
-- 8 periods from 8:00 AM to 5:00 PM with proper breaks
INSERT INTO periods (index_number, start_time, end_time) VALUES
(1, '08:00:00', '08:50:00'),  -- Period 1: 8:00-8:50 AM
(2, '09:00:00', '09:50:00'),  -- Period 2: 9:00-9:50 AM  
(3, '10:00:00', '10:50:00'),  -- Period 3: 10:00-10:50 AM
(4, '11:00:00', '11:50:00'),  -- Period 4: 11:00-11:50 AM
(5, '13:00:00', '13:50:00'),  -- Period 5: 1:00-1:50 PM (after lunch)
(6, '14:00:00', '14:50:00'),  -- Period 6: 2:00-2:50 PM
(7, '15:00:00', '15:50:00'),  -- Period 7: 3:00-3:50 PM
(8, '16:00:00', '16:50:00');  -- Period 8: 4:00-4:50 PM

-- Restore timetable slots with updated period references
-- Map old period IDs to new ones based on index_number
INSERT INTO timetable_slots (day_of_week, description, period_id, for_class_id, for_course_id, teacher_id, room_id, timetable_id, created_at, updated_at)
SELECT 
    ts.day_of_week,
    ts.description,
    p.id as new_period_id,
    ts.for_class_id,
    ts.for_course_id,
    ts.teacher_id,
    ts.room_id,
    ts.timetable_id,
    ts.created_at,
    ts.updated_at
FROM temp_timetable_slots ts
JOIN periods p ON p.index_number = (
    SELECT index_number 
    FROM periods old_p 
    WHERE old_p.id = ts.period_id 
    LIMIT 1
)
WHERE ts.period_id IS NOT NULL;

-- Drop the temporary table
DROP TABLE temp_timetable_slots;

-- Add a unique constraint to prevent future duplicates
ALTER TABLE periods ADD CONSTRAINT unique_period_index UNIQUE (index_number);
