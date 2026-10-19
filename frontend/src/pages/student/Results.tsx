import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { SimpleStudentLayout } from "@/components/Dashboard/SimpleStudentLayout";
import { LoadingState } from "@/components/Shared/LoadingState";
import { ErrorState } from "@/components/Shared/ErrorState";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import type { PageDto } from "@/types/level";
import { useAuth } from "@/hooks/useAuth";
import { useStudentResults } from "@/features/grades/use-student-results";
import type { StudentResult } from "@/features/grades/types";

/** The 1-based `page` search parameter; anything that is not a positive integer means the first page. */
function requestedPage(value: string | null) {
  return value !== null && /^[1-9]\d{0,8}$/.test(value) ? Number(value) : 1;
}

function ResultsList({
  data,
  page,
  onPageChange,
}: {
  data: PageDto<StudentResult>;
  page: number;
  onPageChange: (page: number) => void;
}) {
  const { t, i18n } = useTranslation();
  const totalPages = Math.ceil(data.totalElements / data.size);

  return (
    <section aria-label={t("studentResults.listLabel")} className="space-y-4">
      <p className="text-sm text-muted-foreground">
        {t("studentResults.showing", {
          from: data.page * data.size + 1,
          to: data.page * data.size + data.content.length,
          total: data.totalElements,
        })}
      </p>
      <ul className="space-y-3">
        {data.content.map((result) => (
          <li key={result.id}>
            <Card>
              <CardContent className="space-y-3 p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <h2 className="min-w-0 break-words font-medium">
                    {result.content || t("studentResults.untitled")}
                  </h2>
                  <p className="text-sm">
                    <span className="text-muted-foreground">{t("studentResults.score")}: </span>
                    <span className="font-semibold">{result.score ?? t("studentResults.notRecorded")}</span>
                  </p>
                </div>
                <dl className="grid grid-cols-1 gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
                  {result.courseName && (
                    <div className="flex min-w-0 gap-1">
                      <dt className="text-muted-foreground">{t("studentResults.course")}:</dt>
                      <dd className="break-words">{result.courseName}</dd>
                    </div>
                  )}
                  {result.className && (
                    <div className="flex min-w-0 gap-1">
                      <dt className="text-muted-foreground">{t("studentResults.class")}:</dt>
                      <dd className="break-words">{result.className}</dd>
                    </div>
                  )}
                  {result.teacherName && (
                    <div className="flex min-w-0 gap-1">
                      <dt className="text-muted-foreground">{t("studentResults.teacher")}:</dt>
                      <dd className="break-words">{result.teacherName}</dd>
                    </div>
                  )}
                  {result.gradedAt && (
                    <div className="flex min-w-0 gap-1">
                      <dt className="text-muted-foreground">{t("studentResults.gradedOn")}:</dt>
                      <dd>
                        <time dateTime={result.gradedAt}>
                          {new Date(result.gradedAt).toLocaleDateString(i18n.language)}
                        </time>
                      </dd>
                    </div>
                  )}
                </dl>
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>
      <nav aria-label={t("studentResults.pagination")} className="flex items-center justify-between gap-2">
        <Button variant="outline" size="sm" disabled={page <= 1} onClick={() => onPageChange(page - 1)}>
          <ChevronLeft className="rtl:rotate-180" aria-hidden="true" />
          {t("dataTable.pagination.previousPage")}
        </Button>
        <span className="text-sm">{t("dataTable.pagination.pageOf", { page, pages: totalPages })}</span>
        <Button variant="outline" size="sm" disabled={page >= totalPages} onClick={() => onPageChange(page + 1)}>
          {t("dataTable.pagination.nextPage")}
          <ChevronRight className="rtl:rotate-180" aria-hidden="true" />
        </Button>
      </nav>
    </section>
  );
}

function StudentResultsContent({ studentId }: { studentId: number }) {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const page = requestedPage(searchParams.get("page"));
  const results = useStudentResults(studentId, page - 1);
  const data = results.data;

  const goToPage = (target: number, replace = false) =>
    setSearchParams(target === 1 ? {} : { page: String(target) }, { replace });

  const totalPages = data ? Math.ceil(data.totalElements / data.size) : 0;
  // A bookmarked page beyond the last one (for example after results were removed) moves to the last page.
  const pastLastPage = data !== undefined && data.content.length === 0 && data.totalElements > 0;
  useEffect(() => {
    if (pastLastPage) goToPage(totalPages, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pastLastPage, totalPages]);

  return (
    <SimpleStudentLayout title={t("studentResults.title")} subtitle={t("studentResults.subtitle")}>
      {results.isPending || pastLastPage ? (
        <LoadingState title={t("studentResults.loading")} />
      ) : results.isError ? (
        <ErrorState title={t("studentResults.error")} onRetry={() => void results.refetch()} />
      ) : results.data.totalElements === 0 ? (
        <Card>
          <CardContent className="p-6">
            <p className="text-sm text-muted-foreground">{t("studentResults.empty")}</p>
          </CardContent>
        </Card>
      ) : (
        <ResultsList data={results.data} page={page} onPageChange={goToPage} />
      )}
    </SimpleStudentLayout>
  );
}

export default function StudentResults() {
  const { user, isAuthenticated } = useAuth();
  if (!isAuthenticated || user?.role !== "STUDENT") return null;
  return <StudentResultsContent key={user.id} studentId={user.id} />;
}
