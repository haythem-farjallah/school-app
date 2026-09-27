import { Eye, MoreHorizontal, Pencil, Trash2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import type { Student } from "@/types/student";
import { studentName } from "../display";

interface StudentRowActionsProps {
  student: Student;
  detailPath: string;
  onEdit: (student: Student) => void;
  onDelete: (student: Student) => void;
}

export function StudentRowActions({ student, detailPath, onEdit, onDelete }: StudentRowActionsProps) {
  const { t, i18n } = useTranslation();

  return (
    // Non-modal, so the edit sheet or delete dialog opened from an item can take focus cleanly.
    <DropdownMenu dir={i18n.dir()} modal={false}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="size-9 text-muted-foreground hover:text-foreground"
          aria-label={t("students.actions.menu", { name: studentName(student) })}
        >
          <MoreHorizontal aria-hidden="true" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-44">
        <DropdownMenuItem asChild>
          <Link to={detailPath}>
            <Eye className="me-2 size-4" aria-hidden="true" />
            {t("students.actions.view")}
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={() => onEdit(student)}>
          <Pencil className="me-2 size-4" aria-hidden="true" />
          {t("students.actions.edit")}
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          onSelect={() => onDelete(student)}
          className="text-destructive focus:bg-destructive/10 focus:text-destructive"
        >
          <Trash2 className="me-2 size-4" aria-hidden="true" />
          {t("students.actions.delete")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
