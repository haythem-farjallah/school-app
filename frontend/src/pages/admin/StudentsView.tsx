import * as React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, User, Mail, Phone, Calendar, Home, GraduationCap, BookOpen, Edit } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { useStudent } from "@/features/students/hooks/use-students";
import { EditStudentSheet } from "@/features/students/components/student-sheet";

const StudentsView = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const studentId = id ? parseInt(id, 10) : undefined;

  console.log("👁️ StudentsView - Component mounted with id:", id, "parsed studentId:", studentId);

  const { data: student, isLoading, error } = useStudent(studentId);

  console.log("👁️ StudentsView - Hook result:", {
    student,
    isLoading,
    error: error?.message,
    hasData: !!student
  });

  if (isLoading) {
    console.log("👁️ StudentsView - Loading state");
    return (
      <div className="space-y-6">
        <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/80 via-indigo-50/40 to-purple-50/20 shadow-xl backdrop-blur-sm">
          <CardContent className="p-6">
            <div className="animate-pulse">
              <div className="h-8 bg-blue-200 rounded w-1/3 mb-4"></div>
              <div className="h-4 bg-blue-200 rounded w-1/2"></div>
            </div>
          </CardContent>
        </Card>
        <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
          <CardContent className="p-8">
            <div className="animate-pulse space-y-4">
              <div className="h-4 bg-slate-200 rounded w-full"></div>
              <div className="h-4 bg-slate-200 rounded w-3/4"></div>
              <div className="h-4 bg-slate-200 rounded w-1/2"></div>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error || !student) {
    console.error("❌ StudentsView - Error or no student data:", { error, student });
    return (
      <div className="space-y-6">
        <Card className="border-red-200 shadow-lg">
          <CardContent className="pt-6">
            <div className="text-center text-red-600">
              Failed to load student. Please try again.
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  console.log("👁️ StudentsView - Rendering student data:", student);
  
  const formatDate = (dateString: string | null | undefined) => {
    if (!dateString) return "Not provided";
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return "Invalid date";
    }
  };

  const formatGender = (gender: string | null | undefined) => {
    if (!gender) return "Not specified";
    switch (gender) {
      case "M": return "Male";
      case "F": return "Female";
      case "O": return "Other";
      default: return gender;
    }
  };

  const formatEnrollmentYear = (year: number | null | undefined) => {
    if (!year) return "Not specified";
    return year.toString();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/80 via-indigo-50/40 to-purple-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate("/admin/students")}
                className="hover:bg-blue-100 transition-colors duration-200"
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Students
              </Button>
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-blue-900 via-indigo-800 to-purple-700 bg-clip-text text-transparent">
                  Student Details
                </CardTitle>
                <CardDescription className="text-blue-700/80 text-lg">
                  View student information and details
                </CardDescription>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <EditStudentSheet 
                student={student} 
                onSuccess={() => {
                  console.log("✅ StudentsView - Student updated successfully");
                  toast.success("Student updated successfully!");
                }}
                trigger={
                  <Button 
                    variant="outline" 
                    className="hover:bg-blue-50 hover:text-blue-700 transition-colors duration-200"
                  >
                    <Edit className="h-4 w-4 mr-2" />
                    Edit Student
                  </Button>
                }
              />
              <div className="p-3 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg">
                <User className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Student Details */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Personal Information */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-xl">
                <User className="h-5 w-5 text-blue-600" />
                Personal Information
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-slate-600">First Name</label>
                  <p className="text-lg font-semibold text-slate-900">{student?.firstName || "Not provided"}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600">Last Name</label>
                  <p className="text-lg font-semibold text-slate-900">{student?.lastName || "Not provided"}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                    <Mail className="h-4 w-4" />
                    Email
                  </label>
                  <p className="text-lg font-semibold text-slate-900">{student?.email || "Not provided"}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                    <Phone className="h-4 w-4" />
                    Phone
                  </label>
                  <p className="text-lg font-semibold text-slate-900">
                    {student?.telephone || "Not provided"}
                  </p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                    <Calendar className="h-4 w-4" />
                    Birthday
                  </label>
                  <p className="text-lg font-semibold text-slate-900">
                    {formatDate(student?.birthday)}
                  </p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600">Gender</label>
                  <p className="text-lg font-semibold text-slate-900">
                    {formatGender(student?.gender)}
                  </p>
                </div>
              </div>
              <Separator />
              <div>
                <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                  <Home className="h-4 w-4" />
                  Address
                </label>
                                  <p className="text-lg font-semibold text-slate-900">
                    {student?.address || "Not provided"}
                  </p>
              </div>
            </CardContent>
          </Card>

          {/* Academic Information */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-xl">
                <GraduationCap className="h-5 w-5 text-green-600" />
                Academic Information
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                    <GraduationCap className="h-4 w-4" />
                    Grade Level
                  </label>
                  <Badge 
                    variant="outline" 
                    className="bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 hover:from-green-200 hover:to-emerald-200 border border-green-200/60 font-semibold transition-colors duration-200 mt-1"
                  >
                    {student?.gradeLevel || "Not specified"}
                  </Badge>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                    <BookOpen className="h-4 w-4" />
                    Enrollment Year
                  </label>
                  <Badge 
                    variant="secondary" 
                    className="bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 hover:from-purple-200 hover:to-pink-200 border border-purple-200/60 font-semibold transition-colors duration-200 mt-1"
                  >
                    {formatEnrollmentYear(student?.enrollmentYear)}
                  </Badge>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Student ID */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="text-lg">Student ID</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-600">#{student?.id}</div>
            </CardContent>
          </Card>

          {/* Status */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="text-lg">Status</CardTitle>
            </CardHeader>
            <CardContent>
              <Badge 
                variant="outline" 
                className="bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 hover:from-green-200 hover:to-emerald-200 border border-green-200/60 font-semibold transition-colors duration-200"
              >
                Active
              </Badge>
            </CardContent>
          </Card>

          {/* Created Date */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="text-lg">Created</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-600">
                Unknown
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default StudentsView; 