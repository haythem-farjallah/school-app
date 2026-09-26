import { beforeAll, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "react-hot-toast";
import { AddTeacherSheet } from "./teacher-sheet";
import { apiUrl, server } from "@/test/server";
import type { Teacher } from "@/types/teacher";

// jsdom lacks the matchMedia the toaster calls.
beforeAll(() => {
  window.matchMedia = (query: string) =>
    ({ matches: false, media: query, addEventListener() {}, removeEventListener() {} }) as unknown as MediaQueryList;
});

const teacher: Teacher = {
  id: 7,
  firstName: "Theo",
  lastName: "Teacher",
  email: "theo@school.test",
  qualifications: "MSc Mathematics",
  subjectsTaught: "Math, Physics",
  availableHours: 20,
  schedulePreferences: "Morning",
};

function renderSheet(onSuccess: () => void) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(
    <QueryClientProvider client={queryClient}>
      <AddTeacherSheet onSuccess={onSuccess} />
      <Toaster />
    </QueryClientProvider>,
  );
}

// Available hours (20) and schedule preferences ("Morning") keep the sheet's defaults.
async function openFillAndSubmit() {
  const user = userEvent.setup();
  await user.click(screen.getByRole("button", { name: "Add Teacher" }));
  const sheet = await screen.findByRole("dialog");
  await user.type(within(sheet).getByLabelText("First Name"), "Theo");
  await user.type(within(sheet).getByLabelText("Last Name"), "Teacher");
  await user.type(within(sheet).getByLabelText("Email Address"), "theo@school.test");
  await user.type(within(sheet).getByLabelText("Qualifications"), "MSc Mathematics");
  await user.type(within(sheet).getByLabelText("Subjects Taught"), "Math, Physics");
  await user.click(within(sheet).getByRole("button", { name: "Add Teacher" }));
}

describe("AddTeacherSheet", () => {
  it("posts the teacher, closes the sheet and reports success", async () => {
    let body: unknown;
    server.use(
      http.post(apiUrl("/admin/teachers"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ status: "success", data: teacher }, { status: 201 });
      }),
    );
    const onSuccess = vi.fn();
    renderSheet(onSuccess);

    await openFillAndSubmit();

    expect(await screen.findByText("Teacher added successfully!")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(onSuccess).toHaveBeenCalledTimes(1);
    expect(body).toEqual({
      profile: {
        firstName: "Theo",
        lastName: "Teacher",
        email: "theo@school.test",
        telephone: "",
        birthday: "",
        gender: "",
        address: "",
      },
      qualifications: "MSc Mathematics",
      subjectsTaught: "Math, Physics",
      availableHours: 20,
      schedulePreferences: "Morning",
    });
  });

  it("shows the backend error and keeps the sheet open when creation fails", async () => {
    server.use(
      http.post(apiUrl("/admin/teachers"), () =>
        HttpResponse.json({ status: 409, detail: "Email already in use" }, { status: 409 }),
      ),
    );
    const onSuccess = vi.fn();
    renderSheet(onSuccess);

    await openFillAndSubmit();

    expect(await screen.findByText("Email already in use")).toBeInTheDocument();
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();
  });
});
