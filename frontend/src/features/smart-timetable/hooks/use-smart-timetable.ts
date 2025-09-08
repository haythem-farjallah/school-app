import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { http } from '@/lib/http';
import type {
  TimetableOptimizationRequest,
  TimetableOptimizationResult,
  TeacherWorkloadAnalysis,
  TimetableConflictReport,
  TimetableAnalytics,
  EnhancedTimetableSlot
} from '@/types/smart-timetable';
import toast from 'react-hot-toast';

// ===== OPTIMIZATION HOOKS =====

export function useOptimizeTimetable() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (request: TimetableOptimizationRequest): Promise<TimetableOptimizationResult> => {
      const response = await http.post('/smart-timetable/optimize', request);
      return response.data.data;
    },
    onSuccess: (data) => {
      toast.success(`Timetable optimization completed with score: ${(data.finalScore * 100).toFixed(1)}%`);
      queryClient.invalidateQueries({ queryKey: ['timetable-slots'] });
      queryClient.invalidateQueries({ queryKey: ['timetable-analytics'] });
    },
    onError: (error: any) => {
      toast.error(`Optimization failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

export function useReoptimizeTimetable() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      timetableId, 
      additionalConstraints 
    }: { 
      timetableId: number; 
      additionalConstraints: string[] 
    }): Promise<TimetableOptimizationResult> => {
      const response = await http.post(`/smart-timetable/${timetableId}/reoptimize`, additionalConstraints);
      return response.data.data;
    },
    onSuccess: (data) => {
      toast.success(`Re-optimization completed with improved score: ${(data.finalScore * 100).toFixed(1)}%`);
      queryClient.invalidateQueries({ queryKey: ['timetable-slots'] });
      queryClient.invalidateQueries({ queryKey: ['timetable-analytics'] });
    },
    onError: (error: any) => {
      toast.error(`Re-optimization failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

export function useGenerateScenarios() {
  return useMutation({
    mutationFn: async ({ 
      request, 
      scenarioCount 
    }: { 
      request: TimetableOptimizationRequest; 
      scenarioCount: number 
    }): Promise<TimetableOptimizationResult> => {
      const response = await http.post(`/smart-timetable/scenarios?scenarioCount=${scenarioCount}`, request);
      return response.data.data;
    },
    onSuccess: (data) => {
      toast.success(`Generated ${data.alternativeScenarios?.length || 0} optimization scenarios`);
    },
    onError: (error: any) => {
      toast.error(`Scenario generation failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

// ===== WORKLOAD ANALYSIS HOOKS =====

export function useTeacherWorkloadAnalysis(teacherId: number) {
  return useQuery({
    queryKey: ['teacher-workload', teacherId],
    queryFn: async (): Promise<TeacherWorkloadAnalysis> => {
      const response = await http.get(`/smart-timetable/workload/teacher/${teacherId}`);
      return response.data.data;
    },
    enabled: !!teacherId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

export function useAllTeacherWorkloads() {
  return useQuery({
    queryKey: ['teacher-workloads'],
    queryFn: async (): Promise<TeacherWorkloadAnalysis[]> => {
      const response = await http.get('/smart-timetable/workload/all');
      return response.data.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

export function useBalanceTeacherWorkloads() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (timetableId: number): Promise<TimetableOptimizationResult> => {
      const response = await http.post(`/smart-timetable/${timetableId}/balance-workloads`);
      return response.data.data;
    },
    onSuccess: (data) => {
      toast.success(`Teacher workloads balanced successfully! Improvement: ${(data.improvementPercentage || 0).toFixed(1)}%`);
      queryClient.invalidateQueries({ queryKey: ['teacher-workloads'] });
      queryClient.invalidateQueries({ queryKey: ['timetable-analytics'] });
    },
    onError: (error: any) => {
      toast.error(`Workload balancing failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

// ===== CONFLICT DETECTION HOOKS =====

export function useTimetableConflicts(timetableId: number) {
  return useQuery({
    queryKey: ['timetable-conflicts', timetableId],
    queryFn: async (): Promise<TimetableConflictReport> => {
      const response = await http.get(`/smart-timetable/${timetableId}/conflicts`);
      return response.data.data;
    },
    enabled: !!timetableId,
    refetchInterval: 30 * 1000, // Refresh every 30 seconds for real-time conflict detection
  });
}

export function useResolveConflicts() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      timetableId, 
      conflictTypes 
    }: { 
      timetableId: number; 
      conflictTypes: string[] 
    }): Promise<TimetableOptimizationResult> => {
      const response = await http.post(`/smart-timetable/${timetableId}/resolve-conflicts`, conflictTypes);
      return response.data.data;
    },
    onSuccess: (data) => {
      const resolvedCount = data.resolvedConflicts?.length || 0;
      toast.success(`Resolved ${resolvedCount} conflicts successfully!`);
      queryClient.invalidateQueries({ queryKey: ['timetable-conflicts'] });
      queryClient.invalidateQueries({ queryKey: ['timetable-slots'] });
    },
    onError: (error: any) => {
      toast.error(`Conflict resolution failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

// ===== ROOM OPTIMIZATION HOOKS =====

export function useOptimizeRoomUsage() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (timetableId: number): Promise<TimetableOptimizationResult> => {
      const response = await http.post(`/smart-timetable/${timetableId}/optimize-rooms`);
      return response.data.data;
    },
    onSuccess: (data) => {
      const improvement = data.improvementPercentage || 0;
      toast.success(`Room optimization completed! Efficiency improved by ${improvement.toFixed(1)}%`);
      queryClient.invalidateQueries({ queryKey: ['timetable-slots'] });
      queryClient.invalidateQueries({ queryKey: ['timetable-analytics'] });
    },
    onError: (error: any) => {
      toast.error(`Room optimization failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

// ===== REAL-TIME SCHEDULE CHANGES =====

export function useValidateScheduleChange() {
  return useMutation({
    mutationFn: async ({ 
      timetableId, 
      slotId, 
      newTeacherId, 
      newRoomId 
    }: { 
      timetableId: number; 
      slotId: number; 
      newTeacherId?: number; 
      newRoomId?: number 
    }): Promise<boolean> => {
      const params = new URLSearchParams({
        slotId: slotId.toString(),
        ...(newTeacherId && { newTeacherId: newTeacherId.toString() }),
        ...(newRoomId && { newRoomId: newRoomId.toString() })
      });
      
      const response = await http.post(`/smart-timetable/${timetableId}/validate-change?${params}`);
      return response.data.data;
    },
    onError: (error: any) => {
      toast.error(`Validation failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

export function useApplyScheduleChange() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      timetableId, 
      slotId, 
      newTeacherId, 
      newRoomId 
    }: { 
      timetableId: number; 
      slotId: number; 
      newTeacherId?: number; 
      newRoomId?: number 
    }): Promise<TimetableOptimizationResult> => {
      const params = new URLSearchParams({
        slotId: slotId.toString(),
        ...(newTeacherId && { newTeacherId: newTeacherId.toString() }),
        ...(newRoomId && { newRoomId: newRoomId.toString() })
      });
      
      const response = await http.post(`/smart-timetable/${timetableId}/apply-change?${params}`);
      return response.data.data;
    },
    onSuccess: () => {
      toast.success('Schedule change applied successfully!');
      queryClient.invalidateQueries({ queryKey: ['timetable-slots'] });
      queryClient.invalidateQueries({ queryKey: ['timetable-conflicts'] });
      queryClient.invalidateQueries({ queryKey: ['timetable-analytics'] });
    },
    onError: (error: any) => {
      toast.error(`Schedule change failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

// ===== ANALYTICS HOOKS =====

export function useTimetableAnalytics(timetableId: number) {
  return useQuery({
    queryKey: ['timetable-analytics', timetableId],
    queryFn: async (): Promise<TimetableAnalytics> => {
      const response = await http.get(`/smart-timetable/${timetableId}/analytics`);
      return response.data.data;
    },
    enabled: !!timetableId,
    staleTime: 2 * 60 * 1000, // 2 minutes
    refetchInterval: 5 * 60 * 1000, // Refresh every 5 minutes
  });
}

// ===== ENHANCED TIMETABLE SLOTS =====

export function useEnhancedTimetableSlots(timetableId: number) {
  return useQuery({
    queryKey: ['timetable-slots', timetableId],
    queryFn: async (): Promise<EnhancedTimetableSlot[]> => {
      // This would typically call the existing timetable API and enhance the data
      const response = await http.get(`/timetables/${timetableId}/slots`);
      
      // Transform the basic slots to enhanced slots with additional UI data
      return response.data.data.map((slot: any) => ({
        ...slot,
        isSelected: false,
        isDragging: false,
        hasConflict: false,
        isOptimal: true,
      }));
    },
    enabled: !!timetableId,
    staleTime: 1 * 60 * 1000, // 1 minute
  });
}

// ===== UTILITY HOOKS =====

export function useOptimizationStatus(optimizationId?: string) {
  return useQuery({
    queryKey: ['optimization-status', optimizationId],
    queryFn: async (): Promise<TimetableOptimizationResult> => {
      // This would be a polling endpoint to check optimization status
      const response = await http.get(`/smart-timetable/status/${optimizationId}`);
      return response.data.data;
    },
    enabled: !!optimizationId,
    refetchInterval: (data) => {
      // Stop polling when optimization is complete
      return data?.status === 'COMPLETED' || data?.status === 'FAILED' ? false : 2000;
    },
  });
}

// ===== BULK OPERATIONS =====

export function useBulkSlotOperations() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      operation, 
      slotIds, 
      parameters 
    }: { 
      operation: 'DELETE' | 'MOVE' | 'DUPLICATE' | 'ASSIGN_TEACHER' | 'ASSIGN_ROOM'; 
      slotIds: number[]; 
      parameters?: Record<string, any> 
    }) => {
      const response = await http.post('/smart-timetable/bulk-operations', {
        operation,
        slotIds,
        parameters
      });
      return response.data.data;
    },
    onSuccess: (data, variables) => {
      const { operation, slotIds } = variables;
      toast.success(`Bulk ${operation.toLowerCase()} completed for ${slotIds.length} slots`);
      queryClient.invalidateQueries({ queryKey: ['timetable-slots'] });
      queryClient.invalidateQueries({ queryKey: ['timetable-conflicts'] });
    },
    onError: (error: any) => {
      toast.error(`Bulk operation failed: ${error.response?.data?.message || error.message}`);
    }
  });
}

// ===== PREFERENCES HOOKS =====

export function useUpdateTeacherPreferences() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async ({ 
      teacherId, 
      preferences 
    }: { 
      teacherId: number; 
      preferences: {
        preferredTimeSlots: string[];
        avoidedTimeSlots: string[];
        maxDailyHours: number;
        preferredRooms: number[];
      }
    }) => {
      const response = await http.put(`/teachers/${teacherId}/preferences`, preferences);
      return response.data.data;
    },
    onSuccess: () => {
      toast.success('Teacher preferences updated successfully!');
      queryClient.invalidateQueries({ queryKey: ['teacher-workloads'] });
    },
    onError: (error: any) => {
      toast.error(`Failed to update preferences: ${error.response?.data?.message || error.message}`);
    }
  });
}

// ===== EXPORT HOOKS =====

export function useExportTimetable() {
  return useMutation({
    mutationFn: async ({ 
      timetableId, 
      format, 
      options 
    }: { 
      timetableId: number; 
      format: 'PDF' | 'EXCEL' | 'CSV'; 
      options?: Record<string, any> 
    }) => {
      const response = await http.post(`/timetables/${timetableId}/export`, {
        format,
        options
      }, {
        responseType: 'blob'
      });
      
      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `timetable-${timetableId}.${format.toLowerCase()}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      
      return response.data;
    },
    onSuccess: (data, variables) => {
      toast.success(`Timetable exported as ${variables.format} successfully!`);
    },
    onError: (error: any) => {
      toast.error(`Export failed: ${error.response?.data?.message || error.message}`);
    }
  });
}
