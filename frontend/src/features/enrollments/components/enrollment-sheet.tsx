import * as React from "react";
import toast from "react-hot-toast";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { AutoForm } from "@/form/AutoForm";
import { updateEnrollmentStatusFormDefinition, type UpdateEnrollmentStatusFormValues } from "../enrollmentForm.definition";
import { useUpdateEnrollmentStatus } from "../hooks/use-enrollments";
import type { Enrollment } from "@/types/enrollment";

interface EditEnrollmentSheetProps {
  enrollment: Enrollment;
  onSuccess?: () => void;
  trigger: React.ReactNode;
}

export function EditEnrollmentSheet({ enrollment, onSuccess, trigger }: EditEnrollmentSheetProps) {
  const [isOpen, setIsOpen] = React.useState(false);
  const updateMutation = useUpdateEnrollmentStatus();

  const handleSubmit = async (values: unknown) => {
    try {
      const formData = values as UpdateEnrollmentStatusFormValues;
      
      console.log("Updating enrollment status:", { id: enrollment.id, ...formData });
      
      await updateMutation.mutateAsync({
        id: enrollment.id,
        data: formData,
      });

      toast.success("Enrollment status updated successfully");
      setIsOpen(false);
      onSuccess?.();
    } catch (error) {
      console.error("Failed to update enrollment status:", error);
      toast.error("Failed to update enrollment status. Please try again.");
    }
  };

  const defaultValues: UpdateEnrollmentStatusFormValues = {
    status: enrollment.status,
  };

  return (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetTrigger asChild>
        {trigger}
      </SheetTrigger>
      <SheetContent 
        side="right" 
        className="w-full sm:max-w-md bg-white border-l border-gray-200 shadow-xl"
        style={{ zIndex: 1000 }}
      >
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <SheetTitle className="text-xl font-semibold text-gray-900">
            Update Enrollment Status
          </SheetTitle>
          <SheetDescription className="text-gray-600">
            Update the enrollment status for <strong>{enrollment.studentName}</strong> in <strong>{enrollment.className}</strong>.
          </SheetDescription>
        </SheetHeader>

        <div className="py-6">
          <div className="mb-6 p-4 bg-gray-50 rounded-lg border">
            <h4 className="font-medium text-gray-900 mb-2">Current Enrollment</h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Student:</span>
                <span className="font-medium">{enrollment.studentName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Class:</span>
                <span className="font-medium">{enrollment.className}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Current Status:</span>
                <span className="font-medium capitalize">{enrollment.status.toLowerCase()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Enrolled:</span>
                <span className="font-medium">
                  {new Date(enrollment.enrolledAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          </div>

          <AutoForm
            formSchema={updateEnrollmentStatusFormDefinition}
            onSubmit={handleSubmit}
            defaultValues={defaultValues}
            fieldConfig={{
              status: {
                label: "New Status",
                description: "Select the new enrollment status",
                placeholder: "Choose status...",
              },
            }}
            submitButton={{
              text: updateMutation.isPending ? "Updating..." : "Update Status",
              className: "w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg transition-colors duration-200",
              disabled: updateMutation.isPending,
            }}
          >
            <div className="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-200">
              <p className="text-sm text-blue-800">
                <strong>Note:</strong> Changing the enrollment status will affect the student's access to class materials and grade reporting.
              </p>
            </div>
          </AutoForm>
        </div>
      </SheetContent>
    </Sheet>
  );
} 