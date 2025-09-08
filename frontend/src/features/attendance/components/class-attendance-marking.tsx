import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { Checkbox } from '../../../components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../../components/ui/select';
import { 
  Users, 
  UserCheck, 
  UserX, 
  Clock, 
  AlertCircle, 
  CheckSquare,
  Square,
  Save
} from 'lucide-react';
import { AttendanceStatus, UserType } from '../../../types/attendance';
import { 
  useStudentsForClass,
  useTeacherAttendanceClass,
  useMarkAttendanceForClass 
} from '../hooks/use-attendance';
import { useAllTeacherClasses } from '../../../hooks/useTeacherClasses';
import { AttendanceStatusBadge } from './attendance-status-badge';
import toast from 'react-hot-toast';

interface StudentAttendanceRow {
  userId: number;
  userName: string;
  status: AttendanceStatus;
  isSelected: boolean;
}

export function ClassAttendanceMarking() {
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [currentDate] = useState(new Date().toISOString().split('T')[0]);
  const [studentAttendance, setStudentAttendance] = useState<StudentAttendanceRow[]>([]);
  const [selectAll, setSelectAll] = useState(false);

  // Fetch teacher's classes
  const { data: teacherClasses, isLoading: classesLoading } = useAllTeacherClasses();

  // Fetch students for selected class (using attendance endpoint that teachers can access)
  // For now, let's use the simple students endpoint
  const { 
    data: classStudents, 
    isLoading: studentsLoading,
    error: studentsError,
    refetch: refetchStudents 
  } = useStudentsForClass(selectedClassId || 0);

  // TODO: Use the teacher attendance class view when we have teacher ID
  // const { 
  //   data: classDetails, 
  //   isLoading: classDetailsLoading,
  //   error: classDetailsError 
  // } = useTeacherAttendanceClass(teacherId, selectedClassId, courseId);

  // Mark attendance mutation
  const markAttendanceMutation = useMarkAttendanceForClass();

  // Update student attendance when class students change
  useEffect(() => {
    console.log('🔍 Class students changed:', classStudents);
    if (classStudents && classStudents.length > 0) {
      setStudentAttendance(
        classStudents.map(student => ({
          userId: student.userId,
          userName: student.userName,
          status: student.status || AttendanceStatus.PRESENT, // Use existing status or default to present
          isSelected: false,
        }))
      );
      setSelectAll(false);
    } else {
      setStudentAttendance([]);
    }
  }, [classStudents]);

  const handleClassSelect = (classId: string) => {
    const parsedClassId = parseInt(classId);
    setSelectedClassId(parsedClassId);
    setStudentAttendance([]);
    setSelectAll(false);
  };

  const handleStudentStatusChange = (userId: number, status: AttendanceStatus) => {
    setStudentAttendance(prev =>
      prev.map(student =>
        student.userId === userId ? { ...student, status } : student
      )
    );
  };

  const handleStudentSelection = (userId: number, isSelected: boolean) => {
    setStudentAttendance(prev => {
      const updated = prev.map(student =>
        student.userId === userId ? { ...student, isSelected } : student
      );
      
      // Update select all state
      const allSelected = updated.every(student => student.isSelected);
      setSelectAll(allSelected);
      
      return updated;
    });
  };

  const handleSelectAll = (checked: boolean) => {
    setSelectAll(checked);
    setStudentAttendance(prev =>
      prev.map(student => ({ ...student, isSelected: checked }))
    );
  };


  const handleSaveAttendance = async () => {
    if (!selectedClassId) return;

    const attendanceList = studentAttendance.map(student => ({
      userId: student.userId,
      date: currentDate,
      status: student.status,
      userType: UserType.STUDENT,
      classId: selectedClassId,
    }));

    try {
      await markAttendanceMutation.mutateAsync({
        classId: selectedClassId,
        date: currentDate,
        attendanceList,
      });
      
      toast.success(`Attendance saved for ${attendanceList.length} students`);
      refetchStudents();
      
      // Reset selections
      setSelectAll(false);
      setStudentAttendance(prev =>
        prev.map(student => ({ ...student, isSelected: false }))
      );
    } catch (error) {
      console.error('Failed to save attendance:', error);
      toast.error('Failed to save attendance');
    }
  };

  const selectedStudentsCount = studentAttendance.filter(s => s.isSelected).length;
  const getStatusIcon = (status: AttendanceStatus) => {
    switch (status) {
      case AttendanceStatus.PRESENT:
        return <UserCheck className="h-4 w-4 text-green-600" />;
      case AttendanceStatus.ABSENT:
        return <UserX className="h-4 w-4 text-red-600" />;
      case AttendanceStatus.LATE:
        return <Clock className="h-4 w-4 text-yellow-600" />;
      case AttendanceStatus.EXCUSED:
        return <AlertCircle className="h-4 w-4 text-blue-600" />;
      default:
        return <Users className="h-4 w-4 text-gray-600" />;
    }
  };

  if (classesLoading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        <span className="ml-3 text-gray-600">Loading classes...</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Mark Class Attendance</h2>
          <p className="text-gray-600">
            Select a class and mark attendance for students
          </p>
        </div>
        <div className="text-sm text-gray-500">
          Date: {new Date(currentDate).toLocaleDateString()}
        </div>
      </div>

      {/* Class Selection */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="h-5 w-5" />
            Select Class
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Select onValueChange={handleClassSelect}>
            <SelectTrigger className="w-full">
              <SelectValue placeholder="Choose a class to mark attendance" />
            </SelectTrigger>
            <SelectContent>
              {teacherClasses && teacherClasses.length > 0 ? (
                teacherClasses.map((cls) => (
                  <SelectItem key={cls.id} value={cls.id.toString()}>
                    <div className="flex items-center justify-between w-full">
                      <span>{cls.name}</span>
                      <Badge variant="outline" className="ml-2">
                        {cls.enrolled} students
                      </Badge>
                    </div>
                  </SelectItem>
                ))
              ) : (
                <SelectItem value="no-classes" disabled>
                  No classes available
                </SelectItem>
              )}
            </SelectContent>
          </Select>

          {teacherClasses && teacherClasses.length === 0 && (
            <p className="text-sm text-gray-500 mt-2">
              No classes assigned to you. Please contact your administrator.
            </p>
          )}
        </CardContent>
      </Card>

      {/* Student List */}
      {selectedClassId && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <Users className="h-5 w-5" />
                Students ({studentAttendance.length})
              </CardTitle>
              
              {/* Bulk Actions */}
              {selectedStudentsCount > 0 && (
                <div className="flex items-center gap-2">
                  <span className="text-sm text-gray-600">
                    {selectedStudentsCount} selected
                  </span>
                </div>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {studentsLoading ? (
              <div className="flex items-center justify-center p-8">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
                <span className="ml-3 text-gray-600">Loading students...</span>
              </div>
            ) : studentsError ? (
              <div className="text-center py-8 text-red-500">
                <AlertCircle className="h-12 w-12 mx-auto mb-4" />
                <p>Failed to load students</p>
                <p className="text-sm text-gray-500 mt-2">
                  {studentsError?.message || 'Unknown error occurred'}
                </p>
                <Button 
                  onClick={() => refetchStudents()} 
                  variant="outline" 
                  className="mt-4"
                >
                  Retry
                </Button>
              </div>
            ) : studentAttendance.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <Users className="h-12 w-12 mx-auto mb-4 opacity-50" />
                <p>No students found for this class</p>
                <p className="text-xs text-gray-400 mt-2">
                  Class ID: {selectedClassId} | Date: {currentDate}
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Select All */}
                <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                  <Checkbox
                    checked={selectAll}
                    onCheckedChange={handleSelectAll}
                  />
                  <div className="flex items-center gap-2">
                    {selectAll ? <CheckSquare className="h-4 w-4" /> : <Square className="h-4 w-4" />}
                    <span className="font-medium">Select All</span>
                  </div>
                </div>

                {/* Student List */}
                <div className="space-y-2">
                  {studentAttendance.map((student) => (
                    <div
                      key={student.userId}
                      className={`flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition-colors ${
                        student.isSelected ? 'bg-blue-50 border-blue-200' : ''
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <Checkbox
                          checked={student.isSelected}
                          onCheckedChange={(checked) => 
                            handleStudentSelection(student.userId, checked as boolean)
                          }
                        />
                        <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                          <Users className="h-4 w-4 text-blue-600" />
                        </div>
                        <div>
                          <div className="font-medium">{student.userName}</div>
                          <div className="text-sm text-gray-500">ID: {student.userId}</div>
                        </div>
                      </div>

                      <div className="flex items-center gap-3">
                        <AttendanceStatusBadge status={student.status} />
                        <Select
                          value={student.status}
                          onValueChange={(value) => 
                            handleStudentStatusChange(student.userId, value as AttendanceStatus)
                          }
                        >
                          <SelectTrigger className="w-32">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value={AttendanceStatus.PRESENT}>
                              <div className="flex items-center gap-2">
                                {getStatusIcon(AttendanceStatus.PRESENT)}
                                Present
                              </div>
                            </SelectItem>
                            <SelectItem value={AttendanceStatus.ABSENT}>
                              <div className="flex items-center gap-2">
                                {getStatusIcon(AttendanceStatus.ABSENT)}
                                Absent
                              </div>
                            </SelectItem>
                            <SelectItem value={AttendanceStatus.LATE}>
                              <div className="flex items-center gap-2">
                                {getStatusIcon(AttendanceStatus.LATE)}
                                Late
                              </div>
                            </SelectItem>
                            <SelectItem value={AttendanceStatus.EXCUSED}>
                              <div className="flex items-center gap-2">
                                {getStatusIcon(AttendanceStatus.EXCUSED)}
                                Excused
                              </div>
                            </SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Save Button */}
                <div className="flex justify-end pt-4 border-t">
                  <Button 
                    onClick={handleSaveAttendance}
                    disabled={markAttendanceMutation.isPending}
                    className="bg-green-600 hover:bg-green-700"
                  >
                    <Save className="w-4 h-4 mr-2" />
                    {markAttendanceMutation.isPending ? 'Saving...' : 'Save Attendance'}
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
