import * as React from "react";
import { ArrowLeft, Pencil } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";

import { PageContainer } from "@/components/Layout/PageContainer";
import { PageHeader } from "@/components/Layout/PageHeader";
import { ErrorState } from "@/components/Shared/ErrorState";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EditStudentSheet } from "@/features/students/components/student-sheet";
import { formatStudentDate, genderLabel, gradeLevelLabel, studentName } from "@/features/students/display";
import { useStudent } from "@/features/students/hooks/use-students";
import { useStudentsPath } from "@/features/students/paths";
import { cn } from "@/lib/utils";

const StudentsView = () => {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const studentsPath = useStudentsPath();
  const studentId = id && /^\d+$/.test(id) ? Number(id) : undefined;
  const { data: student, isPending, isError, error, refetch } = useStudent(studentId);
  const [editOpen, setEditOpen] = React.useState(false);

  const back = (
    <Link to={studentsPath} className="inline-flex items-center gap-1 hover:text-foreground">
      <ArrowLeft className="size-4 rtl:rotate-180" aria-hidden="true" />
      {t("students.detail.back")}
    </Link>
  );
  const backButton = (
    <Button variant="outline" asChild>
      <Link to={studentsPath}>{t("students.detail.back")}</Link>
    </Button>
  );

  if (studentId === undefined || (isError && error.response?.status === 404)) {
    return (
      <PageContainer>
        <PageHeader breadcrumb={back} title={t("students.detail.title")} />
        <ErrorState
          title={t("students.error.notFoundTitle")}
          description={t("students.error.notFoundDescription")}
          action={backButton}
          className="rounded-xl border border-border bg-card"
        />
      </PageContainer>
    );
  }

  if (isError) {
    return (
      <PageContainer>
        <PageHeader breadcrumb={back} title={t("students.detail.title")} />
        <ErrorState
          title={t("students.error.detailTitle")}
          description={t("students.error.detailDescription")}
          onRetry={() => void refetch()}
          action={backButton}
          className="rounded-xl border border-border bg-card"
        />
      </PageContainer>
    );
  }

  if (isPending) {
    return (
      <PageContainer>
        <PageHeader breadcrumb={back} title={t("students.detail.title")} />
        <div role="status" aria-label={t("students.detail.loading")} className="space-y-6">
          <div className="flex items-center gap-4 rounded-xl border border-border bg-card p-6">
            <Skeleton className="size-14 rounded-full" />
            <div className="space-y-2">
              <Skeleton className="h-5 w-48" />
              <Skeleton className="h-4 w-64" />
            </div>
          </div>
          <div className="grid gap-6 lg:grid-cols-3">
            <Skeleton className="h-64 rounded-xl lg:col-span-2" />
            <Skeleton className="h-64 rounded-xl" />
          </div>
        </div>
      </PageContainer>
    );
  }

  const notProvided = t("students.detail.notProvided");
  const name = studentName(student);
  const initials = `${student.firstName.charAt(0)}${student.lastName.charAt(0)}`.toUpperCase();

  const personal: [string, string][] = [
    [t("students.form.firstName.label"), student.firstName || notProvided],
    [t("students.form.lastName.label"), student.lastName || notProvided],
    [t("students.form.email.label"), student.email || notProvided],
    [t("students.form.phone.label"), student.telephone?.trim() || notProvided],
    [t("students.form.birthday.label"), formatStudentDate(t, student.birthday, i18n.language)],
    [t("students.form.gender.label"), genderLabel(t, student.gender) ?? notProvided],
    [t("students.form.address.label"), student.address?.trim() || notProvided],
  ];
  const academic: [string, string][] = [
    [t("students.form.gradeLevel.label"), gradeLevelLabel(t, student.gradeLevel) ?? notProvided],
    [t("students.form.enrollmentYear.label"), student.enrollmentYear != null ? String(student.enrollmentYear) : notProvided],
    [t("students.detail.studentId"), `#${student.id}`],
  ];

  return (
    <PageContainer>
      <PageHeader
        breadcrumb={back}
        title={t("students.detail.title")}
        actions={
          <Button onClick={() => setEditOpen(true)}>
            <Pencil aria-hidden="true" />
            {t("students.detail.edit")}
          </Button>
        }
      />

      <section
        aria-label={name}
        className="flex items-center gap-4 rounded-xl border border-border bg-card p-6 text-card-foreground shadow-xs"
      >
        <div
          aria-hidden="true"
          className="flex size-14 shrink-0 items-center justify-center rounded-full bg-primary-soft text-lg font-semibold text-primary"
        >
          {initials}
        </div>
        <div className="min-w-0">
          <p className="truncate text-xl font-semibold text-foreground">
            <bdi>{name}</bdi>
          </p>
          <p className="truncate text-sm text-muted-foreground">
            <bdi>{student.email}</bdi>
          </p>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-3">
        <DetailCard title={t("students.detail.personal")} items={personal} className="lg:col-span-2" />
        <DetailCard title={t("students.detail.academic")} items={academic} />
      </div>

      <EditStudentSheet student={student} open={editOpen} onOpenChange={setEditOpen} />
    </PageContainer>
  );
};

function DetailCard({ title, items, className }: { title: string; items: [string, string][]; className?: string }) {
  const titleId = React.useId();

  return (
    <section
      aria-labelledby={titleId}
      className={cn("rounded-xl border border-border bg-card p-6 text-card-foreground shadow-xs", className)}
    >
      <h2 id={titleId} className="text-base font-semibold text-foreground">
        {title}
      </h2>
      <dl className="mt-4 grid gap-x-6 gap-y-4 sm:grid-cols-2">
        {items.map(([label, value]) => (
          <div key={label} className="min-w-0">
            <dt className="text-sm text-muted-foreground">{label}</dt>
            {/* bdi keeps left-to-right data such as phone numbers and addresses intact in right-to-left pages. */}
            <dd className="mt-0.5 break-words text-sm font-medium text-foreground">
              <bdi>{value}</bdi>
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

export default StudentsView;
