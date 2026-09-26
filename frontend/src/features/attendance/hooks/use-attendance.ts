import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api-client";
import type { 
  Attendance, 
  CreateAttendanceRequest, 
  AttendanceStatistics,
  AttendanceFilters
} from "@/types/attendance";
import type { ApiResponse, PageDto } from "@/types/level";

const ATTENDANCE_KEY = "attendance";

// Get paginated attendance records (like useGrades)
export function useAttendanceRecords(filters: AttendanceFilters = {}) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "records", filters],
    queryFn: async (): Promise<{
      data: Attendance[];
      totalItems: number;
      totalPages: number;
      currentPage: number;
    }> => {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get<ApiResponse<PageDto<Attendance>>>(`/v1/attendance/filter?${params.toString()}`);
      const page = response.data.data;
      return {
        data: page.content,
        totalItems: page.totalElements,
        totalPages: Math.ceil(page.totalElements / page.size),
        currentPage: page.page,
      };
    },
  });
}

// Get attendance statistics for table
export function useAttendanceStatistics(filters: { startDate?: string; endDate?: string } = {}) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "table-statistics", filters],
    queryFn: async (): Promise<AttendanceStatistics> => {
      const params = new URLSearchParams();
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          params.append(key, value.toString());
        }
      });

      const response = await api.get<ApiResponse<AttendanceStatistics>>(`/v1/attendance/statistics?${params.toString()}`);
      return response.data.data;
    },
  });
}

// Create single attendance record
export function useCreateAttendance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (data: CreateAttendanceRequest): Promise<Attendance> => {
      const response = await api.post<ApiResponse<Attendance>>("/v1/attendance", data);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to record attendance:', error);
    },
  });
}

// Update attendance record
export function useUpdateAttendance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      id, 
      data 
    }: { 
      id: number; 
      data: Partial<CreateAttendanceRequest> 
    }): Promise<Attendance> => {
      const response = await api.put<ApiResponse<Attendance>>(`/v1/attendance/${id}`, data);
      return response.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to update attendance:', error);
    },
  });
}

// Delete attendance record
export function useDeleteAttendance() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (id: number): Promise<void> => {
      await api.delete(`/v1/attendance/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to delete attendance:', error);
    },
  });
}

// Teacher-specific attendance hooks

// Get absent students for teacher today
export function useTeacherAbsentStudents(teacherId: number, date?: string) {
  const targetDate = date || new Date().toISOString().split('T')[0];
  
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "teacher-absent", teacherId, targetDate],
    queryFn: async (): Promise<Attendance[]> => {
      const response = await api.get<ApiResponse<Attendance[]>>(`/v1/attendance/teacher/${teacherId}/absent-students?date=${targetDate}`);
      return response.data.data;
    },
    enabled: !!teacherId,
  });
}

// Get teacher's weekly attendance summary
export function useTeacherWeeklySummary(teacherId: number, startOfWeek: string) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "teacher-weekly", teacherId, startOfWeek],
    queryFn: async (): Promise<Record<string, Attendance[]>> => {
      const response = await api.get<ApiResponse<Record<string, Attendance[]>>>(`/v1/attendance/teacher/${teacherId}/weekly-summary?startOfWeek=${startOfWeek}`);
      return response.data.data;
    },
    enabled: !!teacherId && !!startOfWeek,
  });
}

// Get students for a timetable slot with attendance status
export function useStudentsForSlot(slotId: number, date?: string) {
  const targetDate = date || new Date().toISOString().split('T')[0];
  
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "slot-students", slotId, targetDate],
    queryFn: async (): Promise<Attendance[]> => {
      const response = await api.get<ApiResponse<Attendance[]>>(`/v1/attendance/slot/${slotId}/students?date=${targetDate}`);
      return response.data.data;
    },
    enabled: !!slotId,
  });
}

// Get students for a class (simple list)
export function useStudentsForClass(classId: number) {
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "class-students", classId],
    queryFn: async (): Promise<Attendance[]> => {
      const response = await api.get<ApiResponse<Attendance[]>>(`/v1/attendance/class/${classId}/students-simple`);
      return response.data.data;
    },
    enabled: !!classId && classId > 0,
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Check if teacher can mark attendance for a specific slot
export function useCanTeacherMarkAttendance(teacherId: number, slotId: number, date?: string) {
  const targetDate = date || new Date().toISOString().split('T')[0];
  
  return useQuery({
    queryKey: [ATTENDANCE_KEY, "can-mark", teacherId, slotId, targetDate],
    queryFn: async (): Promise<boolean> => {
      const response = await api.get<ApiResponse<boolean>>(`/v1/attendance/teacher/${teacherId}/can-mark/${slotId}?date=${targetDate}`);
      return response.data.data;
    },
    enabled: !!teacherId && !!slotId,
  });
}

// Mark attendance for all students in a timetable slot
export function useMarkAttendanceForSlot() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      slotId, 
      date, 
      attendanceList 
    }: { 
      slotId: number; 
      date?: string; 
      attendanceList: CreateAttendanceRequest[] 
    }): Promise<Attendance[]> => {
      const targetDate = date || new Date().toISOString().split('T')[0];
      const response = await api.post<ApiResponse<Attendance[]>>(`/v1/attendance/slot/${slotId}/mark?date=${targetDate}`, attendanceList);
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "slot-students", variables.slotId] 
      });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to mark attendance:', error);
    },
  });
}

// Mark attendance for all students in a class (for virtual slots)
export function useMarkAttendanceForClass() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      classId, 
      date, 
      attendanceList 
    }: { 
      classId: number; 
      date?: string; 
      attendanceList: CreateAttendanceRequest[] 
    }): Promise<Attendance[]> => {
      const targetDate = date || new Date().toISOString().split('T')[0];
      const response = await api.post<ApiResponse<Attendance[]>>(`/v1/attendance/class/${classId}/mark?date=${targetDate}`, attendanceList);
      return response.data.data;
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: [ATTENDANCE_KEY] });
      queryClient.invalidateQueries({ 
        queryKey: [ATTENDANCE_KEY, "class-students", variables.classId] 
      });
      // Don't show toast here as components handle it
    },
    onError: (error: any) => {
      console.error('Failed to mark attendance:', error);
    },
  });
}