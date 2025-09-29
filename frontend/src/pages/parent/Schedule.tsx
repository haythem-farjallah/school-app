import { useState } from "react";
import { AlertTriangle } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

// Hooks and utilities
import { useAuth } from "@/hooks/useAuth";
import { http } from "@/lib/http";

// Types
import { ParentDashboardData, TimetableData, ChildInfo } from "@/types/parent-dashboard";

// Components
import ParentWeeklyTimetable from "@/components/parent/ParentWeeklyTimetable";

const ParentSchedule = () => {
  const { user } = useAuth();
  const [selectedChild, setSelectedChild] = useState<number | null>(null);

  // Fetch parent dashboard data (includes children info)
  const { data: dashboardData, isLoading: isChildrenLoading, error } = useQuery<ParentDashboardData | null>({
    queryKey: ['parent-dashboard', user?.id],
    queryFn: async (): Promise<ParentDashboardData | null> => {
      if (!user?.id) return null;
      const response = await http.get(`/v1/dashboard/parent/${user?.id}`);
      return response.data || null;
    },
    enabled: !!user?.id,
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 3
  });

  // Fetch timetables for selected child or all children
  const { data: timetableData, isLoading: isTimetableLoading } = useQuery<TimetableData | null>({
    queryKey: ['parent-children-timetables', user?.id, selectedChild],
    queryFn: async (): Promise<TimetableData | null> => {
      if (!user?.id) return null;
      const response = await http.get(`/v1/dashboard/parent/children-timetables/${user?.id}`);
      return response.data || null;
    },
    enabled: !!user?.id,
    staleTime: 10 * 60 * 1000, // 10 minutes
    retry: 3
  });

  if (isChildrenLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen">
        <AlertTriangle className="h-12 w-12 text-red-500 mb-4" />
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Unable to load timetables</h2>
        <p className="text-gray-600 mb-4">Please try refreshing the page or contact support if the issue persists.</p>
        <button 
          onClick={() => window.location.reload()} 
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Refresh Page
        </button>
      </div>
    );
  }

  const children: ChildInfo[] = dashboardData?.children || [];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Timetables
            </h1>
            <p className="text-gray-600 mt-1">
              View your {children.length} child{children.length !== 1 ? 'ren' : ''}'s weekly schedules
            </p>
          </div>
        </div>
      </div>

      {/* Children Selector */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        {children.length > 1 && (
          <div className="flex items-center space-x-4 mb-6">
            <label className="text-sm font-medium text-gray-700">Select Child:</label>
            <select
              value={selectedChild || 'all'}
              onChange={(e) => setSelectedChild(e.target.value === 'all' ? null : Number(e.target.value))}
              className="px-3 py-2 border border-gray-300 rounded-md bg-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="all">All Children</option>
              {children.map((child: ChildInfo) => (
                <option key={child.studentId} value={child.studentId}>
                  {child.name} - {child.currentClass}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Timetable Content */}
        <ParentWeeklyTimetable
          children={children}
          selectedChild={selectedChild}
          timetableData={timetableData || null}
          isLoading={isTimetableLoading}
          onChildSelect={setSelectedChild}
        />
      </div>
    </div>
  );
};

export default ParentSchedule;
