import type { ColumnDef } from "@tanstack/react-table";
import type { TFunction } from "i18next";
import { Link } from "react-router-dom";

import type { Student } from "@/types/student";
import { genderLabel, gradeLevelLabel, studentName } from "../display";
import { NotProvided } from "./not-provided";
import { StudentRowActions } from "./student-row-actions";

interface StudentColumnsOptions {
  t: TFunction;
  basePath: string;
  onEdit: (student: Student) => void;
  onDelete: (student: Student) => void;
}

// Display-only columns: the list endpoints do not sort, so no column offers sorting.
export function getStudentColumns({ t, basePath, onEdit, onDelete }: StudentColumnsOptions): ColumnDef<Student>[] {
  return [
    {
      id: "student",
      header: t("students.table.student"),
      cell: ({ row }) => (
        <Link
          to={`${basePath}/view/${row.original.id}`}
          className="font-medium text-foreground underline-offset-4 hover:text-primary hover:underline focus-visible:underline"
        >
          {studentName(row.original)}
        </Link>
      ),
    },
    {
      accessorKey: "email",
      header: t("students.table.email"),
      cell: ({ row }) => <span className="text-muted-foreground">{row.original.email}</span>,
    },
    {
      accessorKey: "gradeLevel",
      header: t("students.table.gradeLevel"),
      cell: ({ row }) => gradeLevelLabel(t, row.original.gradeLevel) ?? <NotProvided />,
    },
    {
      accessorKey: "enrollmentYear",
      header: t("students.table.enrollmentYear"),
      cell: ({ row }) => row.original.enrollmentYear ?? <NotProvided />,
    },
    {
      accessorKey: "gender",
      header: t("students.table.gender"),
      cell: ({ row }) => genderLabel(t, row.original.gender) ?? <NotProvided />,
    },
    {
      id: "actions",
      header: () => <span className="sr-only">{t("students.table.actions")}</span>,
      cell: ({ row }) => (
        <div className="flex justify-end">
          <StudentRowActions
            student={row.original}
            detailPath={`${basePath}/view/${row.original.id}`}
            onEdit={onEdit}
            onDelete={onDelete}
          />
        </div>
      ),
    },
  ];
}
