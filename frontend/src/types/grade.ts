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

export interface CreateGradeRequest {
  enrollmentId: number;
  courseId: number;
  score: number;
  maxScore: number;
  weight: number;
  content: string;
  description?: string;
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