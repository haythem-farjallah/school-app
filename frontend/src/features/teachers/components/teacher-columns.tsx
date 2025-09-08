import type { ColumnDef } from "@tanstack/react-table";
import { MoreHorizontal, Edit, Trash2, Eye, User, Mail, Clock, BookOpen } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { DataTableColumnHeader } from "@/components/data-table/data-table-column-header";
import type { Teacher } from "@/types/teacher";
import { EditTeacherSheet } from "./teacher-sheet";

export function getTeachersColumns(actions?: {
  onView?: (teacher: Teacher) => void;
  onEdit?: (teacher: Teacher) => void;
  onDelete?: (teacher: Teacher) => void;
  onSuccess?: () => void;
}): ColumnDef<Teacher>[] {
  return [
    {
      id: "select",
      header: ({ table }) => (
        <Checkbox
          checked={table.getIsAllPageRowsSelected()}
          onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
          aria-label="Select all"
          className="translate-y-[2px]"
        />
      ),
      cell: ({ row }) => (
        <Checkbox
          checked={row.getIsSelected()}
          onCheckedChange={(value) => row.toggleSelected(!!value)}
          aria-label="Select row"
          className="translate-y-[2px]"
        />
      ),
      enableSorting: false,
      enableHiding: false,
      size: 40,
    },
    {
      accessorKey: "firstName",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="First Name" />
      ),
      cell: ({ row }) => {
        const firstName = row.getValue("firstName") as string;
        const teacher = row.original;
        return (
          <div 
            className="flex items-center space-x-3 cursor-pointer hover:bg-blue-50 rounded-lg p-2 -m-2 transition-colors duration-200"
            onClick={() => actions?.onView?.(teacher)}
          >
            <div className="h-10 w-10 rounded-full bg-gradient-to-r from-blue-100 to-indigo-100 border border-blue-200/60 flex items-center justify-center">
              <User className="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <div className="font-semibold text-slate-900 hover:text-blue-700 transition-colors duration-200">{firstName}</div>
            </div>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "text",
        label: "First Name",
      },
      size: 250,
    },
    {
      accessorKey: "lastName",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Last Name" />
      ),
      cell: ({ row }) => {
        const lastName = row.getValue("lastName") as string;
        const teacher = row.original;
        return (
          <div 
            className="font-semibold text-slate-900 hover:text-blue-700 cursor-pointer transition-colors duration-200"
            onClick={() => actions?.onView?.(teacher)}
          >
            {lastName}
          </div>
        );
      },
      enableColumnFilter: true,
      enableSorting: true,
      meta: {
        variant: "text",
        label: "Last Name",
      },
      size: 150,
    },
    {
      accessorKey: "email",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Email" />
      ),
      cell: ({ row }) => {
        const email = row.getValue("email") as string;
        return (
          <div className="flex items-center space-x-2">
            <Mail className="h-4 w-4 text-gray-500" />
            <span className="text-sm text-slate-700 font-medium">{email}</span>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "text",
        label: "Email",
      },
      size: 250,
    },
    {
      accessorKey: "qualifications",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Qualifications" />
      ),
      cell: ({ row }) => {
        const qualifications = row.getValue("qualifications") as string;
        return (
          <Badge 
            variant="outline" 
            className="bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 hover:from-green-200 hover:to-emerald-200 border border-green-200/60 font-semibold transition-colors duration-200"
          >
            {qualifications}
          </Badge>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "select",
        label: "Qualifications",
        options: [
          { label: "Bachelor's Degree", value: "Bachelor's Degree" },
          { label: "Master's Degree", value: "Master's Degree" },
          { label: "PhD", value: "PhD" },
          { label: "Teaching Certificate", value: "Teaching Certificate" },
          { label: "Professional Diploma", value: "Professional Diploma" },
        ],
      },
      size: 200,
    },
    {
      accessorKey: "subjectsTaught",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Subjects" />
      ),
      cell: ({ row }) => {
        const subjects = row.getValue("subjectsTaught") as string;
        return (
          <div className="flex items-center space-x-2">
            <BookOpen className="h-4 w-4 text-purple-600" />
            <Badge 
              variant="secondary" 
              className="bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 hover:from-purple-200 hover:to-pink-200 border border-purple-200/60 font-semibold transition-colors duration-200"
            >
              {subjects}
            </Badge>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "multiSelect",
        label: "Subjects Taught",
        options: [
          { label: "Mathematics", value: "Mathematics" },
          { label: "English", value: "English" },
          { label: "Science", value: "Science" },
          { label: "History", value: "History" },
          { label: "Geography", value: "Geography" },
          { label: "Physics", value: "Physics" },
          { label: "Chemistry", value: "Chemistry" },
          { label: "Biology", value: "Biology" },
          { label: "Art", value: "Art" },
          { label: "Music", value: "Music" },
          { label: "Physical Education", value: "Physical Education" },
          { label: "Computer Science", value: "Computer Science" },
        ],
      },
      size: 180,
    },
    {
      accessorKey: "availableHours",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Available Hours" />
      ),
      cell: ({ row }) => {
        const hours = row.getValue("availableHours") as number;
        return (
          <div className="flex items-center space-x-2">
            <Clock className="h-4 w-4 text-orange-600" />
            <Badge 
              variant="secondary" 
              className="bg-gradient-to-r from-orange-100 to-yellow-100 text-orange-800 hover:from-orange-200 hover:to-yellow-200 border border-orange-200/60 font-semibold transition-colors duration-200"
            >
              {hours}h/week
            </Badge>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "range",
        label: "Available Hours",
      },
      size: 140,
    },
    {
      accessorKey: "schedulePreferences",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Schedule Preference" />
      ),
      cell: ({ row }) => {
        const preference = row.getValue("schedulePreferences") as string;
        return (
          <Badge 
            variant="outline" 
            className="bg-gradient-to-r from-indigo-100 to-blue-100 text-indigo-800 hover:from-indigo-200 hover:to-blue-200 border border-indigo-200/60 font-semibold transition-colors duration-200"
          >
            {preference}
          </Badge>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "text",
        label: "Schedule Preferences",
      },
      size: 150,
    },
    {
      accessorKey: "gender",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Gender" />
      ),
      cell: ({ row }) => {
        const gender = row.getValue("gender") as string | null;
        if (!gender) return <span className="text-slate-400">-</span>;
        
        return (
          <Badge 
            variant="outline" 
            className={`font-semibold ${
              gender === 'Male' 
                ? 'bg-gradient-to-r from-blue-100 to-cyan-100 text-blue-800 border-blue-200/60'
                : gender === 'Female'
                ? 'bg-gradient-to-r from-pink-100 to-rose-100 text-pink-800 border-pink-200/60'
                : 'bg-gradient-to-r from-gray-100 to-slate-100 text-gray-800 border-gray-200/60'
            }`}
          >
            {gender}
          </Badge>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "select",
        label: "Gender",
        options: [
          { label: "Male", value: "Male" },
          { label: "Female", value: "Female" },
          { label: "Other", value: "Other" },
        ],
      },
      size: 120,
    },
    {
      id: "actions",
      header: "Actions",
      cell: ({ row }) => {
        const teacher = row.original;
        
        return (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className="flex h-8 w-8 p-0 data-[state=open]:bg-muted"
              >
                <MoreHorizontal className="h-4 w-4" />
                <span className="sr-only">Open menu</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-[160px]">
              <DropdownMenuItem onClick={() => actions?.onView?.(teacher)}>
                <Eye className="mr-2 h-4 w-4" />
                View
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <EditTeacherSheet 
                  teacher={teacher} 
                  onSuccess={() => actions?.onSuccess?.()} 
                  trigger={
                    <div className="flex items-center w-full cursor-pointer">
                      <Edit className="mr-2 h-4 w-4" />
                      Edit
                    </div>
                  }
                />
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem 
                onClick={() => actions?.onDelete?.(teacher)}
                className="text-red-600 focus:text-red-600"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        );
      },
      enableSorting: false,
      enableHiding: false,
      size: 80,
    },
  ];
} 