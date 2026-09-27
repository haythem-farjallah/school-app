import { beforeAll, describe, expect, it } from "vitest";
import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import StudentsView from "./StudentsView";
import { apiUrl, server } from "@/test/server";
import type { SupportedLocale } from "@/services/i18n";
import type { Student } from "@/types/student";
import { installDomShims, renderStudentsRoute, student } from "@/features/students/test-utils";

beforeAll(installDomShims);

function serve(body: Student) {
  server.use(http.get(apiUrl(`/v1/students/${body.id}`), () => HttpResponse.json({ status: "success", data: body })));
}

function renderView({ id = "5", role = "ADMIN", lng = "en" }: { id?: string; role?: "ADMIN" | "STAFF"; lng?: SupportedLocale } = {}) {
  const area = role === "STAFF" ? "staff" : "admin";
  return renderStudentsRoute(<StudentsView />, {
    role,
    lng,
    url: `/${area}/students/view/${id}`,
    path: `/${area}/students/view/:id`,
  });
}

function field(section: HTMLElement, label: string) {
  return within(section).getByText(label, { selector: "dt" }).nextElementSibling;
}

describe("StudentsView", () => {
  it("shows the student's personal and academic information without invented fields", async () => {
    serve(student);
    const { container } = renderView();

    const personal = await screen.findByRole("region", { name: "Personal information" });
    expect(screen.getByRole("heading", { level: 1, name: "Student details" })).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "Sam Student" })).toHaveTextContent("sam@school.test");
    expect(field(personal, "Phone")).toHaveTextContent("+216 20 000 111");
    expect(field(personal, "Birthday")).toHaveTextContent("April 2, 2010");
    expect(field(personal, "Gender")).toHaveTextContent("Male");
    expect(field(personal, "Address")).toHaveTextContent("1 School Road");
    const academic = screen.getByRole("region", { name: "Academic information" });
    expect(field(academic, "Grade level")).toHaveTextContent("High school");
    expect(field(academic, "Enrollment year")).toHaveTextContent("2024");
    expect(field(academic, "Student ID")).toHaveTextContent("#5");

    expect(container.textContent).not.toMatch(/Active|Status|Created|Unknown/);
  });

  it("says when optional values are missing and keeps unknown stored values readable", async () => {
    serve({ ...student, telephone: null, birthday: null, address: "", gender: "X", gradeLevel: null, enrollmentYear: null });
    renderView();

    const personal = await screen.findByRole("region", { name: "Personal information" });
    expect(field(personal, "Phone")).toHaveTextContent("Not provided");
    expect(field(personal, "Birthday")).toHaveTextContent("Not provided");
    expect(field(personal, "Address")).toHaveTextContent("Not provided");
    expect(field(personal, "Gender")).toHaveTextContent("X");
    const academic = screen.getByRole("region", { name: "Academic information" });
    expect(field(academic, "Grade level")).toHaveTextContent("Not provided");
    expect(field(academic, "Enrollment year")).toHaveTextContent("Not provided");
  });

  it("formats the birthday and labels for the current language", async () => {
    serve({ ...student, gender: "F" });
    renderView({ lng: "fr" });

    const personal = await screen.findByRole("region", { name: "Informations personnelles" });
    expect(field(personal, "Date de naissance")).toHaveTextContent("2 avril 2010");
    expect(field(personal, "Genre")).toHaveTextContent("Féminin");
  });

  it("renders in Arabic with an Arabic date and no raw keys", async () => {
    serve({ ...student, telephone: null });
    const { container } = renderView({ lng: "ar" });

    const personal = await screen.findByRole("region", { name: "المعلومات الشخصية" });
    expect(screen.getByRole("heading", { level: 1, name: "تفاصيل الطالب" })).toBeInTheDocument();
    expect(field(personal, "الهاتف")).toHaveTextContent("غير متوفر");
    expect(field(personal, "الجنس")).toHaveTextContent("ذكر");
    expect(field(personal, "تاريخ الميلاد")?.textContent).toBe(
      new Intl.DateTimeFormat("ar", { dateStyle: "long", timeZone: "UTC" }).format(Date.UTC(2010, 3, 2)),
    );
    expect(container.textContent).not.toMatch(/\b(students|common)\.[a-zA-Z]/);
  });

  it("links staff back to the staff list and opens the edit sheet", async () => {
    serve(student);
    renderView({ role: "STAFF" });

    await screen.findByRole("region", { name: "Personal information" });
    expect(screen.getByRole("link", { name: "Back to students" })).toHaveAttribute("href", "/staff/students");
    await userEvent.setup().click(screen.getByRole("button", { name: "Edit student" }));
    expect(await screen.findByRole("dialog", { name: "Edit student" })).toBeInTheDocument();
  });

  it("shows a not-found state with a way back", async () => {
    server.use(
      http.get(apiUrl("/v1/students/404"), () =>
        HttpResponse.json({ status: 404, detail: "Student not found" }, { status: 404 }),
      ),
    );
    renderView({ id: "404" });

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Student not found");
    expect(within(alert).getByRole("link", { name: "Back to students" })).toHaveAttribute("href", "/admin/students");
  });

  it("shows an error state that retries", async () => {
    let fail = true;
    server.use(
      http.get(apiUrl("/v1/students/5"), () =>
        fail
          ? HttpResponse.json({ status: 500, detail: "boom" }, { status: 500 })
          : HttpResponse.json({ status: "success", data: student }),
      ),
    );
    renderView();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("This student could not be loaded");
    fail = false;
    await userEvent.setup().click(within(alert).getByRole("button", { name: "Retry" }));
    expect(await screen.findByRole("region", { name: "Personal information" })).toBeInTheDocument();
  });

  it("treats a non-numeric id as not found without calling the backend", async () => {
    renderView({ id: "abc" });

    expect(await screen.findByRole("alert")).toHaveTextContent("Student not found");
  });
});
