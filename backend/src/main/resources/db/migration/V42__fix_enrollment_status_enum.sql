-- Ensure enrollment_status enum exists and is properly configured
DO $$
BEGIN
    -- Check if the enum type exists, if not create it
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'enrollment_status') THEN
        CREATE TYPE enrollment_status AS ENUM ('PENDING', 'ACTIVE', 'COMPLETED', 'DROPPED', 'SUSPENDED');
    END IF;
    
    -- Ensure the enrollments table status column uses the enum type
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'enrollments' AND column_name = 'status' 
               AND data_type != 'USER-DEFINED') THEN
        -- Convert the column to use the enum type
        ALTER TABLE enrollments ALTER COLUMN status TYPE enrollment_status USING status::enrollment_status;
    END IF;
END $$;
