import React, { useState, useEffect } from "react";
import { X, User, Calendar, TrendingUp, Clock, CheckCircle, XCircle, AlertCircle } from "lucide-react";
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell
} from "recharts";
import { useQuery } from "@tanstack/react-query";
import { http } from "@/lib/http";
import { ChildInfo } from "@/types/parent-dashboard";

interface ChildDetailsModalProps {
  child: ChildInfo;
  isOpen: boolean;
  onClose: () => void;
}

// Types for API responses
interface AttendanceRecord {
  date: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
  userId: number;
}

interface AttendanceStats {
  totalDays: number;
  presentDays: number;
  absentDays: number;
  attendanceRate: number;
}

export const ChildDetailsModal: React.FC<ChildDetailsModalProps> = ({
  child,
  isOpen,
  onClose
}) => {

  // Get current date and 30 days ago for attendance data
  const endDate = new Date();
  const startDate = new Date();
  startDate.setDate(startDate.getDate() - 30);

  // Fetch attendance records for the child
  const { data: attendanceRecords, isLoading: attendanceLoading } = useQuery<AttendanceRecord[]>({
    queryKey: ['child-attendance', child.studentId, startDate.toISOString().split('T')[0], endDate.toISOString().split('T')[0]],
    queryFn: async (): Promise<AttendanceRecord[]> => {
      const response = await http.get(`/v1/attendance/user/${child.studentId}`, {
        params: {
          startDate: startDate.toISOString().split('T')[0],
          endDate: endDate.toISOString().split('T')[0]
        }
      });
      return response.data || [];
    },
    enabled: !!child.studentId && isOpen,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  // Process attendance data for charts - PER COURSE CALCULATION
  const processAttendanceData = () => {
    if (!attendanceRecords || attendanceRecords.length === 0) {
      return { 
        monthlyData: [], 
        weeklyData: [], 
        statusData: [],
        dailyData: [],
        stats: {
          totalDays: 0,
          presentDays: 0,
          absentDays: 0,
          lateDays: 0,
          attendanceRate: 0
        }
      };
    }

    // Group attendance by date to calculate daily attendance rates
    const attendanceByDate = attendanceRecords.reduce((acc, record) => {
      const date = record.date;
      if (!acc[date]) {
        acc[date] = [];
      }
      acc[date].push(record);
      return acc;
    }, {} as Record<string, AttendanceRecord[]>);

    // Calculate daily statistics
    let totalAttendanceRate = 0;
    let totalDaysWithAttendance = 0;
    let totalAbsentCourses = 0;
    let totalPresentCourses = 0;
    let totalLateCourses = 0;
    const totalCoursesTracked = attendanceRecords.length;

    Object.values(attendanceByDate).forEach(dayAttendance => {
      const presentCourses = dayAttendance.filter(r => r.status === 'PRESENT').length;
      const absentCourses = dayAttendance.filter(r => r.status === 'ABSENT').length;
      const lateCourses = dayAttendance.filter(r => r.status === 'LATE').length;
      
      totalPresentCourses += presentCourses;
      totalAbsentCourses += absentCourses;
      totalLateCourses += lateCourses;
      
      // Calculate daily attendance rate (present courses / total courses that day)
      const dailyAttendanceRate = dayAttendance.length > 0 ? 
        (presentCourses / dayAttendance.length) * 100 : 0;
      
      totalAttendanceRate += dailyAttendanceRate;
      totalDaysWithAttendance++;
    });

    // Calculate overall attendance rate (average of daily rates)
    const attendanceRate = totalDaysWithAttendance > 0 ? 
      Math.round(totalAttendanceRate / totalDaysWithAttendance) : 0;

    // Group by week for weekly chart - now using daily attendance rates
    const weeklyMap = new Map<string, { present: number; absent: number; late: number; startDate: Date; totalDays: number }>();
    
    // Group by month for monthly trend - now using daily attendance rates
    const monthlyMap = new Map<string, { attendanceRate: number; totalDays: number }>();

    // Status distribution for pie chart - now showing course-level data
    const statusData = [
      { name: 'Present', value: totalPresentCourses, color: '#10B981', icon: '✓' },
      { name: 'Absent', value: totalAbsentCourses, color: '#EF4444', icon: '✗' },
      { name: 'Late', value: totalLateCourses, color: '#F59E0B', icon: '⏰' }
    ].filter(item => item.value > 0);

    // Process each day's attendance data
    Object.entries(attendanceByDate).forEach(([dateStr, dayAttendance]) => {
      const date = new Date(dateStr);
      const weekKey = getWeekKey(date);
      const monthKey = date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' });

      // Calculate daily metrics
      const presentCourses = dayAttendance.filter(r => r.status === 'PRESENT').length;
      const absentCourses = dayAttendance.filter(r => r.status === 'ABSENT').length;
      const lateCourses = dayAttendance.filter(r => r.status === 'LATE').length;
      const dailyAttendanceRate = dayAttendance.length > 0 ? 
        (presentCourses / dayAttendance.length) * 100 : 0;

      // Weekly data - aggregate daily attendance
      if (!weeklyMap.has(weekKey.label)) {
        weeklyMap.set(weekKey.label, { 
          present: 0, 
          absent: 0, 
          late: 0, 
          startDate: weekKey.startDate,
          totalDays: 0
        });
      }
      const weekData = weeklyMap.get(weekKey.label)!;
      // For weekly chart, we'll show average daily attendance for that week
      weekData.present += presentCourses;
      weekData.absent += absentCourses;
      weekData.late += lateCourses;
      weekData.totalDays++;

      // Monthly data - track attendance rates
      if (!monthlyMap.has(monthKey)) {
        monthlyMap.set(monthKey, { attendanceRate: 0, totalDays: 0 });
      }
      const monthData = monthlyMap.get(monthKey)!;
      monthData.attendanceRate += dailyAttendanceRate;
      monthData.totalDays++;
    });

    // Convert to arrays and sort
    const weeklyData = Array.from(weeklyMap.entries())
      .map(([week, data]) => ({
        week,
        present: data.present,
        absent: data.absent,
        late: data.late,
        total: data.present + data.absent + data.late,
        rate: data.present + data.absent + data.late > 0 
          ? Math.round((data.present / (data.present + data.absent + data.late)) * 100) 
          : 0
      }))
      .sort((a, b) => a.week.localeCompare(b.week))
      .slice(-6); // Last 6 weeks

    const monthlyData = Array.from(monthlyMap.entries())
      .map(([month, data]) => ({
        month,
        attendanceRate: data.totalDays > 0 ? Math.round(data.attendanceRate / data.totalDays) : 0,
        target: 90,
        totalDays: data.totalDays
      }))
      .sort((a, b) => a.month.localeCompare(b.month));

    // Recent daily data for trend - now showing daily attendance rates
    const dailyData = Object.entries(attendanceByDate)
      .slice(-14) // Last 14 days
      .map(([dateStr, dayAttendance]) => {
        const presentCourses = dayAttendance.filter(r => r.status === 'PRESENT').length;
        const dailyRate = dayAttendance.length > 0 ? (presentCourses / dayAttendance.length) * 100 : 0;
        return {
          date: new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
          attendanceRate: Math.round(dailyRate),
          coursesPresent: presentCourses,
          totalCourses: dayAttendance.length
        };
      });

    return { 
      monthlyData, 
      weeklyData, 
      statusData,
      dailyData,
      stats: {
        totalDays: totalDaysWithAttendance, // Days with attendance records
        presentDays: totalPresentCourses, // Total present courses
        absentDays: totalAbsentCourses, // Total absent courses  
        lateDays: totalLateCourses, // Total late courses
        attendanceRate, // Overall attendance rate (average of daily rates)
        totalCoursesTracked // Total course records
      }
    };
  };

  const getWeekKey = (date: Date) => {
    const startOfWeek = new Date(date);
    startOfWeek.setDate(date.getDate() - date.getDay()); // Start of week (Sunday)
    const endOfWeek = new Date(startOfWeek);
    endOfWeek.setDate(startOfWeek.getDate() + 6);
    
    const startMonth = startOfWeek.toLocaleDateString('en-US', { month: 'short' });
    const startDay = startOfWeek.getDate();
    const endDay = endOfWeek.getDate();
    
    return {
      label: `${startMonth} ${startDay}-${endDay}`,
      startDate: startOfWeek
    };
  };

  const { monthlyData, weeklyData, statusData, dailyData, stats } = processAttendanceData();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-5xl w-full max-h-[85vh] overflow-y-auto">
        {/* Header */}
        <div className="relative bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-800 text-white p-6">
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600/20 to-purple-600/20"></div>
          <div className="relative flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="w-16 h-16 bg-white/20 backdrop-blur-sm rounded-xl flex items-center justify-center border border-white/30">
                <User className="h-8 w-8 text-white" />
              </div>
              <div>
                <h2 className="text-2xl font-bold text-white mb-1">{child.name}</h2>
                <p className="text-blue-100 text-base mb-2">{child.currentClass}</p>
                <div className="flex items-center bg-white/20 backdrop-blur-sm px-3 py-1 rounded-full border border-white/30">
                  <CheckCircle className="h-4 w-4 mr-2" />
                  <span className="font-medium text-sm">{stats.attendanceRate}% Attendance Rate</span>
                </div>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-white/20 rounded-full transition-all duration-200 border border-white/30"
            >
              <X className="h-5 w-5 text-white" />
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="p-6 bg-gray-50/50">
          {/* Enhanced Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-gradient-to-br from-emerald-500 to-green-600 p-4 rounded-xl text-white shadow-lg hover:shadow-xl transition-all duration-300">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-emerald-100 text-xs font-medium mb-1">Present Courses</p>
                  <p className="text-2xl font-bold">{stats.presentDays}</p>
                  <p className="text-emerald-200 text-xs mt-1">{stats.attendanceRate}% daily avg</p>
                </div>
                <div className="bg-white/20 p-2 rounded-lg">
                  <CheckCircle className="h-6 w-6" />
                </div>
              </div>
            </div>
            
            <div className="bg-gradient-to-br from-red-500 to-rose-600 p-4 rounded-xl text-white shadow-lg hover:shadow-xl transition-all duration-300">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-red-100 text-xs font-medium mb-1">Absent Courses</p>
                  <p className="text-2xl font-bold">{stats.absentDays}</p>
                  <p className="text-red-200 text-xs mt-1">{(stats.totalCoursesTracked || 0) > 0 ? Math.round((stats.absentDays / (stats.totalCoursesTracked || 1)) * 100) : 0}% of courses</p>
                </div>
                <div className="bg-white/20 p-2 rounded-lg">
                  <XCircle className="h-6 w-6" />
                </div>
              </div>
            </div>
            
            <div className="bg-gradient-to-br from-amber-500 to-orange-600 p-4 rounded-xl text-white shadow-lg hover:shadow-xl transition-all duration-300">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-amber-100 text-xs font-medium mb-1">Late Courses</p>
                  <p className="text-2xl font-bold">{stats.lateDays}</p>
                  <p className="text-amber-200 text-xs mt-1">{(stats.totalCoursesTracked || 0) > 0 ? Math.round((stats.lateDays / (stats.totalCoursesTracked || 1)) * 100) : 0}% of courses</p>
                </div>
                <div className="bg-white/20 p-2 rounded-lg">
                  <AlertCircle className="h-6 w-6" />
                </div>
              </div>
            </div>
            
            <div className="bg-gradient-to-br from-indigo-500 to-purple-600 p-4 rounded-xl text-white shadow-lg hover:shadow-xl transition-all duration-300">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-indigo-100 text-xs font-medium mb-1">School Days</p>
                  <p className="text-2xl font-bold">{stats.totalDays}</p>
                  <p className="text-indigo-200 text-xs mt-1">{stats.totalCoursesTracked || 0} courses total</p>
                </div>
                <div className="bg-white/20 p-2 rounded-lg">
                  <Calendar className="h-6 w-6" />
                </div>
              </div>
            </div>
          </div>

          {/* Enhanced Charts Section */}
          {attendanceLoading ? (
            <div className="flex flex-col items-center justify-center py-12 bg-white rounded-xl shadow-lg">
              <div className="animate-spin rounded-full h-12 w-12 border-4 border-blue-200 border-t-blue-600 mb-3"></div>
              <p className="text-gray-600">Loading attendance analytics...</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
              {/* Attendance Overview - Pie Chart */}
              <div className="bg-white p-6 rounded-xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300">
                <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                  <div className="w-6 h-6 bg-gradient-to-r from-purple-500 to-pink-500 rounded-lg flex items-center justify-center mr-2">
                    <TrendingUp className="h-4 w-4 text-white" />
                  </div>
                  Attendance Overview
                </h3>
                {statusData.length > 0 ? (
                  <ResponsiveContainer width="100%" height={220}>
                    <PieChart>
                      <Pie
                        data={statusData}
                        cx="50%"
                        cy="50%"
                        innerRadius={45}
                        outerRadius={75}
                        paddingAngle={5}
                        dataKey="value"
                      >
                        {statusData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip 
                        contentStyle={{ 
                          backgroundColor: 'rgba(255, 255, 255, 0.95)', 
                          border: 'none',
                          borderRadius: '12px',
                          boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1)'
                        }}
                        formatter={(value: number, name: string) => [
                          `${value} courses`, 
                          name
                        ]}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex items-center justify-center h-48 text-gray-500">
                    <p>No attendance data available</p>
                  </div>
                )}
                
                {/* Legend */}
                <div className="mt-3 space-y-1">
                  {statusData.map((item, index) => (
                    <div key={index} className="flex items-center justify-between">
                      <div className="flex items-center">
                        <div 
                          className="w-3 h-3 rounded-full mr-2" 
                          style={{ backgroundColor: item.color }}
                        ></div>
                        <span className="text-xs font-medium text-gray-700">{item.name}</span>
                      </div>
                      <span className="text-xs font-bold text-gray-900">{item.value}</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Monthly Trend - Area Chart */}
              <div className="bg-white p-6 rounded-xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300">
                <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                  <div className="w-6 h-6 bg-gradient-to-r from-blue-500 to-cyan-500 rounded-lg flex items-center justify-center mr-2">
                    <TrendingUp className="h-4 w-4 text-white" />
                  </div>
                  Monthly Trend
                </h3>
                <ResponsiveContainer width="100%" height={220}>
                  <AreaChart data={monthlyData}>
                    <defs>
                      <linearGradient id="attendanceGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.8}/>
                        <stop offset="95%" stopColor="#3B82F6" stopOpacity={0.1}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                    <XAxis 
                      dataKey="month" 
                      stroke="#64748b" 
                      fontSize={12}
                      tickLine={false}
                      axisLine={false}
                    />
                    <YAxis 
                      stroke="#64748b" 
                      domain={[0, 100]} 
                      fontSize={12}
                      tickLine={false}
                      axisLine={false}
                    />
                    <Tooltip 
                      contentStyle={{ 
                        backgroundColor: 'rgba(255, 255, 255, 0.95)', 
                        border: 'none',
                        borderRadius: '12px',
                        boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1)'
                      }}
                      formatter={(value: number, name: string) => [
                        `${value}%`, 
                        name === 'attendanceRate' ? 'Attendance' : 'Target'
                      ]}
                    />
                    <Area 
                      type="monotone" 
                      dataKey="attendanceRate" 
                      stroke="#3B82F6" 
                      strokeWidth={3}
                      fill="url(#attendanceGradient)" 
                    />
                    <Line 
                      type="monotone" 
                      dataKey="target" 
                      stroke="#EF4444" 
                      strokeDasharray="5 5"
                      strokeWidth={2}
                      dot={false}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>

              {/* Weekly Pattern - Enhanced Bar Chart */}
              <div className="bg-white p-6 rounded-xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300">
                <h3 className="text-lg font-bold text-gray-900 mb-4 flex items-center">
                  <div className="w-6 h-6 bg-gradient-to-r from-emerald-500 to-teal-500 rounded-lg flex items-center justify-center mr-2">
                    <Calendar className="h-4 w-4 text-white" />
                  </div>
                  Weekly Pattern
                </h3>
                <ResponsiveContainer width="100%" height={220}>
                  <BarChart data={weeklyData} barCategoryGap="20%">
                    <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                    <XAxis 
                      dataKey="week" 
                      stroke="#64748b" 
                      fontSize={11}
                      tickLine={false}
                      axisLine={false}
                      angle={-45}
                      textAnchor="end"
                      height={80}
                    />
                    <YAxis 
                      stroke="#64748b" 
                      fontSize={12}
                      tickLine={false}
                      axisLine={false}
                    />
                    <Tooltip 
                      contentStyle={{ 
                        backgroundColor: 'rgba(255, 255, 255, 0.95)', 
                        border: 'none',
                        borderRadius: '12px',
                        boxShadow: '0 10px 25px -5px rgba(0, 0, 0, 0.1)'
                      }}
                      formatter={(value: number, name: string) => [
                        `${value} courses`, 
                        name === 'present' ? 'Present' : name === 'absent' ? 'Absent' : 'Late'
                      ]}
                    />
                    <Bar dataKey="present" stackId="a" fill="#10B981" radius={[0, 0, 0, 0]} />
                    <Bar dataKey="late" stackId="a" fill="#F59E0B" radius={[0, 0, 0, 0]} />
                    <Bar dataKey="absent" stackId="a" fill="#EF4444" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-gradient-to-r from-gray-50 to-gray-100 border-t border-gray-200 flex justify-between items-center">
          <div className="text-xs text-gray-600">
            <span className="font-medium">Last updated:</span> {new Date().toLocaleDateString()}
          </div>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-medium rounded-lg hover:from-blue-700 hover:to-indigo-700 transition-all duration-200 shadow-lg hover:shadow-xl"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChildDetailsModal;
