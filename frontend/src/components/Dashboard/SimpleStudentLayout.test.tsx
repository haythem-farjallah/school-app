import { afterEach, describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { I18nextProvider } from "react-i18next";
import { Provider } from "react-redux";
import { MemoryRouter } from "react-router-dom";
import type { ComponentType } from "react";
import { SimpleStudentLayout } from "./SimpleStudentLayout";
import StudentDashboard from "@/pages/student/Dashboard";
import StudentEvents from "@/pages/student/Events";
import StudentExams from "@/pages/student/Exams";
import StudentResults from "@/pages/student/Results";
import { queryClient } from "@/lib/query-client";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { createTestI18n } from "@/test/i18n";

const user = { id: "1", name: "Ada Lovelace", email: "ada@fixtures.school.test", role: "Student" };

function renderInApp(ui: React.ReactNode) {
  return render(
    <HelmetProvider>
      <I18nextProvider i18n={createTestI18n()}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter>{ui}</MemoryRouter>
          </QueryClientProvider>
        </Provider>
      </I18nextProvider>
    </HelmetProvider>,
  );
}

afterEach(() => {
  store.dispatch(resetAuth());
  queryClient.clear();
});

describe("SimpleStudentLayout", () => {
  it("renders the page title and children without header, greeting or clock chrome of its own", () => {
    renderInApp(
      <SimpleStudentLayout title="Exams" subtitle="Your exams" user={user}>
        <p>Page content</p>
      </SimpleStudentLayout>,
    );

    expect(screen.getByRole("heading", { level: 1, name: "Exams" })).toBeInTheDocument();
    expect(screen.getAllByRole("heading", { level: 1 })).toHaveLength(1);
    expect(screen.getByText("Page content")).toBeInTheDocument();
    expect(screen.queryByRole("banner")).not.toBeInTheDocument();
    expect(screen.queryByRole("main")).not.toBeInTheDocument();
    expect(screen.queryByText(/Good (morning|afternoon|evening)/)).not.toBeInTheDocument();
    expect(screen.queryByText(user.name)).not.toBeInTheDocument();
    expect(screen.queryByText(/\d{1,2}:\d{2}/)).not.toBeInTheDocument();
  });

  it("keeps setting the document title and description", async () => {
    renderInApp(
      <SimpleStudentLayout title="Exams" subtitle="Your exams" user={user}>
        <p>Page content</p>
      </SimpleStudentLayout>,
    );

    await waitFor(() => expect(document.title).toBe("Exams - School Management System"));
    expect(document.querySelector('meta[name="description"]')).toHaveAttribute("content", "Your exams");
  });

  it("falls back to a generated description when there is no subtitle", async () => {
    renderInApp(
      <SimpleStudentLayout title="Results" user={user}>
        <p>Page content</p>
      </SimpleStudentLayout>,
    );

    await waitFor(() =>
      expect(document.querySelector('meta[name="description"]')).toHaveAttribute(
        "content",
        "Results dashboard for school management",
      ),
    );
  });
});

describe("student pages built on SimpleStudentLayout", () => {
  it.each([
    ["Dashboard", StudentDashboard, "Student Dashboard"],
    ["Exams", StudentExams, "Exams"],
    ["Results", StudentResults, "All Results"],
    ["Events", StudentEvents, "All Events"],
  ] as [string, ComponentType, string][])("%s renders one page title and no banner or main landmark", (_name, Page, title) => {
    store.dispatch(
      loginSuccess({
        user: { id: 1, email: "ada@fixtures.school.test", firstName: "Ada", lastName: "Lovelace", role: "STUDENT" },
        accessToken: "access-token",
        refreshToken: "refresh-token",
      }),
    );
    renderInApp(<Page />);

    expect(screen.getByRole("heading", { level: 1, name: title })).toBeInTheDocument();
    expect(screen.queryByRole("banner")).not.toBeInTheDocument();
    expect(screen.queryByRole("main")).not.toBeInTheDocument();
    expect(screen.queryByText(/Good (morning|afternoon|evening)/)).not.toBeInTheDocument();
  });
});
