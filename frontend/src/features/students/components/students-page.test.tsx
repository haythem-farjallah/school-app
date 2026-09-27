import { beforeAll, describe, expect, it } from "vitest";
import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse, delay } from "msw";
import { StudentsPage } from "./students-page";
import { apiUrl, server } from "@/test/server";
import { installDomShims, pageOf, renderStudentsRoute, student } from "../test-utils";

beforeAll(installDomShims);

const rawKey = /\b(students|common|dataTable)\.[a-zA-Z]/;
const other = { ...student, id: 6, firstName: "Lina", lastName: "Haddad", email: "lina@school.test", gender: "F", gradeLevel: "MIDDLE", enrollmentYear: 2023 };

function renderList(options: Partial<Parameters<typeof renderStudentsRoute>[1]> = {}) {
  const role = options.role ?? "ADMIN";
  const area = role === "STAFF" ? "staff" : "admin";
  return renderStudentsRoute(<StudentsPage />, { url: `/${area}/students`, path: `/${area}/students`, ...options, role });
}

describe("StudentsPage", () => {
  it("shows a skeleton while the first page loads, then the students", async () => {
    server.use(
      http.get(apiUrl("/v1/students"), async () => {
        await delay(50);
        return HttpResponse.json(pageOf([student, other], { totalElements: 2 }));
      }),
    );
    renderList();

    expect(screen.getByRole("status", { name: "Loading students…" })).toBeInTheDocument();
    const table = await screen.findByRole("table", { name: "Students" });

    expect(within(table).getAllByRole("columnheader").map((th) => th.textContent)).toEqual([
      "Student",
      "Email",
      "Grade level",
      "Enrollment year",
      "Gender",
      "Actions",
    ]);
    const row = within(table).getByRole("row", { name: /Sam Student/ });
    expect(within(row).getByRole("link", { name: "Sam Student" })).toHaveAttribute("href", "/admin/students/view/5");
    expect(row).toHaveTextContent("sam@school.test");
    expect(row).toHaveTextContent("High school");
    expect(row).toHaveTextContent("2024");
    expect(row).toHaveTextContent("Male");
    expect(within(table).getByRole("row", { name: /Lina Haddad/ })).toHaveTextContent("Female");
    expect(screen.getByText("Total: 2")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "Students" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Add student" })).toHaveAttribute("href", "/admin/students/create");
  });

  it("offers no fake statistics, selection, bulk actions or sorting", async () => {
    server.use(http.get(apiUrl("/v1/students"), () => HttpResponse.json(pageOf([student]))));
    renderList();

    await screen.findByRole("table", { name: "Students" });
    expect(screen.queryByText(/Selected/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Total Pages/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/rows selected/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /email|enroll|export|sort/i })).not.toBeInTheDocument();
    expect(screen.getByText("Page 1 of 1")).toBeInTheDocument();
  });

  it("shows an empty state with an add action when there are no students", async () => {
    server.use(http.get(apiUrl("/v1/students"), () => HttpResponse.json(pageOf([]))));
    renderList();

    expect(await screen.findByText("No students yet")).toBeInTheDocument();
    expect(screen.getByText("Add your first student to get started.")).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "Add student" })).toHaveLength(2);
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("searches the backend after typing, keeps the query in the URL and distinguishes no results", async () => {
    const queries: string[] = [];
    server.use(
      http.get(apiUrl("/v1/students"), () => HttpResponse.json(pageOf([student, other]))),
      http.get(apiUrl("/v1/students/search"), ({ request }) => {
        const q = new URL(request.url).searchParams.get("q") ?? "";
        queries.push(q);
        return HttpResponse.json(pageOf(q === "lina" ? [other] : []));
      }),
    );
    const { lastUrlQuery } = renderList();
    await screen.findByRole("table", { name: "Students" });
    const user = userEvent.setup();

    await user.type(screen.getByRole("searchbox", { name: "Search students" }), "lina");

    await waitFor(() => expect(screen.queryAllByRole("link", { name: "Sam Student" })).toHaveLength(0));
    expect(within(screen.getByRole("table")).getByRole("link", { name: "Lina Haddad" })).toBeInTheDocument();
    expect(queries).toEqual(["lina"]);
    await waitFor(() => expect(lastUrlQuery()).toBe("?q=lina"));
    expect(screen.getByText("Results: 1")).toBeInTheDocument();

    await user.clear(screen.getByRole("searchbox", { name: "Search students" }));
    await user.type(screen.getByRole("searchbox", { name: "Search students" }), "zzz");

    expect(await screen.findByText("No students found")).toBeInTheDocument();
    expect(screen.getByText("Nothing matches “zzz”. Try another name or email.")).toBeInTheDocument();
    expect(screen.queryByText("No students yet")).not.toBeInTheDocument();

    await user.click(screen.getAllByRole("button", { name: "Clear search" })[0]);

    expect(within(await screen.findByRole("table")).getByRole("link", { name: "Sam Student" })).toBeInTheDocument();
    expect(screen.getByRole("searchbox", { name: "Search students" })).toHaveValue("");
    await waitFor(() => expect(lastUrlQuery()).toBe(""));
  });

  it("starts from a search in the URL", async () => {
    let q: string | null = null;
    server.use(
      http.get(apiUrl("/v1/students/search"), ({ request }) => {
        q = new URL(request.url).searchParams.get("q");
        return HttpResponse.json(pageOf([other]));
      }),
    );
    renderList({ url: "/admin/students?q=lina" });

    expect(within(await screen.findByRole("table")).getByRole("link", { name: "Lina Haddad" })).toBeInTheDocument();
    expect(q).toBe("lina");
    expect(screen.getByRole("searchbox", { name: "Search students" })).toHaveValue("lina");
  });

  it("shows an error state whose retry refetches", async () => {
    let fail = true;
    server.use(
      http.get(apiUrl("/v1/students"), () =>
        fail ? HttpResponse.json({ status: 500, detail: "boom" }, { status: 500 }) : HttpResponse.json(pageOf([student])),
      ),
    );
    renderList();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Students could not be loaded");
    fail = false;
    await userEvent.setup().click(within(alert).getByRole("button", { name: "Retry" }));

    expect(within(await screen.findByRole("table")).getByRole("link", { name: "Sam Student" })).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("deletes a student after confirmation and refreshes the list", async () => {
    let remaining = [student, other];
    server.use(
      http.get(apiUrl("/v1/students"), () => HttpResponse.json(pageOf(remaining))),
      http.delete(apiUrl("/v1/students/5"), () => {
        remaining = [other];
        return HttpResponse.json({ status: "success", data: null });
      }),
    );
    renderList();
    const user = userEvent.setup();
    const table = await screen.findByRole("table", { name: "Students" });

    await user.click(within(table).getByRole("button", { name: "Actions for Sam Student" }));
    await user.click(await screen.findByRole("menuitem", { name: "Delete" }));

    const dialog = await screen.findByRole("dialog", { name: "Delete Sam Student?" });
    expect(dialog).toHaveTextContent("This removes the student from active records. This action cannot be undone.");
    await user.click(within(dialog).getByRole("button", { name: "Delete" }));

    expect(await screen.findByText("Sam Student was deleted")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    await waitFor(() => expect(screen.queryAllByRole("link", { name: "Sam Student" })).toHaveLength(0));
    expect(within(table).getByRole("link", { name: "Lina Haddad" })).toBeInTheDocument();
  });

  it("keeps the dialog open and shows the backend message when deletion fails", async () => {
    server.use(
      http.get(apiUrl("/v1/students"), () => HttpResponse.json(pageOf([student]))),
      http.delete(apiUrl("/v1/students/5"), () =>
        HttpResponse.json({ status: 409, detail: "Student has active enrollments" }, { status: 409 }),
      ),
    );
    renderList();
    const user = userEvent.setup();
    const table = await screen.findByRole("table", { name: "Students" });

    await user.click(within(table).getByRole("button", { name: "Actions for Sam Student" }));
    await user.click(await screen.findByRole("menuitem", { name: "Delete" }));
    const dialog = await screen.findByRole("dialog", { name: "Delete Sam Student?" });
    await user.click(within(dialog).getByRole("button", { name: "Delete" }));

    expect(await screen.findByText("Student has active enrollments")).toBeInTheDocument();
    expect(screen.getByRole("dialog", { name: "Delete Sam Student?" })).toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: "Delete" })).toBeEnabled();
  });

  it("keeps staff inside the staff area", async () => {
    server.use(http.get(apiUrl("/v1/students"), () => HttpResponse.json(pageOf([student]))));
    renderList({ role: "STAFF" });
    const user = userEvent.setup();

    const table = await screen.findByRole("table", { name: "Students" });
    expect(screen.getByRole("link", { name: "Add student" })).toHaveAttribute("href", "/staff/students/create");
    expect(within(table).getByRole("link", { name: "Sam Student" })).toHaveAttribute("href", "/staff/students/view/5");

    await user.click(within(table).getByRole("button", { name: "Actions for Sam Student" }));
    expect(await screen.findByRole("menuitem", { name: "View details" })).toHaveAttribute("href", "/staff/students/view/5");
  });

  it("renders in Traditional Chinese without raw keys", async () => {
    server.use(http.get(apiUrl("/v1/students"), () => HttpResponse.json(pageOf([]))));
    const { container } = renderList({ lng: "zh-TW" });

    expect(await screen.findByText("尚無學生")).toBeInTheDocument();
    expect(screen.getByRole("heading", { level: 1, name: "學生" })).toBeInTheDocument();
    expect(screen.getByRole("searchbox", { name: "搜尋學生" })).toBeInTheDocument();
    expect(container.textContent).not.toMatch(rawKey);
  });

  it("renders in Arabic, including values mapped from backend codes", async () => {
    server.use(http.get(apiUrl("/v1/students"), () => HttpResponse.json(pageOf([student], { totalElements: 1 }))));
    const { container } = renderList({ lng: "ar" });

    const table = await screen.findByRole("table", { name: "الطلاب" });
    const row = within(table).getByRole("row", { name: /Sam Student/ });
    expect(row).toHaveTextContent("المرحلة الثانوية");
    expect(row).toHaveTextContent("ذكر");
    expect(screen.getByText("الصفحة 1 من 1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "الانتقال إلى الصفحة التالية" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "إضافة طالب" })).toBeInTheDocument();
    expect(container.textContent).not.toMatch(rawKey);
  });
});
