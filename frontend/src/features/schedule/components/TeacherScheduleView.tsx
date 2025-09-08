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
  UserCheck
} from 'lucide-react';
import { useTeacherSchedule } from '../hooks/use-schedule';
import { usePeriods } from '../../timetable/hooks';
import { ScheduleGrid } from './ScheduleGrid';
import { useAuth } from '../../../hooks/useAuth';
import { TimetableSlot } from '../../../types/timetable';

interface ClassSummary {
  classId: number;
  className: string;
  totalSlots: number;
  courses: string[];
  students: number; // This would need to be fetched from enrollment API
}

export function TeacherScheduleView() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('schedule');
  
  const { data: teacherSlots, isLoading, error, refetch } = useTeacherSchedule(user?.id);
  const { data: periods, isLoading: periodsLoading } = usePeriods();

  if (isLoading || periodsLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">My Teaching Schedule</h1>
            <p className="text-gray-600">View your classes, students, and teaching assignments</p>
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
            <h1 className="text-3xl font-bold text-gray-900">My Teaching Schedule</h1>
            <p className="text-gray-600">View your classes, students, and teaching assignments</p>
          </div>
        </div>
        <Card>
          <CardContent className="p-6 text-center">
            <div className="text-red-500 mb-4">
              <Clock className="w-12 h-12 mx-auto mb-2" />
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

  const slots = teacherSlots || [];
  const periodsData = periods || [];

  // Calculate statistics
  const totalClasses = new Set(slots.map(slot => slot.forClass?.id).filter(Boolean)).size;
  const totalCourses = new Set(slots.map(slot => slot.forCourse?.id).filter(Boolean)).size;
  const totalSlots = slots.length;
  const weeklyHours = slots.length; // Assuming each slot is 1 hour

  // Group slots by class for class summary
  const classSummaries: ClassSummary[] = [];
  const classMap = new Map<number, TimetableSlot[]>();
  
  slots.forEach(slot => {
    if (slot.forClass) {
      const classId = slot.forClass.id;
      if (!classMap.has(classId)) {
        classMap.set(classId, []);
      }
      classMap.get(classId)!.push(slot);
    }
  });

  classMap.forEach((classSlots, classId) => {
    const firstSlot = classSlots[0];
    if (firstSlot.forClass) {
      const courses = [...new Set(classSlots.map(slot => slot.forCourse?.name).filter(Boolean))];
      classSummaries.push({
        classId,
        className: firstSlot.forClass.name,
        totalSlots: classSlots.length,
        courses,
        students: 25 // Mock data - would need to fetch from enrollment API
      });
    }
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">My Teaching Schedule</h1>
          <p className="text-gray-600">
            Welcome back, {user?.firstName}! Here's your teaching schedule and class overview.
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
                <p className="text-sm font-medium text-gray-600">Total Classes</p>
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
                <p className="text-sm font-medium text-gray-600">Courses Teaching</p>
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
                <p className="text-sm font-medium text-gray-600">Total Students</p>
                <p className="text-2xl font-bold text-orange-600">
                  {classSummaries.reduce((sum, cls) => sum + cls.students, 0)}
                </p>
              </div>
              <div className="p-3 bg-orange-100 rounded-full">
                <GraduationCap className="w-6 h-6 text-orange-600" />
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
            slots={slots}
            periods={periodsData}
            title="Weekly Teaching Schedule"
            showTeacher={false}
            showRoom={true}
            showClass={true}
            emptyMessage="No classes assigned yet. Contact administration for your teaching assignments."
          />
        </TabsContent>

        <TabsContent value="classes" className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {classSummaries.map(classSummary => (
              <Card key={classSummary.classId} className="hover:shadow-lg transition-shadow">
                <CardHeader>
                  <CardTitle className="flex items-center justify-between">
                    <span className="flex items-center gap-2">
                      <Users className="w-5 h-5 text-blue-600" />
                      {classSummary.className}
                    </span>
                    <Badge variant="secondary">{classSummary.totalSlots} slots</Badge>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex items-center gap-2 text-sm text-gray-600">
                      <GraduationCap className="w-4 h-4" />
                      <span>{classSummary.students} students</span>
                    </div>
                    
                    <div>
                      <p className="text-sm font-medium text-gray-700 mb-2">Courses:</p>
                      <div className="flex flex-wrap gap-1">
                        {classSummary.courses.map(course => (
                          <Badge key={course} variant="outline" className="text-xs">
                            {course}
                          </Badge>
                        ))}
                      </div>
                    </div>

                    <div className="pt-3 border-t">
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline" className="flex-1">
                          <UserCheck className="w-4 h-4 mr-1" />
                          Attendance
                        </Button>
                        <Button size="sm" variant="outline" className="flex-1">
                          <BookOpen className="w-4 h-4 mr-1" />
                          Grades
                        </Button>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          {classSummaries.length === 0 && (
            <Card>
              <CardContent className="p-8 text-center">
                <Users className="w-12 h-12 mx-auto mb-4 text-gray-400" />
                <h3 className="text-lg font-semibold text-gray-900 mb-2">No Classes Assigned</h3>
                <p className="text-gray-600">
                  You don't have any classes assigned yet. Contact the administration to get your teaching assignments.
                </p>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
