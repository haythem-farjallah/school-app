import * as React from "react";
import { type Table } from "@tanstack/react-table";
import {
  Trash2,
  Download,
  Edit3,
  UserCheck,
  Mail,
  FileText,
  X,
  Upload
} from "lucide-react";
import {
  DataTableActionBar,
  DataTableActionBarAction,
  DataTableActionBarSelection
} from "./data-table-action-bar";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { BulkImportDialog, type ImportResult, type UserType as ImportUserType } from "./bulk-import-dialog";

export type UserType = 'students' | 'teachers' | 'parents' | 'staff';

export interface UserBulkActionBarProps<TData> {
  table: Table<TData>;
  userType: UserType;
  onBulkDelete?: (ids: number[]) => void;
  onBulkStatusUpdate?: (ids: number[], status: string) => void;
  onBulkExport?: (ids: number[], format: 'csv' | 'xlsx') => void;
  onBulkEmail?: (ids: number[]) => void;
  onBulkImport?: (file: File) => Promise<ImportResult[]>;
  // Additional action props based on user type
  onBulkAssignCourses?: (teacherIds: number[], courseIds: number[]) => void; // for teachers
  onBulkEnrollClasses?: (studentIds: number[], classIds: number[]) => void; // for students
}

export function UserBulkActionBar<TData extends { id: number }>({
  table,
  userType,
  onBulkDelete,
  onBulkStatusUpdate,
  onBulkExport,
  onBulkEmail,
  onBulkImport,
  onBulkAssignCourses,
  onBulkEnrollClasses,
}: UserBulkActionBarProps<TData>) {
  const [showDeleteDialog, setShowDeleteDialog] = React.useState(false);
  const [showStatusDialog, setShowStatusDialog] = React.useState(false);
  const [selectedStatus, setSelectedStatus] = React.useState<string>("");
  const [showExportDialog, setShowExportDialog] = React.useState(false);
  const [exportFormat, setExportFormat] = React.useState<'csv' | 'xlsx'>('csv');
  const [showImportDialog, setShowImportDialog] = React.useState(false);

  const selectedRows = table.getFilteredSelectedRowModel().rows;
  const selectedIds = selectedRows.map(row => row.original.id);
  const selectedCount = selectedIds.length;

  if (selectedCount === 0) return null;

  const handleBulkDelete = () => {
    if (onBulkDelete) {
      onBulkDelete(selectedIds);
      setShowDeleteDialog(false);
      table.toggleAllRowsSelected(false);
    }
  };

  const handleStatusUpdate = () => {
    if (onBulkStatusUpdate && selectedStatus) {
      onBulkStatusUpdate(selectedIds, selectedStatus);
      setShowStatusDialog(false);
      setSelectedStatus("");
      table.toggleAllRowsSelected(false);
    }
  };

  const handleExport = () => {
    if (onBulkExport) {
      onBulkExport(selectedIds, exportFormat);
      setShowExportDialog(false);
      table.toggleAllRowsSelected(false);
    }
  };

  const handleEmail = () => {
    if (onBulkEmail) {
      onBulkEmail(selectedIds);
      table.toggleAllRowsSelected(false);
    }
  };

  const getUserTypeLabel = () => {
    switch (userType) {
      case 'students': return 'student';
      case 'teachers': return 'teacher';
      case 'parents': return 'parent';
      case 'staff': return 'staff member';
      default: return 'user';
    }
  };

  const getStatusOptions = () => {
    switch (userType) {
      case 'students':
        return [
          { value: 'ACTIVE', label: 'Active' },
          { value: 'INACTIVE', label: 'Inactive' },
          { value: 'GRADUATED', label: 'Graduated' },
          { value: 'TRANSFERRED', label: 'Transferred' }
        ];
      case 'teachers':
        return [
          { value: 'ACTIVE', label: 'Active' },
          { value: 'INACTIVE', label: 'Inactive' },
          { value: 'ON_LEAVE', label: 'On Leave' },
          { value: 'RETIRED', label: 'Retired' }
        ];
      case 'staff':
        return [
          { value: 'ACTIVE', label: 'Active' },
          { value: 'INACTIVE', label: 'Inactive' },
          { value: 'ON_LEAVE', label: 'On Leave' }
        ];
      default:
        return [
          { value: 'ACTIVE', label: 'Active' },
          { value: 'INACTIVE', label: 'Inactive' }
        ];
    }
  };

  return (
    <>
      <DataTableActionBar table={table}>
        <DataTableActionBarSelection table={table} />

        {/* Clear Selection */}
        <DataTableActionBarAction
          tooltip="Clear selection"
          onClick={() => table.toggleAllRowsSelected(false)}
        >
          <X />
        </DataTableActionBarAction>

        {/* Bulk Delete */}
        {onBulkDelete && (
          <DataTableActionBarAction
            tooltip={`Delete ${selectedCount} ${getUserTypeLabel()}${selectedCount !== 1 ? 's' : ''}`}
            onClick={() => setShowDeleteDialog(true)}
            variant="destructive"
          >
            <Trash2 />
            Delete ({selectedCount})
          </DataTableActionBarAction>
        )}

        {/* Bulk Status Update */}
        {onBulkStatusUpdate && (
          <DataTableActionBarAction
            tooltip={`Update status for ${selectedCount} ${getUserTypeLabel()}${selectedCount !== 1 ? 's' : ''}`}
            onClick={() => setShowStatusDialog(true)}
          >
            <Edit3 />
            Update Status
          </DataTableActionBarAction>
        )}

        {/* Bulk Export */}
        {onBulkExport && (
          <DataTableActionBarAction
            tooltip={`Export ${selectedCount} ${getUserTypeLabel()}${selectedCount !== 1 ? 's' : ''}`}
            onClick={() => setShowExportDialog(true)}
          >
            <Download />
            Export
          </DataTableActionBarAction>
        )}

        {/* Send Email */}
        {onBulkEmail && (
          <DataTableActionBarAction
            tooltip={`Send email to ${selectedCount} ${getUserTypeLabel()}${selectedCount !== 1 ? 's' : ''}`}
            onClick={handleEmail}
          >
            <Mail />
            Send Email
          </DataTableActionBarAction>
        )}

        {/* Bulk Import */}
        {onBulkImport && (
          <DataTableActionBarAction
            tooltip={`Import ${getUserTypeLabel()}s from file`}
            onClick={() => setShowImportDialog(true)}
          >
            <Upload />
            Import File
          </DataTableActionBarAction>
        )}

        {/* Special Actions by User Type */}
        {userType === 'teachers' && onBulkAssignCourses && (
          <DataTableActionBarAction
            tooltip={`Assign courses to ${selectedCount} teacher${selectedCount !== 1 ? 's' : ''}`}
            onClick={() => {
              // This would open a course selection dialog
              console.log("Assign courses to teachers:", selectedIds);
            }}
          >
            <FileText />
            Assign Courses
          </DataTableActionBarAction>
        )}

        {userType === 'students' && onBulkEnrollClasses && (
          <DataTableActionBarAction
            tooltip={`Enroll ${selectedCount} student${selectedCount !== 1 ? 's' : ''} in classes`}
            onClick={() => {
              // This would open a class selection dialog
              console.log("Enroll students in classes:", selectedIds);
            }}
          >
            <UserCheck />
            Enroll in Classes
          </DataTableActionBarAction>
        )}
      </DataTableActionBar>

      {/* Delete Confirmation Dialog */}
      <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Confirm Bulk Delete</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete {selectedCount} {getUserTypeLabel()}{selectedCount !== 1 ? 's' : ''}?
              This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDeleteDialog(false)}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleBulkDelete}>
              Delete {selectedCount} {getUserTypeLabel()}{selectedCount !== 1 ? 's' : ''}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Status Update Dialog */}
      <Dialog open={showStatusDialog} onOpenChange={setShowStatusDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Update Status</DialogTitle>
            <DialogDescription>
              Select a new status for {selectedCount} {getUserTypeLabel()}{selectedCount !== 1 ? 's' : ''}
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <Select value={selectedStatus} onValueChange={setSelectedStatus}>
              <SelectTrigger>
                <SelectValue placeholder="Select new status" />
              </SelectTrigger>
              <SelectContent>
                {getStatusOptions().map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowStatusDialog(false)}>
              Cancel
            </Button>
            <Button onClick={handleStatusUpdate} disabled={!selectedStatus}>
              Update Status
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Export Dialog */}
      <Dialog open={showExportDialog} onOpenChange={setShowExportDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Export {getUserTypeLabel().charAt(0).toUpperCase() + getUserTypeLabel().slice(1)}s</DialogTitle>
            <DialogDescription>
              Export {selectedCount} {getUserTypeLabel()}{selectedCount !== 1 ? 's' : ''} in your preferred format
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <Select value={exportFormat} onValueChange={(value: 'csv' | 'xlsx') => setExportFormat(value)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="csv">CSV (.csv)</SelectItem>
                <SelectItem value="xlsx">Excel (.xlsx)</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowExportDialog(false)}>
              Cancel
            </Button>
            <Button onClick={handleExport}>
              Export as {exportFormat.toUpperCase()}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Import Dialog */}
      {onBulkImport && (
        <BulkImportDialog
          open={showImportDialog}
          onOpenChange={setShowImportDialog}
          userType={userType as ImportUserType}
          onImport={onBulkImport}
        />
      )}
    </>
  );
}
