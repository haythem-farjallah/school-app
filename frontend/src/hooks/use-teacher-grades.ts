import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { http } from '@/lib/http';
import toast from 'react-hot-toast';

// Enhanced grade management types
export interface CreateEnhancedGradeRequest {
  studentId: number;
  classId: number;
  courseId: number;
  examType: ExamType;
  semester: Semester;
  score: number;
  maxScore: number;
  teacherRemarks?: string;
  gradedAt?: string;
}

export interface BulkEnhancedGradeEntryRequest {
  classId: number;
  courseId: number;
  examType: ExamType;
  semester: Semester;
  grades: Array<{
    studentId: number;
    score: number;
    maxScore: number;
    teacherRemarks?: string;
  }>;
}

export interface EnhancedGradeResponse {
  id: number;
  studentId: number;
  classId: number;
  courseId: number;
  examType: ExamType;
  semester: Semester;
  score: number;
  maxScore: number;
  percentage: number;
  teacherRemarks?: string;
  gradedAt: string;
  studentName: string;
  className: string;
  courseName: string;
}

export enum ExamType {
  FIRST_EXAM = 'FIRST_EXAM',
  SECOND_EXAM = 'SECOND_EXAM', 
  FINAL_EXAM = 'FINAL_EXAM',
  QUIZ = 'QUIZ',
  ASSIGNMENT = 'ASSIGNMENT',
  PROJECT = 'PROJECT',
  LAB_REPORT = 'LAB_REPORT',
  PRESENTATION = 'PRESENTATION',
  HOMEWORK = 'HOMEWORK'
}

export enum Semester {
  FIRST = 'FIRST',
  SECOND = 'SECOND'
}

// Hook to create a single enhanced grade
export function useCreateEnhancedGrade() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: CreateEnhancedGradeRequest) => {
      const response = await http.post<EnhancedGradeResponse>('/v1/grades/enhanced', data);
      return response.data;
    },
    onSuccess: (_, variables) => {
      toast.success('Grade saved successfully!');
      
      // Invalidate related queries
      queryClient.invalidateQueries({ 
        queryKey: ['teacher-grade-classes'] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['teacher-grade-class', undefined, variables.classId, variables.courseId] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['grades'] 
      });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to save grade';
      toast.error(message);
    },
  });
}

// Hook to create bulk enhanced grades
export function useCreateBulkEnhancedGrades() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: BulkEnhancedGradeEntryRequest) => {
      const response = await http.post<EnhancedGradeResponse[]>('/v1/grades/bulk-entry', data);
      return response.data;
    },
    onSuccess: (_, variables) => {
      toast.success(`Successfully saved grades for ${variables.grades.length} students!`);
      
      // Invalidate related queries
      queryClient.invalidateQueries({ 
        queryKey: ['teacher-grade-classes'] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['teacher-grade-class', undefined, variables.classId, variables.courseId] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['grades'] 
      });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to save grades';
      toast.error(message);
    },
  });
}

// Hook to get grades by teacher
export function useTeacherGrades(teacherId?: number) {
  return useQuery({
    queryKey: ['teacher-grades', teacherId],
    queryFn: async () => {
      if (!teacherId) throw new Error('Teacher ID is required');
      
      const response = await http.get<EnhancedGradeResponse[]>(`/v1/grades/teacher/${teacherId}`);
      return response.data;
    },
    enabled: !!teacherId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Hook to get grades by class
export function useClassGrades(classId?: number) {
  return useQuery({
    queryKey: ['class-grades', classId],
    queryFn: async () => {
      if (!classId) throw new Error('Class ID is required');
      
      const response = await http.get<EnhancedGradeResponse[]>(`/v1/grades/class/${classId}`);
      return response.data;
    },
    enabled: !!classId,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Hook to update a grade
export function useUpdateGrade() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ gradeId, data }: { gradeId: number; data: Partial<CreateEnhancedGradeRequest> }) => {
      const response = await http.patch(`/v1/grades/${gradeId}`, data);
      return response.data;
    },
    onSuccess: () => {
      toast.success('Grade updated successfully!');
      
      // Invalidate related queries
      queryClient.invalidateQueries({ 
        queryKey: ['teacher-grade-classes'] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['teacher-grade-class'] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['grades'] 
      });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to update grade';
      toast.error(message);
    },
  });
}

// Hook to delete a grade
export function useDeleteGrade() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ gradeId, reason }: { gradeId: number; reason: string }) => {
      const response = await http.delete(`/v1/grades/${gradeId}`, {
        data: { reason }
      });
      return response.data;
    },
    onSuccess: () => {
      toast.success('Grade deleted successfully!');
      
      // Invalidate related queries
      queryClient.invalidateQueries({ 
        queryKey: ['teacher-grade-classes'] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['teacher-grade-class'] 
      });
      queryClient.invalidateQueries({ 
        queryKey: ['grades'] 
      });
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to delete grade';
      toast.error(message);
    },
  });
}

// Utility functions for grade calculations
export const calculateGradePercentage = (score: number, maxScore: number): number => {
  return maxScore > 0 ? Math.round((score / maxScore) * 100 * 10) / 10 : 0;
};

export const calculateLetterGrade = (percentage: number): string => {
  if (percentage >= 90) return 'A';
  if (percentage >= 80) return 'B';
  if (percentage >= 70) return 'C';
  if (percentage >= 60) return 'D';
  return 'F';
};

export const getGradeColor = (letterGrade: string): string => {
  switch (letterGrade) {
    case 'A': return 'bg-green-100 text-green-800 border-green-200 font-medium';
    case 'B': return 'bg-blue-100 text-blue-800 border-blue-200 font-medium';
    case 'C': return 'bg-yellow-100 text-yellow-800 border-yellow-200 font-medium';
    case 'D': return 'bg-orange-100 text-orange-800 border-orange-200 font-medium';
    case 'F': return 'bg-red-100 text-red-800 border-red-200 font-medium';
    default: return 'bg-gray-100 text-gray-800 border-gray-200 font-medium';
  }
};

// Exam type display names
export const examTypeDisplayNames: Record<ExamType, string> = {
  [ExamType.FIRST_EXAM]: 'First Exam',
  [ExamType.SECOND_EXAM]: 'Second Exam',
  [ExamType.FINAL_EXAM]: 'Final Exam',
  [ExamType.QUIZ]: 'Quiz',
  [ExamType.ASSIGNMENT]: 'Assignment',
  [ExamType.PROJECT]: 'Project',
  [ExamType.LAB_REPORT]: 'Lab Report',
  [ExamType.PRESENTATION]: 'Presentation',
  [ExamType.HOMEWORK]: 'Homework',
};

// Semester display names
export const semesterDisplayNames: Record<Semester, string> = {
  [Semester.FIRST]: 'First Semester',
  [Semester.SECOND]: 'Second Semester',
};
