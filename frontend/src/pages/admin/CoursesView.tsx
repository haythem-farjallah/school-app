import * as React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ArrowLeft, BookOpen, Palette, User, Clock, Edit, Trash2 } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { EditCourseSheet } from "@/features/courses/components/course-sheet";
import { useDeleteCourse } from "@/features/courses/hooks/use-courses";
import type { Course } from "@/types/course";

const CoursesView = () => {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [course, setCourse] = React.useState<Course | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const deleteMutation = useDeleteCourse();

  // Mock data for demonstration - in real app, fetch from API
  React.useEffect(() => {
    const fetchCourse = async () => {
      try {
        setIsLoading(true);
        // Mock course data - replace with actual API call
        const mockCourse: Course = {
          id: parseInt(id || "1"),
          name: "Advanced Mathematics",
          color: "#3B82F6",
          credit: 4.0,
          weeklyCapacity: 6,
          teacherId: 1,
        };
        setCourse(mockCourse);
      } catch (err) {
        setError("Failed to load course details");
        toast.error("Failed to load course details");
      } finally {
        setIsLoading(false);
      }
    };

    if (id) {
      fetchCourse();
    }
  }, [id]);

  const handleDelete = React.useCallback(async () => {
    if (!course) return;
    
    if (window.confirm(`Are you sure you want to delete "${course.name}"?`)) {
      try {
        await deleteMutation.mutateAsync(course.id);
        toast.success(`Course "${course.name}" deleted successfully`);
        navigate("/admin/courses");
      } catch {
        toast.error("Failed to delete course");
      }
    }
  }, [course, deleteMutation, navigate]);

  const handleEditSuccess = React.useCallback(() => {
    toast.success("Course updated successfully!");
    // Refresh course data
    window.location.reload();
  }, []);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-4"></div>
          <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        </div>
        <Card>
          <CardContent className="pt-6">
            <div className="space-y-4">
              <div className="h-4 bg-gray-200 rounded w-3/4"></div>
              <div className="h-4 bg-gray-200 rounded w-1/2"></div>
              <div className="h-4 bg-gray-200 rounded w-2/3"></div>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="space-y-6">
        <Card className="border-red-200">
          <CardContent className="pt-6">
            <div className="text-center text-red-600">
              {error || "Course not found"}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate("/admin/courses")}
            className="flex items-center space-x-2"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back to Courses</span>
          </Button>
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{course.name}</h1>
            <p className="text-gray-600">Course Details</p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <EditCourseSheet 
            course={course} 
            onSuccess={handleEditSuccess}
            trigger={
              <Button variant="outline" size="sm" className="flex items-center space-x-2">
                <Edit className="h-4 w-4" />
                <span>Edit</span>
              </Button>
            }
          />
          <Button
            variant="destructive"
            size="sm"
            onClick={handleDelete}
            className="flex items-center space-x-2"
          >
            <Trash2 className="h-4 w-4" />
            <span>Delete</span>
          </Button>
        </div>
      </div>

      {/* Course Details */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Basic Information */}
        <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/80 via-indigo-50/40 to-purple-50/20 shadow-xl backdrop-blur-sm">
          <CardHeader className="pb-4">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-lg">
                <BookOpen className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <CardTitle className="text-xl font-bold bg-gradient-to-r from-blue-900 via-indigo-800 to-purple-700 bg-clip-text text-transparent">
                  Course Information
                </CardTitle>
                <CardDescription className="text-blue-700/80">
                  Basic course details and settings
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-600">Course ID</span>
              <Badge variant="secondary" className="font-mono">
                #{course.id}
              </Badge>
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-600">Course Name</span>
              <span className="font-semibold text-gray-900">{course.name}</span>
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-600">Course Color</span>
              <div className="flex items-center space-x-2">
                <div
                  className="h-6 w-6 rounded-full border-2 border-white shadow-lg ring-2 ring-slate-200/80"
                  style={{ backgroundColor: course.color }}
                />
                <span className="text-sm font-mono text-gray-700">{course.color}</span>
              </div>
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-600">Credit</span>
              <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">
                {course.credit} credits
              </Badge>
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-600">Weekly Hours</span>
              <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                {course.weeklyCapacity} hours/week
              </Badge>
            </div>
          </CardContent>
        </Card>

        {/* Teacher Assignment */}
        <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/60 to-pink-50/30 shadow-xl backdrop-blur-sm">
          <CardHeader className="pb-4">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-gradient-to-br from-purple-100 to-pink-100 rounded-lg">
                <User className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <CardTitle className="text-xl font-bold bg-gradient-to-r from-purple-900 to-pink-700 bg-clip-text text-transparent">
                  Teacher Assignment
                </CardTitle>
                <CardDescription className="text-purple-700/80">
                  Assigned teacher information
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center space-x-3">
              <div className="h-12 w-12 rounded-full bg-gradient-to-r from-purple-100 to-pink-100 border border-purple-200/60 flex items-center justify-center">
                <span className="text-lg font-semibold text-purple-700">T</span>
              </div>
              <div>
                <div className="font-semibold text-gray-900">Teacher #{course.teacherId}</div>
                <div className="text-sm text-gray-600">Assigned Teacher</div>
              </div>
            </div>
            <Separator />
            <div className="text-sm text-gray-600">
              This teacher is responsible for delivering the course content and managing student progress.
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Additional Information */}
      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
        <CardHeader className="pb-4">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-slate-100 to-gray-100 rounded-lg">
              <Clock className="h-6 w-6 text-slate-600" />
            </div>
            <div>
              <CardTitle className="text-xl font-bold text-slate-900">
                Course Schedule
              </CardTitle>
              <CardDescription className="text-slate-600">
                Weekly schedule and time allocation
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="text-center p-4 bg-slate-50 rounded-lg">
              <div className="text-2xl font-bold text-slate-700">{course.weeklyCapacity}</div>
              <div className="text-sm text-slate-600">Hours per Week</div>
            </div>
            <div className="text-center p-4 bg-slate-50 rounded-lg">
              <div className="text-2xl font-bold text-slate-700">{course.credit * 15}</div>
              <div className="text-sm text-slate-600">Total Hours</div>
            </div>
            <div className="text-center p-4 bg-slate-50 rounded-lg">
              <div className="text-2xl font-bold text-slate-700">{course.credit}</div>
              <div className="text-sm text-slate-600">Credits</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default CoursesView; 