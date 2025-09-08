import React, { useState } from 'react';
import { TeacherScheduleView } from '../../features/timetable/components/TeacherScheduleView';
import { useTeachers } from '@/features/teachers/hooks/use-teachers';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { 
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../components/ui/select';
import { 
  GraduationCap, 
  Users, 
  RefreshCw,
  AlertCircle 
} from 'lucide-react';
import { Teacher } from '../../types/teacher';

const TeacherSchedulePage: React.FC = () => {
  const [selectedTeacherId, setSelectedTeacherId] = useState<number | null>(null);
  const { data, isLoading, error, refetch } = useTeachers();
  
  const teachers = data?.data || [];
  const selectedTeacher = teachers.find((t: Teacher) => t.id === selectedTeacherId);

  if (isLoading) {
    return (
      <div className="p-8">
        <div className="flex items-center justify-center">
          <RefreshCw className="w-6 h-6 animate-spin mr-2" />
          <span>Loading teachers...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8">
        <Card>
          <CardContent className="p-8">
            <div className="text-center">
              <AlertCircle className="w-12 h-12 mx-auto mb-4 text-red-500" />
              <p className="text-red-600 font-medium">Error loading teachers</p>
              <Button 
                onClick={() => refetch()}
                variant="outline"
                className="mt-4"
              >
                <RefreshCw className="w-4 h-4 mr-2" />
                Retry
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!teachers.length) {
    return (
      <div className="p-8">
        <Card>
          <CardContent className="p-8">
            <div className="text-center">
              <Users className="w-12 h-12 mx-auto mb-4 text-gray-400" />
              <p className="text-gray-600 font-medium">No teachers found</p>
              <p className="text-gray-500 text-sm">Please add teachers to view their schedules.</p>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-8">
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-800 flex items-center gap-3">
              <GraduationCap className="w-8 h-8" />
              Teacher Schedules
            </h1>
            <p className="text-gray-600 mt-2">
              View individual teacher timetables and workload information
            </p>
          </div>
          <Button 
            onClick={() => refetch()}
            variant="outline"
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
        </div>

        {/* Teacher Selection */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="w-5 h-5" />
              Select Teacher ({teachers.length} available)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-4">
              <div className="flex-1 max-w-md">
                <Select
                  value={selectedTeacherId?.toString() || ''}
                  onValueChange={(value) => setSelectedTeacherId(value ? Number(value) : null)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Choose a teacher to view their schedule" />
                  </SelectTrigger>
                  <SelectContent>
                    {teachers.map((teacher: Teacher) => (
                      <SelectItem key={teacher.id} value={teacher.id.toString()}>
                        <div className="flex items-center gap-2">
                          <GraduationCap className="w-4 h-4" />
                          {teacher.firstName} {teacher.lastName}
                          {teacher.subjectsTaught && (
                            <span className="text-xs text-gray-500">
                              - {teacher.subjectsTaught}
                            </span>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              
              {selectedTeacher && (
                <div className="text-sm text-gray-600">
                  <p className="font-medium">{selectedTeacher.firstName} {selectedTeacher.lastName}</p>
                  <p className="text-xs">{selectedTeacher.email}</p>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Teacher Schedule */}
        {selectedTeacherId ? (
          <TeacherScheduleView 
            teacherId={selectedTeacherId} 
            teacher={selectedTeacher}
          />
        ) : (
          <Card>
            <CardContent className="p-12">
              <div className="text-center">
                <GraduationCap className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                <h3 className="text-lg font-medium text-gray-600 mb-2">
                  Select a Teacher
                </h3>
                <p className="text-gray-500">
                  Choose a teacher from the dropdown above to view their weekly schedule and workload information.
                </p>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
};

export default TeacherSchedulePage;
