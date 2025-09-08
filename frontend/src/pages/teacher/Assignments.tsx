// Teacher Assignment Management Page
// Comprehensive interface for teachers to manage their assignments

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { 
  Plus, 
  BookOpen, 
  Users, 
  Calendar, 
  TrendingUp,
  Clock,
  CheckCircle,
  AlertCircle,
  FileText,
  BarChart3
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { 
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

import { AssignmentList } from '@/features/assignments/components/assignment-list';
import { AssignmentForm } from '@/features/assignments/components/assignment-form';
import { 
  useAssignmentsByTeacher,
  useCreateAssignment,
  useUpdateAssignment,
  useDeleteAssignment,
  usePublishAssignment,
  useCloseAssignment,
  useArchiveAssignment,
  useBulkAssignmentOperation
} from '@/features/assignments/hooks/use-assignments';
import { useClasses } from '@/features/classes/hooks/use-classes';
import { useCourses } from '@/features/courses/hooks/use-courses';
import { useAuth } from '@/hooks/useAuth';
import { getRoleClasses } from '@/lib/theme';

import type { 
  Assignment, 
  CreateAssignmentRequest, 
  UpdateAssignmentRequest,
  AssignmentFilters 
} from '@/types/assignment';

const TeacherAssignments = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const teacherId = user?.id || '';
  const roleClasses = getRoleClasses(user?.role);

  // State management
  const [activeTab, setActiveTab] = useState('all');
  const [filters, setFilters] = useState<AssignmentFilters>({});
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  // Data fetching
  const { 
    data: assignmentsResponse, 
    isLoading: isLoadingAssignments,
    refetch: refetchAssignments 
  } = useAssignmentsByTeacher(teacherId, {
    filters: { ...filters, ...getTabFilters(activeTab) },
    enabled: !!teacherId,
  });

  const { data: classesResponse } = useClasses({ size: 100 });
  const { data: coursesResponse } = useCourses({ size: 100 });

  // Mutations
  const createMutation = useCreateAssignment();
  const updateMutation = useUpdateAssignment();
  const deleteMutation = useDeleteAssignment();
  const publishMutation = usePublishAssignment();
  const closeMutation = useCloseAssignment();
  const archiveMutation = useArchiveAssignment();
  const bulkMutation = useBulkAssignmentOperation();

  const assignments = assignmentsResponse?.assignments || [];
  const totalCount = assignmentsResponse?.totalCount || 0;
  const classes = classesResponse?.data || [];
  const courses = coursesResponse?.data || [];

  // Tab filters
  function getTabFilters(tab: string): AssignmentFilters {
    switch (tab) {
      case 'active':
        return { status: ['published', 'active'] };
      case 'draft':
        return { status: ['draft'] };
      case 'closed':
        return { status: ['closed'] };
      case 'overdue':
        return { isOverdue: true };
      case 'needs-grading':
        return { needsGrading: true };
      default:
        return {};
    }
  }

  // Statistics
  const stats = {
    total: totalCount,
    active: assignments.filter(a => ['published', 'active'].includes(a.status)).length,
    draft: assignments.filter(a => a.status === 'draft').length,
    needsGrading: assignments.filter(a => a.submissionCount > a.gradedCount).length,
    overdue: assignments.filter(a => new Date(a.dueDate) < new Date() && a.status === 'active').length,
  };

  // Event handlers
  const handleCreateAssignment = (data: CreateAssignmentRequest) => {
    createMutation.mutate(data, {
      onSuccess: () => {
        setIsCreateDialogOpen(false);
        refetchAssignments();
      },
    });
  };

  const handleUpdateAssignment = (data: UpdateAssignmentRequest) => {
    updateMutation.mutate(data, {
      onSuccess: () => {
        setIsEditDialogOpen(false);
        setSelectedAssignment(null);
        refetchAssignments();
      },
    });
  };

  const handleDeleteAssignment = () => {
    if (selectedAssignment) {
      deleteMutation.mutate(selectedAssignment.id, {
        onSuccess: () => {
          setIsDeleteDialogOpen(false);
          setSelectedAssignment(null);
          refetchAssignments();
        },
      });
    }
  };

  const handleAssignmentView = (assignment: Assignment) => {
    navigate(`/teacher/assignments/${assignment.id}`);
  };

  const handleAssignmentEdit = (assignment: Assignment) => {
    setSelectedAssignment(assignment);
    setIsEditDialogOpen(true);
  };

  const handleAssignmentDelete = (assignment: Assignment) => {
    setSelectedAssignment(assignment);
    setIsDeleteDialogOpen(true);
  };

  const handleAssignmentPublish = (assignment: Assignment) => {
    publishMutation.mutate(assignment.id, {
      onSuccess: () => refetchAssignments(),
    });
  };

  const handleAssignmentClose = (assignment: Assignment) => {
    closeMutation.mutate(assignment.id, {
      onSuccess: () => refetchAssignments(),
    });
  };

  const handleAssignmentArchive = (assignment: Assignment) => {
    archiveMutation.mutate(assignment.id, {
      onSuccess: () => refetchAssignments(),
    });
  };

  const handleBulkAction = (action: string, assignmentIds: string[]) => {
    bulkMutation.mutate({
      operation: action as any,
      assignmentIds,
    }, {
      onSuccess: () => refetchAssignments(),
    });
  };

  return (
    <>
      <Helmet>
        <title>My Assignments - Teacher Portal</title>
        <meta name="description" content="Manage your assignments, track submissions, and grade student work" />
      </Helmet>

      <div className="container mx-auto p-6 space-y-8">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">My Assignments</h1>
            <p className="text-gray-600 mt-1">
              Create, manage, and track your assignments
            </p>
          </div>
          
          <Button onClick={() => setIsCreateDialogOpen(true)} className={roleClasses.button}>
            <Plus className="mr-2 h-4 w-4" />
            Create Assignment
          </Button>
        </div>

        {/* Statistics Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
          <Card className="border-blue-200 bg-gradient-to-br from-blue-50 to-blue-100">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-blue-900">Total Assignments</CardTitle>
              <BookOpen className="h-4 w-4 text-blue-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-700">{stats.total}</div>
            </CardContent>
          </Card>

          <Card className="border-green-200 bg-gradient-to-br from-green-50 to-green-100">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-green-900">Active</CardTitle>
              <CheckCircle className="h-4 w-4 text-green-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-700">{stats.active}</div>
            </CardContent>
          </Card>

          <Card className="border-yellow-200 bg-gradient-to-br from-yellow-50 to-yellow-100">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-yellow-900">Drafts</CardTitle>
              <FileText className="h-4 w-4 text-yellow-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-yellow-700">{stats.draft}</div>
            </CardContent>
          </Card>

          <Card className="border-purple-200 bg-gradient-to-br from-purple-50 to-purple-100">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-purple-900">Need Grading</CardTitle>
              <BarChart3 className="h-4 w-4 text-purple-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-purple-700">{stats.needsGrading}</div>
            </CardContent>
          </Card>

          <Card className="border-red-200 bg-gradient-to-br from-red-50 to-red-100">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-red-900">Overdue</CardTitle>
              <AlertCircle className="h-4 w-4 text-red-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-red-700">{stats.overdue}</div>
            </CardContent>
          </Card>
        </div>

        {/* Assignment Tabs */}
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-6">
            <TabsTrigger value="all">All</TabsTrigger>
            <TabsTrigger value="active">Active</TabsTrigger>
            <TabsTrigger value="draft">Drafts</TabsTrigger>
            <TabsTrigger value="closed">Closed</TabsTrigger>
            <TabsTrigger value="overdue">Overdue</TabsTrigger>
            <TabsTrigger value="needs-grading">Need Grading</TabsTrigger>
          </TabsList>

          <TabsContent value={activeTab} className="mt-6">
            <AssignmentList
              assignments={assignments}
              totalCount={totalCount}
              isLoading={isLoadingAssignments}
              filters={filters}
              onFiltersChange={setFilters}
              onAssignmentView={handleAssignmentView}
              onAssignmentEdit={handleAssignmentEdit}
              onAssignmentDelete={handleAssignmentDelete}
              onAssignmentPublish={handleAssignmentPublish}
              onAssignmentClose={handleAssignmentClose}
              onAssignmentArchive={handleAssignmentArchive}
              onBulkAction={handleBulkAction}
              onCreateNew={() => setIsCreateDialogOpen(true)}
              showBulkActions={true}
              showCreateButton={false} // We have it in the header
              showFilters={true}
            />
          </TabsContent>
        </Tabs>

        {/* Create Assignment Dialog */}
        <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Create New Assignment</DialogTitle>
              <DialogDescription>
                Create a new assignment for your students
              </DialogDescription>
            </DialogHeader>
            
            <AssignmentForm
              courses={courses.map(c => ({ id: c.id.toString(), name: c.name }))}
              classes={classes.map(c => ({ id: c.id.toString(), name: c.name }))}
              onSubmit={handleCreateAssignment}
              onCancel={() => setIsCreateDialogOpen(false)}
              isLoading={createMutation.isPending}
            />
          </DialogContent>
        </Dialog>

        {/* Edit Assignment Dialog */}
        <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Edit Assignment</DialogTitle>
              <DialogDescription>
                Update assignment details and settings
              </DialogDescription>
            </DialogHeader>
            
            {selectedAssignment && (
              <AssignmentForm
                assignment={selectedAssignment}
                courses={courses.map(c => ({ id: c.id.toString(), name: c.name }))}
                classes={classes.map(c => ({ id: c.id.toString(), name: c.name }))}
                onSubmit={handleUpdateAssignment}
                onCancel={() => {
                  setIsEditDialogOpen(false);
                  setSelectedAssignment(null);
                }}
                isLoading={updateMutation.isPending}
              />
            )}
          </DialogContent>
        </Dialog>

        {/* Delete Confirmation Dialog */}
        <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Delete Assignment</DialogTitle>
              <DialogDescription>
                Are you sure you want to delete this assignment? This action cannot be undone.
                {selectedAssignment && (
                  <div className="mt-2 p-3 bg-gray-50 rounded-md">
                    <p className="font-medium">{selectedAssignment.title}</p>
                    <p className="text-sm text-gray-600">{selectedAssignment.courseName}</p>
                  </div>
                )}
              </DialogDescription>
            </DialogHeader>
            
            <div className="flex justify-end space-x-2 mt-4">
              <Button
                variant="outline"
                onClick={() => {
                  setIsDeleteDialogOpen(false);
                  setSelectedAssignment(null);
                }}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleDeleteAssignment}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Delete Assignment'}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>
    </>
  );
};

export default TeacherAssignments; 