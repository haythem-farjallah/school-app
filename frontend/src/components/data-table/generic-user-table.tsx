import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { type ColumnDef } from "@tanstack/react-table";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableFilterList } from "@/components/data-table/data-table-filter-list";
import { DataTableFilterMenu } from "@/components/data-table/data-table-filter-menu";
import { FilterPresets } from "@/components/data-table/filter-presets";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Users, UserCheck, TrendingUp, Plus, Upload } from "lucide-react";
import { BulkImportDialog } from "@/components/data-table/bulk-import-dialog";
import { BulkEmailDialog } from "@/components/data-table/bulk-email-dialog";
import { type Parser, useQueryState, useQueryStates, parseAsInteger, parseAsString } from "nuqs";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { UserBulkActionBar } from "@/components/data-table/user-bulk-action-bar";

/**
 * Configuration for the generic user table
 */
export interface GenericUserTableConfig<T> {
  // Entity configuration
  entityType: 'teachers' | 'students' | 'staff';
  entityDisplayName: string;
  entityDisplayNamePlural: string;
  
  // Routes
  createRoute: string;
  viewRoute: (id: number) => string;
  
  // Data and columns
  columns: ColumnDef<T>[];
  
  // API parameter mapping
  filterParamMap: Record<string, string>;
  
  // Hooks
  useData: (params: any) => {
    data: { data: T[]; totalItems: number; totalPages: number } | undefined;
    isLoading: boolean;
    error: any;
    refetch: () => void;
  };
  useDelete: () => {
    mutateAsync: (id: number) => Promise<void>;
    isPending: boolean;
  };
  useBulkOperations: () => {
    bulkDelete: { mutate: (ids: number[]) => void };
    bulkStatusUpdate: { mutate: (data: { ids: number[]; status: string }) => void };
    bulkExport: { mutate: (data: { ids: number[]; format: 'csv' | 'xlsx' }) => void };
    bulkEmail: { mutateAsync: (data: any) => Promise<void>; isPending: boolean };
    bulkImport: { mutateAsync: (file: File) => Promise<void> };
  };
  
  // Color theme
  colorTheme: {
    primary: string;
    secondary: string;
    accent: string;
  };
}

interface GenericUserTableProps<T> {
  config: GenericUserTableConfig<T>;
}

