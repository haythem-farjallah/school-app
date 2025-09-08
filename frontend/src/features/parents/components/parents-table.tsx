import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Users, UserCheck, TrendingUp, Plus, Upload } from "lucide-react";
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

import { useParents, useDeleteParent } from "../hooks/use-parents";
import { useBulkDeleteParents, useBulkUpdateParentStatus, useBulkExportParents, useBulkImportParents } from "../hooks/use-parents-bulk";
import { getParentsColumns } from "./parent-columns";
import type { Parent } from "@/types/parent";
import { UserBulkActionBar } from "@/components/data-table/user-bulk-action-bar";

export function ParentsTable() {
  const navigate = useNavigate();
  const [deleteDialogOpen, setDeleteDialogOpen] = React.useState(false);
  const [parentToDelete, setParentToDelete] = React.useState<Parent | null>(null);
  const [showImportDialog, setShowImportDialog] = React.useState(false);

  const deleteMutation = useDeleteParent();

  // Bulk operation hooks
  const bulkDeleteMutation = useBulkDeleteParents();
  const bulkStatusMutation = useBulkUpdateParentStatus();
  const bulkExportMutation = useBulkExportParents();
  const bulkImportMutation = useBulkImportParents();

  const handleView = React.useCallback((parent: Parent) => {
    console.log("👁️ ParentsTable - Viewing parent:", parent);
    navigate(`/admin/parents/view/${parent.id}`);
  }, [navigate]);

  const handleEdit = React.useCallback((parent: Parent) => {
    console.log("✏️ ParentsTable - Editing parent:", parent);
    // Edit logic is now handled by the EditParentSheet component
  }, []);

  const handleDelete = React.useCallback((parent: Parent) => {
    console.log("🗑️ ParentsTable - Deleting parent:", parent);
    setParentToDelete(parent);
    setDeleteDialogOpen(true);
  }, []);

  const confirmDelete = React.useCallback(async () => {
    if (!parentToDelete) return;
    
    console.log("✅ ParentsTable - Confirming delete for:", parentToDelete);
    
    try {
      await deleteMutation.mutateAsync(parentToDelete.id);
      toast.success(`Parent "${parentToDelete.firstName} ${parentToDelete.lastName}" deleted successfully`);
      refetch();
    } catch (error) {
      console.error("❌ ParentsTable - Delete failed:", error);
      toast.error("Failed to delete parent");
    } finally {
      setDeleteDialogOpen(false);
      setParentToDelete(null);
    }
  }, [deleteMutation, parentToDelete]);

  const handleCreate = React.useCallback(() => {
    console.log("➕ ParentsTable - Creating new parent");
    navigate("/admin/parents/create");
  }, [navigate]);

  // Bulk operation handlers
  const handleBulkDelete = React.useCallback((ids: number[]) => {
    console.log("🗑️ ParentsTable - Bulk deleting parents:", ids);
    bulkDeleteMutation.mutate(ids);
  }, [bulkDeleteMutation]);

  const handleBulkStatusUpdate = React.useCallback((ids: number[], status: string) => {
    console.log("✏️ ParentsTable - Bulk updating status:", { ids, status });
    bulkStatusMutation.mutate({ ids, status });
  }, [bulkStatusMutation]);

  const handleBulkExport = React.useCallback((ids: number[], format: 'csv' | 'xlsx') => {
    console.log("📊 ParentsTable - Bulk exporting:", { ids, format });
    bulkExportMutation.mutate({ ids, format });
  }, [bulkExportMutation]);

  const handleBulkEmail = React.useCallback((ids: number[]) => {
    console.log("📧 ParentsTable - Sending bulk email to parents:", ids);
    toast.success(`Email dialog would open for ${ids.length} parents`);
  }, []);

  const handleBulkImport = React.useCallback(async (file: File) => {
    console.log("📁 ParentsTable - Importing parents from file:", file.name);
    return await bulkImportMutation.mutateAsync(file);
  }, [bulkImportMutation]);

  const columns = React.useMemo(
    () => getParentsColumns({
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
      telephone: "telephoneLike",
      preferredContactMethod: "preferredContactMethodLike",
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

  const { data: parentsResponse, isLoading, error, refetch } = useParents({
    page: currentPageIndex,
    size: pageSize,
    ...apiParams,
  });

  const parents = parentsResponse?.data ?? [];
  const totalElements = parentsResponse?.totalItems ?? 0;
  const totalPages = parentsResponse?.totalPages ?? 0;

  const { table } = useDataTable({
    data: parents,
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
    console.error("❌ ParentsTable - Error loading parents:", error);
    return (
      <Card className="border-red-200 shadow-lg">
        <CardContent className="pt-6">
          <div className="text-center text-red-600">
            Failed to load parents. Please try again.
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/80 via-pink-50/40 to-rose-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <CardTitle className="text-3xl font-bold bg-gradient-to-r from-purple-900 via-pink-800 to-rose-700 bg-clip-text text-transparent">
                Parents Management
              </CardTitle>
              <CardDescription className="text-purple-700/80 text-lg">
                Manage and organize parent information efficiently
              </CardDescription>
            </div>
            <div className="flex gap-3">
              <Button 
                variant="outline"
                onClick={() => setShowImportDialog(true)}
                className="border-purple-300 text-purple-700 hover:bg-purple-50 shadow-lg hover:shadow-xl transition-all duration-300"
              >
                <Upload className="mr-2 h-4 w-4" />
                Import Parents
              </Button>
              <Button
                onClick={handleCreate}
                className="bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 shadow-lg hover:shadow-xl transition-all duration-300"
              >
                <Plus className="mr-2 h-4 w-4" />
                Create Parent
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/60 to-pink-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-purple-900">Total Parents</CardTitle>
            <Users className="h-6 w-6 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-purple-700">{totalElements}</div>
            <p className="text-purple-600/70 text-sm mt-1">Active parents in system</p>
          </CardContent>
        </Card>
        
        <Card className="border-pink-200/60 bg-gradient-to-br from-pink-50/60 to-rose-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-pink-900">Selected</CardTitle>
            <UserCheck className="h-6 w-6 text-pink-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-pink-700">
              {table.getFilteredSelectedRowModel().rows.length}
            </div>
            <p className="text-pink-600/70 text-sm mt-1">Parents selected</p>
          </CardContent>
        </Card>
        
        <Card className="border-rose-200/60 bg-gradient-to-br from-rose-50/60 to-red-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-rose-900">Total Pages</CardTitle>
            <TrendingUp className="h-6 w-6 text-rose-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-rose-700">{totalPages}</div>
            <p className="text-rose-600/70 text-sm mt-1">Pages of data</p>
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
                    userType="parents"
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
                  className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-purple-50/40 px-6 py-4 backdrop-blur-sm"
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
              This action cannot be undone. This will permanently delete the parent{" "}
              <span className="font-semibold">
                {parentToDelete?.firstName} {parentToDelete?.lastName}
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
        userType="parents"
        onImport={handleBulkImport}
      />
    </div>
  );
} 