-- Simple fix for enrollment status column
-- Convert enum to varchar without complex comparisons

DO $$
BEGIN
    -- Check if enrollments table exists
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'enrollments') THEN
        
        -- Check current column type
        IF EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'enrollments' 
                   AND column_name = 'status' 
                   AND udt_name = 'enrollment_status') THEN
            
            RAISE NOTICE 'Converting enrollment_status enum to varchar';
            
            -- Step 1: Add new varchar column
            ALTER TABLE enrollments ADD COLUMN status_new VARCHAR(20);
            
            -- Step 2: Copy data using a simple approach
            -- Use string representation of enum values
            UPDATE enrollments SET status_new = 
                CASE 
                    WHEN status IS NULL THEN 'PENDING'
                    ELSE status::text
                END;
            
            -- Step 3: Drop old column
            ALTER TABLE enrollments DROP COLUMN status;
            
            -- Step 4: Rename new column
            ALTER TABLE enrollments RENAME COLUMN status_new TO status;
            
            -- Step 5: Set constraints
            ALTER TABLE enrollments ALTER COLUMN status SET NOT NULL;
            ALTER TABLE enrollments ALTER COLUMN status SET DEFAULT 'PENDING';
            
            -- Step 6: Add check constraint
            ALTER TABLE enrollments ADD CONSTRAINT enrollments_status_check 
                CHECK (status IN ('PENDING', 'ACTIVE', 'COMPLETED', 'DROPPED', 'SUSPENDED'));
            
            RAISE NOTICE 'Successfully converted enrollment status to varchar';
            
        ELSE
            RAISE NOTICE 'Enrollment status column is not an enum or does not exist';
        END IF;
        
    ELSE
        RAISE NOTICE 'Enrollments table does not exist';
    END IF;
    
END $$;
