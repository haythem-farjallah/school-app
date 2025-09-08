import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { type Parser, useQueryState, useQueryStates, parseAsInteger, parseAsString, parseAsArrayOf } from "nuqs";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { GraduationCap, Users, BookOpen, Plus } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import { useUserRole } from "@/hooks/useUserRole";
import { getRoleClasses } from "@/lib/theme";

import { useClasses, useDeleteClass } from "../hooks/use-classes";
import { getClassesColumns } from "./class-columns";
import type { Class } from "@/types/class";

export function ClassesTable() {
  const navigate = useNavigate();
  const userRole = useUserRole();
  const deleteMutation = useDeleteClass();
  const { user } = useAuth();
  const roleClasses = getRoleClasses(user?.role);
  
  // Dynamic base path based on user role
  const basePath = userRole?.toLowerCase() === 'staff' ? '/staff' : '/admin';

  // URL pagination parameters (managed by useDataTable)
  const [urlPage] = useQueryState("page", parseAsInteger.withDefault(1));
  const [pageSize] = useQueryState("perPage", parseAsInteger.withDefault(10));
  const currentPageIndex = urlPage - 1;

  // Build filter parsers dynamically - we'll define columns later
  const filterParsers = React.useMemo(() => {
    const parsers: Record<string, Parser<string> | Parser<string[]>> = {};
    // Define the parsers based on known filterable columns
    const filterableColumns = [
      { key: "name", enableColumnFilter: true, hasOptions: false },
      { key: "yearOfStudy", enableColumnFilter: true, hasOptions: true },
      { key: "maxStudents", enableColumnFilter: true, hasOptions: false },
    ];
    
    filterableColumns.forEach((col) => {
      if (col.enableColumnFilter) {
        if (col.hasOptions) {
          // Use array parser for select filters with options
          parsers[col.key] = parseAsArrayOf(parseAsString, ",").withDefault([]).withOptions({ shallow: true });
        } else {
          // Use string parser for text/number filters
          parsers[col.key] = parseAsString.withDefault("").withOptions({ shallow: true });
        }
      }
    });
    return parsers;
  }, []);

  const [filterValues] = useQueryStates(filterParsers);
  
  console.log("🔍 ClassesTable - Filter values:", filterValues);

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      name: "name",
      yearOfStudy: "yearOfStudy",
      maxStudents: "maxStudents",
    };

    const params: Record<string, unknown> = {};

    Object.entries(filterValues).forEach(([key, val]) => {
      if (typeof val === "string" && val.trim()) {
        const backendKey = keyMap[key] ?? key;
        
        // Convert string to number for numeric fields
        if (key === "yearOfStudy") {
          const numVal = parseInt(val.trim());
          if (!isNaN(numVal)) {
            params[backendKey] = numVal;
          }
        } else if (key === "maxStudents") {
          const numVal = parseInt(val.trim());
          if (!isNaN(numVal)) {
            params[backendKey] = numVal;
          }
        } else {
          params[backendKey] = val.trim();
        }
      } else if (Array.isArray(val) && val.length > 0) {
        // Handle faceted filter arrays (from DataTableFacetedFilter)
        const backendKey = keyMap[key] ?? key;
        
        if (key === "yearOfStudy") {
          // Convert array of strings to array of numbers
          const numVals = val.map(v => parseInt(v)).filter(v => !isNaN(v));
          if (numVals.length > 0) {
            params[backendKey] = numVals.length === 1 ? numVals[0] : numVals;
          }
        } else if (key === "maxStudents") {
          // Convert array of strings to array of numbers
          const numVals = val.map(v => parseInt(v)).filter(v => !isNaN(v));
          if (numVals.length > 0) {
            params[backendKey] = numVals.length === 1 ? numVals[0] : numVals;
          }
        } else {
          params[backendKey] = val.length === 1 ? val[0] : val;
        }
      }
    });

    console.log("🔍 ClassesTable - API params:", params);
    return params;
  }, [filterValues]);

  const { data: classesResponse, isLoading, error, refetch } = useClasses({
    page: currentPageIndex,
    size: pageSize,
    ...apiParams,
  });

  const classes = classesResponse?.data ?? [];
  const totalItems = classesResponse?.totalItems ?? 0;
  const totalPages = Math.ceil(totalItems / pageSize);

  const handleView = React.useCallback((classItem: Class) => {
    console.log("👁️ ClassesTable - Viewing class:", classItem);
    navigate(`${basePath}/classes/view/${classItem.id}`);
  }, [navigate, basePath]);

  const handleEdit = React.useCallback((classItem: Class) => {
    console.log("✏️ ClassesTable - Editing class:", classItem);
    // Edit logic is now handled by the EditClassSheet component
  }, []);

  const handleDelete = React.useCallback((classItem: Class) => {
    console.log("🗑️ ClassesTable - Deleting class:", classItem);
    if (window.confirm(`Are you sure you want to delete the class "${classItem.name}"?`)) {
      deleteMutation.mutate(classItem.id, {
        onSuccess: () => {
          toast.success("Class deleted successfully");
          refetch();
        },
        onError: (error) => {
          console.error("❌ ClassesTable - Delete error:", error);
          toast.error("Failed to delete class");
        },
      });
    }
  }, [deleteMutation, refetch]);

  const handleCreate = React.useCallback(() => {
    console.log("➕ ClassesTable - Creating new class");
    navigate(`${basePath}/classes/create`);
  }, [navigate, basePath]);

  const columns = React.useMemo(
    () => getClassesColumns({
      onView: handleView,
      onEdit: handleEdit,
      onDelete: handleDelete,
      onSuccess: () => refetch(),
      roleClasses,
    }),
    [handleView, handleEdit, handleDelete, refetch, roleClasses]
  );

  const { table } = useDataTable({
    data: classes,
    columns,
    pageCount: totalPages,
    initialState: {
      pagination: {
        pageIndex: currentPageIndex,
        pageSize,
      },
    },
  });

  if (isLoading) {
    return <DataTableSkeleton columnCount={5} rowCount={10} />;
  }

  if (error) {
    return (
      <div className="text-center py-8">
        <p className="text-red-500">Error loading classes: {error.message}</p>
        <Button onClick={() => refetch()} variant="outline" className="mt-2">
          Try Again
        </Button>
      </div>
    );
  }

  // Calculate statistics
  const totalStudents = classes.reduce((sum, cls) => sum + (cls.studentIds?.length || 0), 0);
  const averageClassSize = classes.length > 0 ? Math.round(totalStudents / classes.length) : 0;

  return (
    <div className="space-y-6">
      {/* Header with stats cards */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-gray-900">Classes</h2>
          <p className="text-gray-600">
            {user?.role === 'TEACHER' 
              ? 'View your assigned classes and student enrollment' 
              : 'Manage school classes and student enrollment'
            }
          </p>
        </div>
        {user?.role !== 'TEACHER' && (
          <Button onClick={handleCreate} className={roleClasses.button}>
            <Plus className="mr-2 h-4 w-4" />
            Add Class
          </Button>
        )}
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className={`${roleClasses.primaryLight} ${roleClasses.primaryBorder}`}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className={`text-sm font-medium ${roleClasses.primary}`}>{`Total Classes`}</CardTitle>
            <GraduationCap className={`h-4 w-4 ${roleClasses.primary}`} />
          </CardHeader>
          <CardContent>
            <div className={`text-3xl font-bold ${roleClasses.primary}`}>{totalItems}</div>
            <p className={`${roleClasses.primary}/70 text-sm mt-1`}>Active classes in system</p>
          </CardContent>
        </Card>

        <Card className="border-green-200 bg-gradient-to-br from-green-50 to-emerald-50">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-green-800">Total Students</CardTitle>
            <Users className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-green-700">{totalStudents}</div>
            <p className="text-green-600/70 text-sm mt-1">Students enrolled</p>
          </CardContent>
        </Card>

        <Card className="border-purple-200 bg-gradient-to-br from-purple-50 to-violet-50">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-purple-800">Average Class Size</CardTitle>
            <BookOpen className="h-4 w-4 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-purple-700">{averageClassSize}</div>
            <p className="text-purple-600/70 text-sm mt-1">Students per class</p>
          </CardContent>
        </Card>
      </div>

      {/* Data Table */}
      <Card className="border-gray-200 shadow-sm">
        <CardContent className="p-0">
          <DataTableToolbar table={table} />
          <DataTable table={table} />
        </CardContent>
      </Card>
    </div>
  );
} 