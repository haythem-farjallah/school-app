import React, { useState } from "react";
import { Calendar, Clock, CheckCircle, XCircle, MinusCircle, ChevronLeft, ChevronRight, User } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { useQuery } from "@tanstack/react-query";
import { http } from "@/lib/http";

interface StudentAttendanceViewProps {
  isLoading?: boolean;
}

interface AttendanceRecord {
  id: number;
  date: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
  remarks?: string;
  courseName?: string;
  teacherName?: string;
}

interface StudentAttendanceData {
  studentId: number;
  studentName: string;
  attendanceRate: number;
  presentDays: number;
  absentDays: number;
  lateDays: number;
  excusedDays: number;
  recentAttendance: AttendanceRecord[];
  attendanceByDate: { [key: string]: AttendanceRecord };
}

// Time slots for the day (8 AM to 4 PM)
const TIME_SLOTS = [
  "08:00", "09:00", "10:00", "11:00", "12:00", 
  "13:00", "14:00", "15:00", "16:00"
];

const DAYS = [
  "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Sunday"
];

// Get week date range for a specific week offset (Monday-Friday + Sunday)
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
  // Sunday
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  weekDates.push(sunday);
  
  return weekDates;
};

// Format time to match display format
const formatTimeForDisplay = (time: string) => {
  const [hours, minutes] = time.split(':');
  const hour = parseInt(hours);
  const ampm = hour >= 12 ? 'PM' : 'AM';
  const displayHour = hour > 12 ? hour - 12 : (hour === 0 ? 12 : hour);
  return `${displayHour}:${minutes} ${ampm}`;
};

// Get attendance status color and icon
const getAttendanceStatusDisplay = (status: string) => {
  switch (status?.toUpperCase()) {
    case 'PRESENT':
      return {
        color: 'bg-green-100 border-green-300 text-green-800',
        icon: CheckCircle,
        label: 'Present',
        badgeColor: 'bg-green-500'
      };
    case 'ABSENT':
      return {
        color: 'bg-red-100 border-red-300 text-red-800',
        icon: XCircle,
        label: 'Absent',
        badgeColor: 'bg-red-500'
      };
    case 'LATE':
      return {
        color: 'bg-yellow-100 border-yellow-300 text-yellow-800',
        icon: Clock,
        label: 'Late',
        badgeColor: 'bg-yellow-500'
      };
    case 'EXCUSED':
      return {
        color: 'bg-blue-100 border-blue-300 text-blue-800',
        icon: MinusCircle,
        label: 'Excused',
        badgeColor: 'bg-blue-500'
      };
    default:
      return {
        color: 'bg-gray-100 border-gray-300 text-gray-800',
        icon: MinusCircle,
        label: 'Not Recorded',
        badgeColor: 'bg-gray-400'
      };
  }
};

