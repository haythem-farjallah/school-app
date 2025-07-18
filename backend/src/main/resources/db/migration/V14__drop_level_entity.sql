-- Drop level-related tables and relationships
DROP TABLE IF EXISTS level_courses CASCADE;
DROP TABLE IF EXISTS levels CASCADE;

-- Remove level_id from classes and resources
ALTER TABLE classes DROP COLUMN IF EXISTS level_id;
ALTER TABLE resources DROP COLUMN IF EXISTS level_id; 