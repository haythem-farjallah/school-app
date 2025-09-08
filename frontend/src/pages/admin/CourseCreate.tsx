import * as React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, BookOpen, Plus } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import { useMutationApi } from "@/hooks/useMutationApi";
import { http } from "@/lib/http";
import type { Course } from "@/types/course";
import { courseSchema, courseFields, type CourseValues } from "@/features/courses/courseForm.definition";

const CoursesCreate = () => {
  const navigate = useNavigate();

  const createCourseMutation = useMutationApi<Course, CourseValues>(
    async (data) => {
      const response = await http.post<Course>("/v1/courses", data);
      return response.data;
    },
    {
      onSuccess: () => {
        toast.success("Course created successfully!");
        navigate("/admin/courses");
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to create course";
        toast.error(message);
      },
    }
  );

  const createCourseRecipe: FormRecipe = {
    schema: courseSchema,
    fields: courseFields,
    onSubmit: async (values: unknown) => {
      await createCourseMutation.mutateAsync(values as CourseValues);
    },
  };

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
            <h1 className="text-3xl font-bold text-gray-900">Create New Course</h1>
            <p className="text-gray-600">Add a new course to the system</p>
          </div>
        </div>
      </div>

      {/* Create Form */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Form Card */}
        <div className="lg:col-span-2">
          <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/80 via-indigo-50/40 to-purple-50/20 shadow-xl backdrop-blur-sm">
            <CardHeader className="pb-6">
              <div className="flex items-center space-x-3">
                <div className="p-2 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-lg">
                  <Plus className="h-6 w-6 text-blue-600" />
                </div>
                <div>
                  <CardTitle className="text-2xl font-bold bg-gradient-to-r from-blue-900 via-indigo-800 to-purple-700 bg-clip-text text-transparent">
                    Course Information
                  </CardTitle>
                  <CardDescription className="text-blue-700/80">
                    Fill in the course details below
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <AutoForm
                recipe={createCourseRecipe}
                defaultValues={{
                  name: "",
                  color: "#3B82F6",
                  credit: 1.0,
                  weeklyCapacity: 3,
                }}
                submitLabel="Create Course"
                submitClassName="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-lg hover:shadow-xl transition-all duration-300"
              />
            </CardContent>
          </Card>
        </div>

        {/* Info Card */}
        <div className="lg:col-span-1">
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader className="pb-4">
              <div className="flex items-center space-x-3">
                <div className="p-2 bg-gradient-to-br from-slate-100 to-gray-100 rounded-lg">
                  <BookOpen className="h-6 w-6 text-slate-600" />
                </div>
                <div>
                  <CardTitle className="text-xl font-bold text-slate-900">
                    Course Guidelines
                  </CardTitle>
                  <CardDescription className="text-slate-600">
                    Important information about course creation
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-3">
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0"></div>
                  <div>
                    <h4 className="font-semibold text-slate-900">Course Name</h4>
                    <p className="text-sm text-slate-600">Use descriptive names that clearly indicate the subject matter</p>
                  </div>
                </div>
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-indigo-500 rounded-full mt-2 flex-shrink-0"></div>
                  <div>
                    <h4 className="font-semibold text-slate-900">Color Selection</h4>
                    <p className="text-sm text-slate-600">Choose a unique color to help distinguish this course in schedules and calendars</p>
                  </div>
                </div>
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-purple-500 rounded-full mt-2 flex-shrink-0"></div>
                  <div>
                    <h4 className="font-semibold text-slate-900">Credit Hours</h4>
                    <p className="text-sm text-slate-600">Set appropriate credit values based on course difficulty and content depth</p>
                  </div>
                </div>
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-green-500 rounded-full mt-2 flex-shrink-0"></div>
                  <div>
                    <h4 className="font-semibold text-slate-900">Weekly Hours</h4>
                    <p className="text-sm text-slate-600">Define how many hours per week this course will be taught</p>
                  </div>
                </div>
                <div className="flex items-start space-x-3">
                  <div className="w-2 h-2 bg-orange-500 rounded-full mt-2 flex-shrink-0"></div>
                  <div>
                    <h4 className="font-semibold text-slate-900">Teacher Assignment</h4>
                    <p className="text-sm text-slate-600">Assign a qualified teacher who will be responsible for this course</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default CoursesCreate; 