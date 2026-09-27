import toast from "react-hot-toast";
import { useTranslation } from "react-i18next";

import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import { getApiErrorMessage } from "@/lib/api-error";
import type { Student } from "@/types/student";
import { studentEditFields, studentEditSchema, type StudentEditValues } from "../studentForm.definition";
import { useUpdateStudent } from "../hooks/use-students";
import { studentName } from "../display";

interface EditStudentSheetProps {
  /** The student being edited; the sheet renders nothing without one. */
  student: Student | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** Edits the fields PATCH /v1/students/{id} accepts: name, grade level and enrollment year. */
export function EditStudentSheet({ student, open, onOpenChange }: EditStudentSheetProps) {
  const { t, i18n } = useTranslation();
  const updateStudent = useUpdateStudent();

  if (!student) return null;

  const recipe: FormRecipe = {
    schema: studentEditSchema,
    fields: studentEditFields,
    onSubmit: async (values: unknown) => {
      const { firstName, lastName, gradeLevel, enrollmentYear } = values as StudentEditValues;
      await updateStudent.mutateAsync(
        { id: student.id, firstName, lastName, gradeLevel, enrollmentYear },
        {
          onSuccess: () => {
            toast.success(t("students.edit.success"));
            onOpenChange(false);
          },
          onError: (error) => toast.error(getApiErrorMessage(error, t("students.edit.error"))),
        },
      );
    },
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side={i18n.dir() === "rtl" ? "left" : "right"}
        className="flex w-full flex-col gap-6 overflow-y-auto sm:max-w-md"
      >
        <SheetHeader className="space-y-1 pe-8 text-start sm:text-start">
          <SheetTitle>{t("students.edit.title")}</SheetTitle>
          <SheetDescription>{t("students.edit.description", { name: studentName(student) })}</SheetDescription>
        </SheetHeader>
        <p className="rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">{t("students.edit.profileNote")}</p>
        <AutoForm
          // Remount per student so the form starts from that student's values.
          key={student.id}
          recipe={recipe}
          animate={false}
          defaultValues={{
            firstName: student.firstName,
            lastName: student.lastName,
            gradeLevel: student.gradeLevel ?? "",
            enrollmentYear: student.enrollmentYear ?? undefined,
          }}
          submitLabel="students.edit.submit"
        />
      </SheetContent>
    </Sheet>
  );
}
