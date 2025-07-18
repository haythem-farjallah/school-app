import * as React from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { ArrowLeft, GraduationCap, Users, BookOpen } from "lucide-react";
import { motion } from "framer-motion";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { AutoForm } from "@/form/AutoForm";
import { classFormDefinition, type ClassFormValues } from "@/features/classes/classForm.definition";
import { useCreateClass } from "@/features/classes/hooks/use-classes";

export default function ClassesCreate() {
  const navigate = useNavigate();
  const createMutation = useCreateClass();

  const handleSubmit = async (values: unknown) => {
    try {
      const classValues = values as ClassFormValues;
      await createMutation.mutateAsync(classValues);
      toast.success("Class created successfully");
      navigate("/admin/classes");
    } catch (error) {
      console.error("Failed to create class:", error);
      toast.error("Failed to create class");
    }
  };

  const formDefinition = React.useMemo(() => ({
    ...classFormDefinition,
    onSubmit: handleSubmit,
  }), [handleSubmit]);

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
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Create New Class</h1>
            <p className="text-gray-600 mt-1">Add a new class to your school system</p>
          </div>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Form Section */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="lg:col-span-2"
          >
            <Card className="border-gray-200 shadow-sm">
              <CardHeader className="bg-gradient-to-r from-blue-50 to-indigo-50 border-b border-gray-200">
                <CardTitle className="text-xl font-semibold text-gray-900 flex items-center gap-2">
                  <GraduationCap className="h-5 w-5 text-blue-600" />
                  Class Information
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6">
                <AutoForm
                  recipe={formDefinition}
                  submitLabel="Create Class"
                />
              </CardContent>
            </Card>
          </motion.div>

          {/* Guidelines Section */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="space-y-6"
          >
            <Card className="border-blue-200 bg-gradient-to-br from-blue-50 to-indigo-50">
              <CardHeader>
                <CardTitle className="text-lg font-semibold text-blue-900 flex items-center gap-2">
                  <Users className="h-5 w-5" />
                  Class Guidelines
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <h4 className="font-medium text-blue-800">Class Name</h4>
                  <p className="text-sm text-blue-700">
                    Use a descriptive name that includes grade level and section (e.g., "Grade 10A", "Computer Science 2024")
                  </p>
                </div>
                <div className="space-y-2">
                  <h4 className="font-medium text-blue-800">Year of Study</h4>
                  <p className="text-sm text-blue-700">
                    Select the appropriate grade level (1-12) based on your school's system
                  </p>
                </div>
                <div className="space-y-2">
                  <h4 className="font-medium text-blue-800">Maximum Students</h4>
                  <p className="text-sm text-blue-700">
                    Set a reasonable capacity based on classroom size and teaching effectiveness
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card className="border-green-200 bg-gradient-to-br from-green-50 to-emerald-50">
              <CardHeader>
                <CardTitle className="text-lg font-semibold text-green-900 flex items-center gap-2">
                  <BookOpen className="h-5 w-5" />
                  Best Practices
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-start gap-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full mt-2 flex-shrink-0"></div>
                  <p className="text-sm text-green-700">
                    Consider optimal class sizes for different subjects and age groups
                  </p>
                </div>
                <div className="flex items-start gap-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full mt-2 flex-shrink-0"></div>
                  <p className="text-sm text-green-700">
                    Ensure class names are unique and easily identifiable
                  </p>
                </div>
                <div className="flex items-start gap-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full mt-2 flex-shrink-0"></div>
                  <p className="text-sm text-green-700">
                    Plan for future enrollment growth when setting capacity
                  </p>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </div>
  );
} 