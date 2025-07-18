import * as React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, User2, Plus } from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import type { CreateParentData } from "@/types/parent";
import { parentSchema, parentFields, type ParentValues } from "@/features/parents/parentForm.definition";
import { useCreateParent } from "@/features/parents/hooks/use-parents";

const ParentsCreate = () => {
  const navigate = useNavigate();

  console.log("➕ ParentsCreate - Component mounted");

  const createParentMutation = useCreateParent();

  const handleFormSubmit = async (values: unknown) => {
    const parentValues = values as ParentValues;
    
    console.log("➕ ParentsCreate - Creating parent with data:", parentValues);
    
    const parentData: CreateParentData = {
      profile: {
        firstName: parentValues.firstName,
        lastName: parentValues.lastName,
        email: parentValues.email,
        telephone: parentValues.telephone,
        birthday: parentValues.birthday,
        gender: parentValues.gender,
        address: parentValues.address,
      },
      preferredContactMethod: parentValues.preferredContactMethod,
      relation: parentValues.relation,
      childrenEmails: parentValues.children?.map((student: { email: string }) => student.email) || [],
    };
    
    console.log("➕ ParentsCreate - Transformed data:", parentData);
    await createParentMutation.mutateAsync(parentData);
  };

  const createParentRecipe: FormRecipe = {
    schema: parentSchema,
    fields: parentFields,
    onSubmit: handleFormSubmit,
  };

  React.useEffect(() => {
    if (createParentMutation.isSuccess) {
      console.log("✅ ParentsCreate - Parent created successfully");
      toast.success("Parent created successfully!");
      navigate("/admin/parents");
    }
  }, [createParentMutation.isSuccess, navigate]);

  React.useEffect(() => {
    if (createParentMutation.isError) {
      console.error("❌ ParentsCreate - Failed to create parent:", createParentMutation.error);
      const error = createParentMutation.error as { response?: { data?: { message?: string } } };
      const message = error?.response?.data?.message || "Failed to create parent";
      toast.error(message);
    }
  }, [createParentMutation.isError, createParentMutation.error]);

  return (
    <div className="space-y-6">
      {/* Header */}
      <Card className="border-purple-200/60 bg-gradient-to-br from-purple-50/80 via-pink-50/40 to-rose-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate("/admin/parents")}
                className="hover:bg-purple-100 hover:text-purple-700 transition-colors duration-200"
              >
                <ArrowLeft className="mr-2 h-4 w-4" />
                Back to Parents
              </Button>
              <div className="space-y-2">
                <CardTitle className="text-3xl font-bold bg-gradient-to-r from-purple-900 via-pink-800 to-rose-700 bg-clip-text text-transparent">
                  Create New Parent
                </CardTitle>
                <CardDescription className="text-purple-700/80 text-lg">
                  Add a new parent to the school management system
                </CardDescription>
              </div>
            </div>
            <div className="p-3 bg-gradient-to-br from-purple-500 to-pink-600 rounded-lg">
              <User2 className="h-6 w-6 text-white" />
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Create Form */}
      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm">
        <CardHeader className="pb-6 border-b border-slate-200/60">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-purple-100 to-pink-100 rounded-lg">
              <Plus className="h-6 w-6 text-purple-600" />
            </div>
            <div>
              <CardTitle className="text-2xl font-bold bg-gradient-to-r from-purple-900 to-pink-700 bg-clip-text text-transparent">
                Parent Information
              </CardTitle>
              <CardDescription className="text-gray-600">
                Fill in the parent's details and assign children below
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="pt-6">
          <div className="max-w-2xl">
            <AutoForm
              recipe={createParentRecipe}
              defaultValues={{
                firstName: "",
                lastName: "",
                email: "",
                telephone: "",
                birthday: "",
                gender: "",
                address: "",
                preferredContactMethod: "",
                relation: "",
                children: [],
              }}
              submitLabel="Create Parent"
              submitClassName="w-full bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 shadow-lg hover:shadow-xl transition-all duration-300"
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ParentsCreate; 