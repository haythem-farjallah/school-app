import * as React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Users2 } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import { useCreateStaff } from "@/features/staff/hooks/use-staff";
import { staffSchema, staffFields, type StaffValues } from "@/features/staff/staffForm.definition";
import type { CreateStaffRequest } from "@/types/staff";

export default function StaffsCreate() {
  const navigate = useNavigate();
  const createStaffMutation = useCreateStaff();

  const handleBack = React.useCallback(() => {
    navigate("/admin/staff");
  }, [navigate]);

  const createStaffRecipe: FormRecipe = {
    schema: staffSchema,
    fields: staffFields,
    onSubmit: async (values: unknown) => {
      console.log("➕ StaffsCreate - Form submitted with values:", values);
      const formValues = values as StaffValues;
      
      const staffData: CreateStaffRequest = {
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone || "",
          birthday: formValues.birthday || undefined,
          gender: (formValues.gender as 'M' | 'F' | 'O') || 'M',
          address: formValues.address || "",
          role: 'STAFF',
        },
        staffType: formValues.staffType,
        department: formValues.department,
      };
      console.log("➕ StaffsCreate - Transformed staff data:", staffData);
      await createStaffMutation.mutateAsync(staffData);
      toast.success("Staff member created successfully!");
      navigate("/admin/staff");
    },
  };

  return (
    <div className="container mx-auto py-8 space-y-8">
      {/* Header */}
      <Card className="border-emerald-200/60 bg-gradient-to-br from-emerald-50/80 via-teal-50/40 to-cyan-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <Button
                variant="ghost"
                size="sm"
                onClick={handleBack}
                className="text-emerald-700 hover:text-emerald-900 hover:bg-emerald-100/50"
              >
                <ArrowLeft className="mr-2 h-4 w-4" />
                Back to Staff
              </Button>
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-emerald-900 via-teal-800 to-cyan-700 bg-clip-text text-transparent">
                  Create New Staff Member
                </CardTitle>
                <CardDescription className="text-emerald-700/80 text-lg">
                  Add a new staff member to the system with all necessary details
                </CardDescription>
              </div>
            </div>
            <div className="p-3 bg-gradient-to-br from-emerald-100 to-teal-100 rounded-lg">
              <Users2 className="h-8 w-8 text-emerald-600" />
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Form */}
      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm max-w-4xl mx-auto">
        <CardHeader className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-emerald-50/40">
          <CardTitle className="text-2xl font-bold text-slate-900">
            Staff Information
          </CardTitle>
          <CardDescription className="text-slate-600">
            Fill in the details below to create a new staff member profile
          </CardDescription>
        </CardHeader>
        <CardContent className="p-8">
          <AutoForm
            recipe={createStaffRecipe}
            defaultValues={{
              firstName: "",
              lastName: "",
              email: "",
              telephone: "",
              birthday: "",
              gender: "",
              address: "",
              staffType: "ADMINISTRATIVE",
              department: "",
            }}
            submitLabel="Create Staff Member"
            submitClassName="bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 shadow-lg hover:shadow-xl transition-all duration-300 w-full"
          />
        </CardContent>
      </Card>
    </div>
  );
} 