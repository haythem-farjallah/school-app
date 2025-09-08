import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { 
  Calendar,
  ChevronLeft,
  ChevronRight,
  Users,
  User,
  TrendingUp,
  BarChart3
} from "lucide-react";
import { 
  format, 
  startOfWeek, 
  endOfWeek, 
  eachDayOfInterval, 
  addWeeks, 
  subWeeks,
  isSameDay,
  isWeekend
} from "date-fns";

import { AttendanceStatusDot } from "./attendance-status-badge";
import { useClassAttendanceRange } from "../hooks/use-attendance";
import { useStudents } from "@/features/students/hooks/use-students";
import type { AttendanceStatus, StudentAttendanceRow } from "@/types/attendance";

interface WeeklyCalendarViewProps {
  classId: number;
  className: string;
  selectedDate: Date;
  onDateChange: (date: Date) => void;
}

export function WeeklyCalendarView({
  classId,
  className,
  selectedDate,
  onDateChange,
}: WeeklyCalendarViewProps) {
  const weekStart = startOfWeek(selectedDate, { weekStartsOn: 1 }); // Monday
  const weekEnd = endOfWeek(selectedDate, { weekStartsOn: 1 }); // Sunday
  const weekDays = eachDayOfInterval({ start: weekStart, end: weekEnd });
  
  // Only show weekdays (Monday-Friday) for school attendance
  const schoolDays = weekDays.filter(day => !isWeekend(day));

  const startDateString = format(weekStart, 'yyyy-MM-dd');
  const endDateString = format(weekEnd, 'yyyy-MM-dd');

  // Fetch students in the class
  const { data: studentsResponse } = useStudents({ 
    classId, 
    size: 100 
  });
  
  // Fetch attendance data for the week
  const { data: attendanceData } = useClassAttendanceRange(
    classId, 
    startDateString, 
    endDateString
  );

  const students = studentsResponse?.data || [];
  const attendance = attendanceData || [];

  // Process attendance data into student rows
  const studentRows: StudentAttendanceRow[] = React.useMemo(() => {
    return students.map(student => {
      const studentAttendance = attendance.filter(a => a.userId === student.id);
      const attendanceMap: Record<string, AttendanceStatus> = {};
      
      studentAttendance.forEach(record => {
        attendanceMap[record.date] = record.status;
      });

      // Calculate statistics
      const present = studentAttendance.filter(a => a.status === AttendanceStatus.PRESENT).length;
      const absent = studentAttendance.filter(a => a.status === AttendanceStatus.ABSENT).length;
      const late = studentAttendance.filter(a => a.status === AttendanceStatus.LATE).length;
      const excused = studentAttendance.filter(a => a.status === AttendanceStatus.EXCUSED).length;
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

  // Calculate weekly statistics
  const weeklyStats = React.useMemo(() => {
    const totalStudents = students.length;
    const totalPossibleAttendance = totalStudents * schoolDays.length;
    
    let totalPresent = 0;
    let totalAbsent = 0;
    let totalLate = 0;
    let totalExcused = 0;

    studentRows.forEach(row => {
      totalPresent += row.statistics.present;
      totalAbsent += row.statistics.absent;
      totalLate += row.statistics.late;
      totalExcused += row.statistics.excused;
    });

    const overallRate = totalPossibleAttendance > 0 
      ? (totalPresent / totalPossibleAttendance) * 100 
      : 0;

    return {
      totalStudents,
      totalPresent,
      totalAbsent,
      totalLate,
      totalExcused,
      overallRate,
    };
  }, [studentRows, students.length, schoolDays.length]);

  const handlePreviousWeek = () => {
    onDateChange(subWeeks(selectedDate, 1));
  };

  const handleNextWeek = () => {
    onDateChange(addWeeks(selectedDate, 1));
  };

  const getAttendanceForDay = (studentId: number, date: Date): AttendanceStatus | undefined => {
    const dateString = format(date, 'yyyy-MM-dd');
    const studentRow = studentRows.find(row => row.userId === studentId);
    return studentRow?.attendances[dateString];
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-purple-200/60 bg-gradient-to-r from-purple-50/80 to-pink-50/40">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-2xl font-bold text-purple-900 flex items-center">
                <Calendar className="h-6 w-6 mr-2" />
                Weekly Attendance - {className}
              </CardTitle>
              <p className="text-purple-700 mt-1">
                {format(weekStart, 'MMM d')} - {format(weekEnd, 'MMM d, yyyy')}
              </p>
            </div>
            <div className="flex items-center space-x-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handlePreviousWeek}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => onDateChange(new Date())}
              >
                Today
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleNextWeek}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Weekly Statistics */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-slate-900">{weeklyStats.totalStudents}</div>
            <div className="text-sm text-slate-600">Students</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{weeklyStats.totalPresent}</div>
            <div className="text-sm text-slate-600">Present</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-red-600">{weeklyStats.totalAbsent}</div>
            <div className="text-sm text-slate-600">Absent</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-yellow-600">{weeklyStats.totalLate}</div>
            <div className="text-sm text-slate-600">Late</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-purple-600">
              {Math.round(weeklyStats.overallRate)}%
            </div>
            <div className="text-sm text-slate-600">Weekly Rate</div>
          </CardContent>
        </Card>
      </div>

      {/* Weekly Calendar Grid */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <BarChart3 className="h-5 w-5 mr-2" />
            Weekly Attendance Grid
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-slate-50 border-b">
                <tr>
                  <th className="text-left p-4 font-semibold text-slate-900 min-w-[200px]">
                    Student
                  </th>
                  {schoolDays.map((day) => (
                    <th 
                      key={day.toISOString()} 
                      className="text-center p-4 font-semibold text-slate-900 min-w-[100px]"
                    >
                      <div className="flex flex-col items-center">
                        <div className="text-sm">{format(day, 'EEE')}</div>
                        <div className="text-xs text-slate-600">{format(day, 'MMM d')}</div>
                        {isSameDay(day, new Date()) && (
                          <div className="w-2 h-2 bg-blue-500 rounded-full mt-1"></div>
                        )}
                      </div>
                    </th>
                  ))}
                  <th className="text-center p-4 font-semibold text-slate-900 min-w-[100px]">
                    Rate
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {studentRows.map((studentRow) => (
                  <tr key={studentRow.userId} className="hover:bg-slate-50">
                    <td className="p-4">
                      <div className="flex items-center space-x-3">
                        <Avatar className="h-8 w-8">
                          <AvatarFallback className="bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-700 text-xs">
                            <User className="h-4 w-4" />
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <div className="font-medium text-slate-900 text-sm">
                            {studentRow.userName}
                          </div>
                          {studentRow.gradeLevel && (
                            <Badge variant="outline" className="text-xs">
                              {studentRow.gradeLevel}
                            </Badge>
                          )}
                        </div>
                      </div>
                    </td>
                    {schoolDays.map((day) => {
                      const status = getAttendanceForDay(studentRow.userId, day);
                      return (
                        <td key={day.toISOString()} className="p-4 text-center">
                          {status ? (
                            <div className="flex justify-center">
                              <AttendanceStatusDot status={status} size="md" />
                            </div>
                          ) : (
                            <div className="text-slate-300 text-sm">-</div>
                          )}
                        </td>
                      );
                    })}
                    <td className="p-4 text-center">
                      <div className="flex flex-col items-center">
                        <div className={`text-sm font-semibold ${
                          studentRow.statistics.rate >= 90 ? 'text-green-600' :
                          studentRow.statistics.rate >= 75 ? 'text-yellow-600' :
                          'text-red-600'
                        }`}>
                          {Math.round(studentRow.statistics.rate)}%
                        </div>
                        <div className="text-xs text-slate-500">
                          {studentRow.statistics.present}/{studentRow.statistics.total}
                        </div>
                      </div>
                    </td>
                  </tr>
                ))}
                
                {studentRows.length === 0 && (
                  <tr>
                    <td colSpan={schoolDays.length + 2} className="p-8 text-center text-slate-500">
                      <Users className="h-12 w-12 mx-auto mb-3 text-slate-300" />
                      <p>No students found in this class</p>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Legend */}
      <Card>
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-6">
              <div className="flex items-center space-x-2">
                <AttendanceStatusDot status={AttendanceStatus.PRESENT} />
                <span className="text-sm text-slate-600">Present</span>
              </div>
              <div className="flex items-center space-x-2">
                <AttendanceStatusDot status={AttendanceStatus.ABSENT} />
                <span className="text-sm text-slate-600">Absent</span>
              </div>
              <div className="flex items-center space-x-2">
                <AttendanceStatusDot status={AttendanceStatus.LATE} />
                <span className="text-sm text-slate-600">Late</span>
              </div>
              <div className="flex items-center space-x-2">
                <AttendanceStatusDot status={AttendanceStatus.EXCUSED} />
                <span className="text-sm text-slate-600">Excused</span>
              </div>
            </div>
            <div className="flex items-center space-x-2 text-sm text-slate-600">
              <TrendingUp className="h-4 w-4" />
              <span>Weekly attendance patterns</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
