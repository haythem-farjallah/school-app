import { useQuery } from '@tanstack/react-query';
import { http } from '@/lib/http';

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

export interface TeacherClassesResponse {
  data: TeacherClass[];
  totalItems: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

interface UseTeacherClassesOptions {
  page?: number;
  size?: number;
  search?: string;
  enabled?: boolean;
}

export function useTeacherClasses(options: UseTeacherClassesOptions = {}) {
  const { page = 0, size = 10, search, enabled = true } = options;

  return useQuery({
    queryKey: ['teacher-classes', page, size, search],
    queryFn: async (): Promise<TeacherClassesResponse> => {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', size.toString());
      if (search) {
        params.append('search', search);
      }

      const response = await http.get(`/v1/teacher/classes?${params.toString()}`);
      return response.data;
    },
    enabled,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

export function useAllTeacherClasses(search?: string) {
  return useQuery({
    queryKey: ['teacher-classes-all', search],
    queryFn: async (): Promise<TeacherClass[]> => {
      const params = new URLSearchParams();
      if (search) {
        params.append('search', search);
      }

      const response = await http.get(`/v1/teacher/classes/all?${params.toString()}`);
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}

export function useTeacherClassStats() {
  return useQuery({
    queryKey: ['teacher-class-stats'],
    queryFn: async (): Promise<TeacherClassStats> => {
      const response = await http.get('/v1/teacher/classes/stats');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
  });
}
