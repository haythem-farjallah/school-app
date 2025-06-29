import * as React from "react";
import toast from "react-hot-toast";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Users, UserCheck, TrendingUp } from "lucide-react";

import { useTeachers, useDeleteTeacher } from "../hooks/use-teachers";
import { getTeachersColumns } from "./teacher-columns";
import { AddTeacherSheet } from "./teacher-sheet";
import type { Teacher } from "@/types/teacher";

export function TeachersTable() {
  const [page, setPage] = React.useState(0);
  const [pageSize, setPageSize] = React.useState(10);
  const [search, setSearch] = React.useState("");

  const { data: pageData, isLoading, error, refetch } = useTeachers({ size: pageSize });

  const teachers = pageData?.data || [];
  const totalElements = pageData?.totalItems || 0;
  const totalPages = pageData?.totalPages || 0;

  const deleteMutation = useDeleteTeacher();

  const handleView = React.useCallback((teacher: Teacher) => {
    toast(`Viewing teacher: ${teacher.firstName} ${teacher.lastName}`);
    // Implement view logic here
  }, []);

  const handleEdit = React.useCallback((teacher: Teacher) => {
    // Edit logic is now handled by the EditTeacherSheet component
    console.log(`Editing teacher: ${teacher.firstName} ${teacher.lastName}`);
  }, []);

  const handleDelete = React.useCallback(async (teacher: Teacher) => {
    if (window.confirm(`Are you sure you want to delete "${teacher.firstName} ${teacher.lastName}"?`)) {
      try {
        await deleteMutation.mutateAsync(teacher.id);
        toast.success(`Teacher "${teacher.firstName} ${teacher.lastName}" deleted successfully`);
        refetch();
      } catch {
        toast.error("Failed to delete teacher");
      }
    }
  }, [deleteMutation, refetch]);

  const columns = React.useMemo(
    () => getTeachersColumns({
      onView: handleView,
      onEdit: handleEdit,
      onDelete: handleDelete,
      onSuccess: () => refetch(),
    }),
    [handleView, handleEdit, handleDelete, refetch]
  );

  const { table } = useDataTable({
    data: teachers,
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
            Failed to load teachers. Please try again.
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/80 via-indigo-50/40 to-purple-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <CardTitle className="text-3xl font-bold bg-gradient-to-r from-blue-900 via-indigo-800 to-purple-700 bg-clip-text text-transparent">
                Teachers Management
              </CardTitle>
              <CardDescription className="text-blue-700/80 text-lg">
                Manage and organize your teaching staff efficiently
              </CardDescription>
            </div>
            <AddTeacherSheet onSuccess={() => refetch()} />
          </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/60 to-indigo-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-blue-900">Total Teachers</CardTitle>
            <Users className="h-6 w-6 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-blue-700">{totalElements}</div>
            <p className="text-blue-600/70 text-sm mt-1">Active teachers in system</p>
          </CardContent>
        </Card>
        
        <Card className="border-indigo-200/60 bg-gradient-to-br from-indigo-50/60 to-purple-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-indigo-900">Selected</CardTitle>
            <UserCheck className="h-6 w-6 text-indigo-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-indigo-700">
              {table.getFilteredSelectedRowModel().rows.length}
            </div>
            <p className="text-indigo-600/70 text-sm mt-1">Teachers selected</p>
          </CardContent>
        </Card>
        
        <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/60 to-pink-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-purple-900">Total Pages</CardTitle>
            <TrendingUp className="h-6 w-6 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-purple-700">{totalPages}</div>
            <p className="text-purple-600/70 text-sm mt-1">Pages of data</p>
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
              <DataTable table={table} className="bg-white/98">
                <DataTableToolbar 
                  table={table} 
                  className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-blue-50/40 px-6 py-4 backdrop-blur-sm"
                />
              </DataTable>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
} 