import * as React from "react";
import { type ColumnDef } from "@tanstack/react-table";
import { Badge } from "@/components/ui/badge";
import { DataTableColumnHeader } from "@/components/data-table/data-table-column-header";
import { format } from "date-fns";
import { 
  type Enrollment, 
  EnrollmentStatus, 
  getEnrollmentStatusColor, 
  getEnrollmentStatusLabel 
} from "@/types/enrollment";
import { EnrollmentActions } from "./enrollment-actions";

interface EnrollmentColumnsProps {
  onEdit?: (enrollment: Enrollment) => void;
  onView?: (enrollment: Enrollment) => void;
  onTransfer?: (enrollment: Enrollment) => void;
  onDrop?: (enrollment: Enrollment) => void;
  onSuccess?: () => void;
}

export function getEnrollmentColumns({
  onEdit,
  onView,
  onTransfer,
  onDrop,
  onSuccess,
}: EnrollmentColumnsProps): ColumnDef<Enrollment>[] {
  return [
    {
      accessorKey: "studentName",
      id: "studentName",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Student" />
      ),
      cell: ({ row }) => {
        const enrollment = row.original;
        return (
          <div className="flex flex-col">
            <span className="font-medium text-gray-900">{enrollment.studentName}</span>
            <span className="text-sm text-gray-500">{enrollment.studentEmail}</span>
          </div>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "text",
        label: "Student",
        placeholder: "Search students...",
      },
    },
    {
      accessorKey: "className",
      id: "className",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Class" />
      ),
      cell: ({ row }) => {
        const className = row.getValue("className") as string;
        return (
          <span className="font-medium text-gray-900">{className}</span>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "text",
        label: "Class",
        placeholder: "Search classes...",
      },
    },
    {
      accessorKey: "status",
      id: "status",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Status" />
      ),
      cell: ({ row }) => {
        const status = row.getValue("status") as EnrollmentStatus;
        return (
          <Badge 
            className={`${getEnrollmentStatusColor(status)} font-medium px-2 py-1 text-xs rounded-full border`}
          >
            {getEnrollmentStatusLabel(status)}
          </Badge>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "select",
        label: "Status",
        options: [
          { label: "Pending", value: EnrollmentStatus.PENDING },
          { label: "Active", value: EnrollmentStatus.ACTIVE },
          { label: "Completed", value: EnrollmentStatus.COMPLETED },
          { label: "Dropped", value: EnrollmentStatus.DROPPED },
          { label: "Suspended", value: EnrollmentStatus.SUSPENDED },
        ],
      },
    },
    {
      accessorKey: "enrolledAt",
      id: "enrolledAt",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Enrolled Date" />
      ),
      cell: ({ row }) => {
        const enrolledAt = row.getValue("enrolledAt") as string;
        if (!enrolledAt) return <span className="text-gray-400">-</span>;
        
        try {
          const date = new Date(enrolledAt);
          return (
            <span className="text-gray-700">
              {format(date, "MMM dd, yyyy")}
            </span>
          );
        } catch {
          return <span className="text-gray-400">Invalid date</span>;
        }
      },
      enableSorting: true,
    },
    {
      id: "actions",
      cell: ({ row }) => {
        const enrollment = row.original;
        return (
          <EnrollmentActions
            enrollment={enrollment}
            onEdit={onEdit}
            onView={onView}
            onTransfer={onTransfer}
            onDrop={onDrop}
            onSuccess={onSuccess}
          />
        );
      },
    },
  ];
} 