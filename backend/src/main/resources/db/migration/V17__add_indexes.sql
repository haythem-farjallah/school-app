-- Add performance indexes
CREATE INDEX idx_audit_events_acted_by ON audit_events(acted_by_id);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX idx_resource_comments_resource ON resource_comments(on_resource_id);
CREATE INDEX idx_resource_comments_user ON resource_comments(commented_by_id);
CREATE INDEX idx_timetable_slots_class ON timetable_slots(for_class_id);
CREATE INDEX idx_timetable_slots_course ON timetable_slots(for_course_id);
CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_class ON enrollments(class_id);
CREATE INDEX idx_grades_enrollment ON grades(enrollment_id);
CREATE INDEX idx_grades_teacher ON grades(assigned_by_id);
CREATE INDEX idx_announcements_dates ON announcements(start_date, end_date);
CREATE INDEX idx_announcements_importance ON announcements(importance);
CREATE INDEX idx_transfers_student ON transfers(student_id);
CREATE INDEX idx_transfers_requested_at ON transfers(requested_at); 