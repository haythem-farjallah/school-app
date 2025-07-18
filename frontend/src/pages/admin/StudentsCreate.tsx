import * as React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, User, Plus } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import { useMutationApi } from "@/hooks/useMutationApi";
import { http } from "@/lib/http";
import type { Student, CreateStudentData } from "@/types/student";
import { studentSchema, studentFields, type StudentValues } from "@/features/students/studentForm.definition";

const StudentsCreate = () => {
  const navigate = useNavigate();

  console.log("➕ StudentsCreate - Component mounted");

  const createStudentMutation = useMutationApi<Student, CreateStudentData>(
    async (data) => {
      console.log("➕ StudentsCreate - Creating student with data:", data);
      const response = await http.post<{ status: string; data: Student }>("/v1/students", data);
      console.log("➕ StudentsCreate - Response:", response.data);
      return response.data.data;
    },
    {
      onSuccess: () => {
        console.log("✅ StudentsCreate - Student created successfully");
        toast.success("Student created successfully!");
        navigate("/admin/students");
      },
      onError: (error: unknown) => {
        console.error("❌ StudentsCreate - Failed to create student:", error);
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to create student";
        toast.error(message);
      },
    }
  );

  const createStudentRecipe: FormRecipe = {
    schema: studentSchema,
    fields: studentFields,
    onSubmit: async (values: unknown) => {
      console.log("➕ StudentsCreate - Form submitted with values:", values);
      const formValues = values as StudentValues;
      const studentData: CreateStudentData = {
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone,
          birthday: formValues.birthday,
          gender: formValues.gender,
          address: formValues.address,
        },
        gradeLevel: formValues.gradeLevel,
        enrollmentYear: formValues.enrollmentYear || new Date().getFullYear(),
      };
      console.log("➕ StudentsCreate - Transformed student data:", studentData);
      await createStudentMutation.mutateAsync(studentData);
    },
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
                  Create New Student
                </CardTitle>
                <CardDescription className="text-blue-700/80 text-lg">
                  Add a new student to the system
                </CardDescription>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <div className="p-3 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg">
                <User className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Form */}
      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
        <CardContent className="p-8">
          <div className="max-w-4xl mx-auto">
            <div className="mb-8">
              <h3 className="text-xl font-semibold text-slate-900 mb-2 flex items-center gap-2">
                <Plus className="h-5 w-5 text-blue-600" />
                Student Information
              </h3>
              <p className="text-slate-600">
                Fill in the student details below. All required fields are marked with an asterisk (*).
              </p>
            </div>
            
            <div className="bg-gradient-to-r from-blue-50/80 to-indigo-50/40 p-8 rounded-xl border border-blue-200/50 shadow-sm">
              <AutoForm
                recipe={createStudentRecipe}
                submitLabel="Create Student"
                submitClassName="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300 w-full"
              />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default StudentsCreate; 