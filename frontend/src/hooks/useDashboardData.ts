import { useQuery } from "@tanstack/react-query";
import { http } from "@/lib/http";
import { useAuth } from "@/hooks/useAuth";
import type { UserRole } from "@/hooks/useAuth";

// Generic dashboard data interface
interface DashboardData {
  baseInfo: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    role: UserRole;
  };
  type: UserRole;
  stats: Record<string, unknown>;
  recentActivities: Array<{
    id: string;
    title: string;
    description: string;
    timestamp: string;
    type: string;
  }>;
  upcomingEvents: Array<{
    id: string;
    title: string;
    dateTime: string;
    type: string;
    location?: string;
  }>;
  notifications: Array<{
    id: string;
    title: string;
    message: string;
    type: "info" | "warning" | "error" | "success";
    isRead: boolean;
    createdAt: string;
  }>;
}

// Teacher-specific dashboard data
interface TeacherDashboardData extends DashboardData {
  type: "TEACHER";
  stats: {
    totalClasses: number;
    totalStudents: number;
    pendingGrades: number;
    attendanceRate: number;
    weeklyHours: number;
    upcomingClasses: number;
  };
  teachingAssignments: Array<{
    id: number;
    classId: number;
    className: string;
    courseId: number;
    courseName: string;
    courseCode: string;
    weeklyHours: number;
  }>;
  todaySchedule: Array<{
    id: string;
    title: string;
    startTime: string;
    endTime: string;
    location: string;
    studentCount: number;
  }>;
}

// Student-specific dashboard data
interface StudentDashboardData extends DashboardData {
  type: "STUDENT";
  stats: {
    enrolledCourses: number;
    assignmentsDue: number;
    averageGrade: number;
    attendanceRate: number;
    completedAssignments: number;
    upcomingExams: number;
  };
  enrollments: Array<{
    id: number;
    course: {
      id: number;
      name: string;
      code: string;
      credits: number;
    };
    class: {
      id: number;
      name: string;
    };
    currentGrade?: number;
  }>;
  upcomingAssignments: Array<{
    id: number;
    title: string;
    dueDate: string;
    courseName: string;
    priority: "high" | "medium" | "low";
  }>;
}

// Parent-specific dashboard data
interface ParentDashboardData extends DashboardData {
  type: "PARENT";
  stats: {
    totalChildren: number;
    upcomingEvents: number;
    unreadMessages: number;
    avgGradeAllChildren: number;
  };
  children: Array<{
    id: number;
    firstName: string;
    lastName: string;
    grade: string;
    class: string;
    averageGrade: number;
    attendanceRate: number;
    recentGrades: Array<{
      courseName: string;
      score: number;
      date: string;
    }>;
  }>;
  parentTeacherMeetings: Array<{
    id: number;
    teacherName: string;
    courseName: string;
    dateTime: string;
    childName: string;
    status: "scheduled" | "completed" | "cancelled";
  }>;
}

// Staff-specific dashboard data
interface StaffDashboardData extends DashboardData {
  type: "STAFF";
  stats: {
    pendingEnrollments: number;
    activeStudents: number;
    roomUtilization: number;
    systemHealth: number;
    supportTickets: number;
    scheduleChanges: number;
  };
  operationalMetrics: {
    enrollmentsToday: number;
    roomBookingsToday: number;
    messagesProcessed: number;
    reportsGenerated: number;
  };
}

// Admin dashboard data (existing interface)
interface AdminDashboardData extends DashboardData {
  type: "ADMIN";
  stats: {
    totalStudents: number;
    totalTeachers: number;
    totalParents: number;
    totalClasses: number;
    totalCourses: number;
    activeEnrollments: number;
    systemHealth: number;
    serverStatus: string;
  };
}

// Union type for all dashboard data
type RoleDashboardData = 
  | TeacherDashboardData 
  | StudentDashboardData 
  | ParentDashboardData 
  | StaffDashboardData 
  | AdminDashboardData;

/**
 * Unified hook for fetching role-based dashboard data
 */
export function useDashboardData(userId?: number) {
  const { user } = useAuth();
  const targetUserId = userId || user?.id;
  const userRole = user?.role;

  return useQuery({
    queryKey: ["dashboard", userRole, targetUserId],
    queryFn: async (): Promise<RoleDashboardData> => {
      if (!targetUserId || !userRole) {
        throw new Error("User ID and role are required");
      }

      const endpoint = getDashboardEndpoint(userRole, targetUserId);
      const response = await http.get<RoleDashboardData>(endpoint);
      return response.data;
    },
    enabled: !!targetUserId && !!userRole,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
    refetchOnWindowFocus: false,
    retry: 3,
  });
}

