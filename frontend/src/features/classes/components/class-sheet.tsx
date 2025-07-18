import * as React from "react";
import { toast } from "react-hot-toast";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { AutoForm } from "@/form/AutoForm";
import { classFormDefinition, type ClassFormValues } from "../classForm.definition";
import { useUpdateClass } from "../hooks/use-classes";
import type { Class } from "@/types/class";

interface EditClassSheetProps {
  classItem: Class;
  onSuccess?: () => void;
  trigger: React.ReactNode;
}

export function EditClassSheet({ classItem, onSuccess, trigger }: EditClassSheetProps) {
  const [isOpen, setIsOpen] = React.useState(false);
  const updateMutation = useUpdateClass();

  const handleSubmit = async (values: unknown) => {
    try {
      const classValues = values as ClassFormValues;
      console.log("📝 EditClassSheet - Submitting values:", classValues);
      await updateMutation.mutateAsync({
        id: classItem.id,
        data: classValues,
      });
      toast.success("Class updated successfully");
      setIsOpen(false);
      onSuccess?.();
    } catch (error) {
      console.error("Failed to update class:", error);
      toast.error("Failed to update class");
    }
  };

  const formDefinition = React.useMemo(() => ({
    ...classFormDefinition,
    onSubmit: handleSubmit,
  }), [handleSubmit]);

  const defaultValues = React.useMemo(() => ({
    name: classItem.name,
    yearOfStudy: classItem.yearOfStudy,
    maxStudents: classItem.maxStudents,
  }), [classItem]);

  console.log("🔧 EditClassSheet - Class item:", classItem);
  console.log("📋 EditClassSheet - Default values:", defaultValues);

  return (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetTrigger asChild>
        {trigger}
      </SheetTrigger>
      <SheetContent className="sm:max-w-md bg-white border-l shadow-xl">
        <SheetHeader className="pb-6">
          <SheetTitle className="text-xl font-semibold text-gray-900">Edit Class</SheetTitle>
          <SheetDescription className="text-gray-600">
            Update the class information. Click save when you're done.
          </SheetDescription>
        </SheetHeader>
        <div className="mt-6 space-y-6">
          <AutoForm
            recipe={formDefinition}
            defaultValues={defaultValues}
            submitLabel="Save Changes"
          />
        </div>
      </SheetContent>
    </Sheet>
  );
} 