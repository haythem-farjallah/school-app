import { type ColumnDef } from "@tanstack/react-table";

import { Badge } from "@/components/ui/badge";
import { DataTableColumnHeader } from "@/components/data-table/data-table-column-header";
import { Checkbox } from "@/components/ui/checkbox";
import type { Course } from "@/types/course";
import { CourseActions } from "./course-actions";

export function getCoursesColumns(actions?: {
  onView?: (course: Course) => void;
  onEdit?: (course: Course) => void;
  onDelete?: (course: Course) => void;
  onSuccess?: () => void;
}): ColumnDef<Course>[] {
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
      size: 50,
    },
    {
      accessorKey: "id",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="ID" />
      ),
      cell: ({ row }) => (
        <div className="font-mono text-sm font-semibold text-blue-700 bg-blue-50/50 px-2 py-1 rounded-md border border-blue-200/60">
          #{row.getValue("id")}
        </div>
      ),
      size: 80,
      enableColumnFilter: true,
      meta: {
        variant: "number",
        label: "Course ID",
      },
    },
    {
      accessorKey: "name",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Course Name" />
      ),
      cell: ({ row }) => {
        const name = row.getValue("name") as string;
        return (
          <div className="flex items-center space-x-3">
            <div className="font-semibold text-slate-900 group-hover:text-blue-900 transition-colors duration-200">{name}</div>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "text",
        label: "Course Name",
        placeholder: "Search courses...",
      },
    },
    {
      accessorKey: "color",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Color" />
      ),
      cell: ({ row }) => {
        const color = row.getValue("color") as string;
        return (
          <div className="flex items-center space-x-3">
            <div
              className="h-6 w-6 rounded-full border-2 border-white shadow-lg ring-2 ring-slate-200/80 transition-shadow duration-200 hover:ring-slate-300/90"
              style={{ backgroundColor: color }}
            />
            <span className="text-sm text-slate-700 font-mono bg-slate-100/80 px-2 py-1 rounded border">{color}</span>
          </div>
        );
      },
      size: 150,
    },
    {
      accessorKey: "coefficient",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Coefficient" />
      ),
      cell: ({ row }) => {
        const coefficient = row.getValue("coefficient") as number;
        return (
          <Badge 
            variant="secondary" 
            className="bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-800 hover:from-blue-200 hover:to-indigo-200 border border-blue-200/60 font-semibold transition-colors duration-200"
          >
            {coefficient.toFixed(1)}
          </Badge>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "range",
        label: "Coefficient",
      },
      size: 100,
    },
    {
      accessorKey: "teacherId",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Teacher" />
      ),
      cell: ({ row }) => (
        <div className="flex items-center space-x-2">
          <div className="h-8 w-8 rounded-full bg-gradient-to-r from-purple-100 to-pink-100 border border-purple-200/60 flex items-center justify-center">
            <span className="text-xs font-semibold text-purple-700">T</span>
          </div>
          <span className="text-sm text-slate-700 font-medium">
            Teacher #{row.getValue("teacherId")}
          </span>
        </div>
      ),
      enableColumnFilter: true,
      meta: {
        variant: "number",
        label: "Teacher ID",
      },
      size: 140,
    },
    {
      id: "actions",
      header: () => <div className="text-right font-semibold text-slate-700">Actions</div>,
      cell: ({ row }) => (
        <div className="flex justify-end">
          <CourseActions 
            course={row.original} 
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