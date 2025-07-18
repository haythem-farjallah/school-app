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
import type { Teacher, CreateTeacherData } from "@/types/teacher";
import { teacherSchema, teacherFields, type TeacherValues } from "@/features/teachers/teacherForm.definition";

const TeachersCreate = () => {
  const navigate = useNavigate();

  console.log("➕ TeachersCreate - Component mounted");

  const createTeacherMutation = useMutationApi<Teacher, CreateTeacherData>(
    async (data) => {
      console.log("➕ TeachersCreate - Creating teacher with data:", data);
      const response = await http.post<{ status: string; data: Teacher }>("/admin/teachers", data);
      console.log("➕ TeachersCreate - Response:", response.data);
      return response.data.data;
    },
    {
      onSuccess: () => {
        console.log("✅ TeachersCreate - Teacher created successfully");
        toast.success("Teacher created successfully!");
        navigate("/admin/teachers");
      },
      onError: (error: unknown) => {
        console.error("❌ TeachersCreate - Failed to create teacher:", error);
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to create teacher";
        toast.error(message);
      },
    }
  );

  const createTeacherRecipe: FormRecipe = {
    schema: teacherSchema,
    fields: teacherFields,
    onSubmit: async (values: unknown) => {
      console.log("➕ TeachersCreate - Form submitted with values:", values);
      const formValues = values as TeacherValues;
      const teacherData: CreateTeacherData = {
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone,
          birthday: formValues.birthday,
          gender: formValues.gender,
          address: formValues.address,
        },
        qualifications: formValues.qualifications,
        subjectsTaught: formValues.subjectsTaught,
        availableHours: typeof formValues.availableHours === 'string' 
          ? parseInt(formValues.availableHours, 10) 
          : formValues.availableHours,
        schedulePreferences: formValues.schedulePreferences,
      };
      console.log("➕ TeachersCreate - Transformed teacher data:", teacherData);
      await createTeacherMutation.mutateAsync(teacherData);
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
                onClick={() => navigate("/admin/teachers")}
                className="hover:bg-blue-100 transition-colors duration-200"
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Teachers
              </Button>
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-blue-900 via-indigo-800 to-purple-700 bg-clip-text text-transparent">
                  Create New Teacher
                </CardTitle>
                <CardDescription className="text-blue-700/80 text-lg">
                  Add a new teacher to the system
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
                Teacher Information
              </h3>
              <p className="text-slate-600">
                Fill in the teacher details below. All required fields are marked with an asterisk (*).
              </p>
            </div>
            
            <div className="bg-gradient-to-r from-blue-50/80 to-indigo-50/40 p-8 rounded-xl border border-blue-200/50 shadow-sm">
              <AutoForm
                recipe={createTeacherRecipe}
                submitLabel="Create Teacher"
                submitClassName="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300 w-full"
              />
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default TeachersCreate; 