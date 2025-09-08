import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Calendar, Clock, Users, CheckCircle, XCircle, AlertCircle, User, BookOpen } from 'lucide-react';
import { AttendanceStatus, UserType } from '@/types/attendance';
import { 
  useTeacherAbsentStudents, 
  useStudentsForSlot,
  useStudentsForClass,
  useMarkAttendanceForSlot,
  useMarkAttendanceForClass,
  useCanTeacherMarkAttendance 
} from '../hooks/use-attendance';
import { useAllTeacherClasses } from '@/hooks/useTeacherClasses';
import { AttendanceStatusBadge } from './attendance-status-badge';
import toast from 'react-hot-toast';

interface TeacherAttendanceMarkingProps {
  teacherId: number;
  selectedDate?: string;
}

interface StudentAttendanceRow {
  userId: number;
  userName: string;
  status: AttendanceStatus;
  remarks?: string;
  excuse?: string;
}

export function TeacherAttendanceMarking({ teacherId, selectedDate }: TeacherAttendanceMarkingProps) {
  const [currentDate] = useState(selectedDate || new Date().toISOString().split('T')[0]);
  const [selectedSlot, setSelectedSlot] = useState<number | null>(null);
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [studentAttendance, setStudentAttendance] = useState<StudentAttendanceRow[]>([]);

  // Fetch teacher's classes
  const { 
    data: teacherClasses, 
    isLoading: classesLoading
  } = useAllTeacherClasses();

  // Fetch absent students
  const { 
    data: absentStudents, 
    isLoading: absentLoading 
  } = useTeacherAbsentStudents(teacherId, currentDate);

  // Fetch students for selected slot (only for real slots, not virtual ones)
  const { 
    data: slotStudents, 
    isLoading: studentsLoading,
    refetch: refetchStudents 
  } = useStudentsForSlot(selectedSlot && selectedSlot > 0 ? selectedSlot : 0, currentDate);

  // Fetch students for class if it's a virtual slot or selected class
  const { 
    data: classStudents, 
    isLoading: classStudentsLoading,
    refetch: refetchClassStudents 
  } = useStudentsForClass(selectedClassId || 0, currentDate);

  // Check if teacher can mark attendance (only for real slots, not class-based)
  const { 
    data: canMarkAttendance 
  } = useCanTeacherMarkAttendance(teacherId, selectedSlot && selectedSlot > 0 ? selectedSlot : 0, currentDate);

  // Mark attendance mutations
  const markAttendanceMutation = useMarkAttendanceForSlot();
  const markClassAttendanceMutation = useMarkAttendanceForClass();

  // Update student attendance when slot students or class students change
  useEffect(() => {
    const students = selectedClassId ? classStudents : slotStudents;
    if (students) {
      setStudentAttendance(
        students.map(student => ({
          userId: student.userId,
          userName: student.userName,
          status: student.status,
          remarks: student.remarks,
          excuse: student.excuse,
        }))
      );
    }
  }, [slotStudents, classStudents, selectedClassId]);

  const handleClassSelect = (classId: number) => {
    setSelectedClassId(classId);
    setSelectedSlot(-1); // Use virtual slot for class-based attendance
  };

  const handleStatusChange = (userId: number, status: AttendanceStatus) => {
    setStudentAttendance(prev =>
      prev.map(student =>
        student.userId === userId ? { ...student, status } : student
      )
    );
  };

  const handleRemarksChange = (userId: number, remarks: string) => {
    setStudentAttendance(prev =>
      prev.map(student =>
        student.userId === userId ? { ...student, remarks } : student
      )
    );
  };

  const handleMarkAllPresent = () => {
    setStudentAttendance(prev =>
      prev.map(student => ({ ...student, status: AttendanceStatus.PRESENT }))
    );
  };

  const handleMarkAllAbsent = () => {
    setStudentAttendance(prev =>
      prev.map(student => ({ ...student, status: AttendanceStatus.ABSENT }))
    );
  };

  const handleSaveAttendance = async () => {
    if (!selectedSlot && !selectedClassId) return;

    const attendanceList = studentAttendance.map(student => ({
      userId: student.userId,
      timetableSlotId: selectedClassId ? -1 : (selectedSlot || undefined),
      date: currentDate,
      status: student.status,
      userType: UserType.STUDENT,
      remarks: student.remarks,
      excuse: student.excuse,
    }));

    try {
      if (selectedClassId) {
        // Use class-based attendance marking for class selection
        await markClassAttendanceMutation.mutateAsync({
          classId: selectedClassId,
          date: currentDate,
          attendanceList,
        });
      } else if (selectedSlot) {
        // Use slot-based attendance marking for real slots
        await markAttendanceMutation.mutateAsync({
          slotId: selectedSlot,
          date: currentDate,
          attendanceList,
        });
      }
      
      setSelectedSlot(null);
      setSelectedClassId(null);
      if (selectedClassId) {
        refetchClassStudents();
      } else {
        refetchStudents();
      }
      toast.success('Attendance saved successfully!');
    } catch (error) {
      console.error('Failed to save attendance:', error);
      toast.error('Failed to save attendance. Please try again.');
    }
  };

  const getStatusIcon = (status: AttendanceStatus) => {
    switch (status) {
      case AttendanceStatus.PRESENT:
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case AttendanceStatus.ABSENT:
        return <XCircle className="h-4 w-4 text-red-500" />;
      case AttendanceStatus.LATE:
        return <Clock className="h-4 w-4 text-yellow-500" />;
      case AttendanceStatus.EXCUSED:
        return <AlertCircle className="h-4 w-4 text-blue-500" />;
      default:
        return <User className="h-4 w-4 text-gray-500" />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            Attendance Management - {new Date(currentDate).toLocaleDateString()}
          </CardTitle>
        </CardHeader>
      </Card>

      <Tabs defaultValue="schedule" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="schedule">Your Classes</TabsTrigger>
          <TabsTrigger value="absent">Absent Students</TabsTrigger>
          <TabsTrigger value="marking">Mark Attendance</TabsTrigger>
        </TabsList>

        {/* Today's Schedule Tab */}
        <TabsContent value="schedule" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BookOpen className="h-5 w-5" />
                Your Classes
              </CardTitle>
            </CardHeader>
            <CardContent>
              {classesLoading ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
                  <p className="text-gray-600">Loading classes...</p>
                </div>
              ) : !teacherClasses || teacherClasses.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <BookOpen className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>No classes assigned to you</p>
                  <p className="text-sm mt-2">Contact your administrator to get classes assigned.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {teacherClasses.map((cls) => (
                    <div
                      key={cls.id}
                      className={`flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 cursor-pointer transition-colors ${
                        selectedClassId === cls.id ? 'bg-blue-50 border-blue-200' : ''
                      }`}
                      onClick={() => handleClassSelect(cls.id)}
                    >
                      <div className="flex items-center gap-4">
                        <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                          <BookOpen className="h-5 w-5 text-blue-600" />
                        </div>
                        <div>
                          <p className="font-medium">{cls.name}</p>
                          <p className="text-sm text-gray-600">
                            {cls.enrolled || 0} students enrolled
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">
                          <Users className="h-3 w-3 mr-1" />
                          {cls.enrolled || 0} Students
                        </Badge>
                        <Button 
                          variant={selectedClassId === cls.id ? "default" : "outline"} 
                          size="sm"
                        >
                          {selectedClassId === cls.id ? "Selected" : "Mark Attendance"}
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Absent Students Tab */}
        <TabsContent value="absent" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <XCircle className="h-5 w-5 text-red-500" />
                Absent Students Today
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
                <div className="space-y-2">
                  {absentStudents.map((student) => (
                    <div key={student.id} className="flex items-center justify-between p-3 border rounded-lg">
                      <div className="flex items-center gap-3">
                        <XCircle className="h-4 w-4 text-red-500" />
                        <div>
                          <p className="font-medium">{student.userName}</p>
                          <p className="text-sm text-gray-600">{student.className}</p>
                        </div>
                      </div>
                      <AttendanceStatusBadge status={student.status} />
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Mark Attendance Tab */}
        <TabsContent value="marking" className="space-y-4">
          {!selectedSlot && !selectedClassId ? (
            <Card>
              <CardContent className="p-6 text-center">
                <Users className="h-12 w-12 mx-auto mb-4 opacity-50" />
                <p className="text-gray-600">Select a class from "Your Classes" to mark attendance</p>
              </CardContent>
            </Card>
          ) : (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <span className="flex items-center gap-2">
                    <Users className="h-5 w-5" />
                    Mark Attendance
                  </span>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={handleMarkAllPresent}>
                      Mark All Present
                    </Button>
                    <Button variant="outline" size="sm" onClick={handleMarkAllAbsent}>
                      Mark All Absent
                    </Button>
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent>
                {(studentsLoading || classStudentsLoading) ? (
                  <div className="text-center py-4">Loading students...</div>
                ) : !canMarkAttendance && selectedSlot && selectedSlot > 0 && !selectedClassId ? (
                  <div className="text-center py-8 text-red-600">
                    <XCircle className="h-12 w-12 mx-auto mb-4" />
                    <p>You cannot mark attendance for this slot</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="space-y-2">
                      {studentAttendance.map((student) => (
                        <div key={student.userId} className="flex items-center justify-between p-3 border rounded-lg">
                          <div className="flex items-center gap-3">
                            {getStatusIcon(student.status)}
                            <span className="font-medium">{student.userName}</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <select
                              value={student.status}
                              onChange={(e) => handleStatusChange(student.userId, e.target.value as AttendanceStatus)}
                              className="px-3 py-1 border rounded-md text-sm"
                            >
                              <option value={AttendanceStatus.PRESENT}>Present</option>
                              <option value={AttendanceStatus.ABSENT}>Absent</option>
                              <option value={AttendanceStatus.LATE}>Late</option>
                              <option value={AttendanceStatus.EXCUSED}>Excused</option>
                            </select>
                            <input
                              type="text"
                              placeholder="Remarks..."
                              value={student.remarks || ''}
                              onChange={(e) => handleRemarksChange(student.userId, e.target.value)}
                              className="px-3 py-1 border rounded-md text-sm w-32"
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                    
                    <div className="flex justify-end gap-2 pt-4 border-t">
                      <Button 
                        variant="outline" 
                        onClick={() => {
                          setSelectedSlot(null);
                          setSelectedClassId(null);
                        }}
                      >
                        Cancel
                      </Button>
                      <Button 
                        onClick={handleSaveAttendance}
                        disabled={markAttendanceMutation.isPending || markClassAttendanceMutation.isPending}
                      >
                        {(markAttendanceMutation.isPending || markClassAttendanceMutation.isPending) ? 'Saving...' : 'Save Attendance'}
                      </Button>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