export function GenericUserTable<T extends { id: number; firstName?: string; lastName?: string }>({ 
  config 
}: GenericUserTableProps<T>) {
  const navigate = useNavigate();
  const [deleteDialogOpen, setDeleteDialogOpen] = React.useState(false);
  const [entityToDelete, setEntityToDelete] = React.useState<T | null>(null);
  const [showImportDialog, setShowImportDialog] = React.useState(false);
  const [showEmailDialog, setShowEmailDialog] = React.useState(false);
  const [selectedForEmail, setSelectedForEmail] = React.useState<number[]>([]);

  const deleteMutation = config.useDelete();
  const bulkOperations = config.useBulkOperations();

  // Action handlers
  const handleView = React.useCallback((entity: T) => {
    console.log(`👁️ ${config.entityDisplayName}Table - Viewing ${config.entityType}:`, entity);
    navigate(config.viewRoute(entity.id));
  }, [navigate, config]);

  const handleEdit = React.useCallback((entity: T) => {
    console.log(`✏️ ${config.entityDisplayName}Table - Editing ${config.entityType}:`, entity);
    // Edit logic is handled by the EditSheet component
  }, [config]);

  const handleDelete = React.useCallback((entity: T) => {
    console.log(`🗑️ ${config.entityDisplayName}Table - Deleting ${config.entityType}:`, entity);
    setEntityToDelete(entity);
    setDeleteDialogOpen(true);
  }, [config]);

  const confirmDelete = React.useCallback(async () => {
    if (!entityToDelete) return;
    
    console.log(`✅ ${config.entityDisplayName}Table - Confirming delete for:`, entityToDelete);
    
    try {
      await deleteMutation.mutateAsync(entityToDelete.id);
      const entityName = entityToDelete.firstName && entityToDelete.lastName 
        ? `${entityToDelete.firstName} ${entityToDelete.lastName}`
        : `${config.entityDisplayName} #${entityToDelete.id}`;
      toast.success(`${config.entityDisplayName} "${entityName}" deleted successfully`);
      refetch();
    } catch (error) {
      console.error(`❌ ${config.entityDisplayName}Table - Delete failed:`, error);
      toast.error(`Failed to delete ${config.entityDisplayName.toLowerCase()}`);
    } finally {
      setDeleteDialogOpen(false);
      setEntityToDelete(null);
    }
  }, [deleteMutation, entityToDelete, config]);

  const handleCreate = React.useCallback(() => {
    console.log(`➕ ${config.entityDisplayName}Table - Creating new ${config.entityType}`);
    navigate(config.createRoute);
  }, [navigate, config]);

  // Bulk operation handlers
  const handleBulkDelete = React.useCallback((ids: number[]) => {
    console.log(`🗑️ ${config.entityDisplayName}Table - Bulk deleting ${config.entityType}:`, ids);
    bulkOperations.bulkDelete.mutate(ids);
  }, [bulkOperations.bulkDelete, config]);

  const handleBulkStatusUpdate = React.useCallback((ids: number[], status: string) => {
    console.log(`✏️ ${config.entityDisplayName}Table - Bulk updating status:`, { ids, status });
    bulkOperations.bulkStatusUpdate.mutate({ ids, status });
  }, [bulkOperations.bulkStatusUpdate]);

  const handleBulkExport = React.useCallback((ids: number[], format: 'csv' | 'xlsx') => {
    console.log(`📊 ${config.entityDisplayName}Table - Bulk exporting:`, { ids, format });
    bulkOperations.bulkExport.mutate({ ids, format });
  }, [bulkOperations.bulkExport]);

  const handleBulkEmail = React.useCallback((ids: number[]) => {
    console.log(`📧 ${config.entityDisplayName}Table - Opening email dialog for ${config.entityType}:`, ids);
    setSelectedForEmail(ids);
    setShowEmailDialog(true);
  }, [config]);

  const handleSendEmail = React.useCallback(async (emailData: {
    subject: string;
    message: string;
    actionUrl?: string;
    actionText?: string;
  }) => {
    console.log(`📧 ${config.entityDisplayName}Table - Sending email:`, emailData);
    await bulkOperations.bulkEmail.mutateAsync({
      ids: selectedForEmail,
      ...emailData
    });
  }, [selectedForEmail, bulkOperations.bulkEmail]);

  const handleBulkImport = React.useCallback(async (file: File) => {
    console.log(`📁 ${config.entityDisplayName}Table - Importing ${config.entityType} from file:`, file.name);
    return await bulkOperations.bulkImport.mutateAsync(file);
  }, [bulkOperations.bulkImport, config]);

  const columns = React.useMemo(
    () => config.columns.map(col => ({
      ...col,
      // Inject action handlers if this is an actions column
      cell: col.id === 'actions' && typeof col.cell === 'function' 
        ? (props: any) => col.cell?.({
            ...props,
            onView: handleView,
            onEdit: handleEdit,
            onDelete: handleDelete,
            onSuccess: refetch,
          })
        : col.cell
    })),
    [config.columns, handleView, handleEdit, handleDelete]
  );

  // URL pagination parameters
  const [urlPage] = useQueryState("page", parseAsInteger.withDefault(1));
  const [pageSize] = useQueryState("perPage", parseAsInteger.withDefault(10));
  const currentPageIndex = urlPage - 1;

  // Build filter parsers dynamically from column definitions
  const filterParsers = React.useMemo(() => {
    const parsers: Record<string, Parser<string>> = {};
    columns.forEach((col) => {
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
    const params: Record<string, unknown> = {};

    Object.entries(filterValues).forEach(([key, val]) => {
      if (typeof val === "string" && val.trim()) {
        const backendKey = config.filterParamMap[key] ?? key;
        params[backendKey] = val.trim();
      }
    });

    return params;
  }, [filterValues, config.filterParamMap]);

  const { data: dataResponse, isLoading, error, refetch } = config.useData({
    page: currentPageIndex,
    size: pageSize,
    ...apiParams,
  });

  const entities = dataResponse?.data ?? [];
  const totalElements = dataResponse?.totalItems ?? 0;
  const totalPages = dataResponse?.totalPages ?? 0;

  const { table } = useDataTable({
    data: entities,
    columns,
    pageCount: totalPages,
    initialState: {
      pagination: {
        pageIndex: currentPageIndex,
        pageSize,
      },
    },
    enableAdvancedFilter: true, // Enable advanced filtering
  });

  if (error) {
    console.error(`❌ ${config.entityDisplayName}Table - Error loading ${config.entityType}:`, error);
    return (
      <Card className="border-red-200 shadow-lg">
        <CardContent className="pt-6">
          <div className="text-center text-red-600">
            Failed to load {config.entityDisplayNamePlural.toLowerCase()}. Please try again.
          </div>
        </CardContent>
      </Card>
    );
  }

  const themeClass = `border-${config.colorTheme.primary}-200/60`;
  const gradientClass = `bg-gradient-to-br from-${config.colorTheme.primary}-50/80 via-${config.colorTheme.secondary}-50/40 to-${config.colorTheme.accent}-50/20`;

  return (
    <div className="space-y-8">
      {/* Header */}
      <Card className={`${themeClass} ${gradientClass} shadow-xl backdrop-blur-sm`}>
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <CardTitle className={`text-3xl font-bold bg-gradient-to-r from-${config.colorTheme.primary}-900 via-${config.colorTheme.secondary}-800 to-${config.colorTheme.accent}-700 bg-clip-text text-transparent`}>
                {config.entityDisplayNamePlural} Management
              </CardTitle>
              <CardDescription className={`text-${config.colorTheme.primary}-700/80 text-lg`}>
                Manage and organize your {config.entityDisplayNamePlural.toLowerCase()} efficiently
              </CardDescription>
            </div>
            <div className="flex gap-3">
              <Button 
                variant="outline"
                onClick={() => setShowImportDialog(true)}
                className={`border-${config.colorTheme.primary}-300 text-${config.colorTheme.primary}-700 hover:bg-${config.colorTheme.primary}-50 shadow-lg hover:shadow-xl transition-all duration-300`}
              >
                <Upload className="mr-2 h-4 w-4" />
                Import {config.entityDisplayNamePlural}
              </Button>
              <Button 
                onClick={handleCreate}
                className={`bg-gradient-to-r from-${config.colorTheme.primary}-600 to-${config.colorTheme.accent}-600 hover:from-${config.colorTheme.primary}-700 hover:to-${config.colorTheme.accent}-700 shadow-lg hover:shadow-xl transition-all duration-300`}
              >
                <Plus className="mr-2 h-4 w-4" />
                Add {config.entityDisplayName}
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className={`border-${config.colorTheme.primary}-200/60 bg-gradient-to-br from-${config.colorTheme.primary}-50/60 to-${config.colorTheme.secondary}-50/30 hover:shadow-lg transition-shadow duration-300`}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className={`text-lg font-semibold text-${config.colorTheme.primary}-900`}>
              Total {config.entityDisplayNamePlural}
            </CardTitle>
            <Users className={`h-6 w-6 text-${config.colorTheme.primary}-600`} />
          </CardHeader>
          <CardContent>
            <div className={`text-3xl font-bold text-${config.colorTheme.primary}-700`}>{totalElements}</div>
            <p className={`text-${config.colorTheme.primary}-600/70 text-sm mt-1`}>
              Active {config.entityDisplayNamePlural.toLowerCase()} in system
            </p>
          </CardContent>
        </Card>
        
        <Card className={`border-${config.colorTheme.secondary}-200/60 bg-gradient-to-br from-${config.colorTheme.secondary}-50/60 to-${config.colorTheme.accent}-50/30 hover:shadow-lg transition-shadow duration-300`}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className={`text-lg font-semibold text-${config.colorTheme.secondary}-900`}>Selected</CardTitle>
            <UserCheck className={`h-6 w-6 text-${config.colorTheme.secondary}-600`} />
          </CardHeader>
          <CardContent>
            <div className={`text-3xl font-bold text-${config.colorTheme.secondary}-700`}>
              {table.getFilteredSelectedRowModel().rows.length}
            </div>
            <p className={`text-${config.colorTheme.secondary}-600/70 text-sm mt-1`}>
              {config.entityDisplayNamePlural} selected
            </p>
          </CardContent>
        </Card>
        
        <Card className={`border-${config.colorTheme.accent}-200/60 bg-gradient-to-br from-${config.colorTheme.accent}-50/60 to-pink-50/30 hover:shadow-lg transition-shadow duration-300`}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className={`text-lg font-semibold text-${config.colorTheme.accent}-900`}>Total Pages</CardTitle>
            <TrendingUp className={`h-6 w-6 text-${config.colorTheme.accent}-600`} />
          </CardHeader>
          <CardContent>
            <div className={`text-3xl font-bold text-${config.colorTheme.accent}-700`}>{totalPages}</div>
            <p className={`text-${config.colorTheme.accent}-600/70 text-sm mt-1`}>Pages of data</p>
          </CardContent>
        </Card>
      </div>

      {/* Table */}
      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6">
              <DataTableSkeleton columnCount={8} rowCount={pageSize} />
            </div>
          ) : (
            <div className="overflow-hidden rounded-lg">
              <DataTable 
                table={table} 
                className="bg-white/98"
                actionBar={
                  <UserBulkActionBar
                    table={table}
                    userType={config.entityType}
                    onBulkDelete={handleBulkDelete}
                    onBulkStatusUpdate={handleBulkStatusUpdate}
                    onBulkExport={handleBulkExport}
                    onBulkEmail={handleBulkEmail}
                    onBulkImport={handleBulkImport}
                    onBulkAssignCourses={config.entityType === 'teachers' ? 
                      (teacherIds: number[], courseIds: number[]) => {
                        console.log("📚 Bulk assigning courses:", { teacherIds, courseIds });
                        toast.success(`Course assignment dialog would open for ${teacherIds.length} teachers`);
                      } : undefined
                    }
                  />
                }
              >
                <div className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-blue-50/40 backdrop-blur-sm">
                  <DataTableToolbar 
                    table={table} 
                    className="px-6 py-4"
                  >
                    {/* Filter Presets and Advanced Filter Menu */}
                    <div className="flex items-center gap-2">
                      <FilterPresets 
                        table={table} 
                        entityType={config.entityType}
                      />
                      <DataTableFilterMenu table={table} />
                    </div>
                  </DataTableToolbar>
                  
                  {/* Advanced Filter List */}
                  <div className="px-6 pb-4">
                    <DataTableFilterList table={table} />
                  </div>
                </div>
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
              This action cannot be undone. This will permanently delete the {config.entityDisplayName.toLowerCase()}{" "}
              <span className="font-semibold">
                {entityToDelete?.firstName && entityToDelete?.lastName
                  ? `${entityToDelete.firstName} ${entityToDelete.lastName}`
                  : `#${entityToDelete?.id}`
                }
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
        userType={config.entityType}
        onImport={handleBulkImport}
      />

      {/* Email Dialog */}
      <BulkEmailDialog
        open={showEmailDialog}
        onOpenChange={setShowEmailDialog}
        userType={config.entityType}
        selectedCount={selectedForEmail.length}
        onSendEmail={handleSendEmail}
        isLoading={bulkOperations.bulkEmail.isPending}
      />
    </div>
  );
}
