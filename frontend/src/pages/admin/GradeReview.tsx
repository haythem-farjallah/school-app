import React, { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { 
  Users, 
  GraduationCap, 
  CheckCircle, 
  AlertCircle,
  FileText,
  Download,
  Eye,
  Award,
  TrendingUp,
  Clock
} from 'lucide-react';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Progress } from '@/components/ui/progress';
import { Alert, AlertDescription } from '@/components/ui/alert';

import { useAuth } from '@/lib/auth';
import { 
  useStaffGradeReviews,
  useApproveGrades,
  useExportGradeSheet
} from '@/features/grades/hooks/use-grades';
import { useClasses } from '@/features/classes/hooks/use-classes';
import { 
  Semester, 
  type StaffGradeReview,
  getGradeLevel,
  getGradeLevelColor 
} from '@/types/grade';
import toast from 'react-hot-toast';

const StaffGradeReview = () => {
  const { user } = useAuth();
  
  // State management
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<Semester>(Semester.FIRST);
  const [approvalDialogOpen, setApprovalDialogOpen] = useState(false);
  const [selectedStudents, setSelectedStudents] = useState<number[]>([]);
  const [viewStudentId, setViewStudentId] = useState<number | null>(null);

  // Data fetching
  const { data: classes } = useClasses({ size: 100 });
  const { 
    data: gradeReviews, 
    isLoading: reviewsLoading,
    refetch: refetchReviews 
  } = useStaffGradeReviews(selectedClassId || undefined, selectedSemester);

  // Mutations
  const approveGradesMutation = useApproveGrades();
  const exportGradeSheetMutation = useExportGradeSheet();

  const selectedClass = classes?.data.find(c => c.id === selectedClassId);

  // Handle approval
  const handleApproveGrades = async () => {
    if (selectedStudents.length === 0) {
      toast.error('Please select students to approve');
      return;
    }

    try {
      await approveGradesMutation.mutateAsync({
        studentIds: selectedStudents,
        semester: selectedSemester,
        approvedBy: `${user?.firstName} ${user?.lastName}`
      });
      
      toast.success(`Approved grades for ${selectedStudents.length} students`);
      setApprovalDialogOpen(false);
      setSelectedStudents([]);
      refetchReviews();
    } catch (error) {
      console.error('Failed to approve grades:', error);
      toast.error('Failed to approve grades. Please try again.');
    }
  };

  // Handle export
  const handleExportGradeSheet = async (studentId: number) => {
    try {
      const blob = await exportGradeSheetMutation.mutateAsync({
        studentId,
        semester: selectedSemester
      });
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `grade-sheet-${studentId}-${selectedSemester}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success('Grade sheet exported successfully');
    } catch (error) {
      console.error('Failed to export grade sheet:', error);
      toast.error('Failed to export grade sheet. Please try again.');
    }
  };

  // Calculate statistics
  const calculateClassStats = () => {
    if (!gradeReviews || gradeReviews.length === 0) {
      return { totalStudents: 0, approvedCount: 0, pendingCount: 0, averageGrade: 0 };
    }

    const totalStudents = gradeReviews.length;
    const approvedCount = gradeReviews.filter(review => review.isApproved).length;
    const pendingCount = totalStudents - approvedCount;
    const averageGrade = gradeReviews.reduce((sum, review) => sum + review.overallAverage, 0) / totalStudents;

    return { totalStudents, approvedCount, pendingCount, averageGrade };
  };

  const stats = calculateClassStats();

  return (
    <>
      <Helmet>
        <title>Grade Review & Approval - Staff Portal</title>
        <meta name="description" content="Review and approve student grades" />
      </Helmet>

      <div className="container mx-auto p-6 space-y-8">
        {/* Header */}
        <Card className="border-green-200/60 bg-gradient-to-br from-green-50/80 via-emerald-50/40 to-teal-50/20 shadow-xl backdrop-blur-sm">
          <CardHeader className="pb-6">
            <div className="flex items-center justify-between">
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-green-900 via-emerald-800 to-teal-700 bg-clip-text text-transparent">
                  Grade Review & Approval
                </CardTitle>
                <CardDescription className="text-green-700/80 text-lg">
                  Review student grades and approve final grade sheets
                </CardDescription>
              </div>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setApprovalDialogOpen(true)}
                  disabled={selectedStudents.length === 0}
                  className="border-emerald-300 text-emerald-700 hover:bg-emerald-50"
                >
                  <CheckCircle className="mr-2 h-4 w-4" />
                  Approve Selected ({selectedStudents.length})
                </Button>
              </div>
            </div>
          </CardHeader>
        </Card>

        {/* Class and Semester Selection */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <GraduationCap className="h-5 w-5 text-green-600" />
              Select Class & Semester
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Class</label>
                <Select
                  value={selectedClassId?.toString() || ''}
                  onValueChange={(value) => {
                    setSelectedClassId(parseInt(value));
                    setSelectedStudents([]);
                  }}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select class" />
                  </SelectTrigger>
                  <SelectContent>
                    {classes?.data.map((classItem) => (
                      <SelectItem key={classItem.id} value={classItem.id.toString()}>
                        {classItem.name} - Year {classItem.yearOfStudy}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium">Semester</label>
                <Select value={selectedSemester} onValueChange={(value: Semester) => setSelectedSemester(value)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={Semester.FIRST}>First Semester</SelectItem>
                    <SelectItem value={Semester.SECOND}>Second Semester</SelectItem>
                    <SelectItem value={Semester.THIRD}>Third Semester</SelectItem>
                    <SelectItem value={Semester.FINAL}>Final</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {selectedClass && (
              <Alert>
                <GraduationCap className="h-4 w-4" />
                <AlertDescription>
                  <strong>{selectedClass.name}</strong> - Year {selectedClass.yearOfStudy} | 
                  Capacity: {selectedClass.capacity} students
                </AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>

        {/* Statistics */}
        {gradeReviews && gradeReviews.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Total Students</CardTitle>
                <Users className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stats.totalStudents}</div>
                <p className="text-xs text-muted-foreground">
                  In {selectedClass?.name}
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Approved</CardTitle>
                <CheckCircle className="h-4 w-4 text-green-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-green-600">{stats.approvedCount}</div>
                <Progress value={(stats.approvedCount / stats.totalStudents) * 100} className="mt-2" />
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Pending</CardTitle>
                <Clock className="h-4 w-4 text-orange-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-orange-600">{stats.pendingCount}</div>
                <p className="text-xs text-muted-foreground">
                  Need approval
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">Class Average</CardTitle>
                <Award className="h-4 w-4 text-blue-600" />
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-blue-600">{stats.averageGrade.toFixed(1)}%</div>
                <p className="text-xs text-muted-foreground">
                  Overall performance
                </p>
              </CardContent>
            </Card>
          </div>
        )}

        {/* Grade Reviews Table */}
        {gradeReviews && gradeReviews.length > 0 && (
          <Card className="shadow-lg">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FileText className="h-5 w-5 text-emerald-600" />
                Student Grade Reviews - {selectedClass?.name}
              </CardTitle>
              <CardDescription>
                Review and approve grades for {selectedSemester} semester
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left p-4 font-medium">
                        <input
                          type="checkbox"
                          checked={selectedStudents.length === gradeReviews.length}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedStudents(gradeReviews.map(r => r.studentId));
                            } else {
                              setSelectedStudents([]);
                            }
                          }}
                          className="rounded"
                        />
                      </th>
                      <th className="text-left p-4 font-medium">Student</th>
                      <th className="text-left p-4 font-medium">Overall Average</th>
                      <th className="text-left p-4 font-medium">Class Rank</th>
                      <th className="text-left p-4 font-medium">Attendance</th>
                      <th className="text-left p-4 font-medium">Status</th>
                      <th className="text-left p-4 font-medium">Subjects</th>
                      <th className="text-left p-4 font-medium">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {gradeReviews.map((review) => {
                      const letterGrade = getGradeLevel(review.overallAverage);
                      const gradeColor = getGradeLevelColor(review.overallAverage);
                      const needsReview = review.subjects.some(s => s.needsReview);

                      return (
                        <tr key={review.studentId} className="border-b hover:bg-gray-50">
                          <td className="p-4">
                            <input
                              type="checkbox"
                              checked={selectedStudents.includes(review.studentId)}
                              onChange={(e) => {
                                if (e.target.checked) {
                                  setSelectedStudents(prev => [...prev, review.studentId]);
                                } else {
                                  setSelectedStudents(prev => prev.filter(id => id !== review.studentId));
                                }
                              }}
                              className="rounded"
                            />
                          </td>
                          <td className="p-4">
                            <div>
                              <div className="font-medium">{review.studentFirstName} {review.studentLastName}</div>
                              <div className="text-sm text-gray-500">ID: {review.studentId}</div>
                            </div>
                          </td>
                          <td className="p-4">
                            <div className="flex items-center gap-2">
                              <span className={`text-lg font-bold ${gradeColor}`}>
                                {review.overallAverage.toFixed(1)}%
                              </span>
                              <Badge variant={letterGrade === 'A' || letterGrade === 'B' ? 'default' : 'destructive'}>
                                {letterGrade}
                              </Badge>
                            </div>
                          </td>
                          <td className="p-4">
                            <Badge variant="outline">
                              #{review.classRank}
                            </Badge>
                          </td>
                          <td className="p-4">
                            <Badge 
                              variant={review.attendanceRate >= 80 ? "default" : "destructive"}
                              className="text-xs"
                            >
                              {review.attendanceRate.toFixed(1)}%
                            </Badge>
                          </td>
                          <td className="p-4">
                            <div className="flex items-center gap-2">
                              {review.isApproved ? (
                                <Badge variant="default" className="bg-green-100 text-green-800">
                                  <CheckCircle className="w-3 h-3 mr-1" />
                                  Approved
                                </Badge>
                              ) : (
                                <Badge variant="secondary" className="bg-orange-100 text-orange-800">
                                  <Clock className="w-3 h-3 mr-1" />
                                  Pending
                                </Badge>
                              )}
                              {needsReview && (
                                <Badge variant="destructive" className="text-xs">
                                  <AlertCircle className="w-3 h-3 mr-1" />
                                  Review
                                </Badge>
                              )}
                            </div>
                          </td>
                          <td className="p-4">
                            <div className="text-sm">
                              {review.subjects.length} subjects
                              {needsReview && (
                                <div className="text-red-600 text-xs">
                                  {review.subjects.filter(s => s.needsReview).length} need review
                                </div>
                              )}
                            </div>
                          </td>
                          <td className="p-4">
                            <div className="flex gap-2">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setViewStudentId(review.studentId)}
                              >
                                <Eye className="w-4 h-4" />
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleExportGradeSheet(review.studentId)}
                                disabled={exportGradeSheetMutation.isPending}
                              >
                                <Download className="w-4 h-4" />
                              </Button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Loading State */}
        {reviewsLoading && (
          <Card>
            <CardContent className="p-8 text-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading grade reviews...</p>
            </CardContent>
          </Card>
        )}

        {/* Empty State */}
        {!reviewsLoading && selectedClassId && (!gradeReviews || gradeReviews.length === 0) && (
          <Card>
            <CardContent className="p-8 text-center">
              <FileText className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No Grade Reviews</h3>
              <p className="text-gray-600">No grades found for the selected class and semester.</p>
            </CardContent>
          </Card>
        )}

        {/* Approval Dialog */}
        <Dialog open={approvalDialogOpen} onOpenChange={setApprovalDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Approve Grades</DialogTitle>
              <DialogDescription>
                Are you sure you want to approve grades for {selectedStudents.length} students?
              </DialogDescription>
            </DialogHeader>
            <div className="py-4">
              <div className="space-y-2">
                <p><strong>Class:</strong> {selectedClass?.name}</p>
                <p><strong>Semester:</strong> {selectedSemester}</p>
                <p><strong>Students:</strong> {selectedStudents.length}</p>
                <p className="text-sm text-gray-600">
                  Once approved, grade sheets will be finalized and can be exported.
                </p>
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setApprovalDialogOpen(false)}>
                Cancel
              </Button>
              <Button 
                onClick={handleApproveGrades}
                disabled={approveGradesMutation.isPending}
                className="bg-green-600 hover:bg-green-700"
              >
                {approveGradesMutation.isPending ? 'Approving...' : 'Approve Grades'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Student Detail View Dialog */}
        <Dialog open={!!viewStudentId} onOpenChange={() => setViewStudentId(null)}>
          <DialogContent className="max-w-4xl">
            <DialogHeader>
              <DialogTitle>Student Grade Details</DialogTitle>
              <DialogDescription>
                Detailed view of student grades and performance
              </DialogDescription>
            </DialogHeader>
            <div className="py-4 max-h-96 overflow-y-auto">
              {viewStudentId && gradeReviews && (
                <div className="space-y-4">
                  {(() => {
                    const student = gradeReviews.find(r => r.studentId === viewStudentId);
                    if (!student) return null;

                    return (
                      <>
                        <div className="grid grid-cols-2 gap-4 p-4 bg-gray-50 rounded">
                          <div>
                            <h4 className="font-medium">{student.studentFirstName} {student.studentLastName}</h4>
                            <p className="text-sm text-gray-600">Class Rank: #{student.classRank}</p>
                          </div>
                          <div className="text-right">
                            <div className="text-2xl font-bold">{student.overallAverage.toFixed(1)}%</div>
                            <p className="text-sm text-gray-600">Attendance: {student.attendanceRate.toFixed(1)}%</p>
                          </div>
                        </div>

                        <div className="space-y-3">
                          <h5 className="font-medium">Subject Grades:</h5>
                          {student.subjects.map((subject) => (
                            <div key={subject.courseId} className="border rounded p-3">
                              <div className="flex justify-between items-start mb-2">
                                <div>
                                  <h6 className="font-medium">{subject.courseName}</h6>
                                  <p className="text-sm text-gray-600">
                                    Teacher: {subject.teacherName} | Coefficient: {subject.coefficient}
                                  </p>
                                </div>
                                <Badge variant={subject.needsReview ? 'destructive' : 'default'}>
                                  {subject.average.toFixed(1)}%
                                </Badge>
                              </div>
                              
                              <div className="grid grid-cols-3 gap-2 text-sm">
                                <div>First: {subject.grades.firstExam || 'N/A'}</div>
                                <div>Second: {subject.grades.secondExam || 'N/A'}</div>
                                <div>Final: {subject.grades.finalExam || 'N/A'}</div>
                              </div>
                              
                              {subject.teacherRemarks && (
                                <div className="mt-2 p-2 bg-blue-50 rounded text-sm">
                                  <strong>Teacher Remarks:</strong> {subject.teacherRemarks}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      </>
                    );
                  })()}
                </div>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setViewStudentId(null)}>
                Close
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </>
  );
};

export default StaffGradeReview;
