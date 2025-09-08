import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Users2, UserCheck, TrendingUp, Plus, Upload } from "lucide-react";
import { BulkImportDialog } from "@/components/data-table/bulk-import-dialog";
import { type Parser, useQueryState, useQueryStates, parseAsInteger, parseAsString } from "nuqs";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import { useStaff, useDeleteStaff } from "../hooks/use-staff";
import { useBulkDeleteStaff, useBulkUpdateStaffStatus, useBulkExportStaff, useBulkImportStaff } from "../hooks/use-staff-bulk";
import { getStaffColumns } from "./staff-columns";
import type { Staff } from "@/types/staff";
import { UserBulkActionBar } from "@/components/data-table/user-bulk-action-bar";

export function StaffTable() {
  const navigate = useNavigate();
  const [deleteDialogOpen, setDeleteDialogOpen] = React.useState(false);
  const [staffToDelete, setStaffToDelete] = React.useState<Staff | null>(null);
  const [showImportDialog, setShowImportDialog] = React.useState(false);

  const deleteMutation = useDeleteStaff();

  // Bulk operation hooks
  const bulkDeleteMutation = useBulkDeleteStaff();
  const bulkStatusMutation = useBulkUpdateStaffStatus();
  const bulkExportMutation = useBulkExportStaff();
  const bulkImportMutation = useBulkImportStaff();

  const handleView = React.useCallback((staff: Staff) => {
    navigate(`/admin/staff/view/${staff.id}`);
  }, [navigate]);

  const handleEdit = React.useCallback((staff: Staff) => {
    console.log("✏️ StaffTable - Editing staff:", staff);
    // Edit logic is now handled by the EditStaffSheet component
  }, []);

  const handleDelete = React.useCallback((staff: Staff) => {
    console.log("🗑️ StaffTable - Deleting staff:", staff);
    setStaffToDelete(staff);
    setDeleteDialogOpen(true);
  }, []);

  const confirmDelete = React.useCallback(async () => {
    if (!staffToDelete) return;
    
    console.log("✅ StaffTable - Confirming delete for:", staffToDelete);
    
    try {
      await deleteMutation.mutateAsync(staffToDelete.id);
      toast.success(`Staff "${staffToDelete.firstName} ${staffToDelete.lastName}" deleted successfully`);
      refetch();
    } catch (error) {
      console.error("❌ StaffTable - Delete failed:", error);
      toast.error("Failed to delete staff member");
    } finally {
      setDeleteDialogOpen(false);
      setStaffToDelete(null);
    }
  }, [deleteMutation, staffToDelete]);

  const handleCreate = React.useCallback(() => {
    console.log("➕ StaffTable - Creating new staff");
    navigate("/admin/staff/create");
  }, [navigate]);

  // Bulk operation handlers
  const handleBulkDelete = React.useCallback((ids: number[]) => {
    console.log("🗑️ StaffTable - Bulk deleting staff:", ids);
    bulkDeleteMutation.mutate(ids);
  }, [bulkDeleteMutation]);

  const handleBulkStatusUpdate = React.useCallback((ids: number[], status: string) => {
    console.log("✏️ StaffTable - Bulk updating status:", { ids, status });
    bulkStatusMutation.mutate({ ids, status });
  }, [bulkStatusMutation]);

  const handleBulkExport = React.useCallback((ids: number[], format: 'csv' | 'xlsx') => {
    console.log("📊 StaffTable - Bulk exporting:", { ids, format });
    bulkExportMutation.mutate({ ids, format });
  }, [bulkExportMutation]);

  const handleBulkEmail = React.useCallback((ids: number[]) => {
    console.log("📧 StaffTable - Sending bulk email to staff:", ids);
    toast.success(`Email dialog would open for ${ids.length} staff members`);
  }, []);

  const handleBulkImport = React.useCallback(async (file: File) => {
    console.log("📁 StaffTable - Importing staff from file:", file.name);
    return await bulkImportMutation.mutateAsync(file);
  }, [bulkImportMutation]);

  const columns = React.useMemo(
    () => getStaffColumns({
      onView: handleView,
      onEdit: handleEdit,
      onDelete: handleDelete,
      onSuccess: () => refetch(),
    }),
    [handleView, handleEdit, handleDelete]
  );

  // URL pagination parameters (managed by useDataTable)
  const [urlPage] = useQueryState("page", parseAsInteger.withDefault(1));
  const [pageSize] = useQueryState("perPage", parseAsInteger.withDefault(10));
  const currentPageIndex = urlPage - 1;

  // Build filter parsers dynamically from column definitions
  const filterParsers = React.useMemo(() => {
    const parsers: Record<string, Parser<string>> = {};
    columns.forEach((col) => {
      // We need to derive a stable key for the column – prefer `id`, fallback to `accessorKey` when it exists.
      const accessorKey = (col as { accessorKey?: unknown }).accessorKey;
      const key = (col.id ?? (typeof accessorKey === "string" ? accessorKey : undefined)) as
        | string
        | undefined;
      if (key && col.enableColumnFilter) {
        parsers[key] = parseAsString.withDefault("").withOptions({ shallow: true });
      }
    });
    return parsers;
  }, [columns]);

  const [filterValues] = useQueryStates(filterParsers);

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      firstName: "firstNameLike",
      lastName: "lastNameLike",
      email: "emailLike",
      staffType: "staffType",
      department: "department",
    };

    const params: Record<string, unknown> = {};

    Object.entries(filterValues).forEach(([key, val]) => {
      if (typeof val === "string" && val.trim()) {
        const backendKey = keyMap[key] ?? key;
        params[backendKey] = val.trim();
      }
    });

    return params;
  }, [filterValues]);

  const { data: staffResponse, isLoading, error, refetch } = useStaff({
    page: currentPageIndex,
    size: pageSize,
    ...apiParams,
  });

  const staff = staffResponse?.data ?? [];
  const totalElements = staffResponse?.totalItems ?? 0;
  const totalPages = staffResponse?.totalPages ?? 0;

  const { table } = useDataTable({
    data: staff,
    columns,
    pageCount: totalPages,
    initialState: {
      pagination: {
        pageIndex: currentPageIndex,
        pageSize,
      },
    },
    enableAdvancedFilter: false,
  });

  if (error) {
    console.error("❌ StaffTable - Error loading staff:", error);
    return (
      <Card className="border-red-200 shadow-lg">
        <CardContent className="pt-6">
          <div className="text-center text-red-600">
            Failed to load staff members. Please try again.
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <Card className="border-emerald-200/60 bg-gradient-to-br from-emerald-50/80 via-teal-50/40 to-cyan-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <CardTitle className="text-3xl font-bold bg-gradient-to-r from-emerald-900 via-teal-800 to-cyan-700 bg-clip-text text-transparent">
                Staff Management
              </CardTitle>
              <CardDescription className="text-emerald-700/80 text-lg">
                Manage and organize your support staff efficiently
              </CardDescription>
            </div>
            <div className="flex gap-3">
              <Button 
                variant="outline"
                onClick={() => setShowImportDialog(true)}
                className="border-emerald-300 text-emerald-700 hover:bg-emerald-50 shadow-lg hover:shadow-xl transition-all duration-300"
              >
                <Upload className="mr-2 h-4 w-4" />
                Import Staff
              </Button>
              <Button
                onClick={handleCreate}
                className="bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 shadow-lg hover:shadow-xl transition-all duration-300"
              >
                <Plus className="mr-2 h-4 w-4" />
                Create Staff
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-emerald-200/60 bg-gradient-to-br from-emerald-50/60 to-teal-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-emerald-900">Total Staff</CardTitle>
            <Users2 className="h-6 w-6 text-emerald-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-emerald-700">{totalElements}</div>
            <p className="text-emerald-600/70 text-sm mt-1">Active staff in system</p>
          </CardContent>
        </Card>
        
        <Card className="border-teal-200/60 bg-gradient-to-br from-teal-50/60 to-cyan-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-teal-900">Selected</CardTitle>
            <UserCheck className="h-6 w-6 text-teal-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-teal-700">
              {table.getFilteredSelectedRowModel().rows.length}
            </div>
            <p className="text-teal-600/70 text-sm mt-1">Staff selected</p>
          </CardContent>
        </Card>
        
        <Card className="border-cyan-200/60 bg-gradient-to-br from-cyan-50/60 to-blue-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-cyan-900">Total Pages</CardTitle>
            <TrendingUp className="h-6 w-6 text-cyan-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-cyan-700">{totalPages}</div>
            <p className="text-cyan-600/70 text-sm mt-1">Pages of data</p>
          </CardContent>
        </Card>
      </div>

      {/* Table */}
      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6">
              <DataTableSkeleton columnCount={6} rowCount={pageSize} />
            </div>
          ) : (
            <div className="overflow-hidden rounded-lg">
              <DataTable 
                table={table} 
                className="bg-white/98"
                actionBar={
                  <UserBulkActionBar
                    table={table}
                    userType="staff"
                    onBulkDelete={handleBulkDelete}
                    onBulkStatusUpdate={handleBulkStatusUpdate}
                    onBulkExport={handleBulkExport}
                    onBulkEmail={handleBulkEmail}
                    onBulkImport={handleBulkImport}
                  />
                }
              >
                <DataTableToolbar 
                  table={table} 
                  className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-emerald-50/40 px-6 py-4 backdrop-blur-sm"
                />
              </DataTable>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Are you absolutely sure?</DialogTitle>
            <DialogDescription>
              This action cannot be undone. This will permanently delete the staff member{" "}
              <span className="font-semibold">
                {staffToDelete?.firstName} {staffToDelete?.lastName}
              </span>{" "}
              and remove their data from our servers.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              onClick={confirmDelete}
              className="bg-red-600 hover:bg-red-700"
              disabled={deleteMutation.isPending}
            >
              {deleteMutation.isPending ? "Deleting..." : "Delete"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Import Dialog */}
      <BulkImportDialog
        open={showImportDialog}
        onOpenChange={setShowImportDialog}
        userType="staff"
        onImport={handleBulkImport}
      />
    </div>
  );
} 