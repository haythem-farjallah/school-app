import { type ColumnDef } from "@tanstack/react-table";

import { Badge } from "@/components/ui/badge";
import { DataTableColumnHeader } from "@/components/data-table/data-table-column-header";
import { Checkbox } from "@/components/ui/checkbox";
import type { Class } from "@/types/class";
import { ClassActions } from "./class-actions";
import { Link } from "react-router-dom";

const getYearOfStudyColor = (yearOfStudy: number) => {
  if (yearOfStudy <= 3) return 'bg-blue-100 text-blue-800';
  if (yearOfStudy <= 6) return 'bg-green-100 text-green-800';
  if (yearOfStudy <= 9) return 'bg-yellow-100 text-yellow-800';
  return 'bg-purple-100 text-purple-800';
};

const getYearOfStudyLabel = (yearOfStudy: number) => {
  if (yearOfStudy <= 3) return `Grade ${yearOfStudy} (Primary)`;
  if (yearOfStudy <= 6) return `Grade ${yearOfStudy} (Elementary)`;
  if (yearOfStudy <= 9) return `Grade ${yearOfStudy} (Middle)`;
  return `Grade ${yearOfStudy} (High)`;
};

const getCapacityStatus = (currentStudents: number, maxStudents: number) => {
  const percentage = (currentStudents / maxStudents) * 100;
  if (percentage >= 90) return 'bg-red-100 text-red-800';
  if (percentage >= 70) return 'bg-yellow-100 text-yellow-800';
  return 'bg-green-100 text-green-800';
};

export function getClassesColumns(actions?: {
  onView?: (classItem: Class) => void;
  onEdit?: (classItem: Class) => void;
  onDelete?: (classItem: Class) => void;
  onSuccess?: () => void;
}): ColumnDef<Class>[] {
  return [
    {
      id: "select",
      header: ({ table }) => (
        <Checkbox
          checked={
            table.getIsAllPageRowsSelected() ||
            (table.getIsSomePageRowsSelected() && "indeterminate")
          }
          onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
          aria-label="Select all"
          className="translate-y-[2px] border-blue-300/80 data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-blue-600 data-[state=checked]:to-indigo-600 transition-all duration-200"
        />
      ),
      cell: ({ row }) => (
        <Checkbox
          checked={row.getIsSelected()}
          onCheckedChange={(value) => row.toggleSelected(!!value)}
          aria-label="Select row"
          className="translate-y-[2px] border-blue-300/80 data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-blue-600 data-[state=checked]:to-indigo-600 transition-all duration-200"
        />
      ),
      enableSorting: false,
      enableHiding: false,
      size: 40,
    },
    {
      accessorKey: "name",
      id: "name",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Class Name" />
      ),
      cell: ({ row }) => {
        const classItem = row.original;
        return (
          <div className="flex items-center space-x-2">
            <Link 
              to={`/admin/classes/view/${classItem.id}`}
              className="font-medium text-blue-600 hover:text-blue-800 hover:underline transition-colors"
            >
              {classItem.name}
            </Link>
          </div>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "text",
        label: "Class Name",
        placeholder: "Search classes...",
      },
      size: 200,
    },
    {
      accessorKey: "yearOfStudy",
      id: "yearOfStudy",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Year of Study" />
      ),
      cell: ({ row }) => {
        const yearOfStudy = row.getValue("yearOfStudy") as number;
        return (
          <Badge 
            className={`${getYearOfStudyColor(yearOfStudy)} font-medium px-2 py-1 text-xs rounded-full border-0`}
          >
            {getYearOfStudyLabel(yearOfStudy)}
          </Badge>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "select",
        label: "Year of Study",
        options: [
          { label: "Grade 1 (Primary)", value: "1" },
          { label: "Grade 2 (Primary)", value: "2" },
          { label: "Grade 3 (Primary)", value: "3" },
          { label: "Grade 4 (Elementary)", value: "4" },
          { label: "Grade 5 (Elementary)", value: "5" },
          { label: "Grade 6 (Elementary)", value: "6" },
          { label: "Grade 7 (Middle)", value: "7" },
          { label: "Grade 8 (Middle)", value: "8" },
          { label: "Grade 9 (Middle)", value: "9" },
          { label: "Grade 10 (High)", value: "10" },
          { label: "Grade 11 (High)", value: "11" },
          { label: "Grade 12 (High)", value: "12" },
        ],
      },
      size: 180,
    },
    {
      accessorKey: "maxStudents",
      id: "maxStudents",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Capacity" />
      ),
      cell: ({ row }) => {
        const classItem = row.original;
        const currentStudents = classItem.studentIds?.length || 0;
        const maxStudents = classItem.maxStudents;
        
        return (
          <div className="flex items-center space-x-2">
            <Badge 
              className={`${getCapacityStatus(currentStudents, maxStudents)} font-medium px-2 py-1 text-xs rounded-full border-0`}
            >
              {currentStudents}/{maxStudents}
            </Badge>
            <span className="text-gray-500 text-sm">students</span>
          </div>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "number",
        label: "Max Students",
        placeholder: "Max capacity...",
      },
      size: 150,
    },
    {
      id: "studentCount",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Students" />
      ),
      cell: ({ row }) => {
        const classItem = row.original;
        const studentCount = classItem.studentIds?.length || 0;
        return (
          <div className="flex items-center">
            <span className="font-medium text-gray-900">{studentCount}</span>
            <span className="text-gray-500 text-sm ml-1">enrolled</span>
          </div>
        );
      },
      enableSorting: true,
      size: 100,
    },
    {
      id: "actions",
      header: "Actions",
      cell: ({ row }) => (
        <div className="flex justify-end">
          <ClassActions 
            classItem={row.original} 
            onView={actions?.onView}
            onEdit={actions?.onEdit}
            onDelete={actions?.onDelete}
            onSuccess={actions?.onSuccess}
          />
        </div>
      ),
      enableSorting: false,
      enableHiding: false,
      size: 80,
    },
  ];
} 