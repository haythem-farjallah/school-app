import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Bell, Calendar, TrendingUp, UserRound } from "lucide-react";
import { SimpleStudentLayout } from "@/components/Dashboard/SimpleStudentLayout";
import { LoadingState } from "@/components/Shared/LoadingState";
import { ErrorState } from "@/components/Shared/ErrorState";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/hooks/useAuth";
import { useStudentDashboard } from "@/features/dashboard/use-student-dashboard";
import { usePublicAnnouncements } from "@/features/announcements/hooks/use-announcements";

function StudentDashboardContent({ accountId }: { accountId: number }) {
  const { t, i18n } = useTranslation();
  const dashboard = useStudentDashboard(accountId);
  const announcements = usePublicAnnouncements(0, 3, accountId);
  const data = dashboard.data;
  const hasRecordedScore = data?.recentGrades.some(
    (grade) => typeof grade.score === "number" && Number.isFinite(grade.score),
  );
  const average =
    hasRecordedScore &&
    typeof data?.stats.averageGrade === "number" &&
    Number.isFinite(data.stats.averageGrade)
      ? data.stats.averageGrade
      : null;
  const date = (value: string) => (
    <time dateTime={value}>
      {new Date(value).toLocaleDateString(i18n.language)}
    </time>
  );

  return (
    <SimpleStudentLayout
      title={t("studentDashboard.title")}
      subtitle={t("studentDashboard.subtitle")}
    >
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="min-w-0 space-y-6 lg:col-span-2">
          {dashboard.isPending ? (
            <LoadingState title={t("studentDashboard.loading")} />
          ) : dashboard.isError ? (
            <ErrorState
              title={t("studentDashboard.error")}
              onRetry={() => void dashboard.refetch()}
            />
          ) : (
            data && (
              <>
                <Card role="region" aria-label={t("studentDashboard.overview")}>
                  <CardHeader>
                    <CardTitle className="break-words">
                      {data.baseInfo.userInfo.name}
                    </CardTitle>
                    <p className="break-words text-sm text-muted-foreground">
                      {data.baseInfo.userInfo.email}
                    </p>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <h2 className="flex items-center gap-2 font-semibold">
                      <TrendingUp className="size-5" aria-hidden="true" />
                      {t("studentDashboard.overview")}
                    </h2>
                    <div className="space-y-2">
                      <h3 className="text-sm font-medium">
                        {t("studentDashboard.currentEnrollment")}
                      </h3>
                      {data.enrolledClasses.length === 0 ? (
                        <p className="text-sm text-muted-foreground">
                          {t("studentDashboard.noEnrollment")}
                        </p>
                      ) : (
                        <ul className="space-y-2">
                          {data.enrolledClasses.map((clazz) => (
                            <li
                              key={clazz.classId}
                              className="rounded-lg border p-3"
                            >
                              <p className="break-words font-medium">
                                {clazz.className}
                              </p>
                              {clazz.teacherName && (
                                <p className="break-words text-sm text-muted-foreground">
                                  {t("studentDashboard.teachers", {
                                    names: clazz.teacherName,
                                  })}
                                </p>
                              )}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span className="text-sm">
                        {t("studentDashboard.average")}
                      </span>
                      <Badge
                        variant="secondary"
                        aria-label={t("studentDashboard.average")}
                      >
                        {average ?? t("studentDashboard.unavailable")}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {t(
                        average === null
                          ? "studentDashboard.averageUnavailable"
                          : "studentDashboard.averageDescription",
                      )}
                    </p>
                  </CardContent>
                </Card>
                <Card role="region" aria-label={t("studentDashboard.results")}>
                  <CardHeader>
                    <CardTitle>{t("studentDashboard.results")}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {data.recentGrades.length === 0 ? (
                      <p className="text-sm text-muted-foreground">
                        {t("studentDashboard.noResults")}
                      </p>
                    ) : (
                      <ul className="space-y-3">
                        {data.recentGrades.map((grade, index) => (
                          <li
                            key={index}
                            className="space-y-2 rounded-lg border p-3"
                          >
                            {grade.content && (
                              <p className="break-words font-medium">
                                {grade.content}
                              </p>
                            )}
                            {grade.courseName && (
                              <p className="break-words text-sm">
                                {t("studentDashboard.resultClass", {
                                  name: grade.courseName,
                                })}
                              </p>
                            )}
                            <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
                              <span>
                                {t("studentDashboard.score")}:{" "}
                                <span aria-label={t("studentDashboard.score")}>
                                  {grade.score ??
                                    t("studentDashboard.notRecorded")}
                                </span>
                              </span>
                              {grade.gradedAt && date(grade.gradedAt)}
                            </div>
                          </li>
                        ))}
                      </ul>
                    )}
                  </CardContent>
                </Card>
                <Card
                  role="region"
                  aria-label={t("studentDashboard.notifications")}
                >
                  <CardHeader>
                    <CardTitle>{t("studentDashboard.notifications")}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {data.baseInfo.notifications.length === 0 ? (
                      <p className="text-sm text-muted-foreground">
                        {t("studentDashboard.noNotifications")}
                      </p>
                    ) : (
                      <ul className="space-y-3">
                        {data.baseInfo.notifications.map((notification) => (
                          <li
                            key={notification.id}
                            className="space-y-1 break-words rounded-lg border p-3"
                          >
                            <p className="font-medium">{notification.title}</p>
                            <p className="text-sm">{notification.message}</p>
                            {notification.createdAt && (
                              <p className="text-xs text-muted-foreground">
                                {date(notification.createdAt)}
                              </p>
                            )}
                          </li>
                        ))}
                      </ul>
                    )}
                  </CardContent>
                </Card>
              </>
            )
          )}
          <Card role="region" aria-label={t("studentDashboard.timetable")}>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calendar className="size-5" aria-hidden="true" />
                {t("studentDashboard.timetable")}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                {t("studentDashboard.timetableUnavailable")}
              </p>
            </CardContent>
          </Card>
        </div>
        <div className="min-w-0 space-y-6">
          <Card role="region" aria-label={t("studentDashboard.quickActions")}>
            <CardHeader>
              <CardTitle>{t("studentDashboard.quickActions")}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Button
                variant="outline"
                className="w-full justify-start"
                asChild
              >
                <Link to="/student/announcements">
                  <Bell className="mr-2 size-4" aria-hidden="true" />
                  {t("studentDashboard.announcements")}
                </Link>
              </Button>
              <Button
                variant="outline"
                className="w-full justify-start"
                asChild
              >
                <Link to="/student/profile">
                  <UserRound className="mr-2 size-4" aria-hidden="true" />
                  {t("studentDashboard.profile")}
                </Link>
              </Button>
            </CardContent>
          </Card>
          <Card
            role="region"
            aria-label={t("studentDashboard.publicAnnouncements")}
          >
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Bell className="size-5" aria-hidden="true" />
                {t("studentDashboard.publicAnnouncements")}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {announcements.isPending ? (
                <LoadingState
                  title={t("studentDashboard.announcementsLoading")}
                />
              ) : announcements.isError ? (
                <ErrorState
                  title={t("studentDashboard.announcementsError")}
                  onRetry={() => void announcements.refetch()}
                />
              ) : announcements.data?.content.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t("studentDashboard.noAnnouncements")}
                </p>
              ) : (
                <ul className="space-y-3">
                  {announcements.data?.content.map((announcement) => (
                    <li
                      key={announcement.id}
                      className="space-y-2 rounded-lg border p-3"
                    >
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <h3 className="min-w-0 break-words text-sm font-medium">
                          {announcement.title}
                        </h3>
                        <Badge variant="outline">
                          {announcement.importance}
                        </Badge>
                      </div>
                      <p className="whitespace-pre-wrap break-words text-sm text-muted-foreground">
                        {announcement.body}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {t("studentDashboard.published")}:{" "}
                        {date(announcement.createdAt)}
                      </p>
                      {announcement.startDate && (
                        <p className="text-xs text-muted-foreground">
                          {t("studentDashboard.starts")}:{" "}
                          {date(announcement.startDate)}
                        </p>
                      )}
                      {announcement.endDate && (
                        <p className="text-xs text-muted-foreground">
                          {t("studentDashboard.ends")}:{" "}
                          {date(announcement.endDate)}
                        </p>
                      )}
                    </li>
                  ))}
                </ul>
              )}
              <Button
                variant="ghost"
                size="sm"
                className="h-auto w-full whitespace-normal"
                asChild
              >
                <Link to="/student/announcements">
                  {t("studentDashboard.viewAnnouncements")}
                </Link>
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </SimpleStudentLayout>
  );
}

export default function StudentDashboard() {
  const { user, isAuthenticated } = useAuth();
  if (!isAuthenticated || user?.role !== "STUDENT") return null;
  return <StudentDashboardContent key={user.id} accountId={user.id} />;
}
