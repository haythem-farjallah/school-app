-- Add missing columns to schedules table
ALTER TABLE schedules 
ADD COLUMN description VARCHAR(255),
ADD COLUMN start_date TIMESTAMP WITHOUT TIME ZONE,
ADD COLUMN end_date TIMESTAMP WITHOUT TIME ZONE,
ADD COLUMN is_active BOOLEAN DEFAULT true; 