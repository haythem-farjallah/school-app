import React, { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { 
  Users, 
  Calendar, 
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  UserCheck,
  UserX,
  BarChart3,
  Download,
  Plus
} from 'lucide-react';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Progress } from '@/components/ui/progress';

import { 
  useTeacherAttendance,
  useCreateTeacherAttendance,
  useTeacherAttendanceStatistics
} from '@/features/grades/hooks/use-grades';
import { useTeachers } from '@/features/teachers/hooks/use-teachers';
import { 
  TeacherAttendanceStatus,
  type TeacherAttendance as TeacherAttendanceType
} from '@/types/grade';
import toast from 'react-hot-toast';

const TeacherAttendanceManagement = () => {
  // State management
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [selectedTeacherId, setSelectedTeacherId] = useState<number | null>(null);
  const [markAttendanceDialogOpen, setMarkAttendanceDialogOpen] = useState(false);
  const [statisticsDialogOpen, setStatisticsDialogOpen] = useState(false);
  const [attendanceForm, setAttendanceForm] = useState({
    status: TeacherAttendanceStatus.PRESENT,
    remarks: '',
    excuse: '',
    substituteTeacherId: null as number | null
  });

  // Data fetching
  const { data: teachers } = useTeachers({ size: 100 });
  const { 
    data: attendanceRecords, 
    isLoading: attendanceLoading,
    refetch: refetchAttendance 
  } = useTeacherAttendance({
    startDate: selectedDate,
    endDate: selectedDate
  });
  const { 
    data: teacherStats 
  } = useTeacherAttendanceStatistics(selectedTeacherId || undefined);

  // Mutations
  const createAttendanceMutation = useCreateTeacherAttendance();

  // Handle mark attendance
  const handleMarkAttendance = async () => {
    if (!selectedTeacherId) {
      toast.error('Please select a teacher');
      return;
    }

    const selectedTeacher = teachers?.data.find(t => t.id === selectedTeacherId);
    if (!selectedTeacher) return;

    try {
      await createAttendanceMutation.mutateAsync({
        teacherId: selectedTeacherId,
        teacherFirstName: selectedTeacher.firstName,
        teacherLastName: selectedTeacher.lastName,
        teacherEmail: selectedTeacher.email,
        date: selectedDate,
        status: attendanceForm.status,
        remarks: attendanceForm.remarks || undefined,
        excuse: attendanceForm.excuse || undefined,
        substituteTeacherId: attendanceForm.substituteTeacherId || undefined,
        substituteTeacherName: attendanceForm.substituteTeacherId 
          ? teachers?.data.find(t => t.id === attendanceForm.substituteTeacherId)?.firstName + ' ' + 
            teachers?.data.find(t => t.id === attendanceForm.substituteTeacherId)?.lastName
          : undefined,
        recordedById: 1, // Admin user ID - should come from auth
        recordedByName: 'Admin User' // Should come from auth
      });

      toast.success('Attendance marked successfully');
      setMarkAttendanceDialogOpen(false);
      setSelectedTeacherId(null);
      setAttendanceForm({
        status: TeacherAttendanceStatus.PRESENT,
        remarks: '',
        excuse: '',
        substituteTeacherId: null
      });
      refetchAttendance();
    } catch (error) {
      console.error('Failed to mark attendance:', error);
      toast.error('Failed to mark attendance. Please try again.');
    }
  };

  // Get attendance status color
  const getStatusColor = (status: TeacherAttendanceStatus) => {
    switch (status) {
      case TeacherAttendanceStatus.PRESENT:
        return 'bg-green-100 text-green-800';
      case TeacherAttendanceStatus.ABSENT:
        return 'bg-red-100 text-red-800';
      case TeacherAttendanceStatus.LATE:
        return 'bg-yellow-100 text-yellow-800';
      case TeacherAttendanceStatus.SICK_LEAVE:
        return 'bg-blue-100 text-blue-800';
      case TeacherAttendanceStatus.PERSONAL_LEAVE:
        return 'bg-purple-100 text-purple-800';
      case TeacherAttendanceStatus.PROFESSIONAL_DEVELOPMENT:
        return 'bg-indigo-100 text-indigo-800';
      case TeacherAttendanceStatus.SUBSTITUTE_ARRANGED:
        return 'bg-orange-100 text-orange-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  // Get status icon
  const getStatusIcon = (status: TeacherAttendanceStatus) => {
    switch (status) {
      case TeacherAttendanceStatus.PRESENT:
        return <CheckCircle className="w-4 h-4" />;
      case TeacherAttendanceStatus.ABSENT:
        return <XCircle className="w-4 h-4" />;
      case TeacherAttendanceStatus.LATE:
        return <Clock className="w-4 h-4" />;
      default:
        return <AlertCircle className="w-4 h-4" />;
    }
  };

  // Calculate daily statistics
  const calculateDailyStats = () => {
    if (!attendanceRecords) return { total: 0, present: 0, absent: 0, late: 0, onLeave: 0 };

    const total = attendanceRecords.length;
    const present = attendanceRecords.filter(r => r.status === TeacherAttendanceStatus.PRESENT).length;
    const absent = attendanceRecords.filter(r => r.status === TeacherAttendanceStatus.ABSENT).length;
    const late = attendanceRecords.filter(r => r.status === TeacherAttendanceStatus.LATE).length;
    const onLeave = attendanceRecords.filter(r => 
      r.status === TeacherAttendanceStatus.SICK_LEAVE || 
      r.status === TeacherAttendanceStatus.PERSONAL_LEAVE ||
      r.status === TeacherAttendanceStatus.PROFESSIONAL_DEVELOPMENT
    ).length;

    return { total, present, absent, late, onLeave };
  };

  const dailyStats = calculateDailyStats();

  return (
    <>
      <Helmet>
        <title>Teacher Attendance Management - Admin Portal</title>
        <meta name="description" content="Manage teacher attendance and track statistics" />
      </Helmet>

      <div className="container mx-auto p-6 space-y-8">
        {/* Header */}
        <Card className="border-teal-200/60 bg-gradient-to-br from-teal-50/80 via-cyan-50/40 to-blue-50/20 shadow-xl backdrop-blur-sm">
          <CardHeader className="pb-6">
            <div className="flex items-center justify-between">
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-teal-900 via-cyan-800 to-blue-700 bg-clip-text text-transparent">
                  Teacher Attendance Management
                </CardTitle>
                <CardDescription className="text-teal-700/80 text-lg">
                  Track and manage teacher attendance records
                </CardDescription>
              </div>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setMarkAttendanceDialogOpen(true)}
                  className="border-cyan-300 text-cyan-700 hover:bg-cyan-50"
                >
                  <Plus className="mr-2 h-4 w-4" />
                  Mark Attendance
                </Button>
              </div>
            </div>
          </CardHeader>
        </Card>

        {/* Date Selection */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-teal-600" />
              Select Date
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="w-full md:w-64">
              <Input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                className="w-full"
              />
            </div>
          </CardContent>
        </Card>

        {/* Daily Statistics */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Teachers</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{dailyStats.total}</div>
              <p className="text-xs text-muted-foreground">
                Tracked today
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Present</CardTitle>
              <UserCheck className="h-4 w-4 text-green-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">{dailyStats.present}</div>
              <Progress value={dailyStats.total > 0 ? (dailyStats.present / dailyStats.total) * 100 : 0} className="mt-2" />
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Absent</CardTitle>
              <UserX className="h-4 w-4 text-red-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-red-600">{dailyStats.absent}</div>
              <p className="text-xs text-muted-foreground">
                Unexcused absences
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Late</CardTitle>
              <Clock className="h-4 w-4 text-yellow-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-yellow-600">{dailyStats.late}</div>
              <p className="text-xs text-muted-foreground">
                Late arrivals
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">On Leave</CardTitle>
              <AlertCircle className="h-4 w-4 text-blue-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-600">{dailyStats.onLeave}</div>
              <p className="text-xs text-muted-foreground">
                Approved leave
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Attendance Records */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="h-5 w-5 text-cyan-600" />
              Teacher Attendance - {new Date(selectedDate).toLocaleDateString()}
            </CardTitle>
            <CardDescription>
              Daily attendance records for all teachers
            </CardDescription>
          </CardHeader>
          <CardContent>
            {attendanceLoading ? (
              <div className="p-8 text-center">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-teal-600 mx-auto mb-4"></div>
                <p className="text-gray-600">Loading attendance records...</p>
              </div>
            ) : attendanceRecords && attendanceRecords.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left p-4 font-medium">Teacher</th>
                      <th className="text-left p-4 font-medium">Status</th>
                      <th className="text-left p-4 font-medium">Time Recorded</th>
                      <th className="text-left p-4 font-medium">Remarks</th>
                      <th className="text-left p-4 font-medium">Substitute</th>
                      <th className="text-left p-4 font-medium">Recorded By</th>
                      <th className="text-left p-4 font-medium">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {attendanceRecords.map((record) => (
                      <tr key={record.id} className="border-b hover:bg-gray-50">
                        <td className="p-4">
                          <div>
                            <div className="font-medium">{record.teacherFirstName} {record.teacherLastName}</div>
                            <div className="text-sm text-gray-500">{record.teacherEmail}</div>
                          </div>
                        </td>
                        <td className="p-4">
                          <Badge className={`${getStatusColor(record.status)} flex items-center gap-1 w-fit`}>
                            {getStatusIcon(record.status)}
                            {record.status.replace('_', ' ')}
                          </Badge>
                        </td>
                        <td className="p-4">
                          <div className="text-sm">
                            {new Date(record.createdAt).toLocaleTimeString()}
                          </div>
                        </td>
                        <td className="p-4">
                          <div className="text-sm max-w-xs">
                            {record.remarks || record.excuse || '-'}
                          </div>
                        </td>
                        <td className="p-4">
                          <div className="text-sm">
                            {record.substituteTeacherName || '-'}
                          </div>
                        </td>
                        <td className="p-4">
                          <div className="text-sm">
                            {record.recordedByName}
                          </div>
                        </td>
                        <td className="p-4">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setSelectedTeacherId(record.teacherId);
                              setStatisticsDialogOpen(true);
                            }}
                          >
                            <BarChart3 className="w-4 h-4" />
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="p-8 text-center">
                <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">No Attendance Records</h3>
                <p className="text-gray-600">No attendance records found for the selected date.</p>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Mark Attendance Dialog */}
        <Dialog open={markAttendanceDialogOpen} onOpenChange={setMarkAttendanceDialogOpen}>
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>Mark Teacher Attendance</DialogTitle>
              <DialogDescription>
                Record attendance for {new Date(selectedDate).toLocaleDateString()}
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Teacher</Label>
                <Select
                  value={selectedTeacherId?.toString() || ''}
                  onValueChange={(value) => setSelectedTeacherId(parseInt(value))}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select teacher" />
                  </SelectTrigger>
                  <SelectContent>
                    {teachers?.data.map((teacher) => (
                      <SelectItem key={teacher.id} value={teacher.id.toString()}>
                        {teacher.firstName} {teacher.lastName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Status</Label>
                <Select
                  value={attendanceForm.status}
                  onValueChange={(value: TeacherAttendanceStatus) => 
                    setAttendanceForm(prev => ({ ...prev, status: value }))
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={TeacherAttendanceStatus.PRESENT}>Present</SelectItem>
                    <SelectItem value={TeacherAttendanceStatus.ABSENT}>Absent</SelectItem>
                    <SelectItem value={TeacherAttendanceStatus.LATE}>Late</SelectItem>
                    <SelectItem value={TeacherAttendanceStatus.SICK_LEAVE}>Sick Leave</SelectItem>
                    <SelectItem value={TeacherAttendanceStatus.PERSONAL_LEAVE}>Personal Leave</SelectItem>
                    <SelectItem value={TeacherAttendanceStatus.PROFESSIONAL_DEVELOPMENT}>Professional Development</SelectItem>
                    <SelectItem value={TeacherAttendanceStatus.SUBSTITUTE_ARRANGED}>Substitute Arranged</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {attendanceForm.status === TeacherAttendanceStatus.SUBSTITUTE_ARRANGED && (
                <div className="space-y-2">
                  <Label>Substitute Teacher</Label>
                  <Select
                    value={attendanceForm.substituteTeacherId?.toString() || ''}
                    onValueChange={(value) => 
                      setAttendanceForm(prev => ({ ...prev, substituteTeacherId: parseInt(value) }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select substitute teacher" />
                    </SelectTrigger>
                    <SelectContent>
                      {teachers?.data
                        .filter(t => t.id !== selectedTeacherId)
                        .map((teacher) => (
                          <SelectItem key={teacher.id} value={teacher.id.toString()}>
                            {teacher.firstName} {teacher.lastName}
                          </SelectItem>
                        ))}
                    </SelectContent>
                  </Select>
                </div>
              )}

              <div className="space-y-2">
                <Label>Remarks</Label>
                <Textarea
                  value={attendanceForm.remarks}
                  onChange={(e) => setAttendanceForm(prev => ({ ...prev, remarks: e.target.value }))}
                  placeholder="Additional remarks..."
                  className="min-h-[60px]"
                />
              </div>

              {(attendanceForm.status === TeacherAttendanceStatus.ABSENT || 
                attendanceForm.status === TeacherAttendanceStatus.SICK_LEAVE ||
                attendanceForm.status === TeacherAttendanceStatus.PERSONAL_LEAVE) && (
                <div className="space-y-2">
                  <Label>Excuse/Reason</Label>
                  <Textarea
                    value={attendanceForm.excuse}
                    onChange={(e) => setAttendanceForm(prev => ({ ...prev, excuse: e.target.value }))}
                    placeholder="Reason for absence..."
                    className="min-h-[60px]"
                  />
                </div>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setMarkAttendanceDialogOpen(false)}>
                Cancel
              </Button>
              <Button 
                onClick={handleMarkAttendance}
                disabled={!selectedTeacherId || createAttendanceMutation.isPending}
              >
                {createAttendanceMutation.isPending ? 'Saving...' : 'Mark Attendance'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Teacher Statistics Dialog */}
        <Dialog open={statisticsDialogOpen} onOpenChange={setStatisticsDialogOpen}>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Teacher Attendance Statistics</DialogTitle>
              <DialogDescription>
                Detailed attendance statistics for the selected teacher
              </DialogDescription>
            </DialogHeader>
            {teacherStats && (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <h4 className="font-medium">Overall Statistics</h4>
                    <div className="space-y-1 text-sm">
                      <div className="flex justify-between">
                        <span>Total Days:</span>
                        <span className="font-medium">{teacherStats.totalDays}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Present Days:</span>
                        <span className="font-medium text-green-600">{teacherStats.presentDays}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Absent Days:</span>
                        <span className="font-medium text-red-600">{teacherStats.absentDays}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Attendance Rate:</span>
                        <span className="font-bold text-blue-600">{teacherStats.attendanceRate.toFixed(1)}%</span>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <h4 className="font-medium">Leave Breakdown</h4>
                    <div className="space-y-1 text-sm">
                      <div className="flex justify-between">
                        <span>Late Days:</span>
                        <span className="font-medium">{teacherStats.lateDays}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Sick Leave:</span>
                        <span className="font-medium">{teacherStats.sickLeaveDays}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Personal Leave:</span>
                        <span className="font-medium">{teacherStats.personalLeaveDays}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <h4 className="font-medium">Monthly Breakdown</h4>
                  <div className="space-y-2">
                    {teacherStats.monthlyBreakdown.map((month) => (
                      <div key={month.month} className="flex items-center justify-between p-2 bg-gray-50 rounded">
                        <span className="font-medium">{month.month}</span>
                        <div className="flex items-center gap-4">
                          <span className="text-sm text-green-600">Present: {month.present}</span>
                          <span className="text-sm text-red-600">Absent: {month.absent}</span>
                          <span className="text-sm font-medium">{month.rate.toFixed(1)}%</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
            <DialogFooter>
              <Button variant="outline" onClick={() => setStatisticsDialogOpen(false)}>
                Close
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </>
  );
};

export default TeacherAttendanceManagement;
