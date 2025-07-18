import React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, User, Edit, Mail, Phone, Calendar, BookOpen, Clock, MapPin, Award, Users } from "lucide-react";
import toast from "react-hot-toast";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useTeacher } from "@/features/teachers/hooks/use-teachers";
import { EditTeacherSheet } from "@/features/teachers/components/teacher-sheet";

const TeachersView = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const teacherId = id ? parseInt(id, 10) : undefined;

  console.log("👁️ TeachersView - Component mounted with id:", id, "parsed teacherId:", teacherId);

  const { data: teacher, isLoading, error } = useTeacher(teacherId);

  console.log("👁️ TeachersView - Hook result:", {
    teacher,
    isLoading,
    error: error?.message,
    hasData: !!teacher
  });

  if (isLoading) {
    console.log("👁️ TeachersView - Loading state");
    return (
      <div className="space-y-6">
        <Card className="border-blue-200/60 bg-gradient-to-br from-blue-50/80 via-indigo-50/40 to-purple-50/20 shadow-xl backdrop-blur-sm">
          <CardHeader className="pb-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate("/admin/teachers")}
                  className="hover:bg-blue-100 transition-colors duration-200"
                >
                  <ArrowLeft className="h-4 w-4 mr-2" />
                  Back to Teachers
                </Button>
                <div className="space-y-2">
                  <div className="h-8 bg-slate-200 rounded w-48 animate-pulse"></div>
                  <div className="h-4 bg-slate-200 rounded w-32 animate-pulse"></div>
                </div>
              </div>
            </div>
          </CardHeader>
        </Card>
        <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
          <CardContent className="pt-6">
            <div className="h-4 bg-slate-200 rounded w-3/4 animate-pulse"></div>
            <div className="h-4 bg-slate-200 rounded w-1/2 animate-pulse mt-2"></div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error || !teacher) {
    console.error("❌ TeachersView - Error or no teacher data:", { error, teacher });
    return (
      <div className="space-y-6">
        <Card className="border-red-200 shadow-lg">
          <CardContent className="pt-6">
            <div className="text-center text-red-600">
              Failed to load teacher. Please try again.
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  console.log("👁️ TeachersView - Rendering teacher data:", teacher);
  
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
    switch (gender.toUpperCase()) {
      case 'M': return "Male";
      case 'F': return "Female";
      case 'O': return "Other";
      default: return gender;
    }
  };

  const formatAvailableHours = (hours: number | null | undefined) => {
    if (!hours) return "Not specified";
    return `${hours} hours/week`;
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
                onClick={() => navigate("/admin/teachers")}
                className="hover:bg-blue-100 transition-colors duration-200"
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Teachers
              </Button>
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-blue-900 via-indigo-800 to-purple-700 bg-clip-text text-transparent">
                  Teacher Details
                </CardTitle>
                <CardDescription className="text-blue-700/80 text-lg">
                  View teacher information and details
                </CardDescription>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <EditTeacherSheet 
                teacher={teacher} 
                onSuccess={() => {
                  console.log("✅ TeachersView - Teacher updated successfully");
                  // The EditTeacherSheet already shows a success toast, so we don't need to show another one
                }}
                trigger={
                  <Button 
                    variant="outline" 
                    className="hover:bg-blue-50 hover:text-blue-700 transition-colors duration-200"
                  >
                    <Edit className="h-4 w-4 mr-2" />
                    Edit Teacher
                  </Button>
                }
              />
              <div className="p-3 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg">
                <Users className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Teacher Details */}
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
                  <p className="text-lg font-semibold text-slate-900">{teacher?.firstName || "Not provided"}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600">Last Name</label>
                  <p className="text-lg font-semibold text-slate-900">{teacher?.lastName || "Not provided"}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                    <Mail className="h-4 w-4" />
                    Email
                  </label>
                  <p className="text-lg font-semibold text-slate-900">{teacher?.email || "Not provided"}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                    <Phone className="h-4 w-4" />
                    Phone
                  </label>
                  <p className="text-lg font-semibold text-slate-900">
                    {teacher?.telephone || "Not provided"}
                  </p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                    <Calendar className="h-4 w-4" />
                    Birthday
                  </label>
                  <p className="text-lg font-semibold text-slate-900">
                    {formatDate(teacher?.birthday)}
                  </p>
                </div>
                <div>
                  <label className="text-sm font-medium text-slate-600">Gender</label>
                  <p className="text-lg font-semibold text-slate-900">
                    {formatGender(teacher?.gender)}
                  </p>
                </div>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                  <MapPin className="h-4 w-4" />
                  Address
                </label>
                <p className="text-lg font-semibold text-slate-900">
                  {teacher?.address || "Not provided"}
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Professional Information */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-xl">
                <Award className="h-5 w-5 text-green-600" />
                Professional Information
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="text-sm font-medium text-slate-600">Qualifications</label>
                <p className="text-lg font-semibold text-slate-900">
                  {teacher?.qualifications || "Not provided"}
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                  <BookOpen className="h-4 w-4" />
                  Subjects Taught
                </label>
                <p className="text-lg font-semibold text-slate-900">
                  {teacher?.subjectsTaught || "Not provided"}
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-600 flex items-center gap-1">
                  <Clock className="h-4 w-4" />
                  Available Hours
                </label>
                <Badge 
                  variant="outline" 
                  className="bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 hover:from-green-200 hover:to-emerald-200 border border-green-200/60 font-semibold transition-colors duration-200 mt-1"
                >
                  {formatAvailableHours(teacher?.availableHours)}
                </Badge>
              </div>
              <div>
                <label className="text-sm font-medium text-slate-600">Schedule Preferences</label>
                <p className="text-lg font-semibold text-slate-900">
                  {teacher?.schedulePreferences || "Not provided"}
                </p>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Teacher ID */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="text-lg">Teacher ID</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-600">#{teacher?.id}</div>
            </CardContent>
          </Card>

          {/* Status */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="text-lg">Status</CardTitle>
            </CardHeader>
            <CardContent>
              <Badge 
                variant="secondary" 
                className="bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 hover:from-green-200 hover:to-emerald-200 border border-green-200/60 font-semibold transition-colors duration-200"
              >
                Active
              </Badge>
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
            <CardHeader>
              <CardTitle className="text-lg">Quick Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Button 
                variant="outline" 
                className="w-full justify-start hover:bg-blue-50 hover:text-blue-700 transition-colors duration-200"
                onClick={() => {
                  toast.info("View schedule functionality coming soon!");
                }}
              >
                <Calendar className="h-4 w-4 mr-2" />
                View Schedule
              </Button>
              <Button 
                variant="outline" 
                className="w-full justify-start hover:bg-green-50 hover:text-green-700 transition-colors duration-200"
                onClick={() => {
                  toast.info("View classes functionality coming soon!");
                }}
              >
                <BookOpen className="h-4 w-4 mr-2" />
                View Classes
              </Button>
              <Button 
                variant="outline" 
                className="w-full justify-start hover:bg-purple-50 hover:text-purple-700 transition-colors duration-200"
                onClick={() => {
                  toast.info("View performance functionality coming soon!");
                }}
              >
                <Award className="h-4 w-4 mr-2" />
                View Performance
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default TeachersView; 