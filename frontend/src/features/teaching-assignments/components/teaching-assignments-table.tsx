import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { type ColumnDef } from "@tanstack/react-table";
import { Users, BookOpen, GraduationCap, Plus, Upload, MoreHorizontal, Eye, Edit, Trash } from "lucide-react";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableFilterList } from "@/components/data-table/data-table-filter-list";
import { DataTableFilterMenu } from "@/components/data-table/data-table-filter-menu";
import { FilterPresets } from "@/components/data-table/filter-presets";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { 
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import type { TeachingAssignment } from "@/types/teaching-assignment";
import { 
  useTeachingAssignments,
  useDeleteTeachingAssignment
} from "../hooks/use-teaching-assignments";
import { useQueryState, parseAsInteger } from "nuqs";

interface TeachingAssignmentActionsProps {
  assignment: TeachingAssignment;
  onView: (assignment: TeachingAssignment) => void;
  onEdit: (assignment: TeachingAssignment) => void;
  onDelete: (assignment: TeachingAssignment) => void;
}

function TeachingAssignmentActions({ assignment, onView, onEdit, onDelete }: TeachingAssignmentActionsProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="h-8 w-8 p-0">
          <span className="sr-only">Open menu</span>
          <MoreHorizontal className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuLabel>Actions</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={() => onView(assignment)}>
          <Eye className="mr-2 h-4 w-4" />
          View Details
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => onEdit(assignment)}>
          <Edit className="mr-2 h-4 w-4" />
          Edit Assignment
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem 
          onClick={() => onDelete(assignment)}
          className="text-red-600 focus:text-red-600"
        >
          <Trash className="mr-2 h-4 w-4" />
          Delete Assignment
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

export function TeachingAssignmentsTable() {
  const navigate = useNavigate();
  const [deleteDialogOpen, setDeleteDialogOpen] = React.useState(false);
  const [itemToDelete, setItemToDelete] = React.useState<TeachingAssignment | null>(null);

  // URL pagination parameters
  const [urlPage] = useQueryState("page", parseAsInteger.withDefault(1));
  const [pageSize] = useQueryState("perPage", parseAsInteger.withDefault(10));
  const currentPageIndex = urlPage - 1;

  // Data fetching
  const { 
    data: dataResponse, 
    isLoading, 
    error, 
    refetch 
  } = useTeachingAssignments({
    page: urlPage,
    size: pageSize,
  });

  // Mutations
  const deleteMutation = useDeleteTeachingAssignment();

  const assignments = dataResponse?.data ?? [];
  const totalElements = dataResponse?.totalItems ?? 0;
  const totalPages = dataResponse?.totalPages ?? 0;

  // Action handlers
  const handleView = React.useCallback((assignment: TeachingAssignment) => {
    console.log("👁️ TeachingAssignmentsTable - Viewing assignment:", assignment.id);
    navigate(`/admin/teaching-assignments/view/${assignment.id}`);
  }, [navigate]);

  const handleEdit = React.useCallback((assignment: TeachingAssignment) => {
    console.log("✏️ TeachingAssignmentsTable - Editing assignment:", assignment.id);
    navigate(`/admin/teaching-assignments/edit/${assignment.id}`);
  }, [navigate]);

  const handleDelete = React.useCallback((assignment: TeachingAssignment) => {
    console.log("🗑️ TeachingAssignmentsTable - Preparing to delete assignment:", assignment.id);
    setItemToDelete(assignment);
    setDeleteDialogOpen(true);
  }, []);

  // Column definitions with filtering metadata
  const columns: ColumnDef<TeachingAssignment>[] = React.useMemo(() => [
    {
      id: "teacherName",
      accessorFn: (row) => `${row.teacherFirstName} ${row.teacherLastName}`,
      header: "Teacher",
      enableColumnFilter: true,
      meta: { variant: "text", label: "Teacher Name" },
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span className="font-medium">
            {row.original.teacherFirstName} {row.original.teacherLastName}
          </span>
          <span className="text-sm text-muted-foreground">
            {row.original.teacherEmail}
          </span>
        </div>
      ),
    },
    {
      id: "courseInfo",
      accessorFn: (row) => `${row.courseCode} - ${row.courseName}`,
      header: "Course",
      enableColumnFilter: true,
      meta: { variant: "text", label: "Course" },
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span className="font-medium">{row.original.courseName}</span>
          <Badge variant="secondary" className="w-fit text-xs">
            {row.original.courseCode}
          </Badge>
        </div>
      ),
    },
    {
      id: "className",
      accessorKey: "className",
      header: "Class",
      enableColumnFilter: true,
      meta: { variant: "text", label: "Class" },
      cell: ({ row }) => (
        <Badge variant="outline" className="font-medium">
          {row.original.className}
        </Badge>
      ),
    },
    {
      id: "weeklyHours",
      accessorKey: "weeklyHours",
      header: "Weekly Hours",
      enableColumnFilter: true,
      meta: { variant: "number", label: "Weekly Hours" },
      cell: ({ row }) => (
        <div className="text-center">
          <span className="inline-flex items-center justify-center w-8 h-8 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">
            {row.original.weeklyHours}
          </span>
        </div>
      ),
    },
    {
      id: "actions",
      header: "Actions",
      enableColumnFilter: false,
      enableSorting: false,
      cell: ({ row }) => (
        <TeachingAssignmentActions
          assignment={row.original}
          onView={handleView}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      ),
    },
  ], [handleView, handleEdit, handleDelete]);

  const confirmDelete = React.useCallback(async () => {
    if (!itemToDelete) return;

    try {
      await deleteMutation.mutateAsync(itemToDelete.id);
      refetch();
    } catch (err) {
      console.error("Failed to delete teaching assignment:", err);
    } finally {
      setDeleteDialogOpen(false);
      setItemToDelete(null);
    }
  }, [deleteMutation, itemToDelete, refetch]);

  const handleCreate = React.useCallback(() => {
    console.log("➕ TeachingAssignmentsTable - Creating new assignment");
    navigate("/admin/teaching-assignments/create");
  }, [navigate]);



  const { table } = useDataTable({
    data: assignments,
    columns,
    pageCount: totalPages,
    initialState: {
      pagination: {
        pageIndex: currentPageIndex,
        pageSize,
      },
    },
    enableAdvancedFilter: true,
  });

  if (error) {
    console.error("❌ TeachingAssignmentsTable - Error loading assignments:", error);
    return (
      <Card className="border-red-200 shadow-lg">
        <CardContent className="pt-6">
          <div className="text-center text-red-600">
            Failed to load teaching assignments. Please try again.
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/80 via-blue-50/40 to-indigo-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="space-y-2">
              <CardTitle className="text-3xl font-bold bg-gradient-to-r from-purple-900 via-blue-800 to-indigo-700 bg-clip-text text-transparent">
                Teacher-Course Assignments
              </CardTitle>
              <CardDescription className="text-purple-700/80 text-lg">
                Manage teacher assignments to courses and classes
              </CardDescription>
            </div>
            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={() => navigate("/admin/teaching-assignments/linking")}
                className="border-indigo-300 text-indigo-700 hover:bg-indigo-50 shadow-lg hover:shadow-xl transition-all duration-300"
              >
                <Users className="mr-2 h-4 w-4" />
                Assignment Interface
              </Button>
              <Button
                variant="outline"
                onClick={() => toast("Bulk import coming soon!", { icon: "ℹ️" })}
                className="border-purple-300 text-purple-700 hover:bg-purple-50 shadow-lg hover:shadow-xl transition-all duration-300"
              >
                <Upload className="mr-2 h-4 w-4" />
                Import Assignments
              </Button>
              <Button
                onClick={handleCreate}
                className="bg-gradient-to-r from-purple-600 to-blue-600 hover:from-purple-700 hover:to-blue-700 shadow-lg hover:shadow-xl transition-all duration-300"
              >
                <Plus className="mr-2 h-4 w-4" />
                Add Assignment
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/60 to-blue-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-purple-900">Total Assignments</CardTitle>
            <BookOpen className="h-6 w-6 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-purple-700">{totalElements}</div>
            <p className="text-purple-600/70 text-sm mt-1">Active teacher assignments</p>
          </CardContent>
        </Card>

        <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/60 to-indigo-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-blue-900">Selected</CardTitle>
            <Users className="h-6 w-6 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-blue-700">
              {table.getFilteredSelectedRowModel().rows.length}
            </div>
            <p className="text-blue-600/70 text-sm mt-1">Assignments selected</p>
          </CardContent>
        </Card>

        <Card className="border-indigo-200/60 bg-gradient-to-br from-indigo-50/60 to-purple-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-indigo-900">Total Pages</CardTitle>
            <GraduationCap className="h-6 w-6 text-indigo-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-indigo-700">{totalPages}</div>
            <p className="text-indigo-600/70 text-sm mt-1">Pages of data</p>
          </CardContent>
        </Card>
      </div>

      {/* Table */}
      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6">
              <DataTableSkeleton columnCount={columns.length} rowCount={pageSize} />
            </div>
          ) : (
            <div className="overflow-hidden rounded-lg">
              <DataTable table={table} className="bg-white/98">
                <div className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-purple-50/40 backdrop-blur-sm">
                  <DataTableToolbar 
                    table={table} 
                    className="px-6 py-4"
                  >
                    {/* Filter Presets and Advanced Filter Menu */}
                    <div className="flex items-center gap-2">
                      <FilterPresets 
                        table={table} 
                        entityType="teachers" // Use teacher presets for now
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
              This action cannot be undone. This will permanently delete the teaching assignment
              {itemToDelete && (
                <span className="font-semibold">
                  {" "}for {itemToDelete.teacherFirstName} {itemToDelete.teacherLastName} teaching {itemToDelete.courseName} to {itemToDelete.className}
                </span>
              )}
              {" "}and remove it from our servers.
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
    </div>
  );
}

