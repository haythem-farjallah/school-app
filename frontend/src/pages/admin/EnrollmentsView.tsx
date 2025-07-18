import * as React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Edit, UserX, ArrowRightLeft, GraduationCap, Calendar, User, BookOpen } from "lucide-react";
import { motion } from "framer-motion";
import { format } from "date-fns";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { useEnrollment, useDropEnrollment } from "@/features/enrollments/hooks/use-enrollments";
import { EditEnrollmentSheet } from "@/features/enrollments/components/enrollment-sheet";
import { getEnrollmentStatusColor, getEnrollmentStatusLabel } from "@/types/enrollment";

export default function EnrollmentsView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const enrollmentId = id ? parseInt(id, 10) : undefined;
  
  const { data: enrollment, isLoading, error, refetch } = useEnrollment(enrollmentId);
  const dropMutation = useDropEnrollment();

  const handleDrop = async () => {
    if (!enrollment) return;

    const reason = prompt("Please provide a reason for dropping this enrollment:");
    if (!reason || reason.trim().length < 3) {
      toast.error("A valid reason is required to drop an enrollment");
      return;
    }

    try {
      await dropMutation.mutateAsync({
        id: enrollment.id,
        data: { reason: reason.trim() },
      });
      toast.success("Student dropped from enrollment successfully");
      navigate("/admin/enrollments");
    } catch (error) {
      console.error("Failed to drop enrollment:", error);
      toast.error("Failed to drop enrollment. Please try again.");
    }
  };

  const handleTransfer = () => {
    toast.info("Transfer functionality coming soon");
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="space-y-6">
          <div className="flex items-center space-x-4">
            <Button variant="outline" size="sm" onClick={() => navigate("/admin/enrollments")}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Enrollments
            </Button>
          </div>

          <div className="grid gap-6 lg:grid-cols-3">
            <div className="lg:col-span-2 space-y-6">
              <Card>
                <CardHeader>
                  <Skeleton className="h-8 w-48" />
                  <Skeleton className="h-4 w-32" />
                </CardHeader>
                <CardContent className="space-y-4">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-4 w-1/2" />
                </CardContent>
              </Card>
            </div>
            <div>
              <Card>
                <CardHeader>
                  <Skeleton className="h-6 w-32" />
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-32 w-full" />
                </CardContent>
              </Card>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !enrollment) {
    return (
      <div className="container mx-auto py-6">
        <div className="space-y-6">
          <div className="flex items-center space-x-4">
            <Button variant="outline" size="sm" onClick={() => navigate("/admin/enrollments")}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Enrollments
            </Button>
          </div>

          <div className="flex items-center justify-center h-64">
            <div className="text-center">
              <p className="text-gray-500 mb-4">
                {error ? "Failed to load enrollment" : "Enrollment not found"}
              </p>
              <Button onClick={() => refetch()}>Try Again</Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-6"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button
              variant="outline"
              size="sm"
              onClick={() => navigate("/admin/enrollments")}
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Enrollments
            </Button>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div>
                    <CardTitle className="flex items-center space-x-2">
                      <GraduationCap className="h-5 w-5 text-blue-600" />
                      <span>Enrollment Details</span>
                    </CardTitle>
                    <p className="text-sm text-gray-600 mt-1">
                      Enrollment #{enrollment.id}
                    </p>
                  </div>
                  <Badge className={`${getEnrollmentStatusColor(enrollment.status)} font-medium px-3 py-1 text-sm rounded-full border`}>
                    {getEnrollmentStatusLabel(enrollment.status)}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-3">
                    <div className="flex items-start space-x-3">
                      <User className="h-5 w-5 text-gray-400 mt-0.5" />
                      <div>
                        <p className="font-medium text-gray-900">Student</p>
                        <p className="text-sm text-gray-600">{enrollment.studentName}</p>
                        <p className="text-xs text-gray-500">{enrollment.studentEmail}</p>
                      </div>
                    </div>

                    <div className="flex items-start space-x-3">
                      <BookOpen className="h-5 w-5 text-gray-400 mt-0.5" />
                      <div>
                        <p className="font-medium text-gray-900">Class</p>
                        <p className="text-sm text-gray-600">{enrollment.className}</p>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-3">
                    <div className="flex items-start space-x-3">
                      <Calendar className="h-5 w-5 text-gray-400 mt-0.5" />
                      <div>
                        <p className="font-medium text-gray-900">Enrolled Date</p>
                        <p className="text-sm text-gray-600">
                          {format(new Date(enrollment.enrolledAt), "MMMM dd, yyyy")}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-start space-x-3">
                      <GraduationCap className="h-5 w-5 text-gray-400 mt-0.5" />
                      <div>
                        <p className="font-medium text-gray-900">Academic Progress</p>
                        <p className="text-sm text-gray-600">
                          {enrollment.gradeCount || 0} grades recorded
                        </p>
                        {enrollment.finalGrad && (
                          <p className="text-sm font-medium text-blue-600">
                            Final Grade: {enrollment.finalGrad.toFixed(2)}/20
                          </p>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                <Separator />

                <div>
                  <h4 className="font-medium text-gray-900 mb-3">Enrollment Information</h4>
                  <div className="grid gap-3 md:grid-cols-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Enrollment ID:</span>
                      <span className="font-medium">#{enrollment.id}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Status:</span>
                      <Badge className={`${getEnrollmentStatusColor(enrollment.status)} text-xs`}>
                        {getEnrollmentStatusLabel(enrollment.status)}
                      </Badge>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Student ID:</span>
                      <span className="font-medium">#{enrollment.studentId}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Class ID:</span>
                      <span className="font-medium">#{enrollment.classId}</span>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Quick Actions</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <EditEnrollmentSheet
                  enrollment={enrollment}
                  onSuccess={refetch}
                  trigger={
                    <Button className="w-full justify-start bg-green-600 hover:bg-green-700 text-white">
                      <Edit className="mr-3 h-4 w-4" />
                      Edit Status
                    </Button>
                  }
                />

                <Button
                  variant="outline"
                  className="w-full justify-start"
                  onClick={handleTransfer}
                >
                  <ArrowRightLeft className="mr-3 h-4 w-4" />
                  Transfer Student
                </Button>

                <Separator />

                <Button
                  variant="destructive"
                  className="w-full justify-start"
                  onClick={handleDrop}
                  disabled={dropMutation.isPending}
                >
                  <UserX className="mr-3 h-4 w-4" />
                  {dropMutation.isPending ? "Dropping..." : "Drop Enrollment"}
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Navigation</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full justify-start"
                  onClick={() => navigate(`/admin/students/view/${enrollment.studentId}`)}
                >
                  <User className="mr-2 h-4 w-4" />
                  View Student Profile
                </Button>

                <Button
                  variant="outline"
                  size="sm"
                  className="w-full justify-start"
                  onClick={() => navigate(`/admin/classes/view/${enrollment.classId}`)}
                >
                  <BookOpen className="mr-2 h-4 w-4" />
                  View Class Details
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </motion.div>
    </div>
  );
} 