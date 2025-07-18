import * as React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, UserPlus, BookOpen, CheckCircle, AlertCircle } from "lucide-react";
import { motion } from "framer-motion";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import { 
  enrollmentFormDefinition, 
  enrollmentFormFields,
  type EnrollmentFormValues 
} from "@/features/enrollments/enrollmentForm.definition";
import { useEnrollStudent } from "@/features/enrollments/hooks/use-enrollments";
import { useClasses } from "@/features/classes/hooks/use-classes";
import { useStudents } from "@/features/students/hooks/use-students";

export default function EnrollmentsCreate() {
  const navigate = useNavigate();
  const createMutation = useEnrollStudent();

  // Fetch classes and students data for the select options
  const { data: classesPage } = useClasses({ page: 0, size: 100 });
  const { data: studentsPage } = useStudents({ page: 0, size: 100 });
  const classes = classesPage?.data || [];
  const students = studentsPage?.data || [];

  // Create form recipe with dynamic options
  const enrollmentRecipe: FormRecipe = React.useMemo(() => ({
    schema: enrollmentFormDefinition,
    fields: enrollmentFormFields.map(field => {
      if (field.name === "classId") {
        return {
          ...field,
          options: classes.map(cls => ({
            label: cls.name,
            value: cls.id,
          })),
        };
      }
      if (field.name === "studentId") {
        return {
          ...field,
          options: students.map(student => ({
            label: `${student.firstName} ${student.lastName}`,
            value: student.id,
          })),
        };
      }
      return field;
    }),
    onSubmit: async (values: unknown) => {
      try {
        const formData = values as EnrollmentFormValues;
        console.log("Creating enrollment:", formData);
        
        await createMutation.mutateAsync(formData);
        
        toast.success("Student enrolled successfully!");
        navigate("/admin/enrollments");
      } catch (error) {
        console.error("Failed to create enrollment:", error);
        toast.error("Failed to enroll student. Please try again.");
      }
    },
  }), [classes, students, createMutation, navigate]);

  return (
    <div className="container mx-auto py-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-6"
      >
        <div className="flex items-center space-x-4">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate("/admin/enrollments")}
            className="flex items-center space-x-2"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Back to Enrollments</span>
          </Button>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <UserPlus className="h-5 w-5 text-blue-600" />
                  <span>Enroll Student</span>
                </CardTitle>
                <p className="text-sm text-gray-600">
                  Enroll a student in a class to begin their academic journey.
                </p>
              </CardHeader>
              <CardContent>
                <AutoForm
                  recipe={enrollmentRecipe}
                  defaultValues={{
                    status: "ACTIVE",
                  }}
                  submitLabel="Enroll Student"
                  submitClassName="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg transition-colors duration-200"
                />
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <BookOpen className="h-5 w-5 text-green-600" />
                  <span>Enrollment Guidelines</span>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-3">
                  <div className="flex items-start space-x-3">
                    <CheckCircle className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <p className="font-medium text-sm">Student Selection</p>
                      <p className="text-xs text-gray-600">
                        Choose an active student who is not already enrolled in the selected class.
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start space-x-3">
                    <CheckCircle className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <p className="font-medium text-sm">Class Capacity</p>
                      <p className="text-xs text-gray-600">
                        Ensure the class has available capacity before enrolling students.
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start space-x-3">
                    <CheckCircle className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <p className="font-medium text-sm">Status Management</p>
                      <p className="text-xs text-gray-600">
                        Set appropriate status: Active for current students, Pending for future enrollments.
                      </p>
                    </div>
                  </div>

                  <div className="flex items-start space-x-3">
                    <CheckCircle className="h-5 w-5 text-green-500 mt-0.5" />
                    <div>
                      <p className="font-medium text-sm">Grade Entry</p>
                      <p className="text-xs text-gray-600">
                        Final grades are optional and can be added later. Use scale 0-20.
                      </p>
                    </div>
                  </div>
                </div>

                <Separator />

                <div className="space-y-3">
                  <div className="flex items-start space-x-3">
                    <AlertCircle className="h-5 w-5 text-amber-500 mt-0.5" />
                    <div>
                      <p className="font-medium text-sm text-amber-800">Important Notes</p>
                      <ul className="text-xs text-amber-700 space-y-1 mt-1">
                        <li>• Students cannot be enrolled in the same class twice</li>
                        <li>• Class capacity limits will be enforced</li>
                        <li>• Enrollment creates audit trail for tracking</li>
                        <li>• Parents/guardians will be notified automatically</li>
                      </ul>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Quick Actions</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full justify-start"
                  onClick={() => navigate("/admin/students")}
                >
                  <UserPlus className="mr-2 h-4 w-4" />
                  Manage Students
                </Button>
                
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full justify-start"
                  onClick={() => navigate("/admin/classes")}
                >
                  <BookOpen className="mr-2 h-4 w-4" />
                  Manage Classes
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </motion.div>
    </div>
  );
} 