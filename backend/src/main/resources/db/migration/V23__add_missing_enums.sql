-- Add missing enum types
CREATE TYPE user_role AS ENUM ('ADMIN', 'TEACHER', 'STUDENT', 'PARENT', 'STAFF');

-- Create status enum if it doesn't exist (it should be user_status based on V5 migration)
-- But the entity is expecting just 'status', so we'll create it
CREATE TYPE status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');

-- First, update any invalid role values to a valid default
UPDATE users SET role = 'STAFF' WHERE role = 'WORKER' OR role NOT IN ('ADMIN', 'TEACHER', 'STUDENT', 'PARENT', 'STAFF');

-- Update any invalid status values to a valid default
UPDATE users SET status = 'ACTIVE' WHERE status NOT IN ('ACTIVE', 'SUSPENDED', 'DELETED');

-- Now safely update the users table to use the enum types
ALTER TABLE users 
    ALTER COLUMN role TYPE user_role USING role::user_role;

-- Handle the status column with its default value
ALTER TABLE users 
    ALTER COLUMN status DROP DEFAULT;
    
ALTER TABLE users 
    ALTER COLUMN status TYPE status USING status::status;
    
ALTER TABLE users 
    ALTER COLUMN status SET DEFAULT 'ACTIVE'::status; 