import { expect, test } from "@playwright/test";
import type { StudentResult } from "../src/features/grades/types";
import type { PageDto } from "../src/types/level";

const student = {
  email: "student@fixtures.school.test",
  password: "Fixture-Pass-2024",
};

test("student results agree with the real authenticated grades response on desktop and mobile", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email Address").fill(student.email);
  await page.getByLabel("Password").fill(student.password);
  await page.getByRole("button", { name: "Sign In" }).click();
  await expect(page).toHaveURL(/\/$/);

  // The page reads only: any request that is not a GET would be a data write.
  const writes: string[] = [];
  page.on("request", (request) => {
    if (request.url().includes("/api/") && request.method() !== "GET") {
      writes.push(`${request.method()} ${request.url()}`);
    }
  });

  // One real login covers both viewports without exhausting the suite's authentication limit.
  for (const viewport of [
    { width: 1440, height: 1000 },
    { width: 390, height: 844 },
  ]) {
    await test.step(`Compare results with the API response at ${viewport.width}px`, async () => {
      await page.setViewportSize(viewport);
      // Capture the browser's actual request. Do not intercept or fabricate grades.
      const gradesResponse = page.waitForResponse((response) => /\/api\/v1\/grades\/student\/\d+\/paged\?/.test(response.url()));
      await page.goto("/student/results");
      const http = await gradesResponse;
      expect(http.status()).toBe(200);
      expect(http.request().method()).toBe("GET");
      expect(http.request().headers()["authorization"]).toMatch(/^Bearer /);
      const url = new URL(http.url());
      expect(url.searchParams.get("page")).toBe("0");
      expect(url.searchParams.get("sort")).toBe("gradedAt,desc");
      const { data }: { data: PageDto<StudentResult> } = await http.json();

      const main = page.getByRole("main");
      await expect(main.getByRole("heading", { name: "All Results", level: 1 })).toBeVisible();

      if (data.totalElements === 0) {
        await expect(main.getByText("No results have been recorded for you yet.")).toBeVisible();
        await expect(main.getByRole("listitem")).toHaveCount(0);
        await expect(main.getByRole("navigation", { name: "Results pages" })).toHaveCount(0);
      } else {
        const list = main.getByRole("region", { name: "Results" });
        await expect(list.getByText(`Showing 1–${data.content.length} of ${data.totalElements} results`)).toBeVisible();
        await expect(list.getByRole("listitem")).toHaveCount(data.content.length);
        for (const [index, grade] of data.content.entries()) {
          const row = list.getByRole("listitem").nth(index);
          await expect(row.getByRole("heading", { level: 2 })).toHaveText(grade.content || "Untitled result");
          await expect(row.getByText(grade.score === null ? "Not recorded" : String(grade.score), { exact: true })).toBeVisible();
          if (grade.className) await expect(row.getByText(grade.className, { exact: true })).toBeVisible();
          if (grade.teacherName) await expect(row.getByText(grade.teacherName, { exact: true })).toBeVisible();
          if (grade.gradedAt) await expect(row.locator("time")).toHaveAttribute("datetime", grade.gradedAt);
          else await expect(row.locator("time")).toHaveCount(0);
        }
      }

      await expect(main.getByText(/Geography|James Green|Top Performer|Consistent Student|Stella|%/)).toHaveCount(0);
      expect(await page.evaluate("document.documentElement.scrollWidth <= document.documentElement.clientWidth")).toBe(true);
    });
  }

  expect(writes).toEqual([]);
});
