import React, { useState } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent } from '@/components/ui/card';
import { Calendar, BarChart3 } from 'lucide-react';
import { TeacherAttendanceMarking } from '@/features/attendance/components/teacher-attendance-marking';
import { TeacherAttendanceDashboard } from '@/features/attendance/components/teacher-attendance-dashboard';
import { useAuth } from '@/hooks/useAuth';

const TeacherAttendance = () => {
  const { user } = useAuth();
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);

  // Get teacher ID from auth context
  const teacherId = user?.id;

  if (!teacherId) {
    return (
      <div className="p-6">
        <Card>
          <CardContent className="p-6 text-center">
            <p className="text-red-600">Unable to load teacher information</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Attendance Management</h1>
          <p className="text-gray-600 mt-1">
            Manage student attendance for your classes
          </p>
        </div>
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Main Content */}
      <Tabs defaultValue="marking" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="marking" className="flex items-center gap-2">
            <Calendar className="h-4 w-4" />
            Schedule
          </TabsTrigger>
          <TabsTrigger value="dashboard" className="flex items-center gap-2">
            <BarChart3 className="h-4 w-4" />
            Dashboard
          </TabsTrigger>
        </TabsList>

        <TabsContent value="marking" className="mt-6">
          <TeacherAttendanceMarking 
            teacherId={teacherId} 
            selectedDate={selectedDate}
          />
        </TabsContent>

        <TabsContent value="dashboard" className="mt-6">
          <TeacherAttendanceDashboard teacherId={teacherId} />
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default TeacherAttendance;