import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../../components/ui/tabs';
import { 
  Clock, 
  Users, 
  BookOpen, 
  MapPin, 
  Calendar,
  Download,
  RefreshCw,
  GraduationCap,
  User,
  AlertCircle
} from 'lucide-react';
import { useStudentSchedule } from '../hooks/use-schedule';
import { usePeriods } from '../../timetable/hooks';
import { ScheduleGrid } from './ScheduleGrid';
import { useAuth } from '../../../hooks/useAuth';

export function StudentScheduleView() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('schedule');
  
  const { data: scheduleData, isLoading, error, refetch } = useStudentSchedule(user?.id);
  const { data: periods, isLoading: periodsLoading } = usePeriods();

  if (isLoading || periodsLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">My Class Schedule</h1>
            <p className="text-gray-600">View your enrolled classes and weekly schedule</p>
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map(i => (
            <Card key={i} className="animate-pulse">
              <CardContent className="p-6">
                <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                <div className="h-8 bg-gray-200 rounded w-1/2"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">My Class Schedule</h1>
            <p className="text-gray-600">View your enrolled classes and weekly schedule</p>
          </div>
        </div>
        <Card>
          <CardContent className="p-6 text-center">
            <div className="text-red-500 mb-4">
              <AlertCircle className="w-12 h-12 mx-auto mb-2" />
              <p>Error loading your schedule</p>
              <p className="text-sm text-gray-600 mt-2">Please try refreshing the page</p>
            </div>
            <Button onClick={() => refetch()} variant="outline">
              <RefreshCw className="w-4 h-4 mr-2" />
              Retry
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const { enrollments = [], schedules = [], allSlots = [] } = scheduleData || {};
  const periodsData = periods || [];

  // Calculate statistics
  const totalClasses = enrollments.filter(e => e.status === 'ACTIVE').length;
  const totalCourses = new Set(allSlots.map(slot => slot.forCourse?.id).filter(Boolean)).size;
  const weeklyHours = allSlots.length;
  const activeEnrollments = enrollments.filter(e => e.status === 'ACTIVE');

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">My Class Schedule</h1>
          <p className="text-gray-600">
            Welcome back, {user?.firstName}! Here's your class schedule and enrollment information.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => refetch()}>
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
          <Button variant="outline">
            <Download className="w-4 h-4 mr-2" />
            Export PDF
          </Button>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Enrolled Classes</p>
                <p className="text-2xl font-bold text-blue-600">{totalClasses}</p>
              </div>
              <div className="p-3 bg-blue-100 rounded-full">
                <Users className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Courses</p>
                <p className="text-2xl font-bold text-green-600">{totalCourses}</p>
              </div>
              <div className="p-3 bg-green-100 rounded-full">
                <BookOpen className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Weekly Hours</p>
                <p className="text-2xl font-bold text-purple-600">{weeklyHours}</p>
              </div>
              <div className="p-3 bg-purple-100 rounded-full">
                <Clock className="w-6 h-6 text-purple-600" />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Enrollment Status</p>
                <p className="text-2xl font-bold text-green-600">Active</p>
              </div>
              <div className="p-3 bg-green-100 rounded-full">
                <GraduationCap className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="schedule">Weekly Schedule</TabsTrigger>
          <TabsTrigger value="classes">My Classes</TabsTrigger>
        </TabsList>

        <TabsContent value="schedule" className="space-y-6">
          <ScheduleGrid
            slots={allSlots}
            periods={periodsData}
            title="My Weekly Schedule"
            showTeacher={true}
            showRoom={true}
            showClass={false}
            emptyMessage="No classes scheduled yet. Contact administration about your enrollment status."
          />
        </TabsContent>

        <TabsContent value="classes" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {schedules.map(schedule => (
              <Card key={schedule.classId} className="hover:shadow-lg transition-shadow">
                <CardHeader>
                  <CardTitle className="flex items-center justify-between">
                    <span className="flex items-center gap-2">
                      <Users className="w-5 h-5 text-blue-600" />
                      {schedule.className}
                    </span>
                    <Badge variant="secondary">{schedule.slots.length} slots</Badge>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {/* Courses in this class */}
                    <div>
                      <p className="text-sm font-medium text-gray-700 mb-2">Courses:</p>
                      <div className="flex flex-wrap gap-1">
                        {[...new Set(schedule.slots.map(slot => slot.forCourse?.name).filter(Boolean))].map(course => (
                          <Badge key={course} variant="outline" className="text-xs">
                            {course}
                          </Badge>
                        ))}
                      </div>
                    </div>

                    {/* Teachers */}
                    <div>
                      <p className="text-sm font-medium text-gray-700 mb-2">Teachers:</p>
                      <div className="space-y-1">
                        {[...new Set(schedule.slots.map(slot => 
                          slot.teacher ? `${slot.teacher.firstName} ${slot.teacher.lastName}` : null
                        ).filter(Boolean))].map(teacher => (
                          <div key={teacher} className="flex items-center gap-2 text-sm text-gray-600">
                            <User className="w-3 h-3" />
                            {teacher}
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Schedule Summary */}
                    <div>
                      <p className="text-sm font-medium text-gray-700 mb-2">Schedule:</p>
                      <div className="text-sm text-gray-600">
                        {schedule.slots.length > 0 ? (
                          <div className="space-y-1">
                            {schedule.slots.slice(0, 3).map((slot, index) => (
                              <div key={index} className="flex items-center gap-2">
                                <Clock className="w-3 h-3" />
                                <span>
                                  {slot.dayOfWeek.charAt(0) + slot.dayOfWeek.slice(1).toLowerCase()} - {slot.period.name}
                                </span>
                              </div>
                            ))}
                            {schedule.slots.length > 3 && (
                              <div className="text-xs text-gray-500">
                                +{schedule.slots.length - 3} more slots
                              </div>
                            )}
                          </div>
                        ) : (
                          <span className="text-gray-500">No schedule available</span>
                        )}
                      </div>
                    </div>

                    <div className="pt-3 border-t">
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline" className="flex-1">
                          <BookOpen className="w-4 h-4 mr-1" />
                          Grades
                        </Button>
                        <Button size="sm" variant="outline" className="flex-1">
                          <Calendar className="w-4 h-4 mr-1" />
                          Attendance
                        </Button>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          {schedules.length === 0 && (
            <Card>
              <CardContent className="p-8 text-center">
                <Users className="w-12 h-12 mx-auto mb-4 text-gray-400" />
                <h3 className="text-lg font-semibold text-gray-900 mb-2">No Classes Enrolled</h3>
                <p className="text-gray-600 mb-4">
                  You're not enrolled in any classes yet. Contact the administration to enroll in classes.
                </p>
                <Button variant="outline">
                  <Users className="w-4 h-4 mr-2" />
                  Request Enrollment
                </Button>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
