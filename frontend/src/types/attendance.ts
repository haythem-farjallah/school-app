export interface Attendance {
  id?: number;
  userId: number;
  userName: string;
  timetableSlotId: number;
  date: string;
  status: AttendanceStatus;
  userType: UserType;
  classId?: number;
  className?: string;
  remarks?: string;
  markedBy?: number;
  markedAt?: string;
}

export interface TeacherAttendanceClassView {
  classId: number;
  className: string;
  courseId: number;
  courseName: string;
  courseCode: string;
  coefficient: number;
  students: TeacherAttendanceStudent[];
  semester: string;
  attendanceTypes: string[];
}

export interface TeacherAttendanceStudent {
  studentId: number;
  firstName: string;
  lastName: string;
  email: string;
  enrollmentId: number;
  currentStatus: AttendanceStatus;
  attendanceRate: number;
  lastAttendanceDate?: string;
}

export enum AttendanceStatus {
  PRESENT = 'PRESENT',
  ABSENT = 'ABSENT',
  LATE = 'LATE',
  EXCUSED = 'EXCUSED'
}

export enum UserType {
  STUDENT = 'STUDENT',
  TEACHER = 'TEACHER',
  PARENT = 'PARENT',
  ADMIN = 'ADMIN',
  STAFF = 'STAFF'
}

export interface CreateAttendanceRequest {
  userId: number;
  timetableSlotId: number;
  date: string;
  status: AttendanceStatus;
  userType: UserType;
  remarks?: string;
}

export interface BulkAttendanceRequest {
  attendances: CreateAttendanceRequest[];
}

export interface AttendanceStatistics {
  totalDays: number;
  presentDays: number;
  absentDays: number;
  lateDays: number;
  excusedDays: number;
  attendanceRate: number;
}

export interface AttendanceFilters {
  userId?: number;
  classId?: number;
  courseId?: number;
  startDate?: string;
  endDate?: string;
  status?: AttendanceStatus;
  userType?: UserType;
}

export interface AttendanceResponse {
  content: Attendance[];
  page: number;
  size: number;
  totalElements: number;
}

export interface ClassAttendanceSummary {
  classId: number;
  className: string;
  date: string;
  totalStudents: number;
  presentCount: number;
  absentCount: number;
  lateCount: number;
  excusedCount: number;
  attendanceRate: number;
}