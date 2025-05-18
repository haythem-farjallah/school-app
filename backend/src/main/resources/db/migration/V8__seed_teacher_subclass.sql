/* For each teacher BaseUser, create a row in the teacher table */
INSERT INTO teacher (id, qualifications, subjects_taught, available_hours, schedule_preferences)
SELECT id, 'MSc Mathematics', 'Math', 20, 'Morning'
FROM users
WHERE role = 'TEACHER'
  AND id NOT IN (SELECT id FROM teacher);
