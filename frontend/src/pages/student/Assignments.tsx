// Student Assignment Interface
// Interface for students to view and submit assignments

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Helmet } from 'react-helmet-async';
import { 
  BookOpen, 
  Clock, 
  CheckCircle, 
  AlertCircle, 
  Calendar,
  FileText,
  Upload,
  Eye,
  TrendingUp,
  Target
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

import { AssignmentCard } from '@/features/assignments/components/assignment-card';
import { SubmissionForm } from '@/features/assignments/components/submission-form';
import { 
  useAssignmentsByClass,
  useAssignment
} from '@/features/assignments/hooks/use-assignments';
import { 
  useSubmissionsByStudent,
  useSubmitAssignment,
  useUpdateSubmission
} from '@/features/assignments/hooks/use-submissions';
import { useAuth } from '@/hooks/useAuth';
import { cn } from '@/lib/utils';

import type { Assignment, Submission } from '@/types/assignment';

const StudentAssignments = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const studentId = user?.id || '';

  // State management
  const [activeTab, setActiveTab] = useState('all');
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);
  const [isSubmissionDialogOpen, setIsSubmissionDialogOpen] = useState(false);

  // Data fetching - For now, we'll use a mock classId
  // In a real app, this would come from the student's enrollment
  const mockClassId = '1'; // This should be dynamic based on student's classes

  const { 
    data: assignmentsResponse, 
    isLoading: isLoadingAssignments 
  } = useAssignmentsByClass(mockClassId, {
    filters: getTabFilters(activeTab),
    enabled: !!studentId,
  });

  const { 
    data: submissionsResponse 
  } = useSubmissionsByStudent(studentId, {
    enabled: !!studentId,
  });

  const { 
    data: assignmentDetails 
  } = useAssignment(selectedAssignment?.id || '', {
    enabled: !!selectedAssignment?.id,
  });

  // Mutations
  const submitMutation = useSubmitAssignment();
  const updateMutation = useUpdateSubmission();

  const assignments = assignmentsResponse?.assignments || [];
  const submissions = submissionsResponse?.submissions || [];

  // Create a map of submissions by assignment ID
  const submissionMap = submissions.reduce((acc, submission) => {
    acc[submission.assignmentId] = submission;
    return acc;
  }, {} as Record<string, Submission>);

  // Tab filters
  function getTabFilters(tab: string) {
    const now = new Date();
    switch (tab) {
      case 'pending':
        return { 
          status: ['published', 'active'],
          // Filter out assignments that have been submitted
        };
      case 'submitted':
        return { hasSubmissions: true };
      case 'overdue':
        return { 
          status: ['active'],
          dueDateTo: now,
        };
      case 'upcoming':
        return { 
          status: ['published', 'active'],
          dueDateFrom: now,
        };
      default:
        return { status: ['published', 'active', 'closed'] };
    }
  }

  // Filter assignments based on submission status
  const getFilteredAssignments = () => {
    switch (activeTab) {
      case 'pending':
        return assignments.filter(a => !submissionMap[a.id]);
      case 'submitted':
        return assignments.filter(a => submissionMap[a.id]);
      case 'overdue':
        return assignments.filter(a => 
          new Date(a.dueDate) < new Date() && 
          a.status === 'active' && 
          !submissionMap[a.id]
        );
      case 'upcoming':
        return assignments.filter(a => 
          new Date(a.dueDate) > new Date() && 
          ['published', 'active'].includes(a.status)
        );
      default:
        return assignments;
    }
  };

  const filteredAssignments = getFilteredAssignments();

  // Statistics
  const stats = {
    total: assignments.length,
    pending: assignments.filter(a => !submissionMap[a.id] && ['published', 'active'].includes(a.status)).length,
    submitted: assignments.filter(a => submissionMap[a.id]).length,
    overdue: assignments.filter(a => 
      new Date(a.dueDate) < new Date() && 
      a.status === 'active' && 
      !submissionMap[a.id]
    ).length,
    graded: submissions.filter(s => s.grade).length,
  };

  // Event handlers
  const handleAssignmentView = (assignment: Assignment) => {
    setSelectedAssignment(assignment);
    setIsSubmissionDialogOpen(true);
  };

  const handleSubmitAssignment = (assignmentId: string, data: {
    textContent?: string;
    files?: File[];
    urls?: string[];
  }) => {
    submitMutation.mutate({ assignmentId, data }, {
      onSuccess: () => {
        setIsSubmissionDialogOpen(false);
        setSelectedAssignment(null);
      },
    });
  };

  const handleUpdateSubmission = (submissionId: string, data: {
    textContent?: string;
    files?: File[];
    urls?: string[];
  }) => {
    updateMutation.mutate({ id: submissionId, data }, {
      onSuccess: () => {
        setIsSubmissionDialogOpen(false);
        setSelectedAssignment(null);
      },
    });
  };

  const handleFormSubmit = (data: {
    textContent?: string;
    files?: File[];
    urls?: string[];
  }) => {
    if (!selectedAssignment) return;

    const existingSubmission = submissionMap[selectedAssignment.id];
    
    if (existingSubmission) {
      handleUpdateSubmission(existingSubmission.id, data);
    } else {
      handleSubmitAssignment(selectedAssignment.id, data);
    }
  };

  return (
    <>
      <Helmet>
        <title>My Assignments - Student Portal</title>
        <meta name="description" content="View and submit your assignments" />
      </Helmet>

      <div className="container mx-auto p-6 space-y-8">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">My Assignments</h1>
            <p className="text-gray-600 mt-1">
              View and submit your assignments
            </p>
          </div>
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

          <Card className="border-orange-200 bg-gradient-to-br from-orange-50 to-orange-100">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-orange-900">Pending</CardTitle>
              <Clock className="h-4 w-4 text-orange-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-orange-700">{stats.pending}</div>
            </CardContent>
          </Card>

          <Card className="border-green-200 bg-gradient-to-br from-green-50 to-green-100">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-green-900">Submitted</CardTitle>
              <CheckCircle className="h-4 w-4 text-green-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-700">{stats.submitted}</div>
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

          <Card className="border-purple-200 bg-gradient-to-br from-purple-50 to-purple-100">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-purple-900">Graded</CardTitle>
              <Target className="h-4 w-4 text-purple-600" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-purple-700">{stats.graded}</div>
            </CardContent>
          </Card>
        </div>

        {/* Assignment Tabs */}
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="grid w-full grid-cols-5">
            <TabsTrigger value="all">All</TabsTrigger>
            <TabsTrigger value="pending">Pending</TabsTrigger>
            <TabsTrigger value="submitted">Submitted</TabsTrigger>
            <TabsTrigger value="overdue">Overdue</TabsTrigger>
            <TabsTrigger value="upcoming">Upcoming</TabsTrigger>
          </TabsList>

          <TabsContent value={activeTab} className="mt-6">
            {isLoadingAssignments ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {Array.from({ length: 6 }).map((_, index) => (
                  <Card key={index} className="animate-pulse">
                    <CardHeader>
                      <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                      <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-2">
                        <div className="h-3 bg-gray-200 rounded"></div>
                        <div className="h-3 bg-gray-200 rounded w-2/3"></div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            ) : filteredAssignments.length === 0 ? (
              <Card>
                <CardContent className="p-12 text-center">
                  <BookOpen className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">No assignments found</h3>
                  <p className="text-gray-600">
                    {activeTab === 'pending' && 'You have no pending assignments.'}
                    {activeTab === 'submitted' && 'You have not submitted any assignments yet.'}
                    {activeTab === 'overdue' && 'You have no overdue assignments.'}
                    {activeTab === 'upcoming' && 'You have no upcoming assignments.'}
                    {activeTab === 'all' && 'No assignments available at this time.'}
                  </p>
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredAssignments.map((assignment) => {
                  const submission = submissionMap[assignment.id];
                  const isSubmitted = !!submission;
                  const isOverdue = new Date(assignment.dueDate) < new Date() && !isSubmitted;
                  
                  return (
                    <div key={assignment.id} className="relative">
                      <AssignmentCard
                        assignment={assignment}
                        variant="detailed"
                        showActions={false}
                        showTeacher={true}
                        showCourse={true}
                        onView={handleAssignmentView}
                        className={cn(
                          'cursor-pointer transition-all duration-200 hover:shadow-lg',
                          isSubmitted && 'ring-2 ring-green-200',
                          isOverdue && 'ring-2 ring-red-200'
                        )}
                      />
                      
                      {/* Status Overlay */}
                      <div className="absolute top-3 right-3">
                        {isSubmitted ? (
                          <Badge className="bg-green-500 hover:bg-green-600">
                            <CheckCircle className="mr-1 h-3 w-3" />
                            Submitted
                          </Badge>
                        ) : isOverdue ? (
                          <Badge variant="destructive">
                            <AlertCircle className="mr-1 h-3 w-3" />
                            Overdue
                          </Badge>
                        ) : (
                          <Badge variant="outline">
                            <Clock className="mr-1 h-3 w-3" />
                            Pending
                          </Badge>
                        )}
                      </div>

                      {/* Grade Display */}
                      {submission?.grade && (
                        <div className="absolute bottom-3 right-3">
                          <Badge className="bg-purple-500 hover:bg-purple-600">
                            {submission.grade.score}/{submission.grade.maxScore}
                          </Badge>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </TabsContent>
        </Tabs>

        {/* Submission Dialog */}
        <Dialog open={isSubmissionDialogOpen} onOpenChange={setIsSubmissionDialogOpen}>
          <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>
                {selectedAssignment?.title}
              </DialogTitle>
              <DialogDescription>
                {submissionMap[selectedAssignment?.id || ''] 
                  ? 'Update your submission' 
                  : 'Submit your assignment'
                }
              </DialogDescription>
            </DialogHeader>
            
            {selectedAssignment && (
              <SubmissionForm
                assignment={selectedAssignment}
                existingSubmission={submissionMap[selectedAssignment.id]}
                onSubmit={handleFormSubmit}
                isLoading={submitMutation.isPending || updateMutation.isPending}
              />
            )}
          </DialogContent>
        </Dialog>
      </div>
    </>
  );
};

export default StudentAssignments; 