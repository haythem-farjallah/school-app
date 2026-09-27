import toast from "react-hot-toast";
import { useTranslation } from "react-i18next";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { getApiErrorMessage } from "@/lib/api-error";
import type { Student } from "@/types/student";
import { studentName } from "../display";
import { useDeleteStudent } from "../hooks/use-students";

interface DeleteStudentDialogProps {
  student: Student | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DeleteStudentDialog({ student, open, onOpenChange }: DeleteStudentDialogProps) {
  const { t } = useTranslation();
  const deleteStudent = useDeleteStudent();

  if (!student) return null;
  const name = studentName(student);

  const confirm = () =>
    deleteStudent.mutate(student.id, {
      onSuccess: () => {
        toast.success(t("students.delete.success", { name }));
        onOpenChange(false);
      },
      // The dialog stays open so the user can retry or cancel.
      onError: (error) => toast.error(getApiErrorMessage(error, t("students.delete.error"))),
    });

  return (
    <Dialog open={open} onOpenChange={(next) => !deleteStudent.isPending && onOpenChange(next)}>
      <DialogContent className="w-[calc(100%-2rem)] rounded-xl">
        <DialogHeader className="pe-6 text-start sm:text-start">
          <DialogTitle>{t("students.delete.title", { name })}</DialogTitle>
          <DialogDescription>{t("students.delete.description")}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={deleteStudent.isPending}>
            {t("students.delete.cancel")}
          </Button>
          <Button variant="destructive" onClick={confirm} disabled={deleteStudent.isPending}>
            {deleteStudent.isPending ? t("students.delete.pending") : t("students.delete.confirm")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
