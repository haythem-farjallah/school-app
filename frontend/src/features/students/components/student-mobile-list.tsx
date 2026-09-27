import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import type { Student } from "@/types/student";
import { gradeLevelLabel, studentName } from "../display";
import { NotProvided } from "./not-provided";
import { StudentRowActions } from "./student-row-actions";

interface StudentMobileListProps {
  students: Student[];
  basePath: string;
  onEdit: (student: Student) => void;
  onDelete: (student: Student) => void;
}

/** The students list below the md breakpoint, where the table would not fit. */
export function StudentMobileList({ students, basePath, onEdit, onDelete }: StudentMobileListProps) {
  const { t } = useTranslation();

  return (
    <ul className="divide-y divide-border">
      {students.map((student) => {
        const detailPath = `${basePath}/view/${student.id}`;
        return (
          <li key={student.id} className="flex items-start gap-3 px-4 py-3">
            <div className="min-w-0 flex-1 space-y-1">
              <Link to={detailPath} className="block truncate font-medium text-foreground hover:text-primary">
                {studentName(student)}
              </Link>
              <p className="truncate text-sm text-muted-foreground">{student.email}</p>
              <dl className="flex flex-wrap gap-x-4 gap-y-1 text-sm">
                <div className="flex gap-1">
                  <dt className="text-muted-foreground">{t("students.table.gradeLevel")}:</dt>
                  <dd className="text-foreground">{gradeLevelLabel(t, student.gradeLevel) ?? <NotProvided />}</dd>
                </div>
                <div className="flex gap-1">
                  <dt className="text-muted-foreground">{t("students.table.enrollmentYear")}:</dt>
                  <dd className="text-foreground">{student.enrollmentYear ?? <NotProvided />}</dd>
                </div>
              </dl>
            </div>
            <StudentRowActions student={student} detailPath={detailPath} onEdit={onEdit} onDelete={onDelete} />
          </li>
        );
      })}
    </ul>
  );
}
