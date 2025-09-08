-- Insert additional default periods for school day (8:00 AM to 5:00 PM)
-- Only insert periods that don't already exist (to avoid conflicts with V24)
INSERT INTO periods (index_number, start_time, end_time)
SELECT 7, '13:00:00', '14:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 7);

INSERT INTO periods (index_number, start_time, end_time)
SELECT 8, '16:00:00', '17:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 8);

INSERT INTO periods (index_number, start_time, end_time)
SELECT 9, '17:00:00', '18:00:00' WHERE NOT EXISTS (SELECT 1 FROM periods WHERE index_number = 9); 