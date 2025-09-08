/**
 * Teaching Assignment types for teacher-course linking
 */

export interface TeachingAssignment {
  id: number;
  teacherId: number;
  teacherFirstName: string;
  teacherLastName: string;
  teacherEmail: string;
  courseId: number;
  courseName: string;
  courseCode: string;
  classId: number;
  className: string;
  weeklyHours: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeachingAssignmentData {
  teacherId: number;
  courseId: number;
  classId: number;
  weeklyHours?: number;
}

export interface UpdateTeachingAssignmentData {
  teacherId?: number;
  courseId?: number;
  classId?: number;
  weeklyHours?: number;
}

export interface BulkAssignTeacherToCoursesRequest {
  teacherId: number;
  courseIds: number[];
  classId: number;
}

export interface BulkAssignTeachersToClassRequest {
  teacherIds: number[];
  courseId: number;
  classId: number;
}

// Helper computed properties
export interface TeachingAssignmentDisplay extends TeachingAssignment {
  teacherFullName: string;
  courseDisplay: string;
  assignmentSummary: string;
}

