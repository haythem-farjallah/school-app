import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { 
  TrendingUp,
  TrendingDown,
  Users,
  Calendar,
  Award,
  AlertTriangle,
  User,
  Download,
  Filter,
  BarChart3
} from "lucide-react";
import { format, subDays, startOfMonth, endOfMonth } from "date-fns";

import { useAttendanceStatistics, useClassAttendanceRange } from "../hooks/use-attendance";
import { useStudents } from "@/features/students/hooks/use-students";
import { AttendanceStatusBadge } from "./attendance-status-badge";
import type { AttendanceStatus, StudentAttendanceRow } from "@/types/attendance";

interface AttendanceStatisticsProps {
  classId: number;
  className: string;
  dateRange?: {
    startDate: Date;
    endDate: Date;
  };
}

export function AttendanceStatistics({
  classId,
  className,
  dateRange = {
    startDate: startOfMonth(new Date()),
    endDate: endOfMonth(new Date())
  }
}: AttendanceStatisticsProps) {
  const startDateString = format(dateRange.startDate, 'yyyy-MM-dd');
  const endDateString = format(dateRange.endDate, 'yyyy-MM-dd');

  // Fetch students in the class
  const { data: studentsResponse } = useStudents({ 
    classId, 
    size: 100 
  });
  
  // Fetch attendance data for the date range
  const { data: attendanceData } = useClassAttendanceRange(
    classId, 
    startDateString, 
    endDateString
  );

  const students = studentsResponse?.data || [];
  const attendance = attendanceData || [];

  // Process attendance data into student statistics
  const studentStats: StudentAttendanceRow[] = React.useMemo(() => {
    return students.map(student => {
      const studentAttendance = attendance.filter(a => a.userId === student.id);
      const attendanceMap: Record<string, AttendanceStatus> = {};
      
      studentAttendance.forEach(record => {
        attendanceMap[record.date] = record.status;
      });

      // Calculate statistics
      const present = studentAttendance.filter(a => a.status === 'PRESENT').length;
      const absent = studentAttendance.filter(a => a.status === 'ABSENT').length;
      const late = studentAttendance.filter(a => a.status === 'LATE').length;
      const excused = studentAttendance.filter(a => a.status === 'EXCUSED').length;
      const total = studentAttendance.length;
      const rate = total > 0 ? (present / total) * 100 : 0;

      return {
        userId: student.id,
        userName: `${student.firstName} ${student.lastName}`,
        userEmail: student.email,
        gradeLevel: student.gradeLevel,
        attendances: attendanceMap,
        statistics: { present, absent, late, excused, total, rate },
      };
    });
  }, [students, attendance]);

  // Calculate class-wide statistics
  const classStats = React.useMemo(() => {
    const totalStudents = students.length;
    const totalRecords = attendance.length;
    
    const presentCount = attendance.filter(a => a.status === 'PRESENT').length;
    const absentCount = attendance.filter(a => a.status === 'ABSENT').length;
    const lateCount = attendance.filter(a => a.status === 'LATE').length;
    const excusedCount = attendance.filter(a => a.status === 'EXCUSED').length;
    
    const overallRate = totalRecords > 0 ? (presentCount / totalRecords) * 100 : 0;
    
    // Find students with perfect attendance
    const perfectAttendance = studentStats.filter(s => 
      s.statistics.total > 0 && s.statistics.rate === 100
    );
    
    // Find students with concerning attendance (< 80%)
    const concerningAttendance = studentStats.filter(s => 
      s.statistics.total > 0 && s.statistics.rate < 80
    );
    
    // Calculate trends (compare with previous period)
    const daysDiff = Math.ceil((dateRange.endDate.getTime() - dateRange.startDate.getTime()) / (1000 * 60 * 60 * 24));
    const previousStartDate = subDays(dateRange.startDate, daysDiff);
    
    return {
      totalStudents,
      totalRecords,
      presentCount,
      absentCount,
      lateCount,
      excusedCount,
      overallRate,
      perfectAttendance,
      concerningAttendance,
      averageRate: studentStats.length > 0 
        ? studentStats.reduce((sum, s) => sum + s.statistics.rate, 0) / studentStats.length 
        : 0,
    };
  }, [students, attendance, studentStats, dateRange]);

  // Sort students by attendance rate
  const sortedStudents = React.useMemo(() => {
    return [...studentStats].sort((a, b) => b.statistics.rate - a.statistics.rate);
  }, [studentStats]);

  const getAttendanceGrade = (rate: number) => {
    if (rate >= 95) return { grade: 'A+', color: 'text-green-700 bg-green-100' };
    if (rate >= 90) return { grade: 'A', color: 'text-green-600 bg-green-50' };
    if (rate >= 85) return { grade: 'B', color: 'text-blue-600 bg-blue-50' };
    if (rate >= 80) return { grade: 'C', color: 'text-yellow-600 bg-yellow-50' };
    if (rate >= 75) return { grade: 'D', color: 'text-orange-600 bg-orange-50' };
    return { grade: 'F', color: 'text-red-600 bg-red-50' };
  };

  const handleExportReport = () => {
    // Generate CSV report
    const csvData = [
      ['Student Name', 'Email', 'Grade Level', 'Present', 'Absent', 'Late', 'Excused', 'Total', 'Attendance Rate'],
      ...sortedStudents.map(student => [
        student.userName,
        student.userEmail,
        student.gradeLevel || '',
        student.statistics.present.toString(),
        student.statistics.absent.toString(),
        student.statistics.late.toString(),
        student.statistics.excused.toString(),
        student.statistics.total.toString(),
        `${student.statistics.rate.toFixed(1)}%`
      ])
    ];

    const csvContent = csvData.map(row => row.join(',')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `attendance-report-${className}-${format(new Date(), 'yyyy-MM-dd')}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-emerald-200/60 bg-gradient-to-r from-emerald-50/80 to-green-50/40">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-2xl font-bold text-emerald-900 flex items-center">
                <BarChart3 className="h-6 w-6 mr-2" />
                Attendance Statistics - {className}
              </CardTitle>
              <p className="text-emerald-700 mt-1">
                {format(dateRange.startDate, 'MMM d')} - {format(dateRange.endDate, 'MMM d, yyyy')}
              </p>
            </div>
            <Button onClick={handleExportReport} variant="outline">
              <Download className="h-4 w-4 mr-2" />
              Export Report
            </Button>
          </div>
        </CardHeader>
      </Card>

      {/* Overview Statistics */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-slate-900">{classStats.totalStudents}</div>
            <div className="text-sm text-slate-600">Total Students</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-green-600">
              {Math.round(classStats.overallRate)}%
            </div>
            <div className="text-sm text-slate-600">Overall Rate</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-blue-600">
              {classStats.perfectAttendance.length}
            </div>
            <div className="text-sm text-slate-600">Perfect Attendance</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-red-600">
              {classStats.concerningAttendance.length}
            </div>
            <div className="text-sm text-slate-600">Needs Attention</div>
          </CardContent>
        </Card>
      </div>

      {/* Attendance Breakdown */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <TrendingUp className="h-5 w-5 mr-2" />
              Attendance Breakdown
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <AttendanceStatusBadge status="PRESENT" size="sm" />
                  <span className="text-sm">Present</span>
                </div>
                <div className="text-sm font-semibold">{classStats.presentCount}</div>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <AttendanceStatusBadge status="ABSENT" size="sm" />
                  <span className="text-sm">Absent</span>
                </div>
                <div className="text-sm font-semibold">{classStats.absentCount}</div>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <AttendanceStatusBadge status="LATE" size="sm" />
                  <span className="text-sm">Late</span>
                </div>
                <div className="text-sm font-semibold">{classStats.lateCount}</div>
              </div>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <AttendanceStatusBadge status="EXCUSED" size="sm" />
                  <span className="text-sm">Excused</span>
                </div>
                <div className="text-sm font-semibold">{classStats.excusedCount}</div>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Award className="h-5 w-5 mr-2" />
              Recognition
            </CardTitle>
          </CardHeader>
          <CardContent>
            {classStats.perfectAttendance.length > 0 ? (
              <div className="space-y-3">
                <h4 className="font-semibold text-green-700 mb-2">Perfect Attendance 🏆</h4>
                {classStats.perfectAttendance.slice(0, 5).map((student) => (
                  <div key={student.userId} className="flex items-center space-x-3">
                    <Avatar className="h-8 w-8">
                      <AvatarFallback className="bg-green-100 text-green-700 text-xs">
                        <User className="h-4 w-4" />
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1">
                      <div className="text-sm font-medium">{student.userName}</div>
                      <div className="text-xs text-slate-500">
                        {student.statistics.total} days
                      </div>
                    </div>
                    <Badge className="bg-green-100 text-green-800">100%</Badge>
                  </div>
                ))}
                {classStats.perfectAttendance.length > 5 && (
                  <div className="text-xs text-slate-500 text-center">
                    +{classStats.perfectAttendance.length - 5} more students
                  </div>
                )}
              </div>
            ) : (
              <div className="text-center py-4 text-slate-500">
                <Award className="h-8 w-8 mx-auto mb-2 text-slate-300" />
                <p className="text-sm">No perfect attendance yet</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Student Rankings */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <Users className="h-5 w-5 mr-2" />
            Student Attendance Rankings
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="divide-y max-h-96 overflow-y-auto">
            {sortedStudents.map((student, index) => {
              const attendanceGrade = getAttendanceGrade(student.statistics.rate);
              return (
                <div key={student.userId} className="p-4 hover:bg-slate-50">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-4">
                      <div className="flex items-center justify-center w-8 h-8 rounded-full bg-slate-100 text-slate-600 text-sm font-semibold">
                        {index + 1}
                      </div>
                      <Avatar className="h-10 w-10">
                        <AvatarFallback className="bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-700">
                          <User className="h-5 w-5" />
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="font-semibold text-slate-900">
                          {student.userName}
                        </div>
                        <div className="text-sm text-slate-500">
                          {student.statistics.present}P • {student.statistics.absent}A • {student.statistics.late}L • {student.statistics.excused}E
                        </div>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-4">
                      <div className="text-right">
                        <div className="text-sm font-semibold">
                          {student.statistics.rate.toFixed(1)}%
                        </div>
                        <div className="text-xs text-slate-500">
                          {student.statistics.present}/{student.statistics.total}
                        </div>
                      </div>
                      <Badge className={attendanceGrade.color}>
                        {attendanceGrade.grade}
                      </Badge>
                      <div className="w-20">
                        <Progress 
                          value={student.statistics.rate} 
                          className="h-2"
                        />
                      </div>
                      {student.statistics.rate < 80 && (
                        <AlertTriangle className="h-4 w-4 text-red-500" />
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
            
            {sortedStudents.length === 0 && (
              <div className="p-8 text-center text-slate-500">
                <Users className="h-12 w-12 mx-auto mb-3 text-slate-300" />
                <p>No attendance data available</p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Concerning Attendance */}
      {classStats.concerningAttendance.length > 0 && (
        <Card className="border-red-200 bg-red-50">
          <CardHeader>
            <CardTitle className="flex items-center text-red-800">
              <AlertTriangle className="h-5 w-5 mr-2" />
              Students Needing Attention
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {classStats.concerningAttendance.map((student) => (
                <div key={student.userId} className="flex items-center space-x-3 p-3 bg-white rounded-lg">
                  <Avatar className="h-8 w-8">
                    <AvatarFallback className="bg-red-100 text-red-700 text-xs">
                      <User className="h-4 w-4" />
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex-1">
                    <div className="text-sm font-medium">{student.userName}</div>
                    <div className="text-xs text-slate-500">
                      {student.statistics.present}/{student.statistics.total} days present
                    </div>
                  </div>
                  <Badge className="bg-red-100 text-red-800">
                    {student.statistics.rate.toFixed(1)}%
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
