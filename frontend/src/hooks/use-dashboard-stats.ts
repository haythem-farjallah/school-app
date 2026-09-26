import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api-client';
import type { ApiResponse } from '@/types/level';
import { useAppSelector } from '@/stores/store';

interface SystemStats {
  totalStudents: number;
  totalTeachers: number;
  totalParents: number;
  totalClasses: number;
  totalCourses: number;
  activeEnrollments: number;
  systemHealth: number;
  serverStatus: string;
}

interface AdminDashboardResponse {
  baseInfo: {
    user: {
      id: number;
      firstName: string;
      lastName: string;
      email: string;
      role: string;
    };
    welcomeMessage: string;
    lastLogin: string;
    notifications: any[];
    quickActions: any[];
  };
  type: string;
  systemStats: SystemStats;
  systemAlerts: any[];
  enrollmentTrends: Record<string, number>;
  performanceMetrics: Record<string, number>;
  recentSystemActivities: any[];
}

export function useDashboardStats() {
  const user = useAppSelector(state => state.auth.user);
  
  return useQuery({
    queryKey: ['dashboard-stats', user?.id],
    queryFn: async (): Promise<SystemStats> => {
      if (!user?.id) {
        throw new Error('User not authenticated');
      }
      
      const response = await api.get<ApiResponse<AdminDashboardResponse>>(`/v1/dashboard/admin/${user.id}`);
      return response.data.data.systemStats;
    },
    enabled: !!user?.id && user?.role === 'ADMIN',
    refetchInterval: 30000, // Refresh every 30 seconds
    staleTime: 10000, // Consider data stale after 10 seconds
  });
}
