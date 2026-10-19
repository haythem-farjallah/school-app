import { expect, test } from "@playwright/test";
import type { StudentDashboardData } from "../src/features/dashboard/types";
import type { Announcement } from "../src/types/announcement";

const student = {
  email: "student@fixtures.school.test",
  password: "Fixture-Pass-2024",
};

for (const viewport of [
  { width: 1440, height: 1000 },
  { width: 390, height: 844 },
]) {
  test(`student dashboard agrees with real authenticated responses at ${viewport.width}px`, async ({
    page,
  }) => {
    await page.setViewportSize(viewport);
    await page.goto("/login");
    await page.getByLabel("Email Address").fill(student.email);
    await page.getByLabel("Password").fill(student.password);
    await page.getByRole("button", { name: "Sign In" }).click();
    await expect(page).toHaveURL(/\/$/);

    // Capture the browser's actual requests. Do not intercept or fabricate dashboard data.
    const dashboardResponse = page.waitForResponse((response) =>
      response.url().endsWith("/api/v1/dashboard/current-user"),
    );
    const announcementsResponse = page.waitForResponse((response) =>
      response.url().endsWith("/api/v1/announcements/public?page=0&size=3"),
    );
    await page.goto("/student/dashboard");
    const [dashboardHttp, announcementsHttp] = await Promise.all([
      dashboardResponse,
      announcementsResponse,
    ]);
    expect(dashboardHttp.status()).toBe(200);
    expect(announcementsHttp.status()).toBe(200);
    const { data }: { data: StudentDashboardData } = await dashboardHttp.json();
    const { data: publicPage }: { data: { content: Announcement[] } } =
      await announcementsHttp.json();
    expect(data.type).toBe("STUDENT");
    expect(data.baseInfo.userInfo.email).toBe(student.email);

    const main = page.getByRole("main");
    await expect(
      main.getByRole("heading", { name: "Student Dashboard", exact: true }),
    ).toBeVisible();
    const overview = main.getByRole("region", { name: "Academic overview" });
    await expect(
      overview.getByText(data.baseInfo.userInfo.name, { exact: true }),
    ).toBeVisible();
    await expect(
      overview.getByText(data.baseInfo.userInfo.email, { exact: true }),
    ).toBeVisible();
    if (data.enrolledClasses.length === 0) {
      await expect(
        overview.getByText("No current enrollment available."),
      ).toBeVisible();
    } else {
      for (const clazz of data.enrolledClasses) {
        await expect(
          overview.getByText(clazz.className, { exact: true }),
        ).toBeVisible();
        if (clazz.teacherName)
          await expect(
            overview.getByText(`Teachers: ${clazz.teacherName}`, {
              exact: true,
            }),
          ).toBeVisible();
      }
    }
    const average = overview.getByLabel("Recorded grade average", {
      exact: true,
    });
    if (
      data.recentGrades.some((grade) => grade.score !== null) &&
      data.stats.averageGrade !== null
    ) {
      await expect(average).toHaveText(String(data.stats.averageGrade));
    } else {
      await expect(average).toHaveText("Unavailable");
    }

    const results = main.getByRole("region", { name: "Recent results" });
    if (data.recentGrades.length === 0)
      await expect(results.getByText("No results available.")).toBeVisible();
    await expect(results.getByRole("listitem")).toHaveCount(
      data.recentGrades.length,
    );
    for (const [index, grade] of data.recentGrades.entries()) {
      const row = results.getByRole("listitem").nth(index);
      if (grade.courseName)
        await expect(
          row.getByText(`Class: ${grade.courseName}`, { exact: true }),
        ).toBeVisible();
      if (grade.content)
        await expect(
          row.getByText(grade.content, { exact: true }),
        ).toBeVisible();
      await expect(
        row.getByLabel("Recorded score", { exact: true }),
      ).toHaveText(grade.score === null ? "Not recorded" : String(grade.score));
      if (grade.gradedAt)
        await expect(row.locator("time")).toHaveAttribute(
          "datetime",
          grade.gradedAt,
        );
      else await expect(row.locator("time")).toHaveCount(0);
    }

    const notices = main.getByRole("region", { name: "Public announcements" });
    if (publicPage.content.length === 0)
      await expect(
        notices.getByText("No public announcements available."),
      ).toBeVisible();
    await expect(notices.getByRole("listitem")).toHaveCount(
      publicPage.content.length,
    );
    for (const [index, announcement] of publicPage.content.entries()) {
      const row = notices.getByRole("listitem").nth(index);
      await expect(
        row.getByText(announcement.title, { exact: true }),
      ).toBeVisible();
      await expect(
        row.getByText(announcement.body, { exact: true }),
      ).toBeVisible();
      await expect(
        row.getByText(announcement.importance, { exact: true }),
      ).toBeVisible();
      await expect(row.locator("time").first()).toHaveAttribute(
        "datetime",
        announcement.createdAt,
      );
      for (const value of [announcement.startDate, announcement.endDate].filter(
        Boolean,
      )) {
        await expect(row.locator(`time[datetime="${value}"]`)).toBeVisible();
      }
    }

    const notifications = main.getByRole("region", {
      name: "Unread notifications",
    });
    if (data.baseInfo.notifications.length === 0)
      await expect(
        notifications.getByText("No unread notifications."),
      ).toBeVisible();
    await expect(notifications.getByRole("listitem")).toHaveCount(
      data.baseInfo.notifications.length,
    );
    for (const notification of data.baseInfo.notifications) {
      await expect(
        notifications.getByText(notification.title, { exact: true }),
      ).toBeVisible();
      await expect(
        notifications.getByText(notification.message, { exact: true }),
      ).toBeVisible();
    }
    await expect(
      main.getByText(
        "Timetable information is not available on this dashboard yet.",
      ),
    ).toBeVisible();
    await expect(
      main.getByText(
        /85\.4%|92%|Midterm Schedule Released|Library Hours Extended|Science Fair Registration|4A - Physics|Assignment Due Tomorrow/,
      ),
    ).toHaveCount(0);
    await expect(
      main.getByText(/Assignments Due|Attendance Rate|Upcoming Exams/),
    ).toHaveCount(0);
    expect(
      await page.evaluate(
        "document.documentElement.scrollWidth <= document.documentElement.clientWidth",
      ),
    ).toBe(true);
    const actions = main.getByRole("region", { name: "Quick actions" });
    await expect(actions.getByRole("link")).toHaveCount(2);
    await actions.getByRole("link", { name: "Profile", exact: true }).click();
    await expect(page).toHaveURL(/\/student\/profile$/);
    await page.goBack();
    await main.getByRole("link", { name: "View all announcements" }).click();
    await expect(page).toHaveURL(/\/student\/announcements$/);
  });
}
