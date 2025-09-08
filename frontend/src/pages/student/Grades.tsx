import React, { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { 
  GraduationCap, 
  Award, 
  TrendingUp, 
  Calendar,
  Download,
  Target,
  Clock,
  CheckCircle
} from 'lucide-react';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Progress } from '@/components/ui/progress';
import { Separator } from '@/components/ui/separator';

import { useAuth } from '@/lib/auth';
import { 
  useStudentGradeSheet,
  useExportGradeSheet
} from '@/features/grades/hooks/use-grades';
import { 
  Semester, 
  getGradeLevel,
  getGradeLevelColor
} from '@/types/grade';
import toast from 'react-hot-toast';

const StudentGrades = () => {
  const { user } = useAuth();
  const studentId = user?.id;

  // State management
  const [selectedSemester, setSelectedSemester] = useState<Semester>(Semester.FIRST);

  // Data fetching
  const { 
    data: gradeSheet, 
    isLoading: gradeSheetLoading 
  } = useStudentGradeSheet(studentId, selectedSemester);

  // Mutations
  const exportGradeSheetMutation = useExportGradeSheet();

  // Handle export
  const handleExportGradeSheet = async () => {
    if (!studentId) return;

    try {
      const blob = await exportGradeSheetMutation.mutateAsync({
        studentId,
        semester: selectedSemester
      });
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `my-grade-sheet-${selectedSemester}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      toast.success('Grade sheet downloaded successfully');
    } catch (error) {
      console.error('Failed to export grade sheet:', error);
      toast.error('Failed to download grade sheet. Please try again.');
    }
  };

  // Calculate subject statistics
  const calculateSubjectStats = () => {
    if (!gradeSheet?.subjects) return { totalSubjects: 0, passedSubjects: 0, averageGrade: 0 };

    const totalSubjects = gradeSheet.subjects.length;
    const passedSubjects = gradeSheet.subjects.filter(subject => subject.average >= 60).length;
    const averageGrade = gradeSheet.subjects.reduce((sum, subject) => sum + subject.average, 0) / totalSubjects;

    return { totalSubjects, passedSubjects, averageGrade };
  };

  const subjectStats = calculateSubjectStats();

  return (
    <>
      <Helmet>
        <title>My Grades - Student Portal</title>
        <meta name="description" content="View your academic grades and performance" />
      </Helmet>

      <div className="container mx-auto p-6 space-y-8">
        {/* Header */}
        <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/80 via-blue-50/40 to-indigo-50/20 shadow-xl backdrop-blur-sm">
          <CardHeader className="pb-6">
            <div className="flex items-center justify-between">
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-purple-900 via-blue-800 to-indigo-700 bg-clip-text text-transparent">
                  My Academic Grades
                </CardTitle>
                <CardDescription className="text-purple-700/80 text-lg">
                  View your grades, performance, and academic progress
                </CardDescription>
              </div>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={handleExportGradeSheet}
                  disabled={!gradeSheet || exportGradeSheetMutation.isPending}
                  className="border-indigo-300 text-indigo-700 hover:bg-indigo-50"
                >
                  <Download className="mr-2 h-4 w-4" />
                  {exportGradeSheetMutation.isPending ? 'Exporting...' : 'Export PDF'}
                </Button>
              </div>
            </div>
          </CardHeader>
        </Card>

        {/* Semester Selection */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5 text-purple-600" />
              Select Semester
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="w-full md:w-64">
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
          </CardContent>
        </Card>

        {/* Grade Sheet Overview */}
        {gradeSheet && (
          <>
            {/* Student Info & Statistics */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Overall Average</CardTitle>
                  <Award className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-purple-600">
                    {gradeSheet.weightedAverage.toFixed(1)}%
                  </div>
                  <Badge 
                    variant={getGradeLevel(gradeSheet.weightedAverage) === 'A' || getGradeLevel(gradeSheet.weightedAverage) === 'B' ? 'default' : 'destructive'}
                    className="mt-1"
                  >
                    {getGradeLevel(gradeSheet.weightedAverage)} Grade
                  </Badge>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Class Rank</CardTitle>
                  <TrendingUp className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-blue-600">
                    #{gradeSheet.classRank}
                  </div>
                  <p className="text-xs text-muted-foreground">
                    out of {gradeSheet.totalStudents} students
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Attendance Rate</CardTitle>
                  <CheckCircle className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-green-600">
                    {gradeSheet.attendanceRate.toFixed(1)}%
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {gradeSheet.totalAbsences} absences
                  </p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Subjects Passed</CardTitle>
                  <Target className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-emerald-600">
                    {subjectStats.passedSubjects}/{subjectStats.totalSubjects}
                  </div>
                  <Progress 
                    value={(subjectStats.passedSubjects / subjectStats.totalSubjects) * 100} 
                    className="mt-2" 
                  />
                </CardContent>
              </Card>
            </div>

            {/* Grade Sheet Header */}
            <Card className="shadow-lg">
              <CardHeader className="text-center">
                <div className="space-y-2">
                  <h2 className="text-2xl font-bold">Academic Grade Sheet</h2>
                  <div className="text-lg">
                    <strong>{gradeSheet.studentFirstName} {gradeSheet.studentLastName}</strong>
                  </div>
                  <div className="text-sm text-gray-600">
                    Class: {gradeSheet.className} | Year: {gradeSheet.yearOfStudy} | Semester: {selectedSemester}
                  </div>
                  <div className="text-xs text-gray-500">
                    Generated on: {new Date(gradeSheet.generatedAt).toLocaleDateString()}
                  </div>
                </div>
              </CardHeader>
            </Card>

            {/* Subject Grades Table */}
            <Card className="shadow-lg">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <GraduationCap className="h-5 w-5 text-indigo-600" />
                  Subject Grades
                </CardTitle>
                <CardDescription>
                  Detailed breakdown of your performance in each subject
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b-2 border-gray-200">
                        <th className="text-left p-4 font-bold">Subject</th>
                        <th className="text-left p-4 font-bold">Teacher</th>
                        <th className="text-left p-4 font-bold">Coefficient</th>
                        <th className="text-center p-4 font-bold">First Exam</th>
                        <th className="text-center p-4 font-bold">Second Exam</th>
                        <th className="text-center p-4 font-bold">Final Exam</th>
                        <th className="text-center p-4 font-bold">Average</th>
                        <th className="text-center p-4 font-bold">Weighted Score</th>
                        <th className="text-center p-4 font-bold">Grade</th>
                        <th className="text-left p-4 font-bold">Teacher Remarks</th>
                      </tr>
                    </thead>
                    <tbody>
                      {gradeSheet.subjects.map((subject) => {
                        const letterGrade = getGradeLevel(subject.average);
                        const gradeColor = getGradeLevelColor(subject.average);

                        return (
                          <tr key={subject.courseId} className="border-b hover:bg-gray-50">
                            <td className="p-4">
                              <div>
                                <div className="font-medium">{subject.courseName}</div>
                                <div className="text-sm text-gray-500">{subject.courseCode}</div>
                              </div>
                            </td>
                            <td className="p-4">
                              <div className="text-sm">
                                {subject.teacherFirstName} {subject.teacherLastName}
                              </div>
                            </td>
                            <td className="p-4">
                              <Badge variant="outline">{subject.coefficient}</Badge>
                            </td>
                            <td className="p-4 text-center">
                              <span className="font-medium">
                                {subject.grades.firstExam || '-'}
                              </span>
                            </td>
                            <td className="p-4 text-center">
                              <span className="font-medium">
                                {subject.grades.secondExam || '-'}
                              </span>
                            </td>
                            <td className="p-4 text-center">
                              <span className="font-medium">
                                {subject.grades.finalExam || '-'}
                              </span>
                            </td>
                            <td className="p-4 text-center">
                              <span className={`font-bold text-lg ${gradeColor}`}>
                                {subject.average.toFixed(1)}%
                              </span>
                            </td>
                            <td className="p-4 text-center">
                              <span className="font-medium">
                                {subject.weightedScore.toFixed(1)}
                              </span>
                            </td>
                            <td className="p-4 text-center">
                              <Badge 
                                variant={letterGrade === 'A' || letterGrade === 'B' ? 'default' : 'destructive'}
                                className={`font-bold ${gradeColor}`}
                              >
                                {letterGrade}
                              </Badge>
                            </td>
                            <td className="p-4">
                              <div className="text-sm max-w-xs">
                                {subject.teacherRemarks || 'No remarks'}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                <Separator className="my-6" />

                {/* Grade Sheet Summary */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div className="space-y-2">
                    <h4 className="font-bold text-lg">Academic Summary</h4>
                    <div className="space-y-1 text-sm">
                      <div className="flex justify-between">
                        <span>Total Score:</span>
                        <span className="font-medium">{gradeSheet.totalScore}/{gradeSheet.totalMaxScore}</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Weighted Average:</span>
                        <span className="font-bold text-purple-600">{gradeSheet.weightedAverage.toFixed(2)}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Class Rank:</span>
                        <span className="font-medium">#{gradeSheet.classRank} / {gradeSheet.totalStudents}</span>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <h4 className="font-bold text-lg">Attendance Record</h4>
                    <div className="space-y-1 text-sm">
                      <div className="flex justify-between">
                        <span>Attendance Rate:</span>
                        <span className="font-medium text-green-600">{gradeSheet.attendanceRate.toFixed(1)}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span>Total Absences:</span>
                        <span className="font-medium">{gradeSheet.totalAbsences}</span>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <h4 className="font-bold text-lg">Approval Status</h4>
                    <div className="space-y-1 text-sm">
                      {gradeSheet.approvedBy ? (
                        <>
                          <div className="flex items-center gap-2">
                            <CheckCircle className="w-4 h-4 text-green-600" />
                            <span className="text-green-600 font-medium">Approved</span>
                          </div>
                          <div className="text-xs text-gray-600">
                            By: {gradeSheet.approvedBy.staffName}
                          </div>
                          <div className="text-xs text-gray-600">
                            On: {new Date(gradeSheet.approvedBy.approvedAt).toLocaleDateString()}
                          </div>
                        </>
                      ) : (
                        <div className="flex items-center gap-2">
                          <Clock className="w-4 h-4 text-orange-600" />
                          <span className="text-orange-600 font-medium">Pending Approval</span>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </>
        )}

        {/* Loading State */}
        {gradeSheetLoading && (
          <Card>
            <CardContent className="p-8 text-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading your grades...</p>
            </CardContent>
          </Card>
        )}

        {/* Empty State */}
        {!gradeSheetLoading && !gradeSheet && (
          <Card>
            <CardContent className="p-8 text-center">
              <GraduationCap className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No Grades Available</h3>
              <p className="text-gray-600">
                No grades found for the selected semester. Please check with your teachers or select a different semester.
              </p>
            </CardContent>
          </Card>
        )}
      </div>
    </>
  );
};

export default StudentGrades;
