import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Calendar, Users, TrendingUp, Clock, CheckCircle, XCircle, AlertTriangle } from 'lucide-react';
import { useTeacherWeeklySummary, useTeacherAbsentStudents } from '../hooks/use-attendance';
import { AttendanceStatus } from '@/types/attendance';
import { AttendanceStatusBadge } from './attendance-status-badge';

interface TeacherAttendanceDashboardProps {
  teacherId: number;
}

export function TeacherAttendanceDashboard({ teacherId }: TeacherAttendanceDashboardProps) {
  const [selectedWeek, setSelectedWeek] = useState(() => {
    const today = new Date();
    const monday = new Date(today);
    monday.setDate(today.getDate() - today.getDay() + 1);
    return monday.toISOString().split('T')[0];
  });

  const [selectedDate] = useState(new Date().toISOString().split('T')[0]);

  // Fetch weekly summary
  const { 
    data: weeklySummary, 
    isLoading: weeklyLoading 
  } = useTeacherWeeklySummary(teacherId, selectedWeek);

  // Fetch today's absent students
  const { 
    data: absentStudents, 
    isLoading: absentLoading 
  } = useTeacherAbsentStudents(teacherId, selectedDate);

  // Calculate weekly statistics
  const weeklyStats = React.useMemo(() => {
    if (!weeklySummary) return null;

    let totalStudents = 0;
    let totalPresent = 0;
    let totalAbsent = 0;
    let totalLate = 0;
    let totalExcused = 0;

    Object.values(weeklySummary).forEach(dayAttendance => {
      dayAttendance.forEach(attendance => {
        totalStudents++;
        switch (attendance.status) {
          case AttendanceStatus.PRESENT:
            totalPresent++;
            break;
          case AttendanceStatus.ABSENT:
            totalAbsent++;
            break;
          case AttendanceStatus.LATE:
            totalLate++;
            break;
          case AttendanceStatus.EXCUSED:
            totalExcused++;
            break;
        }
      });
    });

    const attendanceRate = totalStudents > 0 ? (totalPresent / totalStudents) * 100 : 0;

    return {
      totalStudents,
      totalPresent,
      totalAbsent,
      totalLate,
      totalExcused,
      attendanceRate: Math.round(attendanceRate * 100) / 100,
    };
  }, [weeklySummary]);

  const getDayName = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', { weekday: 'short' });
  };

  const getWeekDates = (startDate: string) => {
    const dates = [];
    const start = new Date(startDate);
    for (let i = 0; i < 7; i++) {
      const date = new Date(start);
      date.setDate(start.getDate() + i);
      dates.push(date.toISOString().split('T')[0]);
    }
    return dates;
  };

  const navigateWeek = (direction: 'prev' | 'next') => {
    const currentWeek = new Date(selectedWeek);
    const newWeek = new Date(currentWeek);
    newWeek.setDate(currentWeek.getDate() + (direction === 'next' ? 7 : -7));
    setSelectedWeek(newWeek.toISOString().split('T')[0]);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5" />
            Attendance Dashboard
          </CardTitle>
        </CardHeader>
      </Card>

      {/* Weekly Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-2">
              <Users className="h-8 w-8 text-blue-500" />
              <div>
                <p className="text-2xl font-bold">{weeklyStats?.totalStudents || 0}</p>
                <p className="text-sm text-gray-600">Total Students</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-2">
              <CheckCircle className="h-8 w-8 text-green-500" />
              <div>
                <p className="text-2xl font-bold">{weeklyStats?.totalPresent || 0}</p>
                <p className="text-sm text-gray-600">Present</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-2">
              <XCircle className="h-8 w-8 text-red-500" />
              <div>
                <p className="text-2xl font-bold">{weeklyStats?.totalAbsent || 0}</p>
                <p className="text-sm text-gray-600">Absent</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-8 w-8 text-purple-500" />
              <div>
                <p className="text-2xl font-bold">{weeklyStats?.attendanceRate || 0}%</p>
                <p className="text-sm text-gray-600">Attendance Rate</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Weekly Overview */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span className="flex items-center gap-2">
              <Calendar className="h-5 w-5" />
              Weekly Overview
            </span>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={() => navigateWeek('prev')}>
                Previous Week
              </Button>
              <Button variant="outline" size="sm" onClick={() => navigateWeek('next')}>
                Next Week
              </Button>
            </div>
          </CardTitle>
        </CardHeader>
        <CardContent>
          {weeklyLoading ? (
            <div className="text-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
              <p className="text-gray-600">Loading weekly data...</p>
            </div>
          ) : (
            <div className="grid grid-cols-7 gap-4">
              {getWeekDates(selectedWeek).map((date, index) => {
                const dayName = getDayName(date);
                const dayAttendance = weeklySummary?.[dayName.toUpperCase()] || [];
                const isToday = date === selectedDate;

                const dayStats = dayAttendance.reduce(
                  (acc, attendance) => {
                    acc.total++;
                    switch (attendance.status) {
                      case AttendanceStatus.PRESENT:
                        acc.present++;
                        break;
                      case AttendanceStatus.ABSENT:
                        acc.absent++;
                        break;
                      case AttendanceStatus.LATE:
                        acc.late++;
                        break;
                      case AttendanceStatus.EXCUSED:
                        acc.excused++;
                        break;
                    }
                    return acc;
                  },
                  { total: 0, present: 0, absent: 0, late: 0, excused: 0 }
                );

                return (
                  <div
                    key={date}
                    className={`p-4 border rounded-lg ${
                      isToday ? 'border-blue-500 bg-blue-50' : 'border-gray-200'
                    }`}
                  >
                    <div className="text-center">
                      <p className="font-medium text-sm">{dayName}</p>
                      <p className="text-xs text-gray-600 mb-2">
                        {new Date(date).getDate()}
                      </p>
                      
                      {dayStats.total > 0 ? (
                        <div className="space-y-1">
                          <div className="flex justify-center gap-1">
                            {dayStats.present > 0 && (
                              <Badge variant="secondary" className="text-xs bg-green-100 text-green-800">
                                {dayStats.present}P
                              </Badge>
                            )}
                            {dayStats.absent > 0 && (
                              <Badge variant="secondary" className="text-xs bg-red-100 text-red-800">
                                {dayStats.absent}A
                              </Badge>
                            )}
                            {dayStats.late > 0 && (
                              <Badge variant="secondary" className="text-xs bg-yellow-100 text-yellow-800">
                                {dayStats.late}L
                              </Badge>
                            )}
                          </div>
                          <p className="text-xs text-gray-500">
                            {Math.round((dayStats.present / dayStats.total) * 100)}% present
                          </p>
                        </div>
                      ) : (
                        <p className="text-xs text-gray-400">No classes</p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Today's Absent Students */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-orange-500" />
            Today's Absent Students
          </CardTitle>
        </CardHeader>
        <CardContent>
          {absentLoading ? (
            <div className="text-center py-4">Loading absent students...</div>
          ) : !absentStudents || absentStudents.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <CheckCircle className="h-12 w-12 mx-auto mb-4 text-green-500 opacity-50" />
              <p>No absent students today!</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {absentStudents.map((student) => (
                <div key={student.id} className="p-4 border rounded-lg">
                  <div className="flex items-center justify-between mb-2">
                    <p className="font-medium">{student.userName}</p>
                    <AttendanceStatusBadge status={student.status} />
                  </div>
                  <p className="text-sm text-gray-600">{student.className}</p>
                  {student.remarks && (
                    <p className="text-xs text-gray-500 mt-1">
                      <span className="font-medium">Note:</span> {student.remarks}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
