import { useState, useMemo } from 'react';
import { Button } from '../../../components/ui/button';
import { Input } from '../../../components/ui/input';
import { DataTable } from '../../../components/data-table/data-table';
import { DataTableToolbar } from '../../../components/data-table/data-table-toolbar';
import { useDataTable } from '../../../hooks/use-data-table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../../../components/ui/select';
import { 
  Plus, 
  Search, 
  Users, 
  UserCheck, 
  UserX, 
  Clock, 
  AlertCircle,
  Calendar,
  TrendingUp,
  PieChart
} from 'lucide-react';
import { AttendanceDto, AttendanceStatus } from '../../../types/attendance';
import { 
  useAttendanceRecords, 
  useDeleteAttendance,
  useAttendanceStatistics 
} from '../hooks/use-attendance';
import { createAttendanceColumns } from './attendance-columns';
import { AttendanceSheet } from './attendance-sheet';
import toast from 'react-hot-toast';

export function AttendanceTable() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [classFilter, setClassFilter] = useState('all');
  const [dateFilter, setDateFilter] = useState('today');
  const [isSheetOpen, setIsSheetOpen] = useState(false);
  const [selectedAttendance, setSelectedAttendance] = useState<AttendanceDto | undefined>();
  const [sheetMode, setSheetMode] = useState<'create' | 'edit'>('create');

  // Get current date for filtering
  const today = new Date().toISOString().split('T')[0];
  const startDate = dateFilter === 'today' ? today : 
                   dateFilter === 'week' ? new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0] :
                   dateFilter === 'month' ? new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0] : 
                   undefined;

  const {
    data,
    refetch,
    isLoading
  } = useAttendanceRecords({ 
    search,
    status: statusFilter !== 'all' ? statusFilter as AttendanceStatus : undefined,
    classId: classFilter !== 'all' ? parseInt(classFilter) : undefined,
    startDate,
    endDate: today,
  });

  const { data: statistics } = useAttendanceStatistics({
    startDate,
    endDate: today,
  });

  const deleteAttendanceMutation = useDeleteAttendance();

  const handleCreateAttendance = () => {
    setSelectedAttendance(undefined);
    setSheetMode('create');
    setIsSheetOpen(true);
  };

  const handleEditAttendance = (attendance: AttendanceDto) => {
    setSelectedAttendance(attendance);
    setSheetMode('edit');
    setIsSheetOpen(true);
  };

  const handleViewAttendance = (attendance: AttendanceDto) => {
    setSelectedAttendance(attendance);
    setSheetMode('edit');
    setIsSheetOpen(true);
  };

  const handleDeleteAttendance = async (attendanceId: number) => {
    if (window.confirm('Are you sure you want to delete this attendance record?')) {
      try {
        await deleteAttendanceMutation.mutateAsync(attendanceId);
        toast.success('Attendance record deleted successfully');
        refetch();
      } catch (error) {
        console.error('Failed to delete attendance:', error);
        toast.error('Failed to delete attendance record');
      }
    }
  };

  const columns = useMemo(
    () => createAttendanceColumns({
      onEdit: handleEditAttendance,
      onDelete: handleDeleteAttendance,
      onView: handleViewAttendance,
    }),
    [handleEditAttendance, handleDeleteAttendance, handleViewAttendance]
  );

  const attendanceRecords = data?.data || [];
  const totalElements = data?.totalItems || 0;
  const totalPages = data?.totalPages || 0;

  const { table } = useDataTable({
    data: attendanceRecords,
    columns,
    pageCount: totalPages,
    enableAdvancedFilter: false,
  });

  // Calculate attendance distribution
  const attendanceDistribution = useMemo(() => {
    const distribution = { 
      PRESENT: 0, 
      ABSENT: 0, 
      LATE: 0, 
      EXCUSED: 0 
    };
    attendanceRecords.forEach(record => {
      distribution[record.status]++;
    });
    return distribution;
  }, [attendanceRecords]);

  // Calculate attendance rate
  const attendanceRate = useMemo(() => {
    if (attendanceRecords.length === 0) return 0;
    const presentCount = attendanceDistribution.PRESENT + attendanceDistribution.LATE;
    return (presentCount / attendanceRecords.length) * 100;
  }, [attendanceRecords, attendanceDistribution]);

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'PRESENT':
        return <UserCheck className="w-8 h-8 text-green-600" />;
      case 'ABSENT':
        return <UserX className="w-8 h-8 text-red-600" />;
      case 'LATE':
        return <Clock className="w-8 h-8 text-yellow-600" />;
      case 'EXCUSED':
        return <AlertCircle className="w-8 h-8 text-blue-600" />;
      default:
        return <Users className="w-8 h-8 text-gray-600" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PRESENT':
        return 'text-green-600';
      case 'ABSENT':
        return 'text-red-600';
      case 'LATE':
        return 'text-yellow-600';
      case 'EXCUSED':
        return 'text-blue-600';
      default:
        return 'text-gray-600';
    }
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Attendance Management</h2>
          <p className="text-gray-600">
            Track student attendance, monitor patterns, and generate reports
          </p>
        </div>
        <Button onClick={handleCreateAttendance} className="bg-blue-600 hover:bg-blue-700">
          <Plus className="w-4 h-4 mr-2" />
          Mark Attendance
        </Button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <div className="bg-white p-4 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <Users className="w-8 h-8 text-blue-600" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">Total Records</p>
              <p className="text-xl font-bold text-gray-900">{totalElements}</p>
            </div>
          </div>
        </div>
        
        <div className="bg-white p-4 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <TrendingUp className="w-8 h-8 text-green-600" />
            <div className="ml-3">
              <p className="text-sm font-medium text-gray-600">Attendance Rate</p>
              <p className="text-xl font-bold text-gray-900">{attendanceRate.toFixed(1)}%</p>
            </div>
          </div>
        </div>

        {Object.entries(attendanceDistribution).map(([status, count]) => (
          <div key={status} className="bg-white p-4 rounded-lg shadow-sm border">
            <div className="flex items-center">
              {getStatusIcon(status)}
              <div className="ml-3">
                <p className="text-sm font-medium text-gray-600">{status.charAt(0) + status.slice(1).toLowerCase()}</p>
                <p className={`text-xl font-bold ${getStatusColor(status)}`}>{count}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Attendance Distribution Chart */}
      <div className="bg-white p-4 rounded-lg shadow-sm border">
        <h3 className="text-lg font-semibold mb-4 flex items-center">
          <PieChart className="w-5 h-5 mr-2" />
          Attendance Distribution
        </h3>
        <div className="flex items-center space-x-4">
          {Object.entries(attendanceDistribution).map(([status, count]) => (
            <div key={status} className="flex-1 text-center">
              <div className={`text-2xl font-bold ${getStatusColor(status)}`}>
                {status.charAt(0)}
              </div>
              <div className="text-sm text-gray-600">{count} records</div>
              <div className="text-xs text-gray-400">
                {totalElements > 0 ? ((count / totalElements) * 100).toFixed(1) : 0}%
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow-sm border">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="Search students..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>
          
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-full sm:w-48">
              <SelectValue placeholder="Filter by status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Status</SelectItem>
              <SelectItem value="PRESENT">Present</SelectItem>
              <SelectItem value="ABSENT">Absent</SelectItem>
              <SelectItem value="LATE">Late</SelectItem>
              <SelectItem value="EXCUSED">Excused</SelectItem>
            </SelectContent>
          </Select>

          <Select value={classFilter} onValueChange={setClassFilter}>
            <SelectTrigger className="w-full sm:w-48">
              <SelectValue placeholder="Filter by class" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Classes</SelectItem>
              <SelectItem value="1">Class A</SelectItem>
              <SelectItem value="2">Class B</SelectItem>
              <SelectItem value="3">Class C</SelectItem>
            </SelectContent>
          </Select>

          <Select value={dateFilter} onValueChange={setDateFilter}>
            <SelectTrigger className="w-full sm:w-48">
              <SelectValue placeholder="Filter by date" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="today">Today</SelectItem>
              <SelectItem value="week">This Week</SelectItem>
              <SelectItem value="month">This Month</SelectItem>
            </SelectContent>
          </Select>

          {(search || statusFilter !== 'all' || classFilter !== 'all' || dateFilter !== 'today') && (
            <Button
              variant="outline"
              onClick={() => {
                setSearch('');
                setStatusFilter('all');
                setClassFilter('all');
                setDateFilter('today');
              }}
            >
              Clear Filters
            </Button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg shadow-sm border">
        {isLoading ? (
          <div className="flex items-center justify-center p-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            <span className="ml-3 text-gray-600">Loading attendance records...</span>
          </div>
        ) : (
          <DataTable table={table}>
            <DataTableToolbar table={table} />
          </DataTable>
        )}
      </div>

      {/* Attendance Sheet */}
      <AttendanceSheet
        isOpen={isSheetOpen}
        onClose={() => setIsSheetOpen(false)}
        attendance={selectedAttendance}
        mode={sheetMode}
        onSuccess={() => {
          refetch();
          setIsSheetOpen(false);
        }}
      />
    </div>
  );
}
