import React, { useState } from 'react';
import { Calendar, CheckCircle, XCircle, Clock, AlertTriangle } from 'lucide-react';

interface AttendanceRecord {
  id: number;
  date: string;
  status: string;
  remarks: string;
  markedAt: string;
}

interface ChildAttendance {
  studentId: number;
  studentName: string;
  totalDays: number;
  presentDays: number;
  absentDays: number;
  lateDays: number;
  attendanceRate: number;
  attendanceByDate: { [key: string]: { status: string; remarks: string; markedAt: string } };
  recentAttendance: AttendanceRecord[];
}

interface ParentChildrenAttendanceProps {
  data: {
    children: ChildAttendance[];
    dateRange: {
      startDate: string;
      endDate: string;
    };
  };
  selectedChild?: number | null;
  onChildSelect: (childId: number | null) => void;
}

export const ParentChildrenAttendance: React.FC<ParentChildrenAttendanceProps> = ({
  data,
  selectedChild,
  onChildSelect
}) => {
  const [activeTab, setActiveTab] = useState<number>(
    selectedChild || (data.children.length > 0 ? data.children[0].studentId : 0)
  );
  const [viewMode, setViewMode] = useState<'calendar' | 'list'>('calendar');

  const activeChildData = data.children.find(child => child.studentId === activeTab);

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'PRESENT':
        return <CheckCircle className="h-4 w-4 text-green-600" />;
      case 'ABSENT':
        return <XCircle className="h-4 w-4 text-red-600" />;
      case 'LATE':
        return <Clock className="h-4 w-4 text-yellow-600" />;
      default:
        return <AlertTriangle className="h-4 w-4 text-gray-400" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PRESENT':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'ABSENT':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'LATE':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const generateCalendarDays = () => {
    if (!activeChildData) return [];
    
    const startDate = new Date(data.dateRange.startDate);
    const endDate = new Date(data.dateRange.endDate);
    const days = [];
    
    const currentDate = new Date(startDate);
    while (currentDate <= endDate) {
      const dateStr = currentDate.toISOString().split('T')[0];
      const attendanceData = activeChildData.attendanceByDate[dateStr];
      
      days.push({
        date: new Date(currentDate),
        dateStr,
        attendance: attendanceData
      });
      
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    return days;
  };

  const calendarDays = generateCalendarDays();

  if (!data.children.length) {
    return (
      <div className="text-center py-8">
        <Calendar className="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p className="text-gray-500">No attendance data available</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Child Selection Tabs */}
      <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg">
        {data.children.map((child) => (
          <button
            key={child.studentId}
            onClick={() => {
              setActiveTab(child.studentId);
              onChildSelect(child.studentId);
            }}
            className={`flex-1 px-4 py-2 text-sm font-medium rounded-md transition-colors ${
              activeTab === child.studentId
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {child.studentName}
          </button>
        ))}
      </div>

      {/* View Mode Toggle */}
      <div className="flex justify-between items-center">
        <div className="flex space-x-1 bg-gray-100 p-1 rounded-lg">
          <button
            onClick={() => setViewMode('calendar')}
            className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${
              viewMode === 'calendar'
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            Calendar View
          </button>
          <button
            onClick={() => setViewMode('list')}
            className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${
              viewMode === 'list'
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            List View
          </button>
        </div>

        {activeChildData && (
          <div className="text-sm text-gray-600">
            Period: {new Date(data.dateRange.startDate).toLocaleDateString()} - {new Date(data.dateRange.endDate).toLocaleDateString()}
          </div>
        )}
      </div>

      {/* Statistics Summary */}
      {activeChildData && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-green-50 rounded-lg p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{activeChildData.presentDays}</div>
            <div className="text-sm text-green-700">Present Days</div>
          </div>
          <div className="bg-red-50 rounded-lg p-4 text-center">
            <div className="text-2xl font-bold text-red-600">{activeChildData.absentDays}</div>
            <div className="text-sm text-red-700">Absent Days</div>
          </div>
          <div className="bg-yellow-50 rounded-lg p-4 text-center">
            <div className="text-2xl font-bold text-yellow-600">{activeChildData.lateDays}</div>
            <div className="text-sm text-yellow-700">Late Days</div>
          </div>
          <div className="bg-blue-50 rounded-lg p-4 text-center">
            <div className="text-2xl font-bold text-blue-600">{activeChildData.attendanceRate}%</div>
            <div className="text-sm text-blue-700">Attendance Rate</div>
          </div>
        </div>
      )}

      {/* Calendar View */}
      {viewMode === 'calendar' && activeChildData && (
        <div className="bg-white rounded-lg border">
          <div className="grid grid-cols-7 gap-1 p-4">
            {/* Calendar header */}
            {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
              <div key={day} className="text-center text-sm font-medium text-gray-700 p-2">
                {day}
              </div>
            ))}
            
            {/* Calendar days */}
            {calendarDays.map((day, index) => {
              const isWeekend = day.date.getDay() === 0 || day.date.getDay() === 6;
              
              return (
                <div
                  key={index}
                  className={`p-2 text-center text-sm border rounded-lg ${
                    isWeekend ? 'bg-gray-50' : 'bg-white'
                  } ${
                    day.attendance
                      ? getStatusColor(day.attendance.status)
                      : 'border-gray-200'
                  }`}
                >
                  <div className="font-medium">{day.date.getDate()}</div>
                  {day.attendance && (
                    <div className="mt-1 flex justify-center">
                      {getStatusIcon(day.attendance.status)}
                    </div>
                  )}
                  {!day.attendance && !isWeekend && (
                    <div className="mt-1 text-xs text-gray-400">No data</div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* List View */}
      {viewMode === 'list' && activeChildData && (
        <div className="bg-white rounded-lg border">
          <div className="p-4">
            <h4 className="font-medium text-gray-900 mb-4">Recent Attendance Records</h4>
            <div className="space-y-3">
              {activeChildData.recentAttendance.map((record) => (
                <div key={record.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div className="flex items-center space-x-3">
                    {getStatusIcon(record.status)}
                    <div>
                      <div className="font-medium text-gray-900">
                        {new Date(record.date).toLocaleDateString()}
                      </div>
                      <div className="text-sm text-gray-600">
                        {new Date(record.date).toLocaleDateString('en-US', { weekday: 'long' })}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className={`inline-flex px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(record.status)}`}>
                      {record.status}
                    </div>
                    {record.remarks && (
                      <div className="text-xs text-gray-500 mt-1">{record.remarks}</div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Legend */}
      <div className="bg-gray-50 rounded-lg p-4">
        <h4 className="font-medium text-gray-900 mb-3">Status Legend</h4>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
          <div className="flex items-center">
            <CheckCircle className="h-4 w-4 text-green-600 mr-2" />
            <span>Present</span>
          </div>
          <div className="flex items-center">
            <XCircle className="h-4 w-4 text-red-600 mr-2" />
            <span>Absent</span>
          </div>
          <div className="flex items-center">
            <Clock className="h-4 w-4 text-yellow-600 mr-2" />
            <span>Late</span>
          </div>
        </div>
      </div>
    </div>
  );
};
