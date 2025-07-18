import { MoreHorizontal, Edit, Eye, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { EditClassSheet } from "./class-sheet";
import type { Class } from "@/types/class";
import { useNavigate } from "react-router-dom";

interface ClassActionsProps {
  classItem: Class;
  onView?: (classItem: Class) => void;
  onEdit?: (classItem: Class) => void;
  onDelete?: (classItem: Class) => void;
  onSuccess?: () => void;
}

export function ClassActions({ classItem, onView, onDelete, onSuccess }: ClassActionsProps) {
  const navigate = useNavigate();
  
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          className="h-8 w-8 p-0 rounded-full hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-colors duration-200 border border-transparent hover:border-blue-200/60"
        >
          <span className="sr-only">Open menu</span>
          <MoreHorizontal className="h-4 w-4 text-slate-600 hover:text-blue-600 transition-colors duration-200" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48 bg-white/95 backdrop-blur-sm border border-slate-200/80 shadow-xl">
        <DropdownMenuItem 
          onClick={() => {
            if (onView) {
              onView(classItem);
            } else {
              navigate(`/admin/classes/view/${classItem.id}`);
            }
          }}
          className="cursor-pointer hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-blue-50 focus:to-indigo-50"
        >
          <Eye className="mr-3 h-4 w-4 text-blue-600" />
          <span className="font-medium">View Details</span>
        </DropdownMenuItem>
        <EditClassSheet 
          classItem={classItem} 
          onSuccess={onSuccess}
          trigger={
            <DropdownMenuItem 
              onSelect={(e) => {
                e.preventDefault();
                // The sheet trigger will handle opening the sheet
              }}
              className="cursor-pointer hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-green-50 focus:to-emerald-50"
            >
              <Edit className="mr-3 h-4 w-4 text-green-600" />
              <span className="font-medium">Edit Class</span>
            </DropdownMenuItem>
          }
        />
        <DropdownMenuSeparator className="bg-slate-200/60" />
        <DropdownMenuItem 
          onClick={() => onDelete?.(classItem)}
          className="cursor-pointer hover:bg-gradient-to-r hover:from-red-50 hover:to-rose-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-red-50 focus:to-rose-50 text-red-600"
        >
          <Trash2 className="mr-3 h-4 w-4" />
          <span className="font-medium">Delete Class</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
} 