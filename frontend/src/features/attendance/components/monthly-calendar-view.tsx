import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { 
  Calendar,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  Users,
  BarChart3,
  CalendarDays
} from "lucide-react";
import { 
  format, 
  startOfMonth, 
  endOfMonth, 
  eachDayOfInterval, 
  addMonths, 
  subMonths,
  isSameDay,
  isWeekend,
  startOfWeek,
  endOfWeek,
  isSameMonth
} from "date-fns";

import { useClassAttendanceSummary } from "../hooks/use-attendance";
import type { ClassAttendanceSummary } from "@/types/attendance";

interface MonthlyCalendarViewProps {
  classId: number;
  className: string;
  selectedDate: Date;
  onDateChange: (date: Date) => void;
}

export function MonthlyCalendarView({
  classId,
  className,
  selectedDate,
  onDateChange,
}: MonthlyCalendarViewProps) {
  const monthStart = startOfMonth(selectedDate);
  const monthEnd = endOfMonth(selectedDate);
  const calendarStart = startOfWeek(monthStart, { weekStartsOn: 1 });
  const calendarEnd = endOfWeek(monthEnd, { weekStartsOn: 1 });
  
  const calendarDays = eachDayOfInterval({ 
    start: calendarStart, 
    end: calendarEnd 
  });

  const startDateString = format(monthStart, 'yyyy-MM-dd');
  const endDateString = format(monthEnd, 'yyyy-MM-dd');

  // Fetch attendance summary for the month
  const { data: attendanceSummary } = useClassAttendanceSummary(
    classId, 
    startDateString, 
    endDateString
  );

  const summaryData = attendanceSummary || [];

  // Create a map of date -> attendance summary
  const summaryMap = React.useMemo(() => {
    const map: Record<string, ClassAttendanceSummary> = {};
    summaryData.forEach(summary => {
      map[summary.date] = summary;
    });
    return map;
  }, [summaryData]);

  // Calculate monthly statistics
  const monthlyStats = React.useMemo(() => {
    const totalDays = summaryData.length;
    const totalStudents = summaryData.length > 0 ? summaryData[0].totalStudents : 0;
    const totalPresent = summaryData.reduce((sum, day) => sum + day.presentCount, 0);
    const totalAbsent = summaryData.reduce((sum, day) => sum + day.absentCount, 0);
    const totalLate = summaryData.reduce((sum, day) => sum + day.lateCount, 0);
    const totalExcused = summaryData.reduce((sum, day) => sum + day.excusedCount, 0);
    
    const averageRate = summaryData.length > 0 
      ? summaryData.reduce((sum, day) => sum + day.attendanceRate, 0) / summaryData.length
      : 0;

    const bestDay = summaryData.reduce((best, day) => 
      day.attendanceRate > (best?.attendanceRate || 0) ? day : best, 
      null as ClassAttendanceSummary | null
    );

    const worstDay = summaryData.reduce((worst, day) => 
      day.attendanceRate < (worst?.attendanceRate || 100) ? day : worst, 
      null as ClassAttendanceSummary | null
    );

    return {
      totalDays,
      totalStudents,
      totalPresent,
      totalAbsent,
      totalLate,
      totalExcused,
      averageRate,
      bestDay,
      worstDay,
    };
  }, [summaryData]);

  const handlePreviousMonth = () => {
    onDateChange(subMonths(selectedDate, 1));
  };

  const handleNextMonth = () => {
    onDateChange(addMonths(selectedDate, 1));
  };

  const getAttendanceRateColor = (rate: number) => {
    if (rate >= 95) return "bg-green-500";
    if (rate >= 90) return "bg-green-400";
    if (rate >= 85) return "bg-yellow-400";
    if (rate >= 80) return "bg-orange-400";
    if (rate >= 75) return "bg-red-400";
    return "bg-red-500";
  };

  const getAttendanceRateText = (rate: number) => {
    if (rate >= 95) return "text-green-700";
    if (rate >= 90) return "text-green-600";
    if (rate >= 85) return "text-yellow-700";
    if (rate >= 80) return "text-orange-700";
    return "text-red-700";
  };

  // Group days into weeks
  const weeks = [];
  for (let i = 0; i < calendarDays.length; i += 7) {
    weeks.push(calendarDays.slice(i, i + 7));
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-green-200/60 bg-gradient-to-r from-green-50/80 to-emerald-50/40">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-2xl font-bold text-green-900 flex items-center">
                <CalendarDays className="h-6 w-6 mr-2" />
                Monthly Attendance - {className}
              </CardTitle>
              <p className="text-green-700 mt-1">
                {format(selectedDate, 'MMMM yyyy')}
              </p>
            </div>
            <div className="flex items-center space-x-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handlePreviousMonth}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => onDateChange(new Date())}
              >
                This Month
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleNextMonth}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Monthly Statistics */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-slate-900">{monthlyStats.totalDays}</div>
            <div className="text-sm text-slate-600">School Days</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-green-600">
              {Math.round(monthlyStats.averageRate)}%
            </div>
            <div className="text-sm text-slate-600">Average Rate</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-blue-600">
              {monthlyStats.bestDay ? Math.round(monthlyStats.bestDay.attendanceRate) : 0}%
            </div>
            <div className="text-sm text-slate-600">Best Day</div>
            {monthlyStats.bestDay && (
              <div className="text-xs text-slate-500 mt-1">
                {format(new Date(monthlyStats.bestDay.date), 'MMM d')}
              </div>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-orange-600">
              {monthlyStats.worstDay ? Math.round(monthlyStats.worstDay.attendanceRate) : 0}%
            </div>
            <div className="text-sm text-slate-600">Lowest Day</div>
            {monthlyStats.worstDay && (
              <div className="text-xs text-slate-500 mt-1">
                {format(new Date(monthlyStats.worstDay.date), 'MMM d')}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Calendar Grid */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <BarChart3 className="h-5 w-5 mr-2" />
            Monthly Calendar
          </CardTitle>
        </CardHeader>
        <CardContent className="p-4">
          <div className="grid grid-cols-7 gap-1 mb-4">
            {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day) => (
              <div key={day} className="p-2 text-center text-sm font-semibold text-slate-600">
                {day}
              </div>
            ))}
          </div>
          
          <div className="grid grid-cols-7 gap-1">
            {weeks.map((week, weekIndex) =>
              week.map((day) => {
                const dateString = format(day, 'yyyy-MM-dd');
                const summary = summaryMap[dateString];
                const isCurrentMonth = isSameMonth(day, selectedDate);
                const isToday = isSameDay(day, new Date());
                const isSchoolDay = !isWeekend(day) && isCurrentMonth;

                return (
                  <div
                    key={day.toISOString()}
                    className={`
                      relative p-2 min-h-[80px] border rounded-lg transition-colors cursor-pointer
                      ${isCurrentMonth ? 'bg-white hover:bg-slate-50' : 'bg-slate-50 text-slate-400'}
                      ${isToday ? 'ring-2 ring-blue-500 bg-blue-50' : ''}
                      ${!isSchoolDay ? 'opacity-50' : ''}
                    `}
                    onClick={() => onDateChange(day)}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className={`text-sm font-medium ${
                        isToday ? 'text-blue-700' : 
                        isCurrentMonth ? 'text-slate-900' : 'text-slate-400'
                      }`}>
                        {format(day, 'd')}
                      </span>
                      {isToday && (
                        <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                      )}
                    </div>
                    
                    {summary && isSchoolDay && (
                      <div className="space-y-1">
                        <div className={`text-xs font-semibold ${
                          getAttendanceRateText(summary.attendanceRate)
                        }`}>
                          {Math.round(summary.attendanceRate)}%
                        </div>
                        <div className="flex space-x-1">
                          <div 
                            className={`w-2 h-2 rounded-full ${getAttendanceRateColor(summary.attendanceRate)}`}
                            title={`${summary.presentCount}/${summary.totalStudents} present`}
                          ></div>
                        </div>
                        <div className="text-xs text-slate-500">
                          {summary.presentCount}/{summary.totalStudents}
                        </div>
                      </div>
                    )}
                    
                    {!summary && isSchoolDay && isCurrentMonth && (
                      <div className="text-xs text-slate-400">
                        No data
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </CardContent>
      </Card>

      {/* Detailed Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center text-lg">
              <TrendingUp className="h-5 w-5 mr-2" />
              Monthly Breakdown
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-slate-600">Total Present</span>
              <Badge className="bg-green-100 text-green-800">
                {monthlyStats.totalPresent}
              </Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-600">Total Absent</span>
              <Badge className="bg-red-100 text-red-800">
                {monthlyStats.totalAbsent}
              </Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-600">Total Late</span>
              <Badge className="bg-yellow-100 text-yellow-800">
                {monthlyStats.totalLate}
              </Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-600">Total Excused</span>
              <Badge className="bg-orange-100 text-orange-800">
                {monthlyStats.totalExcused}
              </Badge>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center text-lg">
              <Users className="h-5 w-5 mr-2" />
              Class Information
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-slate-600">Total Students</span>
              <Badge variant="outline">
                {monthlyStats.totalStudents}
              </Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-600">School Days</span>
              <Badge variant="outline">
                {monthlyStats.totalDays}
              </Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-600">Average Attendance</span>
              <Badge className={`${
                monthlyStats.averageRate >= 90 ? 'bg-green-100 text-green-800' :
                monthlyStats.averageRate >= 80 ? 'bg-yellow-100 text-yellow-800' :
                'bg-red-100 text-red-800'
              }`}>
                {Math.round(monthlyStats.averageRate)}%
              </Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Legend */}
      <Card>
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-6">
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                <span className="text-sm text-slate-600">95%+ Attendance</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 bg-yellow-400 rounded-full"></div>
                <span className="text-sm text-slate-600">85-94% Attendance</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 bg-red-400 rounded-full"></div>
                <span className="text-sm text-slate-600">Below 85%</span>
              </div>
            </div>
            <div className="text-sm text-slate-500">
              Click on any day to view detailed attendance
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
