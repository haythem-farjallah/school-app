import * as React from "react";
import toast from "react-hot-toast";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Library, BookOpen, TrendingUp } from "lucide-react";

import { useLearningResources, useDeleteLearningResource } from "../hooks/use-learning-resources";
import { getLearningResourceColumns } from "./learning-resource-columns";
import { ResourceViewDialog } from "./resource-view-dialog";
import type { LearningResource } from "@/types/learning-resource";

export function LearningResourcesTable() {
  const [page, setPage] = React.useState(0);
  const [pageSize, setPageSize] = React.useState(10);
  const [search, setSearch] = React.useState("");

  // Dialog states
  const [viewDialogOpen, setViewDialogOpen] = React.useState(false);
  const [selectedResource, setSelectedResource] = React.useState<LearningResource | null>(null);

  const { data: pageData, isLoading, error, refetch } = useLearningResources({ size: pageSize });

  const resources = pageData?.data || [];
  const totalElements = pageData?.totalItems || 0;
  const totalPages = pageData?.totalPages || 0;

  const deleteMutation = useDeleteLearningResource();

  const handleView = React.useCallback((resource: LearningResource) => {
    setSelectedResource(resource);
    setViewDialogOpen(true);
  }, []);

  const handleDelete = React.useCallback(async (resource: LearningResource) => {
    if (window.confirm(`Are you sure you want to delete "${resource.title}"?`)) {
      try {
        await deleteMutation.mutateAsync(resource.id);
        toast.success(`Resource "${resource.title}" deleted successfully`);
        refetch();
      } catch {
        toast.error("Failed to delete resource");
      }
    }
  }, [deleteMutation, refetch]);

  const columns = React.useMemo(
    () => getLearningResourceColumns({
      onView: handleView,
      onDelete: handleDelete,
    }),
    [handleView, handleDelete]
  );

  const { table } = useDataTable({
    data: resources,
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
    const titleFilter = columnFilters.find(filter => filter.id === "title");
    const searchValue = titleFilter?.value as string || "";
    if (searchValue !== search) {
      setSearch(searchValue);
    }
  }, [table, search]);

  if (error) {
    return (
      <Card className="border-red-200 shadow-lg">
        <CardContent className="pt-6">
          <div className="text-center text-red-600">
            Failed to load learning resources. Please try again.
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <Card className="border-blue-200/50 bg-gradient-to-br from-blue-50/90 via-slate-50/60 to-white shadow-lg">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <CardTitle className="text-3xl font-bold text-slate-800">
                Learning Resources
              </CardTitle>
              <CardDescription className="text-slate-600 text-lg">
                Manage educational content, videos, documents, and learning materials
              </CardDescription>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-slate-200/60 bg-white hover:shadow-md transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-slate-700">Total Resources</CardTitle>
            <Library className="h-6 w-6 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-slate-800">{totalElements}</div>
            <p className="text-slate-500 text-sm mt-1">Learning materials available</p>
          </CardContent>
        </Card>
        
        <Card className="border-slate-200/60 bg-white hover:shadow-md transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-slate-700">Selected</CardTitle>
            <BookOpen className="h-6 w-6 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-slate-800">
              {table.getFilteredSelectedRowModel().rows.length}
            </div>
            <p className="text-slate-500 text-sm mt-1">Resources selected</p>
          </CardContent>
        </Card>
        
        <Card className="border-slate-200/60 bg-white hover:shadow-md transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-slate-700">Total Pages</CardTitle>
            <TrendingUp className="h-6 w-6 text-orange-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-slate-800">{totalPages}</div>
            <p className="text-slate-500 text-sm mt-1">Pages of data</p>
          </CardContent>
        </Card>
      </div>

      {/* Table */}
      <Card className="border-slate-200/60 shadow-lg bg-white">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6">
              <DataTableSkeleton columnCount={7} rowCount={pageSize} />
            </div>
          ) : (
            <div className="overflow-hidden rounded-lg">
              <DataTable table={table} className="bg-white/98">
                <DataTableToolbar 
                  table={table} 
                  className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-indigo-50/40 px-6 py-4 backdrop-blur-sm"
                />
              </DataTable>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Dialogs */}
      <ResourceViewDialog
        resource={selectedResource}
        open={viewDialogOpen}
        onOpenChange={setViewDialogOpen}
        onDelete={handleDelete}
      />
    </div>
  );
} 