import { useState } from "react";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { useQuery } from "@tanstack/react-query";

// Hooks and utilities
import { useAuth } from "@/hooks/useAuth";
import { http } from "@/lib/http";

// Types
import { ParentDashboardData, AttendanceData, ChildInfo } from "@/types/parent-dashboard";

// Components
import ParentAttendanceView from "@/components/parent/ParentAttendanceView";

const ParentAttendance = () => {
  const { user } = useAuth();
  const [selectedChild, setSelectedChild] = useState<number | null>(null);
  const [currentWeek, setCurrentWeek] = useState(0);

  // Helper function to get week dates
  const getWeekDates = (weekOffset: number = 0): Date[] => {
    const today = new Date();
    
    // Get current day of week (0 = Sunday, 1 = Monday, ..., 6 = Saturday)
    const currentDayOfWeek = today.getDay();
    
    // Calculate days to subtract to get to Monday
    // If today is Sunday (0), we need to go back 6 days to get to Monday
    // If today is Monday (1), we need to go back 0 days
    // If today is Tuesday (2), we need to go back 1 day, etc.
    const daysToMonday = currentDayOfWeek === 0 ? 6 : currentDayOfWeek - 1;
    
    // Calculate Monday of the current week, then apply week offset
    const monday = new Date(today);
    monday.setDate(today.getDate() - daysToMonday + (weekOffset * 7));
    
    const weekDates: Date[] = [];
    // Monday to Friday
    for (let i = 0; i < 5; i++) {
      const date = new Date(monday);
      date.setDate(monday.getDate() + i);
      weekDates.push(date);
    }
    return weekDates;
  };

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

  // Fetch attendance data for selected child or all children with week support
  const { data: attendanceData, isLoading: isAttendanceLoading, refetch: refetchAttendance } = useQuery<AttendanceData | null>({
    queryKey: ['parent-children-attendance', user?.id, selectedChild, currentWeek],
    queryFn: async (): Promise<AttendanceData | null> => {
      if (!user?.id) return null;
      
      // Calculate date range for the current week
      const weekDates = getWeekDates(currentWeek);
      const startDate = weekDates[0].toISOString().split('T')[0]; // Monday
      
      // Add one day to endDate to make it inclusive of records with timestamps near midnight
      const endDateObj = new Date(weekDates[4]);
      endDateObj.setDate(endDateObj.getDate() + 1);
      const endDate = endDateObj.toISOString().split('T')[0];   // Friday + 1 day
      
      console.log('🎯 Fetching parent attendance for week:', { startDate, endDate, currentWeek });
      
      const response = await http.get(`/v1/dashboard/parent/children-attendance/${user?.id}?startDate=${startDate}&endDate=${endDate}`);
      return response.data || null;
    },
    enabled: !!user?.id,
    staleTime: 2 * 60 * 1000, // 2 minutes - more frequent updates
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
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Unable to load attendance</h2>
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
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">
                Attendance
              </h1>
              <p className="text-gray-600 mt-1">
                Monitor your {children.length} child{children.length !== 1 ? 'ren' : ''}'s attendance records
              </p>
            </div>
            <button
              onClick={() => refetchAttendance()}
              disabled={isAttendanceLoading}
              className={`flex items-center space-x-2 px-4 py-2 rounded-md transition-colors ${
                isAttendanceLoading
                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                  : 'bg-green-600 text-white hover:bg-green-700'
              }`}
            >
              <RefreshCw className={`h-4 w-4 ${isAttendanceLoading ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
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

        {/* Attendance Content */}
        <ParentAttendanceView
          children={children}
          selectedChild={selectedChild}
          attendanceData={attendanceData || null}
          isLoading={isAttendanceLoading}
          onChildSelect={setSelectedChild}
          currentWeek={currentWeek}
          onWeekChange={setCurrentWeek}
        />
      </div>
    </div>
  );
};

export default ParentAttendance;
