export interface Grade {
  id: number;
  score: number;
  maxScore: number;
  weight: number;
  content: string;
  description?: string;
  gradedAt: string;
  createdAt: string;
  updatedAt: string;
  enrollment: {
    id: number;
    student: {
      id: number;
      firstName: string;
      lastName: string;
      email: string;
    };
    class: {
      id: number;
      name: string;
    };
  };
  course: {
    id: number;
    name: string;
    credit: number;
  };
  teacher: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
  };
}

// Enhanced Grade System Types
export enum ExamType {
  FIRST_EXAM = 'FIRST_EXAM',
  SECOND_EXAM = 'SECOND_EXAM', 
  FINAL_EXAM = 'FINAL_EXAM',
  QUIZ = 'QUIZ',
  ASSIGNMENT = 'ASSIGNMENT',
  PROJECT = 'PROJECT',
  PARTICIPATION = 'PARTICIPATION',
}

export enum Semester {
  FIRST = 'FIRST',
  SECOND = 'SECOND', 
  THIRD = 'THIRD',
  FINAL = 'FINAL',
}

export interface EnhancedGrade {
  id: number;
  studentId: number;
  studentFirstName: string;
  studentLastName: string;
  studentEmail: string;
  classId: number;
  className: string;
  courseId: number;
  courseName: string;
  courseCode: string;
  courseCoefficient: number;
  teacherId: number;
  teacherFirstName: string;
  teacherLastName: string;
  teacherEmail: string;
  examType: ExamType;
  semester: Semester;
  score: number;
  maxScore: number;
  percentage: number;
  teacherRemarks?: string;
  teacherSignature?: string;
  gradedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface StudentGradeSheet {
  studentId: number;
  studentFirstName: string;
  studentLastName: string;
  studentEmail: string;
  classId: number;
  className: string;
  yearOfStudy: number;
  semester: Semester;
  subjects: SubjectGrade[];
  totalScore: number;
  totalMaxScore: number;
  weightedAverage: number;
  classRank: number;
  totalStudents: number;
  attendanceRate: number;
  totalAbsences: number;
  generatedAt: string;
  approvedBy?: {
    staffId: number;
    staffName: string;
    approvedAt: string;
  };
}

export interface SubjectGrade {
  courseId: number;
  courseName: string;
  courseCode: string;
  coefficient: number;
  teacherId: number;
  teacherFirstName: string;
  teacherLastName: string;
  grades: {
    firstExam?: number;
    secondExam?: number;
    finalExam?: number;
    quizzes?: number[];
    assignments?: number[];
  };
  average: number;
  weightedScore: number;
  teacherRemarks?: string;
  teacherSignature?: string;
  letterGrade: string;
}

export interface CreateGradeRequest {
  enrollmentId: number;
  courseId: number;
  score: number;
  maxScore: number;
  weight: number;
  content: string;
  description?: string;
}

// Enhanced Grade Requests
export interface CreateEnhancedGradeRequest {
  studentId: number;
  classId: number;
  courseId: number;
  examType: ExamType;
  semester: Semester;
  score: number;
  maxScore: number;
  teacherRemarks?: string;
  teacherSignature?: string;
}

export interface BulkGradeEntryRequest {
  classId: number;
  courseId: number;
  examType: ExamType;
  semester: Semester;
  maxScore: number;
  grades: {
    studentId: number;
    score: number;
    teacherRemarks?: string;
  }[];
  teacherSignature?: string;
}

export interface TeacherGradeClassView {
  classId: number;
  className: string;
  courseId: number;
  courseName: string;
  courseCode: string;
  coefficient: number;
  students: TeacherGradeStudent[];
  semester: Semester;
  examTypes: ExamType[];
}

export interface TeacherGradeStudent {
  studentId: number;
  firstName: string;
  lastName: string;
  email: string;
  enrollmentId: number;
  currentGrades: {
    [key in ExamType]?: {
      score: number;
      maxScore: number;
      percentage: number;
      teacherRemarks?: string;
      gradedAt: string;
    };
  };
  average: number;
  attendanceRate: number;
}

// Staff Grade Review Types
export interface StaffGradeReview {
  studentId: number;
  studentFirstName: string;
  studentLastName: string;
  classId: number;
  className: string;
  semester: Semester;
  subjects: StaffSubjectReview[];
  overallAverage: number;
  classRank: number;
  attendanceRate: number;
  isApproved: boolean;
  approvedAt?: string;
  approvedBy?: string;
}

export interface StaffSubjectReview {
  courseId: number;
  courseName: string;
  courseCode: string;
  coefficient: number;
  teacherName: string;
  grades: {
    firstExam?: number;
    secondExam?: number;
    finalExam?: number;
  };
  average: number;
  teacherRemarks?: string;
  needsReview: boolean;
}

// Teacher Attendance Types (separate from student attendance)
export interface TeacherAttendance {
  id: number;
  teacherId: number;
  teacherFirstName: string;
  teacherLastName: string;
  teacherEmail: string;
  date: string;
  status: TeacherAttendanceStatus;
  courseId?: number;
  courseName?: string;
  classId?: number;
  className?: string;
  remarks?: string;
  excuse?: string;
  substituteTeacherId?: number;
  substituteTeacherName?: string;
  recordedById: number;
  recordedByName: string;
  createdAt: string;
  updatedAt: string;
}

export enum TeacherAttendanceStatus {
  PRESENT = 'PRESENT',
  ABSENT = 'ABSENT',
  LATE = 'LATE',
  SICK_LEAVE = 'SICK_LEAVE',
  PERSONAL_LEAVE = 'PERSONAL_LEAVE',
  PROFESSIONAL_DEVELOPMENT = 'PROFESSIONAL_DEVELOPMENT',
  SUBSTITUTE_ARRANGED = 'SUBSTITUTE_ARRANGED',
}

export interface TeacherAttendanceStatistics {
  teacherId: number;
  teacherName: string;
  totalDays: number;
  presentDays: number;
  absentDays: number;
  lateDays: number;
  sickLeaveDays: number;
  personalLeaveDays: number;
  attendanceRate: number;
  monthlyBreakdown: {
    month: string;
    present: number;
    absent: number;
    rate: number;
  }[];
}

export interface UpdateGradeRequest {
  score?: number;
  maxScore?: number;
  weight?: number;
  content?: string;
  description?: string;
}

export interface GradeFilters {
  enrollmentId?: number;
  courseId?: number;
  teacherId?: number;
  studentId?: number;
  classId?: number;
  content?: string;
  score?: number;
  scoreRange?: [number, number];
  weight?: number;
  gradedAt?: string;
  gradedAtFrom?: string;
  gradedAtTo?: string;
  search?: string;
}

export interface GradeResponse {
  content: Grade[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface GradeStatistics {
  totalGrades: number;
  averageScore: number;
  highestScore: number;
  lowestScore: number;
  gradeDistribution: {
    range: string;
    count: number;
    percentage: number;
  }[];
}

export interface StudentGradeSummary {
  studentId: number;
  studentName: string;
  totalGrades: number;
  averageScore: number;
  weightedAverage: number;
  courseGrades: {
    courseId: number;
    courseName: string;
    grades: Grade[];
    average: number;
    weightedAverage: number;
  }[];
}

export interface ClassGradeSummary {
  classId: number;
  className: string;
  totalStudents: number;
  totalGrades: number;
  classAverage: number;
  highestAverage: number;
  lowestAverage: number;
  studentSummaries: StudentGradeSummary[];
}

export interface TeacherGradeSummary {
  teacherId: number;
  teacherName: string;
  totalGrades: number;
  coursesSummary: {
    courseId: number;
    courseName: string;
    totalGrades: number;
    averageScore: number;
    classes: ClassGradeSummary[];
  }[];
}

// Grade calculation utilities
export const calculatePercentage = (score: number, maxScore: number): number => {
  return maxScore > 0 ? (score / maxScore) * 100 : 0;
};

export const calculateWeightedScore = (score: number, maxScore: number, weight: number): number => {
  return calculatePercentage(score, maxScore) * weight;
};

export const getGradeLevel = (percentage: number): string => {
  if (percentage >= 90) return 'A';
  if (percentage >= 80) return 'B';
  if (percentage >= 70) return 'C';
  if (percentage >= 60) return 'D';
  return 'F';
};

export const getGradeLevelColor = (percentage: number): string => {
  if (percentage >= 90) return 'text-green-600';
  if (percentage >= 80) return 'text-blue-600';
  if (percentage >= 70) return 'text-yellow-600';
  if (percentage >= 60) return 'text-orange-600';
  return 'text-red-600';
};

export const getGradeLevelBadgeColor = (percentage: number): string => {
  if (percentage >= 90) return 'bg-green-100 text-green-800';
  if (percentage >= 80) return 'bg-blue-100 text-blue-800';
  if (percentage >= 70) return 'bg-yellow-100 text-yellow-800';
  if (percentage >= 60) return 'bg-orange-100 text-orange-800';
  return 'bg-red-100 text-red-800';
}; 