import * as React from "react";
import { MoreHorizontal, Eye, Edit, ArrowRightLeft, UserX } from "lucide-react";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { EditEnrollmentSheet } from "./enrollment-sheet";
import { useDropEnrollment } from "../hooks/use-enrollments";
import type { Enrollment } from "@/types/enrollment";

interface EnrollmentActionsProps {
  enrollment: Enrollment;
  onEdit?: (enrollment: Enrollment) => void;
  onView?: (enrollment: Enrollment) => void;
  onTransfer?: (enrollment: Enrollment) => void;
  onDrop?: (enrollment: Enrollment) => void;
  onSuccess?: () => void;
}

export function EnrollmentActions({
  enrollment,
  onEdit,
  onView,
  onTransfer,
  onDrop,
  onSuccess,
}: EnrollmentActionsProps) {
  const navigate = useNavigate();
  const dropMutation = useDropEnrollment();

  const handleView = () => {
    if (onView) {
      onView(enrollment);
    } else {
      navigate(`/admin/enrollments/view/${enrollment.id}`);
    }
  };

  const handleEdit = () => {
    if (onEdit) {
      onEdit(enrollment);
    }
  };

  const handleTransfer = () => {
    if (onTransfer) {
      onTransfer(enrollment);
    } else {
      // Could open a transfer modal here
      toast.info("Transfer functionality coming soon");
    }
  };

  const handleDrop = async () => {
    if (onDrop) {
      onDrop(enrollment);
      return;
    }

    const reason = prompt("Please provide a reason for dropping this enrollment:");
    if (!reason || reason.trim().length < 3) {
      toast.error("A valid reason is required to drop an enrollment");
      return;
    }

    try {
      await dropMutation.mutateAsync({
        id: enrollment.id,
        data: { reason: reason.trim() },
      });
      toast.success("Student dropped from enrollment successfully");
      onSuccess?.();
    } catch (error) {
      console.error("Failed to drop enrollment:", error);
      toast.error("Failed to drop enrollment. Please try again.");
    }
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="h-8 w-8 p-0">
          <span className="sr-only">Open menu</span>
          <MoreHorizontal className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48">
        <DropdownMenuLabel>Actions</DropdownMenuLabel>
        <DropdownMenuSeparator />
        
        <DropdownMenuItem 
          onClick={handleView}
          className="cursor-pointer hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-blue-50 focus:to-indigo-50"
        >
          <Eye className="mr-3 h-4 w-4 text-blue-600" />
          <span className="font-medium">View Details</span>
        </DropdownMenuItem>
        
        <EditEnrollmentSheet 
          enrollment={enrollment} 
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
              <span className="font-medium">Edit Status</span>
            </DropdownMenuItem>
          }
        />

        <DropdownMenuItem 
          onClick={handleTransfer}
          className="cursor-pointer hover:bg-gradient-to-r hover:from-purple-50 hover:to-violet-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-purple-50 focus:to-violet-50"
        >
          <ArrowRightLeft className="mr-3 h-4 w-4 text-purple-600" />
          <span className="font-medium">Transfer Student</span>
        </DropdownMenuItem>

        <DropdownMenuSeparator />
        
        <DropdownMenuItem 
          onClick={handleDrop}
          disabled={dropMutation.isPending}
          className="cursor-pointer hover:bg-gradient-to-r hover:from-red-50 hover:to-rose-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-red-50 focus:to-rose-50 text-red-600"
        >
          <UserX className="mr-3 h-4 w-4" />
          <span className="font-medium">
            {dropMutation.isPending ? "Dropping..." : "Drop Enrollment"}
          </span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
} 