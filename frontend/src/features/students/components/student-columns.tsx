import type { ColumnDef } from "@tanstack/react-table";
import { MoreHorizontal, Edit, Trash2, Eye, User, Mail, GraduationCap, BookOpen } from "lucide-react";

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
import type { Student } from "@/types/student";
import { EditStudentSheet } from "./student-sheet";

export function getStudentsColumns(actions?: {
  onView?: (student: Student) => void;
  onEdit?: (student: Student) => void;
  onDelete?: (student: Student) => void;
  onSuccess?: () => void;
}): ColumnDef<Student>[] {
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
        return (
          <div className="flex items-center space-x-3">
            <div className="h-10 w-10 rounded-full bg-gradient-to-r from-blue-100 to-indigo-100 border border-blue-200/60 flex items-center justify-center">
              <User className="h-5 w-5 text-blue-600" />
            </div>
            <div>
              <div className="font-semibold text-slate-900">{firstName}</div>
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
        return (
          <div className="font-semibold text-slate-900">{lastName}</div>
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
      accessorKey: "gradeLevel",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Grade Level" />
      ),
      cell: ({ row }) => {
        const gradeLevel = row.getValue("gradeLevel") as string;
        return (
          <div className="flex items-center space-x-2">
            <GraduationCap className="h-4 w-4 text-green-600" />
            <Badge 
              variant="outline" 
              className="bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 hover:from-green-200 hover:to-emerald-200 border border-green-200/60 font-semibold transition-colors duration-200"
            >
              {gradeLevel}
            </Badge>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "select",
        label: "Grade Level",
        options: [
          { label: "Grade 1", value: "Grade 1" },
          { label: "Grade 2", value: "Grade 2" },
          { label: "Grade 3", value: "Grade 3" },
          { label: "Grade 4", value: "Grade 4" },
          { label: "Grade 5", value: "Grade 5" },
          { label: "Grade 6", value: "Grade 6" },
          { label: "Grade 7", value: "Grade 7" },
          { label: "Grade 8", value: "Grade 8" },
          { label: "Grade 9", value: "Grade 9" },
          { label: "Grade 10", value: "Grade 10" },
          { label: "Grade 11", value: "Grade 11" },
          { label: "Grade 12", value: "Grade 12" },
        ],
      },
      size: 150,
    },
    {
      accessorKey: "enrollmentYear",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Enrollment Year" />
      ),
      cell: ({ row }) => {
        const year = row.getValue("enrollmentYear") as number;
        return (
          <div className="flex items-center space-x-2">
            <BookOpen className="h-4 w-4 text-purple-600" />
            <Badge 
              variant="secondary" 
              className="bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 hover:from-purple-200 hover:to-pink-200 border border-purple-200/60 font-semibold transition-colors duration-200"
            >
              {year}
            </Badge>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "range",
        label: "Enrollment Year",
      },
      size: 140,
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
      accessorKey: "birthday",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Age" />
      ),
      cell: ({ row }) => {
        const birthday = row.getValue("birthday") as string | null;
        if (!birthday) return <span className="text-slate-400">-</span>;
        
        const age = new Date().getFullYear() - new Date(birthday).getFullYear();
        return (
          <Badge 
            variant="outline" 
            className="bg-gradient-to-r from-indigo-100 to-purple-100 text-indigo-800 border-indigo-200/60 font-semibold"
          >
            {age} years
          </Badge>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "range",
        label: "Age",
        unit: "years",
      },
      size: 100,
    },
    {
      id: "actions",
      header: "Actions",
      cell: ({ row }) => {
        const student = row.original;

        return (
          <div className="flex items-center space-x-2">
            <EditStudentSheet 
              student={student} 
              onSuccess={actions?.onSuccess}
              trigger={
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="hover:bg-blue-50 hover:text-blue-700 transition-colors duration-200"
                >
                  <Edit className="h-4 w-4" />
                </Button>
              }
            />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className="h-8 w-8 p-0 hover:bg-slate-100 transition-colors duration-200"
                >
                  <span className="sr-only">Open menu</span>
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent 
                align="end" 
                className="w-[160px] bg-white/95 backdrop-blur-sm border border-slate-200/60 shadow-xl"
              >
                <DropdownMenuItem
                  onClick={() => actions?.onView?.(student)}
                  className="hover:bg-blue-50 hover:text-blue-700 transition-colors duration-200 cursor-pointer"
                >
                  <Eye className="mr-2 h-4 w-4" />
                  View Details
                </DropdownMenuItem>
                <DropdownMenuSeparator className="bg-slate-200/60" />
                <DropdownMenuItem
                  onClick={() => actions?.onDelete?.(student)}
                  className="hover:bg-red-50 hover:text-red-700 transition-colors duration-200 cursor-pointer text-red-600"
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  Delete
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        );
      },
      enableSorting: false,
      enableHiding: false,
      size: 100,
    },
  ];
} 