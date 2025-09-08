import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { 
  Calendar,
  Search,
  Users,
  CheckCircle2,
  XCircle,
  Clock,
  Save,
  RotateCcw,
  User,
  Copy,
  AlertCircle
} from "lucide-react";
import { format, subDays } from "date-fns";
import toast from "react-hot-toast";

import { AttendanceStatusSelector } from "./attendance-status-badge";
import { 
  useClassAttendance, 
  useBulkMarkAttendance, 
  useCopyPreviousAttendance 
} from "../hooks/use-attendance";
import { useStudents } from "@/features/students/hooks/use-students";
import type { AttendanceStatus } from "@/types/attendance";

interface DailyCalendarViewProps {
  classId: number;
  className: string;
  selectedDate: Date;
  onDateChange: (date: Date) => void;
}

export function DailyCalendarView({
  classId,
  className,
  selectedDate,
  onDateChange,
}: DailyCalendarViewProps) {
  const [searchTerm, setSearchTerm] = React.useState("");
  const [attendanceData, setAttendanceData] = React.useState<Record<number, AttendanceStatus>>({});
  const [hasChanges, setHasChanges] = React.useState(false);

  const dateString = format(selectedDate, 'yyyy-MM-dd');
  const previousDateString = format(subDays(selectedDate, 1), 'yyyy-MM-dd');
  
  // Fetch students in the class
  const { data: studentsResponse } = useStudents({ 
    classId, 
    size: 100 // Get all students in class
  });
  
  // Fetch existing attendance for the date
  const { data: existingAttendance, refetch } = useClassAttendance(classId, dateString);
  
  // Check if previous day has attendance data
  const { data: previousAttendance } = useClassAttendance(classId, previousDateString);
  
  const bulkMarkMutation = useBulkMarkAttendance();
  const copyPreviousMutation = useCopyPreviousAttendance();

  const students = studentsResponse?.data || [];

  // Initialize attendance data from existing records
  React.useEffect(() => {
    if (existingAttendance) {
      const attendanceMap: Record<number, AttendanceStatus> = {};
      existingAttendance.forEach(record => {
        attendanceMap[record.userId] = record.status;
      });
      setAttendanceData(attendanceMap);
      setHasChanges(false);
    } else {
      setAttendanceData({});
      setHasChanges(false);
    }
  }, [existingAttendance]);

  // Filter students based on search
  const filteredStudents = students.filter(student =>
    `${student.firstName} ${student.lastName}`.toLowerCase().includes(searchTerm.toLowerCase()) ||
    student.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Handle status change for individual student
  const handleStatusChange = (userId: number, status: AttendanceStatus) => {
    setAttendanceData(prev => ({
      ...prev,
      [userId]: status
    }));
    setHasChanges(true);
  };

  // Bulk actions
  const handleMarkAllPresent = () => {
    const newData: Record<number, AttendanceStatus> = {};
    filteredStudents.forEach(student => {
      newData[student.id] = AttendanceStatus.PRESENT;
    });
    setAttendanceData(prev => ({ ...prev, ...newData }));
    setHasChanges(true);
    toast.success(`Marked ${filteredStudents.length} students as present`);
  };

  const handleMarkAllAbsent = () => {
    const newData: Record<number, AttendanceStatus> = {};
    filteredStudents.forEach(student => {
      newData[student.id] = AttendanceStatus.ABSENT;
    });
    setAttendanceData(prev => ({ ...prev, ...newData }));
    setHasChanges(true);
    toast.success(`Marked ${filteredStudents.length} students as absent`);
  };

  const handleCopyPrevious = async () => {
    try {
      await copyPreviousMutation.mutateAsync({
        classId,
        fromDate: previousDateString,
        toDate: dateString,
      });
      refetch();
    } catch (error) {
      // Error handled by mutation
    }
  };

  const handleReset = () => {
    if (existingAttendance) {
      const attendanceMap: Record<number, AttendanceStatus> = {};
      existingAttendance.forEach(record => {
        attendanceMap[record.userId] = record.status;
      });
      setAttendanceData(attendanceMap);
    } else {
      setAttendanceData({});
    }
    setHasChanges(false);
    toast.info("Changes reset");
  };

  // Save attendance
  const handleSave = async () => {
    const attendances = Object.entries(attendanceData).map(([userId, status]) => ({
      userId: parseInt(userId),
      status,
    }));

    try {
      await bulkMarkMutation.mutateAsync({
        classId,
        date: dateString,
        attendances,
      });
      setHasChanges(false);
      refetch();
    } catch (error) {
      // Error handled by mutation
    }
  };

  // Calculate statistics
  const stats = React.useMemo(() => {
    const total = filteredStudents.length;
    const marked = Object.keys(attendanceData).length;
    const present = Object.values(attendanceData).filter(s => s === AttendanceStatus.PRESENT).length;
    const absent = Object.values(attendanceData).filter(s => s === AttendanceStatus.ABSENT).length;
    const late = Object.values(attendanceData).filter(s => s === AttendanceStatus.LATE).length;
    const excused = Object.values(attendanceData).filter(s => s === AttendanceStatus.EXCUSED).length;
    
    return { total, marked, present, absent, late, excused };
  }, [filteredStudents, attendanceData]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-blue-200/60 bg-gradient-to-r from-blue-50/80 to-indigo-50/40">
        <CardHeader className="pb-4">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-2xl font-bold text-blue-900 flex items-center">
                <Calendar className="h-6 w-6 mr-2" />
                Daily Attendance - {className}
              </CardTitle>
              <p className="text-blue-700 mt-1">
                {format(selectedDate, 'EEEE, MMMM d, yyyy')}
              </p>
            </div>
            <div className="flex items-center space-x-3">
              <Input
                type="date"
                value={format(selectedDate, 'yyyy-MM-dd')}
                onChange={(e) => onDateChange(new Date(e.target.value))}
                className="w-40"
              />
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Statistics */}
      <div className="grid grid-cols-2 md:grid-cols-6 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-slate-900">{stats.total}</div>
            <div className="text-sm text-slate-600">Total Students</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-green-600">{stats.present}</div>
            <div className="text-sm text-slate-600">Present</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-red-600">{stats.absent}</div>
            <div className="text-sm text-slate-600">Absent</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-yellow-600">{stats.late}</div>
            <div className="text-sm text-slate-600">Late</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-orange-600">{stats.excused}</div>
            <div className="text-sm text-slate-600">Excused</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <div className="text-2xl font-bold text-blue-600">
              {stats.total > 0 ? Math.round((stats.present / stats.total) * 100) : 0}%
            </div>
            <div className="text-sm text-slate-600">Attendance Rate</div>
          </CardContent>
        </Card>
      </div>

      {/* Controls */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col lg:flex-row items-center justify-between space-y-4 lg:space-y-0 lg:space-x-4">
            <div className="flex items-center space-x-2 flex-1">
              <Search className="h-4 w-4 text-slate-400" />
              <Input
                placeholder="Search students..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="max-w-sm"
              />
            </div>
            
            <div className="flex flex-wrap items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handleMarkAllPresent}
                className="text-green-600 border-green-200 hover:bg-green-50"
              >
                <CheckCircle2 className="h-4 w-4 mr-2" />
                Mark All Present
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleMarkAllAbsent}
                className="text-red-600 border-red-200 hover:bg-red-50"
              >
                <XCircle className="h-4 w-4 mr-2" />
                Mark All Absent
              </Button>
              {previousAttendance && previousAttendance.length > 0 && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCopyPrevious}
                  disabled={copyPreviousMutation.isPending}
                  className="text-blue-600 border-blue-200 hover:bg-blue-50"
                >
                  <Copy className="h-4 w-4 mr-2" />
                  Copy Previous Day
                </Button>
              )}
              {hasChanges && (
                <>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={handleReset}
                  >
                    <RotateCcw className="h-4 w-4 mr-2" />
                    Reset
                  </Button>
                  <Button
                    onClick={handleSave}
                    disabled={bulkMarkMutation.isPending}
                    className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
                  >
                    <Save className="h-4 w-4 mr-2" />
                    Save Changes
                  </Button>
                </>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Student List */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <Users className="h-5 w-5 mr-2" />
            Students ({filteredStudents.length})
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="divide-y">
            {filteredStudents.map((student) => (
              <div key={student.id} className="p-4 hover:bg-slate-50 transition-colors">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-4">
                    <Avatar className="h-10 w-10">
                      <AvatarFallback className="bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-700">
                        <User className="h-5 w-5" />
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <div className="font-semibold text-slate-900">
                        {student.firstName} {student.lastName}
                      </div>
                      <div className="text-sm text-slate-500">{student.email}</div>
                      {student.gradeLevel && (
                        <Badge variant="outline" className="text-xs mt-1">
                          {student.gradeLevel}
                        </Badge>
                      )}
                    </div>
                  </div>
                  
                  <AttendanceStatusSelector
                    currentStatus={attendanceData[student.id]}
                    onStatusChange={(status) => handleStatusChange(student.id, status)}
                    size="sm"
                  />
                </div>
              </div>
            ))}
            
            {filteredStudents.length === 0 && (
              <div className="p-8 text-center text-slate-500">
                <Users className="h-12 w-12 mx-auto mb-3 text-slate-300" />
                <p>No students found</p>
                {searchTerm && (
                  <p className="text-sm">Try adjusting your search terms</p>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Unsaved Changes Warning */}
      {hasChanges && (
        <Card className="border-yellow-200 bg-yellow-50">
          <CardContent className="p-4">
            <div className="flex items-center space-x-2 text-yellow-800">
              <AlertCircle className="h-4 w-4" />
              <span className="text-sm font-medium">
                You have unsaved changes. Don't forget to save your attendance data.
              </span>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
