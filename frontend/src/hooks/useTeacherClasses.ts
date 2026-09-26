import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api-client';
import type { ApiResponse } from '@/types/level';

export interface TeacherClassCourse {
  id: number;
  name: string;
  code: string;
  weeklyHours: number;
}

export interface TeacherClass {
  id: number;
  name: string;
  grade: string;
  capacity: number;
  enrolled: number;
  room: string;
  schedule: string;
  averageGrade: number | null;
  status: string;
  courses: TeacherClassCourse[];
  createdAt: string | null;
  updatedAt: string | null;
}

export interface TeacherClassStats {
  totalClasses: number;
  totalStudents: number;
  averageGrade: number;
  totalCapacity: number;
  capacityUsed: number;
}

export function useAllTeacherClasses(search?: string) {
  return useQuery({
    queryKey: ['teacher-classes-all', search],
    queryFn: async (): Promise<TeacherClass[]> => {
      const params = new URLSearchParams();
      if (search) {
        params.append('search', search);
      }

      const response = await api.get<ApiResponse<TeacherClass[]>>(`/v1/teacher/classes/all?${params.toString()}`);
      return response.data.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

export function useTeacherClassStats() {
  return useQuery({
    queryKey: ['teacher-class-stats'],
    queryFn: async (): Promise<TeacherClassStats> => {
      const response = await api.get<ApiResponse<TeacherClassStats>>('/v1/teacher/classes/stats');
      return response.data.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}
