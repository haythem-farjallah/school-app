import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Calendar, Users, CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';
import { useAllTeacherClasses, TeacherClass } from '@/hooks/useTeacherClasses';
import { useStudentsForClass, useMarkAttendanceForClass } from '@/features/attendance/hooks/use-attendance';
import { AttendanceStatus } from '@/types/attendance';
import toast from 'react-hot-toast';

interface StudentAttendance {
  userId: number;
  userName: string;
  status: AttendanceStatus;
}

const TeacherAttendance = () => {
  const { user } = useAuth();
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [selectedTime, setSelectedTime] = useState(new Date().toTimeString().slice(0, 5));
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [selectedCourse, setSelectedCourse] = useState<string>('');
  const [studentAttendance, setStudentAttendance] = useState<StudentAttendance[]>([]);
  const [isSaving, setIsSaving] = useState(false);

  // Get teacher's classes
  const { data: teacherClasses = [], isLoading: classesLoading } = useAllTeacherClasses();
  
  // Get students for selected class
  const { data: classStudents = [], isLoading: studentsLoading, refetch: refetchStudents } = useStudentsForClass(selectedClassId || 0);
  
  // Mutation for saving attendance
  const markAttendanceMutation = useMarkAttendanceForClass();

  // Initialize student attendance when class is selected
  React.useEffect(() => {
    if (classStudents.length > 0) {
      setStudentAttendance(classStudents.map(student => ({
        userId: student.userId,
        userName: student.userName || `Student ${student.userId}`,
        status: student.status || AttendanceStatus.PRESENT
      })));
    }
  }, [classStudents]);

  const handleClassSelect = (classId: number) => {
    setSelectedClassId(classId);
    setStudentAttendance([]);
    
    // Auto-populate subject for this teacher and class
    const selectedClass = teacherClasses.find(c => c.id === classId);
    
    if (selectedClass) {
      let subject = '';
      
      // First, try to get courses from the class
      if (selectedClass.courses && selectedClass.courses.length > 0) {
        const courseNames = selectedClass.courses.map(course => course.name);
        subject = courseNames.join(', ');
      } else {
        // Fallback: derive subject from class name/grade
        const className = selectedClass.name?.toLowerCase() || '';
        const grade = selectedClass.grade?.toLowerCase() || '';
        
        if (className.includes('math') || grade.includes('math')) {
          subject = 'Mathematics';
        } else if (className.includes('science') || grade.includes('science')) {
          subject = 'Science';
        } else if (className.includes('english') || grade.includes('english')) {
          subject = 'English';
        } else if (className.includes('history') || grade.includes('history')) {
          subject = 'History';
        } else if (className.includes('art') || grade.includes('art')) {
          subject = 'Arts';
        } else if (className.includes('pe') || className.includes('physical')) {
          subject = 'Physical Education';
        } else {
          // Final fallback: use class name as subject
          subject = `${selectedClass.name} - Class Activities`;
        }
      }
      
      setSelectedCourse(subject);
    } else {
      setSelectedCourse('General Studies');
    }
  };

  const updateStudentStatus = (userId: number, status: AttendanceStatus) => {
    setStudentAttendance(prev =>
      prev.map(student =>
        student.userId === userId ? { ...student, status } : student
      )
    );
  };

  const markAllPresent = () => {
    setStudentAttendance(prev =>
      prev.map(student => ({ ...student, status: AttendanceStatus.PRESENT }))
    );
  };

  const markAllAbsent = () => {
    setStudentAttendance(prev =>
      prev.map(student => ({ ...student, status: AttendanceStatus.ABSENT }))
    );
  };

  const saveAttendance = async () => {
    if (!selectedClassId) {
      toast.error('Please select a class first');
      return;
    }
    
    if (studentAttendance.length === 0) {
      toast.error('No students found to mark attendance for');
      return;
    }

    if (!selectedCourse.trim()) {
      toast.error('Subject information not available for this class');
      return;
    }

    setIsSaving(true);
    try {
      await markAttendanceMutation.mutateAsync({
        classId: selectedClassId,
        date: selectedDate,
        time: selectedTime,
        course: selectedCourse.trim(),
        teacherId: user?.id,
        attendanceList: studentAttendance.map(student => ({
          userId: student.userId,
          date: selectedDate,
          time: selectedTime,
          status: student.status,
          userType: 'STUDENT' as const,
          course: selectedCourse.trim(),
          remarks: '',
          excuse: ''
        }))
      });
      
      toast.success('Attendance saved successfully! Notifications sent to parents.');
      refetchStudents();
    } catch (error) {
      console.error('Failed to save attendance:', error);
      toast.error('Failed to save attendance. Please try again.');
    } finally {
      setIsSaving(false);
    }
  };

  const getStatusIcon = (status: AttendanceStatus) => {
    switch (status) {
      case AttendanceStatus.PRESENT:
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case AttendanceStatus.ABSENT:
        return <XCircle className="h-5 w-5 text-red-500" />;
      case AttendanceStatus.LATE:
        return <Clock className="h-5 w-5 text-yellow-500" />;
      case AttendanceStatus.EXCUSED:
        return <AlertCircle className="h-5 w-5 text-blue-500" />;
      default:
        return <Users className="h-5 w-5 text-gray-500" />;
    }
  };

  const getStatusColor = (status: AttendanceStatus) => {
    switch (status) {
      case AttendanceStatus.PRESENT:
        return 'bg-green-100 text-green-800 border-green-200';
      case AttendanceStatus.ABSENT:
        return 'bg-red-100 text-red-800 border-red-200';
      case AttendanceStatus.LATE:
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case AttendanceStatus.EXCUSED:
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  if (!user?.id) {
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
          <h1 className="text-2xl font-bold text-gray-900">Attendance Management</h1>
          <p className="text-gray-600 mt-1">Mark student attendance for your classes</p>
        </div>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4 text-gray-500" />
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="flex items-center gap-2">
            <Clock className="h-4 w-4 text-gray-500" />
            <input
              type="time"
              value={selectedTime}
              onChange={(e) => setSelectedTime(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="flex items-center gap-2">
            <div className={`px-3 py-2 border rounded-md min-w-[150px] font-medium ${
              selectedCourse 
                ? 'bg-blue-50 border-blue-200 text-blue-800' 
                : 'bg-gray-50 border-gray-200 text-gray-500'
            }`}>
              {selectedCourse || (selectedClassId ? 'Loading subject...' : 'Select a class first')}
            </div>
          </div>
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
          {classesLoading ? (
            <p className="text-gray-500">Loading classes...</p>
          ) : teacherClasses.length === 0 ? (
            <p className="text-gray-500">No classes assigned to you</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {teacherClasses.map((classItem) => (
                <div
                  key={classItem.id}
                  className={`relative p-6 rounded-lg border-2 transition-all cursor-pointer hover:shadow-md ${
                    selectedClassId === classItem.id
                      ? 'bg-blue-600 border-blue-600 text-white shadow-lg'
                      : 'bg-white border-gray-200 text-gray-900 hover:border-blue-300'
                  }`}
                  onClick={() => handleClassSelect(classItem.id)}
                >
                  <div className="text-center">
                    <h3 className={`font-bold text-xl mb-2 ${
                      selectedClassId === classItem.id ? 'text-white' : 'text-gray-900'
                    }`}>
                      {classItem.name}
                    </h3>
                    <p className={`text-sm ${
                      selectedClassId === classItem.id ? 'text-blue-100' : 'text-gray-600'
                    }`}>
                      {selectedClassId === classItem.id ? classStudents.length : classItem.studentCount || 0} students
                    </p>
                  </div>
                  {selectedClassId === classItem.id && (
                    <div className="absolute top-2 right-2">
                      <CheckCircle className="h-5 w-5 text-white" />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>


      {/* Attendance Marking */}
      {selectedClassId && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <CheckCircle className="h-5 w-5" />
                Mark Attendance
              </CardTitle>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={markAllPresent}
                  className="text-green-600 border-green-200 hover:bg-green-50"
                >
                  Mark All Present
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={markAllAbsent}
                  className="text-red-600 border-red-200 hover:bg-red-50"
                >
                  Mark All Absent
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            {studentsLoading ? (
              <p className="text-gray-500">Loading students...</p>
            ) : studentAttendance.length === 0 ? (
              <p className="text-gray-500">No students found in this class</p>
            ) : (
              <div className="space-y-4">
                {/* Students Table */}
                <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead className="bg-gray-50 border-b border-gray-200">
                        <tr>
                          <th className="text-left py-4 px-6 font-semibold text-gray-900">Student</th>
                          <th className="text-center py-4 px-4 font-semibold text-gray-900">Status</th>
                          <th className="text-center py-4 px-6 font-semibold text-gray-900">Mark Attendance</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-200">
                        {studentAttendance.map((student, index) => (
                          <tr key={student.userId} className="hover:bg-gray-50 transition-colors">
                            {/* Student Info */}
                            <td className="py-4 px-6">
                              <div className="flex items-center gap-3">
                                <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                                  <span className="text-blue-600 font-semibold text-sm">
                                    {student.userName.split(' ').map(n => n[0]).join('')}
                                  </span>
                                </div>
                                <div>
                                  <h4 className="font-semibold text-gray-900">{student.userName}</h4>
                                  <p className="text-sm text-gray-500">#{index + 1}</p>
                                </div>
                              </div>
                            </td>

                            {/* Current Status */}
                            <td className="py-4 px-4 text-center">
                              <div className={`inline-flex items-center gap-2 px-3 py-1 rounded-full text-sm font-medium ${
                                student.status === AttendanceStatus.PRESENT ? 'bg-green-100 text-green-700' :
                                student.status === AttendanceStatus.ABSENT ? 'bg-red-100 text-red-700' :
                                student.status === AttendanceStatus.LATE ? 'bg-yellow-100 text-yellow-700' :
                                'bg-blue-100 text-blue-700'
                              }`}>
                                {getStatusIcon(student.status)}
                                <span>
                                  {student.status === AttendanceStatus.PRESENT ? 'Present' :
                                   student.status === AttendanceStatus.ABSENT ? 'Absent' :
                                   student.status === AttendanceStatus.LATE ? 'Late' :
                                   'Excused'}
                                </span>
                              </div>
                            </td>

                            {/* Action Buttons */}
                            <td className="py-4 px-6">
                              <div className="flex items-center justify-center gap-2">
                                <button
                                  onClick={() => updateStudentStatus(student.userId, AttendanceStatus.PRESENT)}
                                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                                    student.status === AttendanceStatus.PRESENT
                                      ? 'bg-green-500 text-white shadow-md'
                                      : 'bg-green-50 text-green-700 hover:bg-green-100 border border-green-200'
                                  }`}
                                >
                                  ✓ Present
                                </button>
                                
                                <button
                                  onClick={() => updateStudentStatus(student.userId, AttendanceStatus.ABSENT)}
                                  className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                                    student.status === AttendanceStatus.ABSENT
                                      ? 'bg-red-500 text-white shadow-md'
                                      : 'bg-red-50 text-red-700 hover:bg-red-100 border border-red-200'
                                  }`}
                                >
                                  ✗ Absent
                                </button>
                                
                                <button
                                  onClick={() => updateStudentStatus(student.userId, AttendanceStatus.LATE)}
                                  className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                                    student.status === AttendanceStatus.LATE
                                      ? 'bg-yellow-500 text-white shadow-md'
                                      : 'bg-yellow-50 text-yellow-700 hover:bg-yellow-100 border border-yellow-200'
                                  }`}
                                >
                                  ⏰ Late
                                </button>
                                
                                <button
                                  onClick={() => updateStudentStatus(student.userId, AttendanceStatus.EXCUSED)}
                                  className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                                    student.status === AttendanceStatus.EXCUSED
                                      ? 'bg-blue-500 text-white shadow-md'
                                      : 'bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200'
                                  }`}
                                >
                                  📋 Excused
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Summary & Save */}
                <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
                  <div className="flex gap-6">
                    <div className="flex items-center gap-2">
                      <CheckCircle className="h-4 w-4 text-green-500" />
                      <span className="text-sm">
                        Present: {studentAttendance.filter(s => s.status === AttendanceStatus.PRESENT).length}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <XCircle className="h-4 w-4 text-red-500" />
                      <span className="text-sm">
                        Absent: {studentAttendance.filter(s => s.status === AttendanceStatus.ABSENT).length}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Clock className="h-4 w-4 text-yellow-500" />
                      <span className="text-sm">
                        Late: {studentAttendance.filter(s => s.status === AttendanceStatus.LATE).length}
                      </span>
                    </div>
                  </div>
                  <Button
                    onClick={saveAttendance}
                    disabled={isSaving || !selectedClassId || !selectedCourse || studentAttendance.length === 0}
                    className="bg-blue-600 hover:bg-blue-700 text-white disabled:bg-gray-400 disabled:cursor-not-allowed"
                  >
                    {isSaving ? 'Saving...' : 'Save Attendance'}
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
};

export default TeacherAttendance;