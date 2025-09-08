import * as React from "react";
import toast from "react-hot-toast";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Library, BookOpen, TrendingUp, Upload, Eye, Settings } from "lucide-react";

import { useLearningResources, useDeleteLearningResource, useDownloadResource } from "../hooks/use-learning-resources";
import { getLearningResourceColumns } from "./learning-resource-columns";
import { AddLearningResourceSheet } from "./learning-resource-sheet";
import { ResourceViewDialog } from "./resource-view-dialog";
import { ResourceUploadDialog } from "./resource-upload-dialog";
import { ResourceVisibilityDialog } from "./resource-visibility-dialog";
import type { LearningResource } from "@/types/learning-resource";

export function LearningResourcesTable() {
  const [page, setPage] = React.useState(0);
  const [pageSize, setPageSize] = React.useState(10);
  const [search, setSearch] = React.useState("");

  // Dialog states
  const [viewDialogOpen, setViewDialogOpen] = React.useState(false);
  const [uploadDialogOpen, setUploadDialogOpen] = React.useState(false);
  const [visibilityDialogOpen, setVisibilityDialogOpen] = React.useState(false);
  const [selectedResource, setSelectedResource] = React.useState<LearningResource | null>(null);

  const { data: pageData, isLoading, error, refetch } = useLearningResources({ size: pageSize });

  const resources = pageData?.data || [];
  const totalElements = pageData?.totalItems || 0;
  const totalPages = pageData?.totalPages || 0;

  const deleteMutation = useDeleteLearningResource();
  const downloadMutation = useDownloadResource();

  const handleView = React.useCallback((resource: LearningResource) => {
    setSelectedResource(resource);
    setViewDialogOpen(true);
  }, []);

  const handleEdit = React.useCallback((resource: LearningResource) => {
    // Edit logic is handled by the EditLearningResourceSheet component
    console.log(`Editing resource: ${resource.title}`);
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

  const handleVisibility = React.useCallback((resource: LearningResource) => {
    setSelectedResource(resource);
    setVisibilityDialogOpen(true);
  }, []);

  const handleDownload = React.useCallback(async (resource: LearningResource) => {
    if (!resource.filename) {
      toast.error("No file available for download");
      return;
    }

    try {
      const blob = await downloadMutation.mutateAsync(resource.filename);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = resource.filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success(`Downloaded: ${resource.title}`);
    } catch {
      toast.error("Failed to download resource");
    }
  }, [downloadMutation]);

  const columns = React.useMemo(
    () => getLearningResourceColumns({
      onView: handleView,
      onEdit: handleEdit,
      onDelete: handleDelete,
      onDownload: handleDownload,
      onVisibility: handleVisibility,
      onSuccess: () => refetch(),
    }),
    [handleView, handleEdit, handleDelete, handleDownload, handleVisibility, refetch]
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
      <Card className="border-indigo-200/60 bg-gradient-to-br from-indigo-50/80 via-purple-50/40 to-pink-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
                      <div className="flex items-center justify-between">
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-indigo-900 via-purple-800 to-pink-700 bg-clip-text text-transparent">
                  Learning Resources
                </CardTitle>
                <CardDescription className="text-indigo-700/80 text-lg">
                  Manage educational content, videos, documents, and learning materials
                </CardDescription>
              </div>
              <div className="flex items-center space-x-3">
                <Button
                  onClick={() => setUploadDialogOpen(true)}
                  className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
                >
                  <Upload className="h-4 w-4 mr-2" />
                  Upload Files
                </Button>
                <AddLearningResourceSheet onSuccess={() => refetch()} />
              </div>
            </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-indigo-200/60 bg-gradient-to-br from-indigo-50/60 to-purple-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-indigo-900">Total Resources</CardTitle>
            <Library className="h-6 w-6 text-indigo-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-indigo-700">{totalElements}</div>
            <p className="text-indigo-600/70 text-sm mt-1">Learning materials available</p>
          </CardContent>
        </Card>
        
        <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/60 to-pink-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-purple-900">Selected</CardTitle>
            <BookOpen className="h-6 w-6 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-purple-700">
              {table.getFilteredSelectedRowModel().rows.length}
            </div>
            <p className="text-purple-600/70 text-sm mt-1">Resources selected</p>
          </CardContent>
        </Card>
        
        <Card className="border-pink-200/60 bg-gradient-to-br from-pink-50/60 to-rose-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-pink-900">Total Pages</CardTitle>
            <TrendingUp className="h-6 w-6 text-pink-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-pink-700">{totalPages}</div>
            <p className="text-pink-600/70 text-sm mt-1">Pages of data</p>
          </CardContent>
        </Card>
      </div>

      {/* Table */}
      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
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
        onDownload={handleDownload}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />

      <ResourceUploadDialog
        open={uploadDialogOpen}
        onOpenChange={setUploadDialogOpen}
        onSuccess={() => refetch()}
      />

      <ResourceVisibilityDialog
        resource={selectedResource}
        open={visibilityDialogOpen}
        onOpenChange={setVisibilityDialogOpen}
        onSave={async (resourceId, settings) => {
          // TODO: Implement visibility settings save
          console.log('Saving visibility settings:', resourceId, settings);
          toast.success("Visibility settings updated");
        }}
      />
    </div>
  );
} 