-- Fix Flyway migration issue
-- Run this manually in PostgreSQL to clean up the failed migration

-- Remove the failed migration from Flyway schema history
DELETE FROM flyway_schema_history WHERE version = '45';

-- Verify the removal
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;
