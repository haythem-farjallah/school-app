import { useState, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/hooks/useAuth";
import { useTeachingAssignmentsByTeacher } from "@/features/teaching-assignments/hooks/use-teaching-assignments";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { BookOpen, Users, TrendingUp, Plus, FileSpreadsheet, ArrowLeft } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import BulkGradeEntryDialog from "./components/BulkGradeEntryDialog";
import ClassGradeView from "./components/ClassGradeView";

const TeacherGradeWorkflow = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [selectedClassName, setSelectedClassName] = useState<string>("");
  const [showBulkEntry, setShowBulkEntry] = useState(false);
  const [activeTab, setActiveTab] = useState("overview");

  // Fetch teacher's assignments
  const { data: assignmentsResponse, isLoading } = useTeachingAssignmentsByTeacher(
    user?.id ?? 0,
    { size: 100 }
  );

  const assignments = useMemo(() => assignmentsResponse?.data ?? [], [assignmentsResponse?.data]);

  // Define types for better type safety
  interface CourseInfo {
    courseId: number;
    courseName: string;
    courseCode: string;
    weeklyHours: number;
  }

  interface ClassInfo {
    classId: number;
    className: string;
    courses: CourseInfo[];
    totalStudents: number;
  }

  // Group assignments by class for better organization
  const classesByAssignment = useMemo(() => {
    const classMap = new Map<number, ClassInfo>();
    
    assignments.forEach(assignment => {
      const classId = assignment.classId;
      if (!classMap.has(classId)) {
        classMap.set(classId, {
          classId: assignment.classId,
          className: assignment.className,
          courses: [],
          totalStudents: 0, // Will be fetched separately
        });
      }
      
      classMap.get(classId)!.courses.push({
        courseId: assignment.courseId,
        courseName: assignment.courseName,
        courseCode: assignment.courseCode,
        weeklyHours: assignment.weeklyHours,
      });
    });
    
    return Array.from(classMap.values());
  }, [assignments]);

  const handleBulkGradeEntry = (classId: number, className: string) => {
    setSelectedClassId(classId);
    setSelectedClassName(className);
    setShowBulkEntry(true);
  };

  const handleViewClassGrades = (classId: number, className: string) => {
    setSelectedClassId(classId);
    setSelectedClassName(className);
    setActiveTab("class-details");
  };

  if (isLoading) {
    return (
      <div className="p-6 space-y-6">
        <div className="space-y-2">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-4 w-96" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-48 w-full" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="space-y-2">
        <h1 className="text-3xl font-bold text-gray-900">{t('Grade Workflow')}</h1>
        <p className="text-gray-600">
          Manage grades for your classes and courses efficiently.
        </p>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="class-details">Class Details</TabsTrigger>
          <TabsTrigger value="analytics">Analytics</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          {/* Quick Stats */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <Card>
              <CardContent className="p-6">
                <div className="flex items-center space-x-2">
                  <BookOpen className="h-5 w-5 text-blue-500" />
                  <div>
                    <p className="text-sm font-medium text-gray-600">Total Classes</p>
                    <p className="text-2xl font-bold">{classesByAssignment.length}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center space-x-2">
                  <Users className="h-5 w-5 text-green-500" />
                  <div>
                    <p className="text-sm font-medium text-gray-600">Total Courses</p>
                    <p className="text-2xl font-bold">{assignments.length}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center space-x-2">
                  <TrendingUp className="h-5 w-5 text-purple-500" />
                  <div>
                    <p className="text-sm font-medium text-gray-600">Pending Grades</p>
                    <p className="text-2xl font-bold">23</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6">
                <div className="flex items-center space-x-2">
                  <FileSpreadsheet className="h-5 w-5 text-orange-500" />
                  <div>
                    <p className="text-sm font-medium text-gray-600">This Week</p>
                    <p className="text-2xl font-bold">12</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Classes Grid */}
          <div className="space-y-4">
            <h2 className="text-xl font-semibold">Your Classes</h2>
            
            {classesByAssignment.length === 0 ? (
              <Card>
                <CardContent className="p-12 text-center">
                  <BookOpen className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">No Teaching Assignments</h3>
                  <p className="text-gray-600">
                    You don't have any teaching assignments yet. Contact the administrator to get assigned to classes.
                  </p>
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {classesByAssignment.map((classInfo) => (
                  <Card key={classInfo.classId} className="hover:shadow-lg transition-shadow">
                    <CardHeader>
                      <CardTitle className="flex items-center justify-between">
                        <span>{classInfo.className}</span>
                        <Badge variant="outline">
                          {classInfo.courses.length} courses
                        </Badge>
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {/* Courses List */}
                      <div className="space-y-2">
                        <p className="text-sm font-medium text-gray-600">Courses:</p>
                        {classInfo.courses.map((course: CourseInfo) => (
                          <div key={course.courseId} className="flex items-center justify-between text-sm">
                            <span>{course.courseCode} - {course.courseName}</span>
                            <Badge variant="secondary" className="text-xs">
                              {course.weeklyHours}h/week
                            </Badge>
                          </div>
                        ))}
                      </div>

                      {/* Action Buttons */}
                      <div className="flex space-x-2">
                        <Button
                          size="sm"
                          onClick={() => handleBulkGradeEntry(classInfo.classId, classInfo.className)}
                          className="flex-1"
                        >
                          <Plus className="h-4 w-4 mr-1" />
                          Enter Grades
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleViewClassGrades(classInfo.classId, classInfo.className)}
                          className="flex-1"
                        >
                          View Details
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </div>
        </TabsContent>

        <TabsContent value="class-details">
          {selectedClassId ? (
            <div className="space-y-4">
              <Button 
                variant="outline" 
                onClick={() => setActiveTab("overview")}
                className="mb-4"
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Overview
              </Button>
              <ClassGradeView 
                classId={selectedClassId} 
                className={selectedClassName}
                onBack={() => setActiveTab("overview")}
              />
            </div>
          ) : (
            <Card>
              <CardContent className="p-12 text-center">
                <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">Select a Class</h3>
                <p className="text-gray-600">
                  Go back to overview and select a class to view detailed grades.
                </p>
                <Button 
                  variant="outline" 
                  onClick={() => setActiveTab("overview")}
                  className="mt-4"
                >
                  Back to Overview
                </Button>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="analytics">
          <Card>
            <CardContent className="p-12 text-center">
              <TrendingUp className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">Grade Analytics</h3>
              <p className="text-gray-600">
                Advanced grade analytics and reporting features coming soon.
              </p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Bulk Grade Entry Dialog */}
      {showBulkEntry && selectedClassId && (
        <BulkGradeEntryDialog
          classId={selectedClassId}
          className={selectedClassName}
          isOpen={showBulkEntry}
          onClose={() => {
            setShowBulkEntry(false);
            setSelectedClassId(null);
            setSelectedClassName("");
          }}
        />
      )}
    </div>
  );
};

export default TeacherGradeWorkflow;