export const StudentAttendanceView: React.FC<StudentAttendanceViewProps> = ({
  isLoading: propIsLoading
}) => {
  const { user } = useAuth();
  const [currentWeek, setCurrentWeek] = useState(0);
  const weekDates = getWeekDates(currentWeek);

  // Fetch student attendance data
  const { data: attendanceData, isLoading: attendanceLoading } = useQuery<StudentAttendanceData>({
    queryKey: ['student-attendance', user?.id, currentWeek],
    queryFn: async (): Promise<StudentAttendanceData> => {
      if (!user?.id) throw new Error('No user ID');
      
      console.log('🎯 Fetching attendance for student:', user.id, 'week offset:', currentWeek);
      
      // Calculate date range for the current week
      const weekDates = getWeekDates(currentWeek);
      const startDate = weekDates[0].toISOString().split('T')[0]; // Monday
      
      // Add one day to endDate to make it inclusive of records with timestamps near midnight
      const endDateObj = new Date(weekDates[4]);
      endDateObj.setDate(endDateObj.getDate() + 1);
      const endDate = endDateObj.toISOString().split('T')[0];   // Friday + 1 day
      
      console.log('🎯 Date range:', { startDate, endDate });
      
      try {
        // Fetch attendance data from API
        const response = await http.get(`/v1/attendance/user/${user.id}?startDate=${startDate}&endDate=${endDate}`);
        console.log('🎯 Attendance API response:', response);
        
        const apiAttendanceRecords = response.data?.data || response.data || [];
        console.log('🎯 API attendance records:', apiAttendanceRecords);
        
        // Transform API data to match frontend expectations
        const recentAttendance: AttendanceRecord[] = apiAttendanceRecords.map((record: any) => ({
          id: record.id,
          date: record.date,
          status: record.status,
          remarks: record.remarks || record.excuse || record.medicalNote,
          courseName: record.courseName || record.className || 'Unknown Course',
          teacherName: record.teacherName || 'Unknown Teacher'
        }));
        
        // Create attendance by date mapping
        const attendanceByDate: { [key: string]: AttendanceRecord } = {};
        recentAttendance.forEach(record => {
          attendanceByDate[record.date] = record;
        });
        
        // Calculate statistics
        const presentDays = recentAttendance.filter(r => r.status === 'PRESENT').length;
        const absentDays = recentAttendance.filter(r => r.status === 'ABSENT').length;
        const lateDays = recentAttendance.filter(r => r.status === 'LATE').length;
        const excusedDays = recentAttendance.filter(r => r.status === 'EXCUSED').length;
        const totalDays = recentAttendance.length;
        const attendanceRate = totalDays > 0 ? Math.round((presentDays / totalDays) * 100) : 0;
        
        const transformedData: StudentAttendanceData = {
          studentId: user.id,
          studentName: `${user.firstName} ${user.lastName}`,
          attendanceRate,
          presentDays,
          absentDays,
          lateDays,
          excusedDays,
          recentAttendance,
          attendanceByDate
        };
        
        console.log('🎯 Transformed attendance data:', transformedData);
        return transformedData;
        
      } catch (error) {
        console.error('🎯 Error fetching attendance:', error);
        
        // Return empty data structure on error
        return {
          studentId: user.id,
          studentName: `${user.firstName} ${user.lastName}`,
          attendanceRate: 0,
          presentDays: 0,
          absentDays: 0,
          lateDays: 0,
          excusedDays: 0,
          recentAttendance: [],
          attendanceByDate: {}
        };
      }
    },
    enabled: !!user?.id,
    staleTime: 5 * 60 * 1000, // 5 minutes
    retry: 3
  });

  const isLoading = propIsLoading || attendanceLoading;

  // Navigation handlers
  const goToPreviousWeek = () => {
    setCurrentWeek(prev => prev - 1);
  };

  const goToNextWeek = () => {
    // Prevent navigation to future weeks
    if (currentWeek < 0) {
      setCurrentWeek(prev => prev + 1);
    }
  };

  const goToCurrentWeek = () => {
    setCurrentWeek(0);
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-6 bg-gray-200 rounded w-1/4"></div>
          <div className="grid grid-cols-7 gap-4">
            {Array.from({ length: 35 }).map((_, i) => (
              <div key={i} className="h-20 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (!attendanceData) {
    return (
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="text-center text-gray-500">
          No attendance data available
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Week Navigation */}
      <div className="bg-white rounded-lg shadow-sm p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <h2 className="text-xl font-semibold text-gray-900 flex items-center">
              <Calendar className="h-5 w-5 mr-2" />
              My Attendance
            </h2>
            <div className="text-sm text-gray-600">
              {weekDates[0].toLocaleDateString()} - {weekDates[4].toLocaleDateString()}
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            <button 
              onClick={goToPreviousWeek}
              className="p-2 hover:bg-gray-100 rounded-md transition-colors"
              title="Previous Week"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              onClick={goToCurrentWeek}
              className={`text-sm font-medium px-3 py-1 rounded-md transition-colors ${
                currentWeek === 0 
                  ? 'bg-green-100 text-green-700' 
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {currentWeek === 0 ? 'Current Week' : `${Math.abs(currentWeek)} Week${Math.abs(currentWeek) > 1 ? 's' : ''} ${currentWeek < 0 ? 'Ago' : 'Ahead'}`}
            </button>
            <button 
              onClick={goToNextWeek}
              disabled={currentWeek >= 0}
              className={`p-2 rounded-md transition-colors ${
                currentWeek >= 0 
                  ? 'bg-gray-100 text-gray-400 cursor-not-allowed' 
                  : 'hover:bg-gray-100 text-gray-700'
              }`}
              title={currentWeek >= 0 ? "Cannot view future weeks" : "Next Week"}
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Legend */}
      <div className="bg-white rounded-lg shadow-sm p-4">
        <h3 className="text-sm font-medium text-gray-900 mb-3">Attendance Status Legend</h3>
        <div className="flex flex-wrap items-center gap-4">
          {[
            { status: 'PRESENT', label: 'Present' },
            { status: 'ABSENT', label: 'Absent' },
            { status: 'LATE', label: 'Late' },
            { status: 'EXCUSED', label: 'Excused' }
          ].map(({ status, label }) => {
            const { badgeColor } = getAttendanceStatusDisplay(status);
            return (
              <div key={status} className="flex items-center space-x-2">
                <div className={`w-3 h-3 rounded-full ${badgeColor}`}></div>
                <span className="text-sm text-gray-700">{label}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Attendance Grid */}
      <div className="bg-white rounded-lg shadow-sm overflow-hidden">
        {/* Student Header with Stats */}
        <div className="bg-gradient-to-r from-green-600 to-green-700 px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                <User className="h-5 w-5 text-white" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">
                  My Attendance ({attendanceData.studentName})
                </h3>
                <p className="text-green-100 text-sm">
                  {attendanceData.attendanceRate}% attendance rate
                </p>
              </div>
            </div>
            
            <div className="flex items-center space-x-6 text-white">
              <div className="text-center">
                <div className="text-xl font-bold">{attendanceData.presentDays}</div>
                <div className="text-xs text-green-100">Present</div>
              </div>
              <div className="text-center">
                <div className="text-xl font-bold">{attendanceData.absentDays}</div>
                <div className="text-xs text-green-100">Absent</div>
              </div>
              <div className="text-center">
                <div className="text-xl font-bold">{attendanceData.lateDays}</div>
                <div className="text-xs text-green-100">Late</div>
              </div>
            </div>
          </div>
        </div>

        {/* Simplified Attendance Summary */}
        <div className="p-6">
          <h4 className="text-lg font-medium text-gray-900 mb-4">Recent Attendance (Last 7 days)</h4>
          <div className="space-y-3">
            {attendanceData.recentAttendance.slice(0, 7).map((record) => {
              const { badgeColor, label, icon: Icon } = getAttendanceStatusDisplay(record.status);
              return (
                <div key={record.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div className="flex items-center space-x-3">
                    <div className={`w-3 h-3 rounded-full ${badgeColor}`}></div>
                    <div>
                      <div className="font-medium text-gray-900">
                        {new Date(record.date).toLocaleDateString('en-US', { 
                          weekday: 'long', 
                          month: 'short', 
                          day: 'numeric' 
                        })}
                      </div>
                      {record.courseName && (
                        <div className="text-sm text-gray-600">{record.courseName}</div>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Icon className="h-4 w-4 text-gray-600" />
                    <span className="font-medium text-gray-900">{label}</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default StudentAttendanceView;
