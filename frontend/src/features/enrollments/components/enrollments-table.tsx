import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { useQueryStates, parseAsInteger, parseAsString, parseAsArrayOf } from "nuqs";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Users, BookOpen, TrendingUp, Plus, Zap, Clock } from "lucide-react";

import { useEnrollments } from "../hooks/use-enrollments";
import { getEnrollmentColumns } from "./enrollment-columns";
import { http } from "@/lib/http";
// import { useToast } from "@/hooks/use-toast"; // Using react-hot-toast instead

// Filter parsers
const filterParsers = {
  search: parseAsString.withDefault(""),
  status: parseAsArrayOf(parseAsString).withDefault([]),
  page: parseAsInteger.withDefault(0),
};

export function EnrollmentsTable() {
  const navigate = useNavigate();
  const [filterValues] = useQueryStates(filterParsers);
  const [autoEnrollLoading, setAutoEnrollLoading] = React.useState(false);
  
  console.log("🔍 EnrollmentsTable - Filter values:", filterValues);

  // Map filter values to API parameters
  const apiParams = React.useMemo(() => {
    const params: Record<string, unknown> = {};

    // Add search if provided
    if (filterValues.search && filterValues.search.trim()) {
      params.search = filterValues.search.trim();
    }

    // Add status filter (convert array to single value for now)
    if (filterValues.status && filterValues.status.length > 0) {
      // For now, take the first status. Backend supports single status filter
      params.status = filterValues.status[0];
    }

    console.log("🔍 EnrollmentsTable - API params:", params);
    return params;
  }, [filterValues]);

  const {
    data: enrollmentPage,
    isLoading,
    error,
    refetch,
  } = useEnrollments({
    page: filterValues.page,
    size: 10,
    ...apiParams,
  });

  // Extract the actual array and total from the paginated response
  const enrollments = enrollmentPage?.data || [];
  const total = enrollmentPage?.totalItems || 0;

  const columns = React.useMemo(
    () => getEnrollmentColumns({
      onEdit: (enrollment) => {
        console.log("Edit enrollment:", enrollment);
        // Edit handled by sheet in actions
      },
      onView: (enrollment) => {
        navigate(`/admin/enrollments/view/${enrollment.id}`);
      },
      onTransfer: (enrollment) => {
        console.log("Transfer enrollment:", enrollment);
        toast.success("Transfer functionality coming soon");
      },
      onDrop: (enrollment) => {
        console.log("Drop enrollment:", enrollment);
        // Drop handled by actions component
      },
      onSuccess: () => {
        refetch();
      },
    }),
    [navigate, refetch]
  );

  const { table } = useDataTable({
    data: enrollments || [],
    columns,
    pageCount: Math.ceil((total || 0) / 10),
  });

  const handleCreateNew = () => {
    navigate("/admin/enrollments/create");
  };

  const handleAutoEnrollment = async () => {
    setAutoEnrollLoading(true);
    try {
      const response = await http.post('/v1/enrollments/auto-enroll');
      const result = response.data?.data;
      
      if (!result) {
        throw new Error('No data received from auto-enrollment API');
      }
      
      toast.success(
        `Auto-Enrollment Complete! Successfully enrolled ${result.studentsEnrolled || 0} students into ${result.classesUsed || 0} classes (${result.classesCreated || 0} new classes created)`
      );
      
      // Refresh the enrollments table
      refetch();
    } catch (error: unknown) {
      console.error('Auto-enrollment error:', error);
      const errorMessage = error instanceof Error ? error.message : 
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message || "Failed to auto-enroll students";
      toast.error(errorMessage);
    } finally {
      setAutoEnrollLoading(false);
    }
  };

  // Calculate statistics
  const stats = React.useMemo(() => {
    if (!enrollments) return null;

    const totalEnrollments = enrollments.length;
    const activeEnrollments = enrollments.filter(e => e.status === "ACTIVE").length;
    const completedEnrollments = enrollments.filter(e => e.status === "COMPLETED").length;
    const averageGrade = enrollments
      .filter(e => e.finalGrad !== null && e.finalGrad !== undefined)
      .reduce((sum, e) => sum + (e.finalGrad || 0), 0) / 
      enrollments.filter(e => e.finalGrad !== null && e.finalGrad !== undefined).length || 0;

    return {
      totalEnrollments,
      activeEnrollments,
      completedEnrollments,
      averageGrade,
    };
  }, [enrollments]);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold tracking-tight">Enrollments</h1>
        </div>
        <DataTableSkeleton
          columnCount={7}
          cellWidths={["10rem", "15rem", "12rem", "8rem", "8rem", "10rem", "8rem"]}
          shrinkZero
        />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold tracking-tight">Enrollments</h1>
        </div>
        <div className="flex items-center justify-center h-64">
          <div className="text-center">
            <p className="text-gray-500 mb-4">Failed to load enrollments</p>
            <Button onClick={() => refetch()}>Try Again</Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Enrollments</h1>
          <p className="text-muted-foreground">
            Manage student enrollments and track academic progress
          </p>
        </div>
        <div className="flex gap-2">
          <Button 
            onClick={handleAutoEnrollment} 
            disabled={autoEnrollLoading}
            className="bg-yellow-600 hover:bg-yellow-700"
          >
            {autoEnrollLoading ? (
              <>
                <Clock className="mr-2 h-4 w-4 animate-spin" />
                Auto-Enrolling...
              </>
            ) : (
              <>
                <Zap className="mr-2 h-4 w-4" />
                Auto-Enrollment
              </>
            )}
          </Button>
          <Button onClick={handleCreateNew} className="bg-blue-600 hover:bg-blue-700">
            <Plus className="mr-2 h-4 w-4" />
            New Enrollment
          </Button>
        </div>
      </div>

      {/* Statistics Cards */}
      {stats && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Enrollments</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stats.totalEnrollments}</div>
              <p className="text-xs text-muted-foreground">
                All student enrollments
              </p>
            </CardContent>
          </Card>
          
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Active Enrollments</CardTitle>
              <BookOpen className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">{stats.activeEnrollments}</div>
              <p className="text-xs text-muted-foreground">
                Currently active students
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Completed</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-600">{stats.completedEnrollments}</div>
              <p className="text-xs text-muted-foreground">
                Successfully completed
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Average Grade</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-purple-600">
                {stats.averageGrade ? `${stats.averageGrade.toFixed(1)}/20` : "N/A"}
              </div>
              <p className="text-xs text-muted-foreground">
                Overall performance
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      <DataTable table={table}>
        <DataTableToolbar table={table}>
          <div className="ml-auto hidden lg:flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleAutoEnrollment}
              disabled={autoEnrollLoading}
              className="h-8 bg-yellow-50 hover:bg-yellow-100 text-yellow-700 border-yellow-200"
            >
              {autoEnrollLoading ? (
                <>
                  <Clock className="mr-2 h-3 w-3 animate-spin" />
                  Auto-Enrolling...
                </>
              ) : (
                <>
                  <Zap className="mr-2 h-3 w-3" />
                  Auto-Enrollment
                </>
              )}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleCreateNew}
              className="h-8"
            >
              <Plus className="mr-2 h-4 w-4" />
              New Enrollment
            </Button>
          </div>
        </DataTableToolbar>
      </DataTable>
    </div>
  );
} 