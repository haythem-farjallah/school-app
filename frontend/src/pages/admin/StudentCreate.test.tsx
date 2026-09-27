import { beforeAll, describe, expect, it } from "vitest";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import StudentsCreate from "./StudentCreate";
import { apiUrl, server } from "@/test/server";
import { installDomShims, renderStudentsRoute, student } from "@/features/students/test-utils";

beforeAll(installDomShims);

function renderPage(role: "ADMIN" | "STAFF" = "ADMIN", lng: "en" | "fr" = "en") {
  const area = role === "STAFF" ? "staff" : "admin";
  return renderStudentsRoute(<StudentsCreate />, {
    role,
    lng,
    url: `/${area}/students/create`,
    path: `/${area}/students/create`,
  });
}

async function fillAndSubmit() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("First name"), "Sam");
  await user.type(screen.getByLabelText("Last name"), "Student");
  await user.type(screen.getByLabelText("Email"), "sam@school.test");
  await user.type(screen.getByLabelText("Birthday"), "2010-04-02");
  await user.click(screen.getByLabelText("Grade level"));
  await user.click(await screen.findByRole("option", { name: "High school" }));
  await user.click(screen.getByRole("button", { name: "Create student" }));
}

describe("StudentsCreate", () => {
  it("posts the student, reports success and returns to the admin list", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/v1/students"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: student }, { status: 201 });
      }),
    );
    renderPage();
    expect(screen.getByRole("heading", { level: 1, name: "Add student" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to students" })).toHaveAttribute("href", "/admin/students");

    await fillAndSubmit();

    expect(await screen.findByText("Student created")).toBeInTheDocument();
    expect(await screen.findByText("Admin students list")).toBeInTheDocument();
    expect(body).toEqual({
      profile: {
        firstName: "Sam",
        lastName: "Student",
        email: "sam@school.test",
        telephone: null,
        birthday: "2010-04-02",
        gender: null,
        address: null,
      },
      gradeLevel: "HIGH",
      enrollmentYear: new Date().getFullYear(),
    });
  });

  it("returns staff to the staff list", async () => {
    server.use(
      http.post(apiUrl("/v1/students"), () => HttpResponse.json({ status: "success", data: student }, { status: 201 })),
    );
    renderPage("STAFF");
    expect(screen.getByRole("link", { name: "Back to students" })).toHaveAttribute("href", "/staff/students");

    await fillAndSubmit();

    expect(await screen.findByText("Staff students list")).toBeInTheDocument();
  });

  it("shows the backend error and stays on the form when creation fails", async () => {
    server.use(
      http.post(apiUrl("/v1/students"), () =>
        HttpResponse.json({ status: 409, detail: "Email already in use" }, { status: 409 }),
      ),
    );
    renderPage();

    await fillAndSubmit();

    expect(await screen.findByText("Email already in use")).toBeInTheDocument();
    expect(screen.queryByText("Admin students list")).not.toBeInTheDocument();
    expect(screen.getByLabelText("First name")).toHaveValue("Sam");
    expect(screen.getByRole("button", { name: "Create student" })).toBeEnabled();
  });

  it("validates with translated messages before calling the backend", async () => {
    let called = false;
    server.use(
      http.post(apiUrl("/v1/students"), () => {
        called = true;
        return HttpResponse.json({ status: "success", data: student }, { status: 201 });
      }),
    );
    renderPage();
    const user = userEvent.setup();

    await user.type(screen.getByLabelText("Email"), "not-an-email");
    await user.click(screen.getByRole("button", { name: "Create student" }));

    expect(await screen.findByText("Enter a first name.")).toBeInTheDocument();
    expect(screen.getByText("Enter a valid email address.")).toBeInTheDocument();
    expect(screen.getByText("Select a grade level.")).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it("is translated in French", () => {
    const { container } = renderPage("ADMIN", "fr");

    expect(screen.getByRole("heading", { level: 1, name: "Ajouter un élève" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Retour aux élèves" })).toBeInTheDocument();
    expect(screen.getByLabelText("Prénom")).toBeInTheDocument();
    expect(screen.getByLabelText("Année d’inscription")).toHaveValue(new Date().getFullYear());
    expect(screen.getByRole("button", { name: "Créer l’élève" })).toBeInTheDocument();
    expect(container.textContent).not.toMatch(/\b(students|common)\.[a-zA-Z]/);
  });
});
