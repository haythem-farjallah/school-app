import type { ColumnDef } from "@tanstack/react-table";
import { MoreHorizontal, Edit, Trash2, Eye, User2, Mail, MessageCircle, Phone } from "lucide-react";

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
import type { Parent } from "@/types/parent";
import { EditParentSheet } from "./parent-sheet";

export function getParentsColumns(actions?: {
  onView?: (parent: Parent) => void;
  onEdit?: (parent: Parent) => void;
  onDelete?: (parent: Parent) => void;
  onSuccess?: () => void;
}): ColumnDef<Parent>[] {
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
            <div className="h-10 w-10 rounded-full bg-gradient-to-r from-purple-100 to-pink-100 border border-purple-200/60 flex items-center justify-center">
              <User2 className="h-5 w-5 text-purple-600" />
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
      accessorKey: "telephone",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Phone" />
      ),
      cell: ({ row }) => {
        const phone = row.getValue("telephone") as string;
        return phone ? (
          <div className="flex items-center space-x-2">
            <Phone className="h-4 w-4 text-blue-600" />
            <span className="text-sm text-slate-700">{phone}</span>
          </div>
        ) : (
          <span className="text-sm text-gray-400">-</span>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "text",
        label: "Phone",
      },
      size: 140,
    },
    {
      accessorKey: "preferredContactMethod",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Contact Method" />
      ),
      cell: ({ row }) => {
        const method = row.getValue("preferredContactMethod") as string;
        return method ? (
          <div className="flex items-center space-x-2">
            <MessageCircle className="h-4 w-4 text-green-600" />
            <Badge 
              variant="outline" 
              className="bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 hover:from-green-200 hover:to-emerald-200 border border-green-200/60 font-semibold transition-colors duration-200"
            >
              {method}
            </Badge>
          </div>
        ) : (
          <span className="text-sm text-gray-400">-</span>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "text",
        label: "Contact Method",
      },
      size: 150,
    },
    {
      id: "actions",
      header: "Actions",
      cell: ({ row }) => {
        const parent = row.original;

        return (
          <div className="flex items-center space-x-2">
            <EditParentSheet 
              parent={parent} 
              onSuccess={actions?.onSuccess}
              trigger={
                <Button 
                  variant="ghost" 
                  size="sm" 
                  className="hover:bg-purple-50 hover:text-purple-700 transition-colors duration-200"
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
                  onClick={() => actions?.onView?.(parent)}
                  className="hover:bg-purple-50 hover:text-purple-700 transition-colors duration-200 cursor-pointer"
                >
                  <Eye className="mr-2 h-4 w-4" />
                  View Details
                </DropdownMenuItem>
                <DropdownMenuSeparator className="bg-slate-200/60" />
                <DropdownMenuItem
                  onClick={() => actions?.onDelete?.(parent)}
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