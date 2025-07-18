import * as React from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-hot-toast";
import { ArrowLeft, GraduationCap, Users, BookOpen, Edit, Trash2, UserPlus } from "lucide-react";
import { motion } from "framer-motion";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useClass, useDeleteClass } from "@/features/classes/hooks/use-classes";
import { EditClassSheet } from "@/features/classes/components/class-sheet";

const getYearOfStudyColor = (yearOfStudy: number) => {
  if (yearOfStudy <= 3) return 'bg-blue-100 text-blue-800';
  if (yearOfStudy <= 6) return 'bg-green-100 text-green-800';
  if (yearOfStudy <= 9) return 'bg-yellow-100 text-yellow-800';
  return 'bg-purple-100 text-purple-800';
};

const getYearOfStudyLabel = (yearOfStudy: number) => {
  if (yearOfStudy <= 3) return `Grade ${yearOfStudy} (Primary)`;
  if (yearOfStudy <= 6) return `Grade ${yearOfStudy} (Elementary)`;
  if (yearOfStudy <= 9) return `Grade ${yearOfStudy} (Middle)`;
  return `Grade ${yearOfStudy} (High)`;
};

const getCapacityStatus = (currentStudents: number, maxStudents: number) => {
  const percentage = (currentStudents / maxStudents) * 100;
  if (percentage >= 90) return 'bg-red-100 text-red-800';
  if (percentage >= 70) return 'bg-yellow-100 text-yellow-800';
  return 'bg-green-100 text-green-800';
};

