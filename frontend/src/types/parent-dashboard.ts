// Parent Dashboard TypeScript Interfaces

export interface ChildInfo {
  studentId: number;
  name: string;
  currentClass: string;
  classId: number | null;
  attendanceRate: number;
  totalAbsences: number;
  totalDaysTracked: number;
  averageGrade: number;
  academicStanding: string;
  alerts?: string[];
}

export interface ParentDashboardData {
  baseInfo: {
    userInfo: {
      id: number;
      name: string;
      email: string;
      role: string;
      avatarUrl?: string;
    };
    recentActivities: Array<{
      type: string;
      description: string;
      timestamp: string;
    }>;
    notifications: Array<{
      id: number;
      title: string;
      message: string;
      type: string;
      isRead: boolean;
      timestamp: string;
    }>;
    lastLogin: string;
  };
  type: 'PARENT';
  children: ChildInfo[];
  schoolUpdates: Array<{
    id: number;
    title: string;
    content: string;
    date: string;
    type: string;
  }>;
  upcomingEvents: Array<{
    id: number;
    title: string;
    description: string;
    date: string;
    location?: string;
  }>;
}

export interface TimetableSlot {
  id: number;
  startTime: string;
  endTime: string;
  dayOfWeek: string;
  courseName: string;
  className: string;
  teacherName: string;
  roomName: string;
}

export interface ChildTimetable {
  studentId: number;
  studentName: string;
  timetables: Array<{
    classId: number;
    className: string;
    slots: TimetableSlot[];
  }>;
}

export interface TimetableData {
  children: ChildTimetable[];
}

export interface AttendanceRecord {
  id: number;
  date: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
  remarks?: string;
  recordedAt: string;
  courseName?: string;
  teacherName?: string;
}

export interface ChildAttendance {
  studentId: number;
  studentName: string;
  totalDays: number;
  presentDays: number;
  absentDays: number;
  lateDays: number;
  attendanceRate: number;
  attendanceByDate: { [date: string]: AttendanceRecord };
  recentAttendance: AttendanceRecord[];
}

export interface AttendanceData {
  children: ChildAttendance[];
}

export type ViewType = 'overview' | 'timetable' | 'attendance';
