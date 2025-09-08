import React, { useState, useMemo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { 
  BarChart3, 
  Download, 
  Edit, 
  Filter, 
  Plus, 
  TrendingUp, 
  Users,
  FileSpreadsheet,
  Calendar
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import BulkGradeEntryDialog from "./BulkGradeEntryDialog";

// Mock data - replace with actual API calls
const mockStudents = [
  {
    id: 1,
    firstName: "Alice",
    lastName: "Johnson",
    email: "alice.j@school.edu",
    grades: [
      { id: 1, courseCode: "MATH101", courseName: "Mathematics", score: 18.5, maxScore: 20, content: "Midterm Exam", date: "2024-01-15", weight: 2.0 },
      { id: 2, courseCode: "PHYS201", courseName: "Physics", score: 16.0, maxScore: 20, content: "Quiz 1", date: "2024-01-20", weight: 1.0 },
      { id: 3, courseCode: "CHEM101", courseName: "Chemistry", score: 17.5, maxScore: 20, content: "Lab Report", date: "2024-01-25", weight: 1.5 },
    ]
  },
  {
    id: 2,
    firstName: "Bob",
    lastName: "Smith",
    email: "bob.s@school.edu",
    grades: [
      { id: 4, courseCode: "MATH101", courseName: "Mathematics", score: 15.5, maxScore: 20, content: "Midterm Exam", date: "2024-01-15", weight: 2.0 },
      { id: 5, courseCode: "PHYS201", courseName: "Physics", score: 14.0, maxScore: 20, content: "Quiz 1", date: "2024-01-20", weight: 1.0 },
      { id: 6, courseCode: "CHEM101", courseName: "Chemistry", score: 16.0, maxScore: 20, content: "Lab Report", date: "2024-01-25", weight: 1.5 },
    ]
  },
  {
    id: 3,
    firstName: "Charlie",
    lastName: "Brown",
    email: "charlie.b@school.edu",
    grades: [
      { id: 7, courseCode: "MATH101", courseName: "Mathematics", score: 19.0, maxScore: 20, content: "Midterm Exam", date: "2024-01-15", weight: 2.0 },
      { id: 8, courseCode: "PHYS201", courseName: "Physics", score: 17.5, maxScore: 20, content: "Quiz 1", date: "2024-01-20", weight: 1.0 },
    ]
  },
];

const mockCourses = [
  { id: 1, name: "Mathematics", code: "MATH101" },
  { id: 2, name: "Physics", code: "PHYS201" },
  { id: 3, name: "Chemistry", code: "CHEM101" },
];

interface ClassGradeViewProps {
  classId: number;
  className: string;
  onBack: () => void;
}

const ClassGradeView: React.FC<ClassGradeViewProps> = ({ classId, className, onBack }) => {
  const [isLoading, setIsLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCourse, setSelectedCourse] = useState("all");
  const [showBulkEntry, setShowBulkEntry] = useState(false);
  const [students] = useState(mockStudents);
  const [courses] = useState(mockCourses);

  // Filter students based on search and course
  const filteredStudents = useMemo(() => {
    return students.filter(student => {
      const nameMatch = `${student.firstName} ${student.lastName}`
        .toLowerCase()
        .includes(searchTerm.toLowerCase());
      
      const courseMatch = selectedCourse === "all" || 
        student.grades.some(grade => grade.courseCode === selectedCourse);
      
      return nameMatch && courseMatch;
    });
  }, [students, searchTerm, selectedCourse]);

  // Calculate class statistics
  const classStats = useMemo(() => {
    const allGrades = students.flatMap(student => student.grades);
    if (allGrades.length === 0) return null;

    const averageGrade = allGrades.reduce((sum, grade) => sum + (grade.score / grade.maxScore * 20), 0) / allGrades.length;
    const totalStudents = students.length;
    const gradesEntered = allGrades.length;
    
    // Calculate course averages
    const courseAverages = courses.map(course => {
      const courseGrades = allGrades.filter(grade => grade.courseCode === course.code);
      if (courseGrades.length === 0) return { course: course.code, average: 0, count: 0 };
      
      const avg = courseGrades.reduce((sum, grade) => sum + (grade.score / grade.maxScore * 20), 0) / courseGrades.length;
      return { course: course.code, average: avg.toFixed(1), count: courseGrades.length };
    });

    return {
      averageGrade: averageGrade.toFixed(1),
      totalStudents,
      gradesEntered,
      courseAverages,
    };
  }, [students, courses]);

  const handleExportGrades = () => {
    // Mock export functionality
    console.log("Exporting grades for class:", classId);
    // In real implementation, this would generate and download an Excel/CSV file
  };

  const calculateStudentAverage = (studentGrades: any[]) => {
    if (studentGrades.length === 0) return 0;
    const weightedSum = studentGrades.reduce((sum, grade) => sum + (grade.score / grade.maxScore * 20 * grade.weight), 0);
    const totalWeight = studentGrades.reduce((sum, grade) => sum + grade.weight, 0);
    return totalWeight > 0 ? (weightedSum / totalWeight).toFixed(1) : 0;
  };

  const getGradeColor = (percentage: number) => {
    if (percentage >= 16) return "text-green-600 bg-green-50";
    if (percentage >= 12) return "text-yellow-600 bg-yellow-50";
    if (percentage >= 8) return "text-orange-600 bg-orange-50";
    return "text-red-600 bg-red-50";
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">{className} - Grade Overview</h2>
          <p className="text-gray-600">Manage and view grades for all students in this class</p>
        </div>
        <div className="flex gap-2">
          <Button onClick={() => setShowBulkEntry(true)}>
            <Plus className="h-4 w-4 mr-2" />
            Add Grades
          </Button>
          <Button variant="outline" onClick={handleExportGrades}>
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <Users className="h-5 w-5 text-blue-500" />
              <div>
                <p className="text-sm font-medium text-gray-600">Total Students</p>
                <p className="text-2xl font-bold">{classStats?.totalStudents || 0}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <FileSpreadsheet className="h-5 w-5 text-green-500" />
              <div>
                <p className="text-sm font-medium text-gray-600">Grades Entered</p>
                <p className="text-2xl font-bold">{classStats?.gradesEntered || 0}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <TrendingUp className="h-5 w-5 text-purple-500" />
              <div>
                <p className="text-sm font-medium text-gray-600">Class Average</p>
                <p className="text-2xl font-bold">{classStats?.averageGrade || "0.0"}/20</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6">
            <div className="flex items-center space-x-2">
              <BarChart3 className="h-5 w-5 text-orange-500" />
              <div>
                <p className="text-sm font-medium text-gray-600">Courses</p>
                <p className="text-2xl font-bold">{courses.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Filter className="h-5 w-5" />
            Filters
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Search Students</label>
              <Input
                placeholder="Search by name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Filter by Course</label>
              <Select value={selectedCourse} onValueChange={setSelectedCourse}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All Courses</SelectItem>
                  {courses.map((course) => (
                    <SelectItem key={course.code} value={course.code}>
                      {course.code} - {course.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Grades Table */}
      <Tabs defaultValue="students" className="space-y-4">
        <TabsList>
          <TabsTrigger value="students">Students View</TabsTrigger>
          <TabsTrigger value="courses">Courses View</TabsTrigger>
          <TabsTrigger value="analytics">Analytics</TabsTrigger>
        </TabsList>

        <TabsContent value="students" className="space-y-4">
          {filteredStudents.length === 0 ? (
            <Card>
              <CardContent className="p-12 text-center">
                <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">No Students Found</h3>
                <p className="text-gray-600">
                  {searchTerm ? "No students match your search criteria." : "No students in this class yet."}
                </p>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-4">
              {filteredStudents.map((student) => (
                <Card key={student.id}>
                  <CardHeader>
                    <div className="flex justify-between items-center">
                      <div>
                        <CardTitle className="text-lg">
                          {student.firstName} {student.lastName}
                        </CardTitle>
                        <p className="text-sm text-gray-500">{student.email}</p>
                      </div>
                      <div className="text-right">
                        <Badge variant="secondary" className="text-lg px-3 py-1">
                          Avg: {calculateStudentAverage(student.grades)}/20
                        </Badge>
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      {student.grades.length === 0 ? (
                        <p className="text-gray-500 text-center py-4">No grades entered yet</p>
                      ) : (
                        student.grades.map((grade) => (
                          <div key={grade.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                            <div className="flex-1">
                              <div className="flex items-center gap-2">
                                <Badge variant="outline">{grade.courseCode}</Badge>
                                <span className="font-medium">{grade.content}</span>
                              </div>
                              <p className="text-sm text-gray-500 mt-1">
                                {grade.courseName} • {grade.date} • Weight: {grade.weight}x
                              </p>
                            </div>
                            <div className="text-right">
                              <div className={`inline-flex items-center px-2 py-1 rounded-md text-sm font-medium ${getGradeColor(grade.score)}`}>
                                {grade.score}/{grade.maxScore}
                              </div>
                              <p className="text-xs text-gray-500 mt-1">
                                {((grade.score / grade.maxScore) * 100).toFixed(1)}%
                              </p>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="courses" className="space-y-4">
          {courses.map((course) => {
            const courseStats = classStats?.courseAverages.find(ca => ca.course === course.code);
            return (
              <Card key={course.id}>
                <CardHeader>
                  <div className="flex justify-between items-center">
                    <CardTitle>{course.code} - {course.name}</CardTitle>
                    <div className="flex items-center gap-2">
                      <Badge variant="outline">
                        {courseStats?.count || 0} grades
                      </Badge>
                      <Badge variant="secondary">
                        Avg: {courseStats?.average || "0.0"}/20
                      </Badge>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <p className="text-gray-600">
                    Course-specific grade management and analytics will be available here.
                  </p>
                </CardContent>
              </Card>
            );
          })}
        </TabsContent>

        <TabsContent value="analytics">
          <Card>
            <CardContent className="p-12 text-center">
              <BarChart3 className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">Grade Analytics</h3>
              <p className="text-gray-600">
                Detailed analytics including grade distribution, trends, and performance insights coming soon.
              </p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Bulk Grade Entry Dialog */}
      {showBulkEntry && (
        <BulkGradeEntryDialog
          classId={classId}
          className={className}
          isOpen={showBulkEntry}
          onClose={() => setShowBulkEntry(false)}
        />
      )}
    </div>
  );
};

export default ClassGradeView;