/**
 * Hook for fetching role-specific statistics
 */
export function useRoleSpecificStats(role: UserRole, userId?: number) {
  const { user } = useAuth();
  const targetUserId = userId || user?.id;

  return useQuery({
    queryKey: ["stats", role, targetUserId],
    queryFn: async () => {
      if (!targetUserId) {
        throw new Error("User ID is required");
      }

      const endpoint = getStatsEndpoint(role, targetUserId);
      const response = await http.get(endpoint);
      return response.data;
    },
    enabled: !!targetUserId,
    staleTime: 2 * 60 * 1000, // 2 minutes
    refetchInterval: 5 * 60 * 1000, // Auto-refresh every 5 minutes
  });
}

/**
 * Hook for fetching schedule data
 */
export function useScheduleData(role: UserRole, userId?: number, date?: string) {
  const { user } = useAuth();
  const targetUserId = userId || user?.id;
  const targetDate = date || new Date().toISOString().split('T')[0]; // Today by default

  return useQuery({
    queryKey: ["schedule", role, targetUserId, targetDate],
    queryFn: async () => {
      if (!targetUserId) {
        throw new Error("User ID is required");
      }

      const endpoint = getScheduleEndpoint(role, targetUserId);
      const response = await http.get(endpoint, {
        params: { date: targetDate }
      });
      return response.data;
    },
    enabled: !!targetUserId,
    staleTime: 1 * 60 * 1000, // 1 minute
    refetchInterval: 2 * 60 * 1000, // Auto-refresh every 2 minutes
  });
}

/**
 * Hook for fetching notifications
 */
export function useNotifications(userId?: number) {
  const { user } = useAuth();
  const targetUserId = userId || user?.id;

  return useQuery({
    queryKey: ["notifications", targetUserId],
    queryFn: async () => {
      if (!targetUserId) {
        throw new Error("User ID is required");
      }

      const response = await http.get(`/v1/notifications/user/${targetUserId}`);
      return response.data;
    },
    enabled: !!targetUserId,
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: 60 * 1000, // Auto-refresh every minute for real-time feel
  });
}

/**
 * Hook for fetching announcements by role
 */
export function useRoleAnnouncements(role: UserRole, limit = 5) {
  return useQuery({
    queryKey: ["announcements", role, limit],
    queryFn: async () => {
      const response = await http.get("/v1/announcements", {
        params: {
          targetRole: role,
          limit,
          onlyActive: true
        }
      });
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchInterval: 10 * 60 * 1000, // Auto-refresh every 10 minutes
  });
}

// Helper functions
function getDashboardEndpoint(role: UserRole, userId: number): string {
  const roleEndpoints: Record<UserRole, string> = {
    ADMIN: `/v1/dashboard/admin/${userId}`,
    TEACHER: `/v1/dashboard/teacher/${userId}`,
    STUDENT: `/v1/dashboard/student/${userId}`,
    PARENT: `/v1/dashboard/parent/${userId}`,
    STAFF: `/v1/dashboard/staff/${userId}`,
  };

  return roleEndpoints[role];
}

function getStatsEndpoint(role: UserRole, userId: number): string {
  const statsEndpoints: Record<UserRole, string> = {
    ADMIN: `/v1/stats/admin/${userId}`,
    TEACHER: `/v1/stats/teacher/${userId}`,
    STUDENT: `/v1/stats/student/${userId}`,
    PARENT: `/v1/stats/parent/${userId}`,
    STAFF: `/v1/stats/staff/${userId}`,
  };

  return statsEndpoints[role];
}

function getScheduleEndpoint(role: UserRole, userId: number): string {
  const scheduleEndpoints: Record<UserRole, string> = {
    ADMIN: `/v1/schedule/admin/${userId}`,
    TEACHER: `/v1/schedule/teacher/${userId}`,
    STUDENT: `/v1/schedule/student/${userId}`,
    PARENT: `/v1/schedule/parent/${userId}`,
    STAFF: `/v1/schedule/staff/${userId}`,
  };

  return scheduleEndpoints[role];
}

// Export types for use in components
export type {
  DashboardData,
  TeacherDashboardData,
  StudentDashboardData,
  ParentDashboardData,
  StaffDashboardData,
  AdminDashboardData,
  RoleDashboardData,
};
