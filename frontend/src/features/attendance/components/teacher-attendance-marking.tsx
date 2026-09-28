import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Calendar, Clock, Users, CheckCircle, XCircle, AlertCircle, User, BookOpen } from 'lucide-react';
import { AttendanceStatus, UserType } from '@/types/attendance';
import { 
  useTeacherAbsentStudents, 
  useStudentsForSlot,
  useMarkAttendanceForSlot,
  useCanTeacherMarkAttendance 
} from '../hooks/use-attendance';
import { useTeacherSchedule } from '@/features/schedule/hooks/use-schedule';
import type { DayOfWeek } from '@/types/timetable';
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

const DAYS: DayOfWeek[] = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

export function TeacherAttendanceMarking({ teacherId, selectedDate }: TeacherAttendanceMarkingProps) {
  const currentDate = selectedDate || new Date().toISOString().split('T')[0];
  const [selectedSlot, setSelectedSlot] = useState<number | null>(null);
  const [studentAttendance, setStudentAttendance] = useState<StudentAttendanceRow[]>([]);

  // Attendance is taken per timetable slot: the teacher's own slots on the selected day.
  const { 
    data: teacherSlots, 
    isLoading: slotsLoading
  } = useTeacherSchedule(teacherId);
  const dayOfWeek = DAYS[new Date(`${currentDate}T00:00:00`).getDay()];
  const daySlots = (teacherSlots ?? [])
    .filter(slot => slot.dayOfWeek === dayOfWeek)
    .sort((a, b) => (a.period?.index ?? 0) - (b.period?.index ?? 0));

  // Fetch absent students
  const { 
    data: absentStudents, 
    isLoading: absentLoading 
  } = useTeacherAbsentStudents(teacherId, currentDate);

  const { 
    data: slotStudents, 
    isLoading: studentsLoading,
    refetch: refetchStudents 
  } = useStudentsForSlot(selectedSlot ?? 0, currentDate);

  const { 
    data: canMarkAttendance 
  } = useCanTeacherMarkAttendance(teacherId, selectedSlot ?? 0, currentDate);

  const markAttendanceMutation = useMarkAttendanceForSlot();

  useEffect(() => {
    if (slotStudents) {
      setStudentAttendance(
        slotStudents.map(student => ({
          userId: student.userId,
          userName: student.userName,
          status: student.status,
          remarks: student.remarks,
          excuse: student.excuse,
        }))
      );
    }
  }, [slotStudents]);

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
    if (!selectedSlot) return;

    const attendanceList = studentAttendance.map(student => ({
      userId: student.userId,
      timetableSlotId: selectedSlot,
      date: currentDate,
      status: student.status,
      userType: UserType.STUDENT,
      remarks: student.remarks,
      excuse: student.excuse,
    }));

    try {
      await markAttendanceMutation.mutateAsync({
        slotId: selectedSlot,
        date: currentDate,
        attendanceList,
      });
      
      setSelectedSlot(null);
      refetchStudents();
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
          <TabsTrigger value="schedule">Your Slots</TabsTrigger>
          <TabsTrigger value="absent">Absent Students</TabsTrigger>
          <TabsTrigger value="marking">Mark Attendance</TabsTrigger>
        </TabsList>

        {/* Today's Schedule Tab */}
        <TabsContent value="schedule" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BookOpen className="h-5 w-5" />
                Your Slots
              </CardTitle>
            </CardHeader>
            <CardContent>
              {slotsLoading ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto mb-2"></div>
                  <p className="text-gray-600">Loading your timetable...</p>
                </div>
              ) : daySlots.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <BookOpen className="h-12 w-12 mx-auto mb-4 opacity-50" />
                  <p>No timetable slots on this day</p>
                  <p className="text-sm mt-2">Attendance is taken for the slots you teach.</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {daySlots.map((slot) => (
                    <div
                      key={slot.id}
                      className={`flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 cursor-pointer transition-colors ${
                        selectedSlot === slot.id ? 'bg-blue-50 border-blue-200' : ''
                      }`}
                      onClick={() => setSelectedSlot(slot.id)}
                    >
                      <div className="flex items-center gap-4">
                        <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                          <BookOpen className="h-5 w-5 text-blue-600" />
                        </div>
                        <div>
                          <p className="font-medium">{slot.forClass?.name ?? 'Unassigned class'}</p>
                          <p className="text-sm text-gray-600">
                            {slot.forCourse?.name}
                            {slot.period && ` · Period ${slot.period.index} (${slot.period.startTime} - ${slot.period.endTime})`}
                          </p>
                        </div>
                      </div>
                      <Button 
                        variant={selectedSlot === slot.id ? "default" : "outline"} 
                        size="sm"
                      >
                        {selectedSlot === slot.id ? "Selected" : "Mark Attendance"}
                      </Button>
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
          {!selectedSlot ? (
            <Card>
              <CardContent className="p-6 text-center">
                <Users className="h-12 w-12 mx-auto mb-4 opacity-50" />
                <p className="text-gray-600">Select a slot from "Your Slots" to mark attendance</p>
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
                {studentsLoading ? (
                  <div className="text-center py-4">Loading students...</div>
                ) : canMarkAttendance === false ? (
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
                        onClick={() => setSelectedSlot(null)}
                      >
                        Cancel
                      </Button>
                      <Button 
                        onClick={handleSaveAttendance}
                        disabled={markAttendanceMutation.isPending}
                      >
                        {markAttendanceMutation.isPending ? 'Saving...' : 'Save Attendance'}
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
