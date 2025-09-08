import { ColumnDef } from '@tanstack/react-table';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { AttendanceDto, AttendanceStatus } from '../../../types/attendance';
import { 
  MoreHorizontal, 
  Eye, 
  Edit, 
  Trash2, 
  UserCheck, 
  UserX, 
  Clock, 
  AlertCircle,
  User
} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../../../components/ui/dropdown-menu';
import { AttendanceStatusBadge } from './attendance-status-badge';

interface AttendanceColumnsProps {
  onEdit: (attendance: AttendanceDto) => void;
  onDelete: (attendanceId: number) => void;
  onView: (attendance: AttendanceDto) => void;
}

export const createAttendanceColumns = ({
  onEdit,
  onDelete,
  onView,
}: AttendanceColumnsProps): ColumnDef<AttendanceDto>[] => [
  {
    accessorKey: 'userName',
    header: 'Student',
    cell: ({ row }) => {
      const attendance = row.original;
      return (
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
            <User className="h-4 w-4 text-blue-600" />
          </div>
          <div>
            <div className="font-medium">{attendance.userName}</div>
            <div className="text-sm text-muted-foreground">ID: {attendance.userId}</div>
          </div>
        </div>
      );
    },
  },
  {
    accessorKey: 'className',
    header: 'Class',
    cell: ({ row }) => {
      const attendance = row.original;
      return (
        <div>
          <div className="font-medium">{attendance.className || 'N/A'}</div>
          {attendance.courseName && (
            <div className="text-sm text-muted-foreground">{attendance.courseName}</div>
          )}
        </div>
      );
    },
  },
  {
    accessorKey: 'date',
    header: 'Date',
    cell: ({ row }) => {
      const date = row.original.date;
      return (
        <div className="text-sm">
          {new Date(date).toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
          })}
        </div>
      );
    },
  },
  {
    accessorKey: 'status',
    header: 'Status',
    cell: ({ row }) => {
      const attendance = row.original;
      return <AttendanceStatusBadge status={attendance.status} />;
    },
  },
  {
    accessorKey: 'recordedBy',
    header: 'Recorded By',
    cell: ({ row }) => {
      const attendance = row.original;
      return (
        <div className="text-sm">
          {attendance.recordedBy || 'System'}
        </div>
      );
    },
  },
  {
    accessorKey: 'recordedAt',
    header: 'Recorded At',
    cell: ({ row }) => {
      const recordedAt = row.original.recordedAt;
      if (!recordedAt) return <span className="text-muted-foreground">-</span>;
      
      return (
        <div className="text-sm">
          {new Date(recordedAt).toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </div>
      );
    },
  },
  {
    accessorKey: 'remarks',
    header: 'Remarks',
    cell: ({ row }) => {
      const remarks = row.original.remarks;
      return (
        <div className="max-w-32 truncate text-sm" title={remarks || ''}>
          {remarks || '-'}
        </div>
      );
    },
  },
  {
    id: 'actions',
    header: 'Actions',
    cell: ({ row }) => {
      const attendance = row.original;

      return (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="h-8 w-8 p-0">
              <span className="sr-only">Open menu</span>
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => onView(attendance)}>
              <Eye className="mr-2 h-4 w-4" />
              View Details
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onEdit(attendance)}>
              <Edit className="mr-2 h-4 w-4" />
              Edit
            </DropdownMenuItem>
            <DropdownMenuItem 
              onClick={() => onDelete(attendance.id!)} 
              className="text-red-600"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      );
    },
  },
];

// Helper component for status display with icons
export const StatusDisplay = ({ status }: { status: AttendanceStatus }) => {
  const getStatusIcon = () => {
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
        return <User className="h-4 w-4 text-gray-600" />;
    }
  };

  const getStatusColor = () => {
    switch (status) {
      case AttendanceStatus.PRESENT:
        return 'bg-green-100 text-green-800';
      case AttendanceStatus.ABSENT:
        return 'bg-red-100 text-red-800';
      case AttendanceStatus.LATE:
        return 'bg-yellow-100 text-yellow-800';
      case AttendanceStatus.EXCUSED:
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <Badge className={`${getStatusColor()}`}>
      <div className="flex items-center gap-1">
        {getStatusIcon()}
        {status.charAt(0) + status.slice(1).toLowerCase()}
      </div>
    </Badge>
  );
};
