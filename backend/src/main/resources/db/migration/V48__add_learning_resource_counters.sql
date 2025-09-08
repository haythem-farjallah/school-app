-- Add view and download counters to learning_resources table
ALTER TABLE learning_resources 
ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0,
ADD COLUMN download_count BIGINT NOT NULL DEFAULT 0;

-- Create indexes for better performance on counter queries
CREATE INDEX idx_learning_resources_view_count ON learning_resources(view_count);
CREATE INDEX idx_learning_resources_download_count ON learning_resources(download_count);

-- Update existing records to have 0 counters (in case there are any)
UPDATE learning_resources SET view_count = 0, download_count = 0 WHERE view_count IS NULL OR download_count IS NULL;
