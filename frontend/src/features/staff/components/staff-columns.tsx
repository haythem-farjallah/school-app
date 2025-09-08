import type { ColumnDef } from "@tanstack/react-table";
import { MoreHorizontal, Edit, Trash2, Eye, User, Mail, Building, Users2 } from "lucide-react";

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
import type { Staff } from "@/types/staff";
import { EditStaffSheet } from "./staff-sheet";

export function getStaffColumns(actions?: {
  onView?: (staff: Staff) => void;
  onEdit?: (staff: Staff) => void;
  onDelete?: (staff: Staff) => void;
  onSuccess?: () => void;
}): ColumnDef<Staff>[] {
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
            <div className="h-10 w-10 rounded-full bg-gradient-to-r from-emerald-100 to-teal-100 border border-emerald-200/60 flex items-center justify-center">
              <User className="h-5 w-5 text-emerald-600" />
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
      accessorKey: "staffType",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Staff Type" />
      ),
      cell: ({ row }) => {
        const staffType = row.getValue("staffType") as string;
        return (
          <div className="flex items-center space-x-2">
            <Users2 className="h-4 w-4 text-blue-600" />
            <Badge 
              variant="outline" 
              className="bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-800 hover:from-blue-200 hover:to-indigo-200 border border-blue-200/60 font-semibold transition-colors duration-200"
            >
              {staffType}
            </Badge>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "select",
        label: "Staff Type",
        options: [
          { label: "Administrative", value: "ADMINISTRATIVE" },
          { label: "Maintenance", value: "MAINTENANCE" },
          { label: "Security", value: "SECURITY" },
          { label: "Librarian", value: "LIBRARIAN" },
          { label: "Counselor", value: "COUNSELOR" },
        ],
      },
      size: 180,
    },
    {
      accessorKey: "department",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Department" />
      ),
      cell: ({ row }) => {
        const department = row.getValue("department") as string;
        return (
          <div className="flex items-center space-x-2">
            <Building className="h-4 w-4 text-purple-600" />
            <Badge 
              variant="secondary" 
              className="bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 hover:from-purple-200 hover:to-pink-200 border border-purple-200/60 font-semibold transition-colors duration-200"
            >
              {department}
            </Badge>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "select",
        label: "Department",
        options: [
          { label: "Human Resources", value: "Human Resources" },
          { label: "Finance", value: "Finance" },
          { label: "Operations", value: "Operations" },
          { label: "Facilities", value: "Facilities" },
          { label: "Student Services", value: "Student Services" },
          { label: "Academic Affairs", value: "Academic Affairs" },
          { label: "Technology", value: "Technology" },
          { label: "Health Services", value: "Health Services" },
        ],
      },
      size: 180,
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
        const staff = row.original;
        
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
              <DropdownMenuItem onClick={() => actions?.onView?.(staff)}>
                <Eye className="mr-2 h-4 w-4" />
                View
              </DropdownMenuItem>
              <DropdownMenuItem asChild>
                <EditStaffSheet 
                  staff={staff} 
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
                onClick={() => actions?.onDelete?.(staff)}
                className="text-red-600 focus:text-red-600"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        );
      },
      enableHiding: false,
      size: 80,
    },
  ];
} 