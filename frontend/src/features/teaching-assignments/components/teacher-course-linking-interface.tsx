import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { 
  Users, 
  BookOpen, 
  GraduationCap, 
  Plus, 
  Minus,
  Search,
  ArrowRight,
  CheckCircle,
  AlertCircle,
  User,
  Clock,
  Target,
  Zap,
  BarChart3,
  Settings,
  Save,
  RotateCcw
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import toast from "react-hot-toast";

import { useTeachers } from "@/features/teachers/hooks/use-teachers";
import { useCourses } from "@/features/courses/hooks/use-courses";
import { useClasses } from "@/features/classes/hooks/use-classes";
import { 
  useAssignTeacherToCourses,
  useAssignTeachersToClass,
  useBulkCreateTeachingAssignments,
} from "../hooks/use-teaching-assignments";
import type { Teacher } from "@/types/teacher";
import type { Course } from "@/types/course";
import type { Class } from "@/types/class";
import type { CreateTeachingAssignmentData } from "@/types/teaching-assignment";

interface AssignmentPreview {
  teacherId: number;
  teacherName: string;
  teacherEmail: string;
  courseId: number;
  courseName: string;
  courseCode: string;
  classId: number;
  className: string;
  weeklyHours: number;
  isConflict?: boolean;
  conflictReason?: string;
}

interface LinkingStats {
  totalTeachers: number;
  totalCourses: number;
  totalClasses: number;
  pendingAssignments: number;
  completedAssignments: number;
  conflictingAssignments: number;
}

export function TeacherCourseLinkingInterface() {
  const [activeTab, setActiveTab] = React.useState<"teacher-to-courses" | "teachers-to-course" | "bulk-assignment">("teacher-to-courses");
  const [searchTerm, setSearchTerm] = React.useState("");
  const [selectedTeacher, setSelectedTeacher] = React.useState<Teacher | null>(null);
  const [selectedCourse, setSelectedCourse] = React.useState<Course | null>(null);
  const [selectedClass, setSelectedClass] = React.useState<Class | null>(null);
  const [selectedCourses, setSelectedCourses] = React.useState<Course[]>([]);
  const [selectedTeachers, setSelectedTeachers] = React.useState<Teacher[]>([]);
  const [weeklyHours, setWeeklyHours] = React.useState<number>(3);
  const [assignmentPreviews, setAssignmentPreviews] = React.useState<AssignmentPreview[]>([]);

  // Data fetching
  const { data: teachersResponse } = useTeachers({ size: 100 });
  const { data: coursesResponse } = useCourses({ size: 100 });
  const { data: classesResponse } = useClasses({ size: 100 });

  // Mutations
  const assignTeacherToCoursesMutation = useAssignTeacherToCourses();
  const assignTeachersToClassMutation = useAssignTeachersToClass();
  const bulkCreateMutation = useBulkCreateTeachingAssignments();

  const teachers = teachersResponse?.data || [];
  const courses = coursesResponse?.data || [];
  const classes = classesResponse?.data || [];

  // Filter data based on search
  const filteredTeachers = teachers.filter(teacher =>
    `${teacher.firstName} ${teacher.lastName}`.toLowerCase().includes(searchTerm.toLowerCase()) ||
    teacher.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    teacher.qualifications?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredCourses = courses.filter(course =>
    course.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Calculate stats
  const stats: LinkingStats = React.useMemo(() => ({
    totalTeachers: teachers.length,
    totalCourses: courses.length,
    totalClasses: classes.length,
    pendingAssignments: assignmentPreviews.length,
    completedAssignments: 0, // Would come from API
    conflictingAssignments: assignmentPreviews.filter(a => a.isConflict).length,
  }), [teachers.length, courses.length, classes.length, assignmentPreviews]);

  // Handle teacher selection for teacher-to-courses tab
  const handleTeacherSelect = (teacher: Teacher) => {
    setSelectedTeacher(teacher);
    setSelectedCourses([]);
    setAssignmentPreviews([]);
  };

  // Handle course selection for teachers-to-course tab
  const handleCourseSelect = (course: Course) => {
    setSelectedCourse(course);
    setSelectedTeachers([]);
    setAssignmentPreviews([]);
  };

  // Handle class selection
  const handleClassSelect = (classId: string) => {
    const selectedClass = classes.find(c => c.id === parseInt(classId));
    setSelectedClass(selectedClass || null);
    updateAssignmentPreviews();
  };

  // Add course to selection (teacher-to-courses)
  const addCourseToSelection = (course: Course) => {
    if (!selectedCourses.find(c => c.id === course.id)) {
      setSelectedCourses(prev => [...prev, course]);
      updateAssignmentPreviews();
    }
  };

  // Remove course from selection
  const removeCourseFromSelection = (courseId: number) => {
    setSelectedCourses(prev => prev.filter(c => c.id !== courseId));
    updateAssignmentPreviews();
  };

  // Add teacher to selection (teachers-to-course)
  const addTeacherToSelection = (teacher: Teacher) => {
    if (!selectedTeachers.find(t => t.id === teacher.id)) {
      setSelectedTeachers(prev => [...prev, teacher]);
      updateAssignmentPreviews();
    }
  };

  // Remove teacher from selection
  const removeTeacherFromSelection = (teacherId: number) => {
    setSelectedTeachers(prev => prev.filter(t => t.id !== teacherId));
    updateAssignmentPreviews();
  };

  // Update assignment previews
  const updateAssignmentPreviews = React.useCallback(() => {
    if (!selectedClass) return;

    let previews: AssignmentPreview[] = [];

    if (activeTab === "teacher-to-courses" && selectedTeacher) {
      previews = selectedCourses.map(course => ({
        teacherId: selectedTeacher.id,
        teacherName: `${selectedTeacher.firstName} ${selectedTeacher.lastName}`,
        teacherEmail: selectedTeacher.email,
        courseId: course.id,
        courseName: course.name,
                 courseCode: '',
        classId: selectedClass.id,
        className: selectedClass.name,
        weeklyHours,
        isConflict: false, // Would check against existing assignments
      }));
    } else if (activeTab === "teachers-to-course" && selectedCourse) {
      previews = selectedTeachers.map(teacher => ({
        teacherId: teacher.id,
        teacherName: `${teacher.firstName} ${teacher.lastName}`,
        teacherEmail: teacher.email,
        courseId: selectedCourse.id,
        courseName: selectedCourse.name,
                 courseCode: '',
        classId: selectedClass.id,
        className: selectedClass.name,
        weeklyHours,
        isConflict: false,
      }));
    }

    setAssignmentPreviews(previews);
  }, [activeTab, selectedTeacher, selectedCourse, selectedClass, selectedCourses, selectedTeachers, weeklyHours]);

  React.useEffect(() => {
    updateAssignmentPreviews();
  }, [updateAssignmentPreviews]);

  // Execute assignments
  const executeAssignments = async () => {
    if (assignmentPreviews.length === 0) {
      toast.error("No assignments to create");
      return;
    }

    try {
      if (activeTab === "teacher-to-courses" && selectedTeacher && selectedClass) {
        await assignTeacherToCoursesMutation.mutateAsync({
          teacherId: selectedTeacher.id,
          courseIds: selectedCourses.map(c => c.id),
          classId: selectedClass.id,
        });
      } else if (activeTab === "teachers-to-course" && selectedCourse && selectedClass) {
        await assignTeachersToClassMutation.mutateAsync({
          teacherIds: selectedTeachers.map(t => t.id),
          courseId: selectedCourse.id,
          classId: selectedClass.id,
        });
      } else {
        // Bulk create
        const assignments: CreateTeachingAssignmentData[] = assignmentPreviews.map(preview => ({
          teacherId: preview.teacherId,
          courseId: preview.courseId,
          classId: preview.classId,
          weeklyHours: preview.weeklyHours,
        }));
        await bulkCreateMutation.mutateAsync(assignments);
      }

      // Reset form
      resetForm();
         } catch {
       // Error handled by mutation hooks
     }
  };

  const resetForm = () => {
    setSelectedTeacher(null);
    setSelectedCourse(null);
    setSelectedClass(null);
    setSelectedCourses([]);
    setSelectedTeachers([]);
    setAssignmentPreviews([]);
    setWeeklyHours(3);
  };

  const isExecuteDisabled = assignmentPreviews.length === 0 || 
    assignTeacherToCoursesMutation.isPending || 
    assignTeachersToClassMutation.isPending || 
    bulkCreateMutation.isPending;

  return (
    <div className="space-y-8">
      {/* Header */}
      <Card className="border-indigo-200/60 bg-gradient-to-r from-indigo-50/80 to-purple-50/40">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-3xl font-bold text-indigo-900 flex items-center">
                <Target className="h-8 w-8 mr-3" />
                Teacher-Course Linking Interface
              </CardTitle>
              <p className="text-indigo-700 mt-2">
                Efficiently assign teachers to courses and manage teaching assignments
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                onClick={resetForm}
                className="border-indigo-300 text-indigo-700 hover:bg-indigo-50"
              >
                <RotateCcw className="h-4 w-4 mr-2" />
                Reset
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Statistics */}
      <div className="grid grid-cols-2 md:grid-cols-6 gap-4">
        <Card>
          <CardContent className="p-4 text-center">
            <Users className="h-6 w-6 mx-auto mb-2 text-blue-600" />
            <div className="text-2xl font-bold text-slate-900">{stats.totalTeachers}</div>
            <div className="text-sm text-slate-600">Teachers</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <BookOpen className="h-6 w-6 mx-auto mb-2 text-green-600" />
            <div className="text-2xl font-bold text-slate-900">{stats.totalCourses}</div>
            <div className="text-sm text-slate-600">Courses</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <GraduationCap className="h-6 w-6 mx-auto mb-2 text-purple-600" />
            <div className="text-2xl font-bold text-slate-900">{stats.totalClasses}</div>
            <div className="text-sm text-slate-600">Classes</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <Clock className="h-6 w-6 mx-auto mb-2 text-orange-600" />
            <div className="text-2xl font-bold text-slate-900">{stats.pendingAssignments}</div>
            <div className="text-sm text-slate-600">Pending</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <CheckCircle className="h-6 w-6 mx-auto mb-2 text-emerald-600" />
            <div className="text-2xl font-bold text-slate-900">{stats.completedAssignments}</div>
            <div className="text-sm text-slate-600">Completed</div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4 text-center">
            <AlertCircle className="h-6 w-6 mx-auto mb-2 text-red-600" />
            <div className="text-2xl font-bold text-slate-900">{stats.conflictingAssignments}</div>
            <div className="text-sm text-slate-600">Conflicts</div>
          </CardContent>
        </Card>
      </div>

      {/* Main Interface */}
      <Card>
        <CardContent className="p-6">
                     <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as "teacher-to-courses" | "teachers-to-course" | "bulk-assignment")}>
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="teacher-to-courses" className="flex items-center space-x-2">
                <User className="h-4 w-4" />
                <span>Teacher → Courses</span>
              </TabsTrigger>
              <TabsTrigger value="teachers-to-course" className="flex items-center space-x-2">
                <Users className="h-4 w-4" />
                <span>Teachers → Course</span>
              </TabsTrigger>
              <TabsTrigger value="bulk-assignment" className="flex items-center space-x-2">
                <Zap className="h-4 w-4" />
                <span>Bulk Assignment</span>
              </TabsTrigger>
            </TabsList>

            {/* Common Controls */}
            <div className="mt-6 space-y-6">
              {/* Search and Class Selection */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="search">Search</Label>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                    <Input
                      id="search"
                      placeholder="Search teachers, courses..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-10"
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="class">Target Class *</Label>
                  <Select value={selectedClass?.id.toString() || ""} onValueChange={handleClassSelect}>
                    <SelectTrigger>
                      <SelectValue placeholder="Select a class" />
                    </SelectTrigger>
                    <SelectContent>
                      {classes.map((classItem) => (
                        <SelectItem key={classItem.id} value={classItem.id.toString()}>
                          <div className="flex items-center space-x-2">
                            <span>{classItem.name}</span>
                                                         <Badge variant="outline" className="text-xs">
                               Year {classItem.yearOfStudy}
                             </Badge>
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* Weekly Hours */}
              <div className="space-y-2">
                <Label htmlFor="weeklyHours">Weekly Hours</Label>
                <div className="flex items-center space-x-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setWeeklyHours(Math.max(1, weeklyHours - 1))}
                    disabled={weeklyHours <= 1}
                  >
                    <Minus className="h-4 w-4" />
                  </Button>
                  <Input
                    id="weeklyHours"
                    type="number"
                    min="1"
                    max="40"
                    value={weeklyHours}
                    onChange={(e) => setWeeklyHours(parseInt(e.target.value) || 1)}
                    className="w-20 text-center"
                  />
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setWeeklyHours(Math.min(40, weeklyHours + 1))}
                    disabled={weeklyHours >= 40}
                  >
                    <Plus className="h-4 w-4" />
                  </Button>
                  <span className="text-sm text-slate-600">hours per week</span>
                </div>
              </div>
            </div>

            {/* Tab Content */}
            <div className="mt-8">
              <TabsContent value="teacher-to-courses" className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Teacher Selection */}
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center">
                        <User className="h-5 w-5 mr-2" />
                        Select Teacher
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {selectedTeacher ? (
                        <div className="p-4 border rounded-lg bg-blue-50 border-blue-200">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center space-x-3">
                              <Avatar className="h-10 w-10">
                                <AvatarFallback className="bg-blue-100 text-blue-700">
                                  <User className="h-5 w-5" />
                                </AvatarFallback>
                              </Avatar>
                              <div>
                                <div className="font-semibold">
                                  {selectedTeacher.firstName} {selectedTeacher.lastName}
                                </div>
                                <div className="text-sm text-slate-600">{selectedTeacher.email}</div>
                                {selectedTeacher.qualifications && (
                                  <Badge variant="outline" className="text-xs mt-1">
                                    {selectedTeacher.qualifications}
                                  </Badge>
                                )}
                              </div>
                            </div>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setSelectedTeacher(null)}
                            >
                              <Minus className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="space-y-2 max-h-64 overflow-y-auto">
                          {filteredTeachers.map((teacher) => (
                            <div
                              key={teacher.id}
                              className="p-3 border rounded-lg hover:bg-slate-50 cursor-pointer transition-colors"
                              onClick={() => handleTeacherSelect(teacher)}
                            >
                              <div className="flex items-center space-x-3">
                                <Avatar className="h-8 w-8">
                                  <AvatarFallback className="bg-slate-100 text-slate-700 text-xs">
                                    <User className="h-4 w-4" />
                                  </AvatarFallback>
                                </Avatar>
                                <div className="flex-1">
                                  <div className="font-medium text-sm">
                                    {teacher.firstName} {teacher.lastName}
                                  </div>
                                  <div className="text-xs text-slate-500">{teacher.email}</div>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* Course Selection */}
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center">
                        <BookOpen className="h-5 w-5 mr-2" />
                        Select Courses
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {selectedCourses.length > 0 && (
                        <div className="space-y-2">
                          <Label>Selected Courses ({selectedCourses.length})</Label>
                          <div className="space-y-2 max-h-32 overflow-y-auto">
                            {selectedCourses.map((course) => (
                              <div key={course.id} className="flex items-center justify-between p-2 bg-green-50 border border-green-200 rounded">
                                <div>
                                  <span className="font-medium text-sm">{course.name}</span>
                                  
                                </div>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => removeCourseFromSelection(course.id)}
                                >
                                  <Minus className="h-4 w-4" />
                                </Button>
                              </div>
                            ))}
                          </div>
                          <Separator />
                        </div>
                      )}
                      
                      <div className="space-y-2 max-h-48 overflow-y-auto">
                        {filteredCourses
                          .filter(course => !selectedCourses.find(c => c.id === course.id))
                          .map((course) => (
                          <div
                            key={course.id}
                            className="p-3 border rounded-lg hover:bg-slate-50 cursor-pointer transition-colors"
                            onClick={() => addCourseToSelection(course)}
                          >
                            <div className="flex items-center justify-between">
                              <div>
                                <div className="font-medium text-sm">{course.name}</div>
                                {course.name && (
                                  <Badge variant="outline" className="text-xs mt-1">
                                    {course.name}
                                  </Badge>
                                )}
                              </div>
                              <Plus className="h-4 w-4 text-slate-400" />
                            </div>
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </TabsContent>

              <TabsContent value="teachers-to-course" className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Course Selection */}
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center">
                        <BookOpen className="h-5 w-5 mr-2" />
                        Select Course
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {selectedCourse ? (
                        <div className="p-4 border rounded-lg bg-green-50 border-green-200">
                          <div className="flex items-center justify-between">
                            <div>
                              <div className="font-semibold">{selectedCourse.name}</div>

                            </div>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setSelectedCourse(null)}
                            >
                              <Minus className="h-4 w-4" />
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <div className="space-y-2 max-h-64 overflow-y-auto">
                          {filteredCourses.map((course) => (
                            <div
                              key={course.id}
                              className="p-3 border rounded-lg hover:bg-slate-50 cursor-pointer transition-colors"
                              onClick={() => handleCourseSelect(course)}
                            >
                              <div className="flex items-center justify-between">
                                <div>
                                  <div className="font-medium text-sm">{course.name}</div>
                                  {course.name && (
                                    <Badge variant="outline" className="text-xs mt-1">
                                      {course.name}
                                    </Badge>
                                  )}
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </CardContent>
                  </Card>

                  {/* Teachers Selection */}
                  <Card>
                    <CardHeader>
                      <CardTitle className="flex items-center">
                        <Users className="h-5 w-5 mr-2" />
                        Select Teachers
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {selectedTeachers.length > 0 && (
                        <div className="space-y-2">
                          <Label>Selected Teachers ({selectedTeachers.length})</Label>
                          <div className="space-y-2 max-h-32 overflow-y-auto">
                            {selectedTeachers.map((teacher) => (
                              <div key={teacher.id} className="flex items-center justify-between p-2 bg-blue-50 border border-blue-200 rounded">
                                <div className="flex items-center space-x-2">
                                  <Avatar className="h-6 w-6">
                                    <AvatarFallback className="bg-blue-100 text-blue-700 text-xs">
                                      <User className="h-3 w-3" />
                                    </AvatarFallback>
                                  </Avatar>
                                  <span className="font-medium text-sm">
                                    {teacher.firstName} {teacher.lastName}
                                  </span>
                                </div>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => removeTeacherFromSelection(teacher.id)}
                                >
                                  <Minus className="h-4 w-4" />
                                </Button>
                              </div>
                            ))}
                          </div>
                          <Separator />
                        </div>
                      )}
                      
                      <div className="space-y-2 max-h-48 overflow-y-auto">
                        {filteredTeachers
                          .filter(teacher => !selectedTeachers.find(t => t.id === teacher.id))
                          .map((teacher) => (
                          <div
                            key={teacher.id}
                            className="p-3 border rounded-lg hover:bg-slate-50 cursor-pointer transition-colors"
                            onClick={() => addTeacherToSelection(teacher)}
                          >
                            <div className="flex items-center space-x-3">
                              <Avatar className="h-8 w-8">
                                <AvatarFallback className="bg-slate-100 text-slate-700 text-xs">
                                  <User className="h-4 w-4" />
                                </AvatarFallback>
                              </Avatar>
                              <div className="flex-1">
                                <div className="font-medium text-sm">
                                  {teacher.firstName} {teacher.lastName}
                                </div>
                                <div className="text-xs text-slate-500">{teacher.email}</div>
                              </div>
                              <Plus className="h-4 w-4 text-slate-400" />
                            </div>
                          </div>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </TabsContent>

              <TabsContent value="bulk-assignment" className="space-y-6">
                <Card>
                  <CardHeader>
                    <CardTitle className="flex items-center">
                      <Zap className="h-5 w-5 mr-2" />
                      Bulk Assignment (Coming Soon)
                    </CardTitle>
                  </CardHeader>
                  <CardContent>
                    <div className="text-center py-12 text-slate-500">
                      <BarChart3 className="h-16 w-16 mx-auto mb-4 text-slate-300" />
                      <h3 className="text-lg font-semibold mb-2">Advanced Bulk Assignment</h3>
                      <p className="mb-4">
                        Upload CSV files, use templates, and perform complex batch operations.
                      </p>
                      <Button variant="outline" disabled>
                        <Settings className="h-4 w-4 mr-2" />
                        Configure Bulk Operations
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>
            </div>
          </Tabs>
        </CardContent>
      </Card>

      {/* Assignment Preview */}
      <AnimatePresence>
        {assignmentPreviews.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.3 }}
          >
            <Card className="border-emerald-200 bg-emerald-50/50">
              <CardHeader>
                <CardTitle className="flex items-center justify-between">
                  <div className="flex items-center">
                    <CheckCircle className="h-5 w-5 mr-2 text-emerald-600" />
                    Assignment Preview ({assignmentPreviews.length})
                  </div>
                  <div className="flex items-center space-x-2">
                    <Button
                      variant="outline"
                      onClick={resetForm}
                      className="border-slate-300"
                    >
                      <RotateCcw className="h-4 w-4 mr-2" />
                      Clear
                    </Button>
                    <Button
                      onClick={executeAssignments}
                      disabled={isExecuteDisabled}
                      className="bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-700 hover:to-green-700"
                    >
                      <Save className="h-4 w-4 mr-2" />
                      {isExecuteDisabled ? "Processing..." : `Create ${assignmentPreviews.length} Assignments`}
                    </Button>
                  </div>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3 max-h-64 overflow-y-auto">
                  {assignmentPreviews.map((preview, index) => (
                    <motion.div
                      key={`${preview.teacherId}-${preview.courseId}-${preview.classId}`}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: index * 0.05 }}
                      className={`p-4 border rounded-lg ${
                        preview.isConflict 
                          ? 'border-red-200 bg-red-50' 
                          : 'border-emerald-200 bg-white'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center space-x-4">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback className="bg-emerald-100 text-emerald-700 text-xs">
                              <User className="h-4 w-4" />
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <div className="font-medium text-sm">{preview.teacherName}</div>
                            <div className="text-xs text-slate-500">{preview.teacherEmail}</div>
                          </div>
                          <ArrowRight className="h-4 w-4 text-slate-400" />
                                                     <div>
                             <div className="font-medium text-sm">{preview.courseName}</div>
                           </div>
                          <ArrowRight className="h-4 w-4 text-slate-400" />
                          <div>
                            <Badge variant="secondary" className="text-sm">
                              {preview.className}
                            </Badge>
                          </div>
                        </div>
                        <div className="flex items-center space-x-2">
                          <div className="text-sm text-slate-600">
                            {preview.weeklyHours}h/week
                          </div>
                          {preview.isConflict ? (
                            <AlertCircle className="h-4 w-4 text-red-500" />
                          ) : (
                            <CheckCircle className="h-4 w-4 text-emerald-500" />
                          )}
                        </div>
                      </div>
                      {preview.isConflict && preview.conflictReason && (
                        <div className="mt-2 text-xs text-red-600">
                          ⚠️ {preview.conflictReason}
                        </div>
                      )}
                    </motion.div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
