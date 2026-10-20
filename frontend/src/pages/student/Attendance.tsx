import { CalendarDays, ChevronLeft, ChevronRight } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/Shared/EmptyState";
import { ErrorState } from "@/components/Shared/ErrorState";
import { LoadingState } from "@/components/Shared/LoadingState";
import { useAuth } from "@/hooks/useAuth";
import { useStudentAttendance } from "@/features/attendance/hooks/use-student-attendance";
import { adjacentAttendanceMonth, attendanceMonth } from "@/features/attendance/month";

export default function StudentAttendance() {
  const { t, i18n } = useTranslation();
  const { isAuthenticated, user } = useAuth();
  const [params, setParams] = useSearchParams();
  const month = attendanceMonth(params);
  const attendance = useStudentAttendance(month);
  // A local calendar date for the month label, never an API date parsed as a UTC instant.
  const monthLabel = new Intl.DateTimeFormat(i18n.language, { month: "long", year: "numeric", calendar: "gregory" })
    .format(new Date(`${month}-01T12:00:00`));
  const navigateMonth = (offset: -1 | 1) => {
    const next = new URLSearchParams(params);
    next.set("month", adjacentAttendanceMonth(month, offset));
    setParams(next);
  };

  return (
    <div className="mx-auto w-full max-w-4xl space-y-6">
      <header className="space-y-2">
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">{t("studentAttendance.title")}</h1>
        <p className="max-w-2xl text-sm text-muted-foreground">{t("studentAttendance.description")}</p>
      </header>

      <section aria-label={t("studentAttendance.history")} className="min-w-0 rounded-xl border border-border bg-card">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border p-4 sm:p-6">
          <h2 aria-live="polite" className="text-lg font-semibold">{monthLabel}</h2>
          <nav aria-label={t("studentAttendance.monthNavigation")} className="flex gap-2">
            <Button type="button" variant="outline" size="icon" aria-label={t("studentAttendance.previousMonth")}
              disabled={month === "0001-01"} onClick={() => navigateMonth(-1)}>
              <ChevronLeft className="size-4 rtl:rotate-180" aria-hidden="true" />
            </Button>
            <Button type="button" variant="outline" size="icon" aria-label={t("studentAttendance.nextMonth")}
              disabled={month === "9999-12"} onClick={() => navigateMonth(1)}>
              <ChevronRight className="size-4 rtl:rotate-180" aria-hidden="true" />
            </Button>
          </nav>
        </div>

        {!isAuthenticated || user?.role !== "STUDENT" ? (
          <ErrorState title={t("studentAttendance.signIn")} description={t("studentAttendance.sessionRequired")} />
        ) : attendance.isPending ? (
          <LoadingState title={t("studentAttendance.loading")} />
        ) : attendance.isError ? (
          <ErrorState title={t("studentAttendance.error")} description={t("studentAttendance.errorDescription")}
            onRetry={() => { void attendance.refetch(); }} />
        ) : attendance.data.length === 0 ? (
          <EmptyState icon={CalendarDays} title={t("studentAttendance.empty")}
            description={t("studentAttendance.emptyDescription")} />
        ) : (
          <ul aria-label={t("studentAttendance.records")} className="divide-y divide-border">
            {attendance.data.map((record) => (
              <li key={record.id} className="space-y-3 p-4 sm:p-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <time dateTime={record.date} className="text-sm font-medium tabular-nums" dir="ltr">{record.date}</time>
                  <span className="rounded-full bg-muted px-3 py-1 text-sm font-medium">
                    {t(`studentAttendance.statuses.${record.status}`)}
                  </span>
                </div>
                <div className="min-w-0 space-y-1 break-words [overflow-wrap:anywhere]">
                  {record.courseName && <p className="font-medium">{record.courseName}</p>}
                  {record.className && <p className="text-sm text-muted-foreground">{record.className}</p>}
                  {!record.className && !record.courseName && (
                    <p className="text-sm text-muted-foreground">{t("studentAttendance.noDetails")}</p>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
