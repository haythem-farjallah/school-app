import * as React from "react";
import toast from "react-hot-toast";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Users, UserCheck, TrendingUp } from "lucide-react";

import { useParents, useDeleteParent } from "../hooks/use-parents";
import { getParentsColumns } from "./parent-columns.tsx";
import { AddParentSheet } from "./parent-sheet";
import type { Parent } from "@/types/parent";

export function ParentsTable() {
  const [page, setPage] = React.useState(0);
  const [pageSize, setPageSize] = React.useState(10);
  const [search, setSearch] = React.useState("");

  const { data: pageData, isLoading, error, refetch } = useParents({ size: pageSize });
  
  const parents = pageData?.data || [];
  const totalElements = pageData?.totalItems || 0;
  const totalPages = pageData?.totalPages || 0;

  const deleteMutation = useDeleteParent();

  const handleView = React.useCallback((parent: Parent) => {
    toast(`Viewing parent: ${parent.firstName} ${parent.lastName}`);
    // Implement view logic here
  }, []);

  const handleEdit = React.useCallback((parent: Parent) => {
    // Edit logic is now handled by the EditParentSheet component
    console.log(`Editing parent: ${parent.firstName} ${parent.lastName}`);
  }, []);

  const handleDelete = React.useCallback(async (parent: Parent) => {
    if (window.confirm(`Are you sure you want to delete "${parent.firstName} ${parent.lastName}"?`)) {
      try {
        await deleteMutation.mutateAsync(parent.id);
        toast.success(`Parent "${parent.firstName} ${parent.lastName}" deleted successfully`);
        refetch();
      } catch {
        toast.error("Failed to delete parent");
      }
    }
  }, [deleteMutation, refetch]);

  const columns = React.useMemo(
    () => getParentsColumns({
      onView: handleView,
      onEdit: handleEdit,
      onDelete: handleDelete,
      onSuccess: () => refetch(),
    }),
    [handleView, handleEdit, handleDelete, refetch]
  );

  const { table } = useDataTable({
    data: parents,
    columns,
    pageCount: totalPages,
    initialState: {
      pagination: {
        pageIndex: page,
        pageSize,
      },
    },
    enableAdvancedFilter: false,
  });

  // Update page when table pagination changes
  React.useEffect(() => {
    const pagination = table.getState().pagination;
    if (pagination.pageIndex !== page) {
      setPage(pagination.pageIndex);
    }
    if (pagination.pageSize !== pageSize) {
      setPageSize(pagination.pageSize);
    }
  }, [table, page, pageSize]);

  // Handle search from URL or toolbar
  React.useEffect(() => {
    const columnFilters = table.getState().columnFilters;
    const nameFilter = columnFilters.find(filter => filter.id === "firstName");
    const searchValue = nameFilter?.value as string || "";
    if (searchValue !== search) {
      setSearch(searchValue);
    }
  }, [table, search]);

  if (error) {
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
            <AddParentSheet onSuccess={() => refetch()} />
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
              <DataTable table={table} className="bg-white/98">
                <DataTableToolbar 
                  table={table} 
                  className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-purple-50/40 px-6 py-4 backdrop-blur-sm"
                />
              </DataTable>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
} 