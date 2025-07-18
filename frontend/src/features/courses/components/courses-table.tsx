import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { type Parser, useQueryState, useQueryStates, parseAsInteger, parseAsString } from "nuqs";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { BookOpen, Users, TrendingUp, Plus } from "lucide-react";

import { useCourses, useDeleteCourse } from "../hooks/use-courses";
import { getCoursesColumns } from "./course-columns";
import type { Course } from "@/types/course";

export function CoursesTable() {
  const navigate = useNavigate();
  const deleteMutation = useDeleteCourse();

  // URL pagination parameters (managed by useDataTable)
  const [urlPage] = useQueryState("page", parseAsInteger.withDefault(1));
  const [pageSize] = useQueryState("perPage", parseAsInteger.withDefault(10));
  const currentPageIndex = urlPage - 1;

  // Build filter parsers dynamically - we'll define columns later
  const filterParsers = React.useMemo(() => {
    const parsers: Record<string, Parser<string>> = {};
    // Define the parsers based on known filterable columns
    const filterableColumns = [
      { key: "name", enableColumnFilter: true },
      { key: "credit", enableColumnFilter: true },
      { key: "weeklyCapacity", enableColumnFilter: true },
      { key: "teacherId", enableColumnFilter: true },
    ];
    
    filterableColumns.forEach((col) => {
      if (col.enableColumnFilter) {
        parsers[col.key] = parseAsString.withDefault("").withOptions({ shallow: true });
      }
    });
    return parsers;
  }, []);

  const [filterValues] = useQueryStates(filterParsers);

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      name: "nameLike",
      credit: "credit",
      weeklyCapacity: "weeklyCapacity", 
      teacherId: "teacherId",
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

  const { data: coursesResponse, isLoading, error, refetch } = useCourses({
    page: currentPageIndex,
    size: pageSize,
    ...apiParams,
  });

  const courses = coursesResponse?.data ?? [];
  const totalItems = coursesResponse?.totalItems ?? 0;
  const totalPages = Math.ceil(totalItems / pageSize);

  const handleView = React.useCallback((course: Course) => {
    console.log("👁️ CoursesTable - Viewing course:", course);
    navigate(`/admin/courses/view/${course.id}`);
  }, [navigate]);

  const handleEdit = React.useCallback((course: Course) => {
    console.log("✏️ CoursesTable - Editing course:", course);
    // Edit logic is now handled by the EditCourseSheet component
  }, []);

  const handleDelete = React.useCallback((course: Course) => {
    console.log("🗑️ CoursesTable - Deleting course:", course);
    if (window.confirm(`Are you sure you want to delete the course "${course.name}"?`)) {
      deleteMutation.mutate(course.id, {
        onSuccess: () => {
          toast.success("Course deleted successfully");
          refetch();
        },
        onError: (error) => {
          console.error("❌ CoursesTable - Delete error:", error);
          toast.error("Failed to delete course");
        },
      });
    }
  }, [deleteMutation, refetch]);

  const handleCreate = React.useCallback(() => {
    console.log("➕ CoursesTable - Creating new course");
    navigate("/admin/courses/create");
  }, [navigate]);

  const columns = React.useMemo(
    () => getCoursesColumns({
      onView: handleView,
      onEdit: handleEdit,
      onDelete: handleDelete,
      onSuccess: () => refetch(),
    }),
    [handleView, handleEdit, handleDelete, refetch]
  );

  const { table } = useDataTable({
    data: courses,
    columns,
    pageCount: totalPages,
    initialState: {
      pagination: {
        pageIndex: currentPageIndex,
        pageSize,
      },
    },
  });

  if (error) {
    console.error("❌ CoursesTable - Error loading courses:", error);
    return (
      <Card className="border-red-200 shadow-lg">
        <CardContent className="pt-6">
          <div className="text-center text-red-600">
            Failed to load courses. Please try again.
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
                Courses Management
              </CardTitle>
              <CardDescription className="text-blue-700/80 text-lg">
                Manage and organize your courses efficiently
              </CardDescription>
            </div>
            <Button
              onClick={handleCreate}
              className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105"
            >
              <Plus className="mr-2 h-4 w-4" />
              Create Course
            </Button>
          </div>
        </CardHeader>
      </Card>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/60 to-indigo-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-blue-900">Total Courses</CardTitle>
            <BookOpen className="h-6 w-6 text-blue-600" />
          </CardHeader>
          <CardContent>
                            <div className="text-3xl font-bold text-blue-700">{totalItems}</div>
            <p className="text-blue-600/70 text-sm mt-1">Active courses in system</p>
          </CardContent>
        </Card>
        
        <Card className="border-indigo-200/60 bg-gradient-to-br from-indigo-50/60 to-purple-50/30 hover:shadow-lg transition-shadow duration-300">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-lg font-semibold text-indigo-900">Selected</CardTitle>
            <Users className="h-6 w-6 text-indigo-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-indigo-700">
              {table.getFilteredSelectedRowModel().rows.length}
            </div>
            <p className="text-indigo-600/70 text-sm mt-1">Courses selected</p>
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
              <DataTableSkeleton columnCount={6} rowCount={pageSize} />
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