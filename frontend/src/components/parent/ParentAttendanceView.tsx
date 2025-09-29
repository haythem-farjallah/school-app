import React, { useState } from "react";
import { Calendar, Clock, User, CheckCircle, XCircle, MinusCircle, ChevronLeft, ChevronRight } from "lucide-react";
import { ChildAttendance, AttendanceData, ChildInfo } from "@/types/parent-dashboard";

interface ParentAttendanceViewProps {
  children: ChildInfo[];
  selectedChild: number | null;
  attendanceData: AttendanceData | null;
  isLoading: boolean;
  onChildSelect: (childId: number | null) => void;
  currentWeek?: number;
  onWeekChange?: (week: number) => void;
}

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

export const ParentAttendanceView: React.FC<ParentAttendanceViewProps> = ({
  selectedChild,
  attendanceData,
  isLoading,
  currentWeek = 0,
  onWeekChange
}) => {
  const weekDates = getWeekDates(currentWeek);

  // Navigation handlers
  const goToPreviousWeek = () => {
    if (onWeekChange) {
      onWeekChange(currentWeek - 1);
    }
  };

  const goToNextWeek = () => {
    // Prevent navigation to future weeks
    if (currentWeek < 0 && onWeekChange) {
      onWeekChange(currentWeek + 1);
    }
  };

  const goToCurrentWeek = () => {
    if (onWeekChange) {
      onWeekChange(0);
    }
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

  // Process attendance data
  const childrenAttendance: ChildAttendance[] = attendanceData?.children || [];

  // Filter attendance based on selected child
  const displayAttendance = selectedChild 
    ? childrenAttendance.filter(a => a.studentId === selectedChild)
    : childrenAttendance;

  return (
    <div className="space-y-6">
      {/* Week Navigation */}
      <div className="bg-white rounded-lg shadow-sm p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <h2 className="text-xl font-semibold text-gray-900 flex items-center">
              <Calendar className="h-5 w-5 mr-2" />
              Weekly Attendance
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

      {/* Show message if no children or no attendance data */}
      {displayAttendance.length === 0 && !isLoading && (
        <div className="bg-white rounded-lg shadow-sm p-8">
          <div className="text-center text-gray-500">
            <Calendar className="h-16 w-16 mx-auto mb-4 text-gray-300" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No Attendance Data</h3>
            <p className="text-gray-600">
              {selectedChild 
                ? "No attendance records found for the selected child in this time period."
                : "No attendance records found for any children in this time period."
              }
            </p>
            <button
              onClick={goToCurrentWeek}
              className="mt-4 px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors"
            >
              View Current Week
            </button>
          </div>
        </div>
      )}

      {/* Attendance for each child */}
      {displayAttendance.map((childAttendance) => (
        <div key={childAttendance.studentId} className="bg-white rounded-lg shadow-sm overflow-hidden">
          {/* Child Header with Stats */}
          <div className="bg-gradient-to-r from-green-600 to-green-700 px-6 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                  <User className="h-5 w-5 text-white" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-white">
                    Attendance ({childAttendance.studentName})
                  </h3>
                  <p className="text-green-100 text-sm">
                    {childAttendance.attendanceRate}% attendance rate
                  </p>
                </div>
              </div>
              
              <div className="flex items-center space-x-6 text-white">
                <div className="text-center">
                  <div className="text-xl font-bold">{childAttendance.presentDays}</div>
                  <div className="text-xs text-green-100">Present</div>
                </div>
                <div className="text-center">
                  <div className="text-xl font-bold">{childAttendance.absentDays}</div>
                  <div className="text-xs text-green-100">Absent</div>
                </div>
                <div className="text-center">
                  <div className="text-xl font-bold">{childAttendance.lateDays}</div>
                  <div className="text-xs text-green-100">Late</div>
                </div>
              </div>
            </div>
          </div>

          {/* Simplified Attendance List - Same as Student Dashboard */}
          <div className="p-6">
            <h4 className="text-lg font-medium text-gray-900 mb-4">Recent Attendance (Last 7 days)</h4>
            <div className="space-y-3">
              {childAttendance.recentAttendance.slice(0, 7).map((record) => {
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
                        {record.remarks && (
                          <div className="text-sm text-gray-500 mt-1">{record.remarks}</div>
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
              
              {/* Show message if no attendance records */}
              {childAttendance.recentAttendance.length === 0 && (
                <div className="text-center py-8 text-gray-500">
                  <Calendar className="h-12 w-12 mx-auto mb-3 text-gray-300" />
                  <p className="text-lg font-medium">No attendance records found</p>
                  <p className="text-sm">Attendance records will appear here once available</p>
                </div>
              )}
            </div>
          </div>
        </div>
      ))}

    </div>
  );
};

export default ParentAttendanceView;
