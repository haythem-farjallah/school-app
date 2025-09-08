-- Clean up enrollment data to ensure consistency with enum values
-- This migration ensures all existing enrollment records have valid status values

DO $$
BEGIN
    -- Check if enrollments table exists and has data
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'enrollments') THEN
        
        -- Count existing enrollments
        DECLARE
            enrollment_count INTEGER;
            invalid_count INTEGER;
        BEGIN
            SELECT COUNT(*) INTO enrollment_count FROM enrollments;
            RAISE NOTICE 'Found % enrollment records', enrollment_count;
            
            -- Count records with invalid or null status
            SELECT COUNT(*) INTO invalid_count 
            FROM enrollments 
            WHERE status IS NULL 
               OR status NOT IN ('PENDING', 'ACTIVE', 'COMPLETED', 'DROPPED', 'SUSPENDED');
            
            IF invalid_count > 0 THEN
                RAISE NOTICE 'Found % enrollment records with invalid status values', invalid_count;
                
                -- Update invalid status values to PENDING (safest default)
                UPDATE enrollments 
                SET status = 'PENDING'::enrollment_status
                WHERE status IS NULL 
                   OR status NOT IN ('PENDING', 'ACTIVE', 'COMPLETED', 'DROPPED', 'SUSPENDED');
                
                RAISE NOTICE 'Updated % enrollment records to have PENDING status', invalid_count;
            ELSE
                RAISE NOTICE 'All enrollment records have valid status values';
            END IF;
            
            -- Ensure enrolled_at is not null (set to current timestamp if null)
            UPDATE enrollments 
            SET enrolled_at = CURRENT_TIMESTAMP 
            WHERE enrolled_at IS NULL;
            
            -- Log final status distribution
            DECLARE
                pending_count INTEGER;
                active_count INTEGER;
                completed_count INTEGER;
                dropped_count INTEGER;
                suspended_count INTEGER;
            BEGIN
                SELECT COUNT(*) INTO pending_count FROM enrollments WHERE status = 'PENDING';
                SELECT COUNT(*) INTO active_count FROM enrollments WHERE status = 'ACTIVE';
                SELECT COUNT(*) INTO completed_count FROM enrollments WHERE status = 'COMPLETED';
                SELECT COUNT(*) INTO dropped_count FROM enrollments WHERE status = 'DROPPED';
                SELECT COUNT(*) INTO suspended_count FROM enrollments WHERE status = 'SUSPENDED';
                
                RAISE NOTICE 'Enrollment status distribution:';
                RAISE NOTICE '  PENDING: %', pending_count;
                RAISE NOTICE '  ACTIVE: %', active_count;
                RAISE NOTICE '  COMPLETED: %', completed_count;
                RAISE NOTICE '  DROPPED: %', dropped_count;
                RAISE NOTICE '  SUSPENDED: %', suspended_count;
            END;
        END;
    ELSE
        RAISE NOTICE 'enrollments table does not exist yet';
    END IF;
    
    RAISE NOTICE 'Enrollment data cleanup completed successfully';
    
END $$;
