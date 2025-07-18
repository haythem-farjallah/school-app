-- Fix timetables table user_id column issue
-- The original timetables table had user_id NOT NULL, but the new design doesn't use user-specific timetables
-- Instead, timetables are institutional and associated with classes, teachers, and rooms

-- First, check if there are any existing records and handle them
-- If there are existing records with user_id, we need to preserve them or migrate them appropriately

-- Option 1: Make user_id nullable to support both old and new designs
-- This allows backward compatibility while supporting the new institutional timetables
ALTER TABLE timetables ALTER COLUMN user_id DROP NOT NULL;

-- Option 2: If you want to completely remove user_id (uncomment the line below instead)
-- ALTER TABLE timetables DROP COLUMN user_id;

-- Add a comment to document the change
COMMENT ON COLUMN timetables.user_id IS 'Legacy column - nullable for backward compatibility. New timetables are institutional and use class/teacher/room associations.'; 