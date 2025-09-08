-- Ensure enrollment status column is properly configured for ColumnTransformer
-- This migration ensures the enrollment_status column works correctly with Hibernate's ColumnTransformer

DO $$
BEGIN
    -- Ensure the enrollment_status enum exists with all required values
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'enrollment_status') THEN
        CREATE TYPE enrollment_status AS ENUM ('PENDING', 'ACTIVE', 'COMPLETED', 'DROPPED', 'SUSPENDED');
        RAISE NOTICE 'Created enrollment_status enum type';
    ELSE
        RAISE NOTICE 'enrollment_status enum type already exists';
    END IF;
    
    -- Ensure the enrollments table exists
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'enrollments') THEN
        RAISE EXCEPTION 'enrollments table does not exist';
    END IF;
    
    -- Check if the status column exists and has the correct type
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'enrollments' AND column_name = 'status') THEN
        
        -- Get the current data type of the status column
        DECLARE
            current_data_type TEXT;
        BEGIN
            SELECT data_type INTO current_data_type 
            FROM information_schema.columns 
            WHERE table_name = 'enrollments' AND column_name = 'status';
            
            -- If it's not already using the enum type, convert it
            IF current_data_type != 'USER-DEFINED' THEN
                RAISE NOTICE 'Converting status column from % to enrollment_status enum', current_data_type;
                
                -- First, ensure all existing values are valid enum values
                UPDATE enrollments 
                SET status = 'PENDING' 
                WHERE status NOT IN ('PENDING', 'ACTIVE', 'COMPLETED', 'DROPPED', 'SUSPENDED')
                   OR status IS NULL;
                
                -- Convert the column to use the enum type
                ALTER TABLE enrollments 
                ALTER COLUMN status TYPE enrollment_status USING status::enrollment_status;
                
                RAISE NOTICE 'Successfully converted status column to enrollment_status enum';
            ELSE
                RAISE NOTICE 'status column already uses enrollment_status enum type';
            END IF;
        END;
    ELSE
        RAISE EXCEPTION 'status column does not exist in enrollments table';
    END IF;
    
    -- Ensure the column has a proper default value
    ALTER TABLE enrollments 
    ALTER COLUMN status SET DEFAULT 'PENDING'::enrollment_status;
    
    -- Add a check constraint to ensure only valid enum values (redundant but good practice)
    IF NOT EXISTS (SELECT 1 FROM information_schema.check_constraints 
                   WHERE constraint_name = 'enrollments_status_check') THEN
        ALTER TABLE enrollments 
        ADD CONSTRAINT enrollments_status_check 
        CHECK (status IN ('PENDING', 'ACTIVE', 'COMPLETED', 'DROPPED', 'SUSPENDED'));
        
        RAISE NOTICE 'Added check constraint for enrollment status';
    END IF;
    
    RAISE NOTICE 'Enrollment status column configuration completed successfully';
    
END $$;