export default function ClassesView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const deleteMutation = useDeleteClass();

  const { data: classItem, isLoading, error, refetch } = useClass(id ? parseInt(id) : undefined);

  const handleDelete = async () => {
    if (!classItem) return;
    
    if (window.confirm(`Are you sure you want to delete the class "${classItem.name}"?`)) {
      try {
        await deleteMutation.mutateAsync(classItem.id);
        toast.success("Class deleted successfully");
        navigate("/admin/classes");
      } catch (error) {
        console.error("Failed to delete class:", error);
        toast.error("Failed to delete class");
      }
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
        <div className="container mx-auto px-4 py-8">
          <div className="flex items-center gap-4 mb-8">
            <Skeleton className="h-8 w-32" />
            <Skeleton className="h-6 w-px" />
            <div>
              <Skeleton className="h-8 w-48 mb-2" />
              <Skeleton className="h-4 w-32" />
            </div>
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 space-y-6">
              <Skeleton className="h-48 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
            <div className="space-y-6">
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !classItem) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
        <div className="container mx-auto px-4 py-8">
          <div className="text-center py-12">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Class Not Found</h2>
            <p className="text-gray-600 mb-6">
              The class you're looking for doesn't exist or has been removed.
            </p>
            <Button onClick={() => navigate("/admin/classes")} variant="outline">
              Back to Classes
            </Button>
          </div>
        </div>
      </div>
    );
  }

  const currentStudents = classItem.studentIds?.length || 0;
  const currentCourses = classItem.courseIds?.length || 0;
  const currentTeachers = classItem.teacherIds?.length || 0;

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="flex items-center gap-4 mb-8"
        >
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate("/admin/classes")}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Classes
          </Button>
          <div className="h-6 w-px bg-gray-300" />
          <div className="flex-1">
            <h1 className="text-3xl font-bold text-gray-900">{classItem.name}</h1>
            <p className="text-gray-600 mt-1">Class details and information</p>
          </div>
          <div className="flex items-center gap-2">
            <EditClassSheet
              classItem={classItem}
              onSuccess={() => refetch()}
              trigger={
                <Button variant="outline" size="sm" className="flex items-center gap-2">
                  <Edit className="h-4 w-4" />
                  Edit
                </Button>
              }
            />
            <Button
              variant="outline"
              size="sm"
              onClick={handleDelete}
              className="flex items-center gap-2 text-red-600 hover:text-red-700 hover:bg-red-50"
            >
              <Trash2 className="h-4 w-4" />
              Delete
            </Button>
          </div>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="lg:col-span-2 space-y-6"
          >
            {/* Class Information */}
            <Card className="border-gray-200 shadow-sm">
              <CardHeader className="bg-gradient-to-r from-blue-50 to-indigo-50 border-b border-gray-200">
                <CardTitle className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                  <GraduationCap className="h-5 w-5 text-blue-600" />
                  Class Information
                </CardTitle>
                <CardDescription>
                  Basic details about this class
                </CardDescription>
              </CardHeader>
              <CardContent className="p-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-3">
                    <div>
                      <label className="text-sm font-medium text-gray-500 uppercase tracking-wide">
                        Class Name
                      </label>
                      <p className="mt-1 text-lg font-semibold text-gray-900">{classItem.name}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-500 uppercase tracking-wide">
                        Year of Study
                      </label>
                      <div className="mt-1">
                        <Badge className={`${getYearOfStudyColor(classItem.yearOfStudy)} font-medium px-3 py-1`}>
                          {getYearOfStudyLabel(classItem.yearOfStudy)}
                        </Badge>
                      </div>
                    </div>
                  </div>
                  <div className="space-y-3">
                    <div>
                      <label className="text-sm font-medium text-gray-500 uppercase tracking-wide">
                        Maximum Students
                      </label>
                      <p className="mt-1 text-lg font-semibold text-gray-900">{classItem.maxStudents}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-500 uppercase tracking-wide">
                        Current Enrollment
                      </label>
                      <div className="mt-1">
                        <Badge className={`${getCapacityStatus(currentStudents, classItem.maxStudents)} font-medium px-3 py-1`}>
                          {currentStudents}/{classItem.maxStudents} students
                        </Badge>
                      </div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Class Statistics */}
            <Card className="border-gray-200 shadow-sm">
              <CardHeader>
                <CardTitle className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                  <BookOpen className="h-5 w-5 text-green-600" />
                  Class Statistics
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-3xl font-bold text-blue-600">{currentStudents}</div>
                    <div className="text-sm text-blue-600 mt-1">Enrolled Students</div>
                  </div>
                  <div className="text-center p-4 bg-green-50 rounded-lg">
                    <div className="text-3xl font-bold text-green-600">{currentCourses}</div>
                    <div className="text-sm text-green-600 mt-1">Assigned Courses</div>
                  </div>
                  <div className="text-center p-4 bg-purple-50 rounded-lg">
                    <div className="text-3xl font-bold text-purple-600">{currentTeachers}</div>
                    <div className="text-sm text-purple-600 mt-1">Assigned Teachers</div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>

          {/* Sidebar */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="space-y-6"
          >
            {/* Quick Actions */}
            <Card className="border-green-200 bg-gradient-to-br from-green-50 to-emerald-50">
              <CardHeader>
                <CardTitle className="text-lg font-semibold text-green-900 flex items-center gap-2">
                  <UserPlus className="h-5 w-5" />
                  Quick Actions
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <Button className="w-full bg-green-600 hover:bg-green-700">
                  <UserPlus className="mr-2 h-4 w-4" />
                  Enroll Students
                </Button>
                <Button variant="outline" className="w-full">
                  <BookOpen className="mr-2 h-4 w-4" />
                  Assign Courses
                </Button>
                <Button variant="outline" className="w-full">
                  <Users className="mr-2 h-4 w-4" />
                  Assign Teachers
                </Button>
              </CardContent>
            </Card>

            {/* Class Details */}
            <Card className="border-blue-200 bg-gradient-to-br from-blue-50 to-indigo-50">
              <CardHeader>
                <CardTitle className="text-lg font-semibold text-blue-900">
                  Class Details
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium text-blue-800">Class ID</span>
                    <span className="text-sm text-blue-700">#{classItem.id}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium text-blue-800">Enrollment Rate</span>
                    <span className="text-sm text-blue-700">
                      {Math.round((currentStudents / classItem.maxStudents) * 100)}%
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-sm font-medium text-blue-800">Available Spots</span>
                    <span className="text-sm text-blue-700">
                      {classItem.maxStudents - currentStudents}
                    </span>
                  </div>
                  {classItem.assignedRoomId && (
                    <div className="flex justify-between items-center">
                      <span className="text-sm font-medium text-blue-800">Assigned Room</span>
                      <span className="text-sm text-blue-700">Room #{classItem.assignedRoomId}</span>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </div>
  );
} 