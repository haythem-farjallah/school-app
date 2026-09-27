import { ArrowLeft } from "lucide-react";
import toast from "react-hot-toast";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

import { PageContainer } from "@/components/Layout/PageContainer";
import { PageHeader } from "@/components/Layout/PageHeader";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import { getApiErrorMessage } from "@/lib/api-error";
import type { CreateStudentData } from "@/types/student";
import { useCreateStudent } from "@/features/students/hooks/use-students";
import { useStudentsPath } from "@/features/students/paths";
import {
  studentCreateFields,
  studentCreateSchema,
  type StudentCreateValues,
} from "@/features/students/studentForm.definition";

// Optional profile fields left blank are sent as null rather than empty strings.
const blankToNull = (value: string | undefined) => (value?.trim() ? value.trim() : null);

const StudentsCreate = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const studentsPath = useStudentsPath();
  const createStudent = useCreateStudent();

  const recipe: FormRecipe = {
    schema: studentCreateSchema,
    fields: studentCreateFields,
    onSubmit: async (values: unknown) => {
      const form = values as StudentCreateValues;
      const student: CreateStudentData = {
        profile: {
          firstName: form.firstName,
          lastName: form.lastName,
          email: form.email,
          telephone: blankToNull(form.telephone),
          birthday: blankToNull(form.birthday),
          gender: form.gender ?? null,
          address: blankToNull(form.address),
        },
        gradeLevel: form.gradeLevel,
        enrollmentYear: form.enrollmentYear,
      };
      await createStudent.mutateAsync(student, {
        onSuccess: () => {
          toast.success(t("students.create.success"));
          navigate(studentsPath);
        },
        onError: (error) => toast.error(getApiErrorMessage(error, t("students.create.error"))),
      });
    },
  };

  return (
    <PageContainer className="max-w-4xl">
      <PageHeader
        breadcrumb={
          <Link to={studentsPath} className="inline-flex items-center gap-1 hover:text-foreground">
            <ArrowLeft className="size-4 rtl:rotate-180" aria-hidden="true" />
            {t("students.detail.back")}
          </Link>
        }
        title={t("students.create.title")}
        description={t("students.create.description")}
      />

      <section
        aria-label={t("students.create.title")}
        className="rounded-xl border border-border bg-card p-6 text-card-foreground shadow-xs sm:p-8"
      >
        <AutoForm
          recipe={recipe}
          animate={false}
          defaultValues={{ enrollmentYear: new Date().getFullYear() } as StudentCreateValues}
          fieldsClassName="grid gap-x-6 gap-y-5 sm:grid-cols-2"
          submitLabel="students.create.submit"
          submitClassName="sm:w-auto sm:px-6"
        />
      </section>
    </PageContainer>
  );
};

export default StudentsCreate;
