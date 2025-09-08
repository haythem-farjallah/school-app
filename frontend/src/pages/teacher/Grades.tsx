import { useState, useMemo } from 'react';
import { Helmet } from 'react-helmet-async';
import { useQueryState } from 'nuqs';
import { 
  Search, 
  Users, 
  BookOpen, 
  Award,
  TrendingUp,
  ArrowLeft,
  Save,
  GraduationCap,
  MapPin,
  Loader2,
  AlertCircle
} from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useAuth } from '@/hooks/useAuth';
import { getRoleClasses } from '@/lib/theme';
import { 
  useTeacherClasses, 
  useTeacherClassStats, 
  useTeacherGradeClass 
} from '@/hooks/use-teacher-classes';
import { 
  useCreateBulkEnhancedGrades,
  ExamType,
  Semester,
  examTypeDisplayNames,
  semesterDisplayNames,
  calculateGradePercentage,
  calculateLetterGrade,
  getGradeColor
} from '@/hooks/use-teacher-grades';
import toast from 'react-hot-toast';


const TeacherGrades = () => {
  // URL state management with nuqs
  const [selectedClassId, setSelectedClassId] = useQueryState('class');
  const [selectedCourseId, setSelectedCourseId] = useQueryState('course');
  const [globalExamType, setGlobalExamType] = useQueryState('examType');
  const [globalSemester, setGlobalSemester] = useQueryState('semester');
  const { user } = useAuth();
  const roleClasses = getRoleClasses(user?.role);
  
  // Local state
  const [searchTerm, setSearchTerm] = useState('');
  const [studentGrades, setStudentGrades] = useState<{[key: string]: {score: string, remarks: string, maxScore: string}}>({});
  const [isSaving, setIsSaving] = useState(false);

  // Fetch teacher's classes and stats
  const { data: teacherClasses = [], isLoading: classesLoading, error: classesError } = useTeacherClasses();
  const { data: stats, isLoading: statsLoading } = useTeacherClassStats();
  
  // Fetch specific class details when selected
  const { 
    data: classDetails, 
    isLoading: classDetailsLoading, 
    error: classDetailsError 
  } = useTeacherGradeClass(
    undefined, // We don't need teacherId anymore
    selectedClassId ? parseInt(selectedClassId) : undefined,
    selectedCourseId ? parseInt(selectedCourseId) : undefined
  );

  // Mutation for saving grades
  const bulkGradesMutation = useCreateBulkEnhancedGrades();

  // Derived data
  const selectedClass = teacherClasses.find((c: any) => c.id === selectedClassId);
  const students = classDetails?.students || [];

  // Memoized filtered classes
  const filteredClasses = useMemo(() => {
    return teacherClasses.filter((cls: any) =>
      cls.className.toLowerCase().includes(searchTerm.toLowerCase()) ||
      cls.subject.toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [teacherClasses, searchTerm]);

  const handleClassSelect = (classId: string, courseId: number) => {
    setSelectedClassId(classId);
    setSelectedCourseId(courseId.toString());
    // Reset grades when switching classes
    setStudentGrades({});
  };

  const handleBackToClasses = () => {
    setSelectedClassId(null);
    setSelectedCourseId(null);
    setGlobalExamType(null);
    setGlobalSemester(null);
    setStudentGrades({});
  };

  const handleScoreChange = (studentId: string, score: string) => {
    setStudentGrades(prev => ({
      ...prev,
      [studentId]: {
        ...prev[studentId],
        score: score,
        remarks: prev[studentId]?.remarks || '',
        maxScore: prev[studentId]?.maxScore || '100'
      }
    }));
  };

  const handleRemarksChange = (studentId: string, remarks: string) => {
    setStudentGrades(prev => ({
      ...prev,
      [studentId]: {
        ...prev[studentId],
        score: prev[studentId]?.score || '',
        remarks: remarks,
        maxScore: prev[studentId]?.maxScore || '100'
      }
    }));
  };

  const handleMaxScoreChange = (studentId: string, maxScore: string) => {
    setStudentGrades(prev => ({
      ...prev,
      [studentId]: {
        ...prev[studentId],
        score: prev[studentId]?.score || '',
        remarks: prev[studentId]?.remarks || '',
        maxScore: maxScore
      }
    }));
  };

  const handleSaveGrades = async () => {
    if (!selectedClassId || !selectedCourseId || !globalExamType || !globalSemester) {
      toast.error('Please select class, exam type, and semester');
      return;
    }

    // Validate that we have at least one grade to save
    const gradesToSave = Object.entries(studentGrades)
      .filter(([, grade]) => grade.score && parseFloat(grade.score) >= 0)
      .map(([studentId, grade]) => ({
        studentId: parseInt(studentId),
        score: parseFloat(grade.score),
        maxScore: parseFloat(grade.maxScore) || 100,
        teacherRemarks: grade.remarks || undefined,
      }));

    if (gradesToSave.length === 0) {
      toast.error('Please enter at least one grade');
      return;
    }

    setIsSaving(true);
    try {
      await bulkGradesMutation.mutateAsync({
        classId: parseInt(selectedClassId),
        courseId: parseInt(selectedCourseId),
        examType: globalExamType as ExamType,
        semester: globalSemester as Semester,
        grades: gradesToSave,
      });

      // Clear the form after successful save
      setStudentGrades({});
    } catch (error) {
      // Error is handled by the mutation
      console.error('Failed to save grades:', error);
    } finally {
      setIsSaving(false);
    }
  };

  // Show loading state
  if (classesLoading || statsLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading your classes...</p>
        </div>
      </div>
    );
  }

  // Show error state
  if (classesError) {
    console.error('Classes error:', classesError);
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Alert className="max-w-md">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            Failed to load classes. Please check the browser console for details and try refreshing the page.
            <br />
            <small className="text-gray-500 mt-2 block">
              Error: {classesError?.message || 'Unknown error'}
            </small>
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  // Show class selection first, then student grading interface
  if (!selectedClassId) {
    return (
      <>
        <Helmet>
          <title>Grades - Teacher Dashboard</title>
          <meta name="description" content="Select a class to grade students" />
        </Helmet>

        <div className="min-h-screen bg-gray-50">
          {/* Header */}
          <div className="bg-white border-b border-gray-200 shadow-sm">
            <div className="container mx-auto px-6 py-4">
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">Grades</h1>
                  <p className="text-gray-600 mt-1">Select a class to grade students</p>
                </div>
              </div>
            </div>
          </div>

          {/* Main Content */}
          <div className="container mx-auto px-6 py-8">
            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Total Classes</CardTitle>
                  <GraduationCap className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className={`text-2xl font-bold ${roleClasses.primary}`}>
                    {stats?.totalClasses || teacherClasses.length}
                  </div>
                  <p className="text-xs text-muted-foreground">Your classes</p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Total Students</CardTitle>
                  <Users className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {stats?.totalStudents || teacherClasses.reduce((sum: number, cls: any) => sum + cls.enrolled, 0)}
                  </div>
                  <p className="text-xs text-muted-foreground">All students</p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Average Grade</CardTitle>
                  <TrendingUp className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {stats?.averageGrade || 
                     (teacherClasses.length > 0 
                       ? (teacherClasses.reduce((sum: number, cls: any) => sum + cls.averageGrade, 0) / teacherClasses.length).toFixed(1)
                       : '0.0')}%
                  </div>
                  <p className="text-xs text-muted-foreground">Across all classes</p>
                </CardContent>
              </Card>

              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Pending Grades</CardTitle>
                  <BookOpen className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {stats?.pendingGrades || 0}
                  </div>
                  <p className="text-xs text-muted-foreground">Need grading</p>
                </CardContent>
              </Card>
            </div>

            {/* Classes Selection */}
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle>Select Class to Grade</CardTitle>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
                    <Input
                      placeholder="Search classes..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-10 w-64"
                    />
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {filteredClasses.map((classItem: any) => (
                    <Card 
                      key={`${classItem.classId}-${classItem.courseId}`} 
                      className={`cursor-pointer hover:shadow-lg transition-all duration-200 border-2 hover:${roleClasses.primaryBorder} hover:${roleClasses.primaryLight}`}
                      onClick={() => handleClassSelect(classItem.id, classItem.courseId)}
                    >
                      <CardHeader className="pb-3">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <div className={`w-12 h-12 ${roleClasses.primaryLight} rounded-full flex items-center justify-center`}>
                              <GraduationCap className={`h-6 w-6 ${roleClasses.icon}`} />
                            </div>
                            <div>
                              <CardTitle className="text-lg">{classItem.className}</CardTitle>
                              <p className="text-sm text-gray-600">{classItem.subject}</p>
                              <p className="text-xs text-gray-500">{classItem.courseCode}</p>
                            </div>
                          </div>
                          <Badge variant="outline" className="bg-green-100 text-green-800 border-green-200 font-medium">
                            Grade {classItem.grade}
                          </Badge>
                        </div>
                      </CardHeader>
                      <CardContent className="pt-0">
                        <div className="space-y-2">
                          <div className="flex items-center justify-between text-sm">
                            <span className="text-gray-600">Students:</span>
                            <span className="font-medium">{classItem.enrolled}/{classItem.capacity}</span>
                          </div>
                          <div className="flex items-center justify-between text-sm">
                            <span className="text-gray-600">Room:</span>
                            <span className="font-medium flex items-center gap-1">
                              <MapPin className="h-3 w-3" />
                              {classItem.room}
                            </span>
                          </div>
                          <div className="flex items-center justify-between text-sm">
                            <span className="text-gray-600">Average:</span>
                            <Badge variant="outline" className={`text-xs ${roleClasses.badge} font-medium`}>
                              {classItem.averageGrade}%
                            </Badge>
                          </div>
                          <div className="flex items-center justify-between text-sm">
                            <span className="text-gray-600">Coefficient:</span>
                            <span className="font-medium">{classItem.coefficient}</span>
                          </div>
                          <div className="text-xs text-gray-500 mt-2">
                            {classItem.schedule}
                          </div>
                        </div>
                        <Button className={`w-full mt-4 ${roleClasses.button}`}>
                          <Award className="h-4 w-4 mr-2" />
                          Grade Students
                        </Button>
                      </CardContent>
                    </Card>
                  ))}
                </div>

                {filteredClasses.length === 0 && (
                  <div className="text-center py-8">
                    <GraduationCap className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                    <h3 className="text-lg font-medium text-gray-900 mb-2">No classes found</h3>
                    <p className="text-gray-600 mb-4">
                      {searchTerm ? 'No classes match your search criteria.' : 'You don\'t have any classes assigned yet.'}
                    </p>
                    {!searchTerm && (
                      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 max-w-md mx-auto">
                        <p className="text-blue-800 text-sm">
                          <strong>Note:</strong> Classes need to be assigned to you through the admin panel. 
                          Please contact your administrator if you should have classes assigned.
                        </p>
                      </div>
                    )}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </>
    );
  }

  // Show loading state for class details
  if (classDetailsLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading class details...</p>
        </div>
      </div>
    );
  }

  // Show error state for class details
  if (classDetailsError) {
    console.error('Class details error:', classDetailsError);
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <Alert className="max-w-md">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            Failed to load class details. Please check the browser console for details and try going back and selecting the class again.
            <br />
            <small className="text-gray-500 mt-2 block">
              Error: {classDetailsError?.message || 'Unknown error'}
            </small>
            <div className="mt-4">
              <Button 
                variant="outline" 
                onClick={handleBackToClasses}
                className="flex items-center gap-2"
              >
                <ArrowLeft className="h-4 w-4" />
                Back to Classes
              </Button>
            </div>
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  // Show student grading interface
  return (
    <>
      <Helmet>
        <title>Grade Students - {selectedClass?.className} {selectedClass?.subject}</title>
        <meta name="description" content="Grade students in your class" />
      </Helmet>

      <div className="min-h-screen bg-gray-50">
        {/* Header */}
        <div className="bg-white border-b border-gray-200 shadow-sm">
          <div className="container mx-auto px-6 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Button 
                  variant="outline" 
                  onClick={handleBackToClasses}
                  className="flex items-center gap-2 border-gray-300 hover:bg-gray-50 hover:border-gray-400"
                >
                  <ArrowLeft className="h-4 w-4" />
                  Back to Classes
                </Button>
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">
                    Grade Students - {classDetails?.className}
                  </h1>
                  <p className="text-gray-600 mt-1">
                    {classDetails?.courseName} ({classDetails?.courseCode}) • {students.length} students
                  </p>
                </div>
              </div>
              <Button 
                onClick={handleSaveGrades}
                disabled={isSaving || bulkGradesMutation.isPending}
                className={roleClasses.button}
              >
                {(isSaving || bulkGradesMutation.isPending) ? (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                <Save className="h-4 w-4 mr-2" />
                )}
                Save All Grades
              </Button>
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="container mx-auto px-6 py-8">
          {/* Class Info & Global Settings Card */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <GraduationCap className={`h-5 w-5 ${roleClasses.icon}`} />
                Class Information & Exam Settings
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-5 gap-4 mb-6">
                <div>
                  <span className="text-sm text-gray-600">Class:</span>
                  <p className="font-medium">{classDetails?.className}</p>
                </div>
                <div>
                  <span className="text-sm text-gray-600">Course:</span>
                  <p className="font-medium">{classDetails?.courseName}</p>
                </div>
                <div>
                  <span className="text-sm text-gray-600">Code:</span>
                  <p className="font-medium">{classDetails?.courseCode}</p>
                </div>
                <div>
                  <span className="text-sm text-gray-600">Coefficient:</span>
                  <p className="font-medium">{classDetails?.coefficient}</p>
                </div>
                <div>
                  <span className="text-sm text-gray-600">Students:</span>
                  <p className="font-medium">{students.length}</p>
                </div>
              </div>
              
              {/* Global Exam Settings */}
              <div className="border-t pt-6">
                <h3 className="text-lg font-medium mb-4">Exam Settings (Applied to All Students)</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Semester <span className="text-red-500">*</span>
                    </label>
                    <Select
                      value={globalSemester || ''}
                      onValueChange={setGlobalSemester}
                    >
                      <SelectTrigger className={`border-gray-300 focus:${roleClasses.primaryBorder} focus:ring-blue-500`}>
                        <SelectValue placeholder="Select semester" />
                      </SelectTrigger>
                      <SelectContent>
                        {Object.entries(semesterDisplayNames).map(([key, value]) => (
                          <SelectItem key={key} value={key}>{value}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Exam Type <span className="text-red-500">*</span>
                    </label>
                    <Select
                      value={globalExamType || ''}
                      onValueChange={setGlobalExamType}
                    >
                      <SelectTrigger className={`border-gray-300 focus:${roleClasses.primaryBorder} focus:ring-blue-500`}>
                        <SelectValue placeholder="Select exam type for all students" />
                      </SelectTrigger>
                      <SelectContent>
                        {Object.entries(examTypeDisplayNames).map(([key, value]) => (
                          <SelectItem key={key} value={key}>{value}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Students Grading Table */}
          <Card>
            <CardHeader>
              <CardTitle>Student Grades</CardTitle>
              <p className="text-sm text-gray-600">
                Enter scores, max scores, and individual remarks for each student. Exam type and semester are set globally above.
              </p>
            </CardHeader>
            <CardContent>
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow className="hover:bg-gray-50">
                      <TableHead>Student</TableHead>
                      <TableHead>Current Average</TableHead>
                      <TableHead>Attendance</TableHead>
                      <TableHead>Exam Type</TableHead>
                      <TableHead>Score</TableHead>
                      <TableHead>Max Score</TableHead>
                      <TableHead>Percentage</TableHead>
                      <TableHead>Grade</TableHead>
                      <TableHead>Remarks</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {students.map((student: any) => {
                      const currentGrade = studentGrades[student.studentId] || { score: '', remarks: '', maxScore: '100' };
                      const score = parseFloat(currentGrade.score) || 0;
                      const maxScore = parseFloat(currentGrade.maxScore) || 100;
                      const percentage = calculateGradePercentage(score, maxScore);
                      const letterGrade = score > 0 ? calculateLetterGrade(percentage) : '-';
                      
                      return (
                        <TableRow 
                          key={student.studentId} 
                          className={`hover:${roleClasses.primaryLight}/50 transition-colors duration-150 cursor-pointer`}
                        >
                          <TableCell>
                            <div>
                              <div className="font-medium">{student.firstName} {student.lastName}</div>
                              <div className="text-sm text-gray-500">ID: {student.studentId}</div>
                              <div className="text-xs text-gray-400">{student.email}</div>
                            </div>
                          </TableCell>
                          <TableCell>
                            <Badge 
                              variant="outline" 
                              className="bg-blue-100 text-blue-800 border-blue-200 font-medium"
                            >
                              {student.average ? student.average.toFixed(1) : 'N/A'}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge 
                              variant="outline" 
                              className={student.attendanceRate >= 80 ? 'bg-green-100 text-green-800 border-green-200 font-medium' : 'bg-red-100 text-red-800 border-red-200 font-medium'}
                            >
                              {student.attendanceRate ? student.attendanceRate.toFixed(1) : '0.0'}%
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge 
                              variant="outline" 
                              className={globalExamType ? 'bg-purple-100 text-purple-800 border-purple-200 font-medium' : 'bg-gray-100 text-gray-600 border-gray-200'}
                            >
                              {globalExamType ? examTypeDisplayNames[globalExamType as ExamType] : 'Not Selected'}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Input
                              type="number"
                              value={currentGrade.score}
                              onChange={(e) => handleScoreChange(student.studentId.toString(), e.target.value)}
                              placeholder="0"
                              min="0"
                              step="0.1"
                              className={`w-20 text-center font-medium border-gray-300 focus:${roleClasses.primaryBorder} focus:ring-blue-500 hover:${roleClasses.primaryBorder} transition-colors`}
                            />
                          </TableCell>
                          <TableCell>
                            <Input
                              type="number"
                              value={currentGrade.maxScore}
                              onChange={(e) => handleMaxScoreChange(student.studentId.toString(), e.target.value)}
                              placeholder="100"
                              min="1"
                              step="0.1"
                              className={`w-20 text-center font-medium border-gray-300 focus:${roleClasses.primaryBorder} focus:ring-blue-500 hover:${roleClasses.primaryBorder} transition-colors`}
                            />
                          </TableCell>
                          <TableCell>
                            <Badge 
                              variant="outline" 
                              className="bg-gray-100 text-gray-800 border-gray-200 font-medium"
                            >
                              {percentage.toFixed(1)}%
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge 
                              variant="outline" 
                              className={getGradeColor(letterGrade)}
                            >
                              {letterGrade}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Input
                              value={currentGrade.remarks}
                              onChange={(e) => handleRemarksChange(student.studentId.toString(), e.target.value)}
                              placeholder="Individual remarks..."
                              className={`w-48 text-sm border-gray-300 focus:${roleClasses.primaryBorder} focus:ring-blue-500 hover:${roleClasses.primaryBorder} transition-colors`}
                            />
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </div>

              {students.length === 0 && (
                <div className="text-center py-8">
                  <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">No students found</h3>
                  <p className="text-gray-600">This class doesn't have any enrolled students yet.</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </>
  );
};

export default TeacherGrades;