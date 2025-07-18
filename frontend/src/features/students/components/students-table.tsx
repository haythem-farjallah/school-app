import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";


import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Users, UserCheck, TrendingUp, Plus } from "lucide-react";
import { type Parser, useQueryState, useQueryStates, parseAsInteger, parseAsString } from "nuqs";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";


import { useStudents, useDeleteStudent } from "../hooks/use-students";
import { getStudentsColumns } from "./student-columns.tsx";
import type { Student } from "@/types/student";

export function StudentsTable() {
  const navigate = useNavigate();
  const [deleteDialogOpen, setDeleteDialogOpen] = React.useState(false);
  const [studentToDelete, setStudentToDelete] = React.useState<Student | null>(null);

  const deleteMutation = useDeleteStudent();

  const handleView = React.useCallback((student: Student) => {
    console.log("👁️ StudentsTable - Viewing student:", student);
    navigate(`/admin/students/view/${student.id}`);
  }, [navigate]);

  const handleEdit = React.useCallback((student: Student) => {
    console.log("✏️ StudentsTable - Editing student:", student);
    // Edit logic is now handled by the EditStudentSheet component
  }, []);

  const handleDelete = React.useCallback((student: Student) => {
    console.log("🗑️ StudentsTable - Deleting student:", student);
    setStudentToDelete(student);
    setDeleteDialogOpen(true);
  }, []);

  const confirmDelete = React.useCallback(async () => {
    if (!studentToDelete) return;
    
    console.log("✅ StudentsTable - Confirming delete for:", studentToDelete);
    
    try {
      await deleteMutation.mutateAsync(studentToDelete.id);
      toast.success(`Student "${studentToDelete.firstName} ${studentToDelete.lastName}" deleted successfully`);
      refetch();
    } catch (error) {
      console.error("❌ StudentsTable - Delete failed:", error);
      toast.error("Failed to delete student");
    } finally {
      setDeleteDialogOpen(false);
      setStudentToDelete(null);
    }
  }, [deleteMutation, studentToDelete]);

  const handleCreate = React.useCallback(() => {
    console.log("➕ StudentsTable - Creating new student");
    navigate("/admin/students/create");
  }, [navigate]);

  const columns = React.useMemo(
    () => getStudentsColumns({
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
      gradeLevel: "gradeLevel", // same name
      enrollmentYear: "enrollmentYear", // same name
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

  const { data: studentsResponse, isLoading, error, refetch } = useStudents({
    page: currentPageIndex,
    size: pageSize,
    ...apiParams,
  });

  const students = studentsResponse?.data ?? [];
  const totalElements = studentsResponse?.totalItems ?? 0;
  const totalPages = studentsResponse?.totalPages ?? 0;

  const { table } = useDataTable({
    data: students,
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
    console.error("❌ StudentsTable - Error loading students:", error);
    return (
      <Card className="border-red-200 shadow-lg">
        <CardContent className="pt-6">
          <div className="text-center text-red-600">
            Failed to load students. Please try again.
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
                Students Management
              </CardTitle>
              <CardDescription className="text-blue-700/80 text-lg">
                Manage and organize your student body efficiently
              </CardDescription>
            </div>
            <Button 
              onClick={handleCreate}
              className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300"
            >
              <Plus className="mr-2 h-4 w-4" />
              Add Student
            </Button>
          </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/60 to-indigo-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-blue-900">Total Students</CardTitle>
            <Users className="h-6 w-6 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-blue-700">{totalElements}</div>
            <p className="text-blue-600/70 text-sm mt-1">Active students in system</p>
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
            <p className="text-indigo-600/70 text-sm mt-1">Students selected</p>
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

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Are you absolutely sure?</DialogTitle>
            <DialogDescription>
              This action cannot be undone. This will permanently delete the student{" "}
              <span className="font-semibold">
                {studentToDelete?.firstName} {studentToDelete?.lastName}
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
    </div>
  );
} 