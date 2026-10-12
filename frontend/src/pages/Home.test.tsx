import { afterEach, describe, expect, it } from "vitest";
import { act, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClientProvider } from "@tanstack/react-query";
import { HelmetProvider } from "react-helmet-async";
import { I18nextProvider } from "react-i18next";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import Home from "./Home";
import { queryClient } from "@/lib/query-client";
import { setSessionEndedHandler } from "@/lib/session";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { createTestI18n } from "@/test/i18n";
import { supportedLocales, type SupportedLocale } from "@/services/i18n";

type TestUser = { id: number; email: string; firstName: string; lastName: string; role: string };

const user = (role: string, overrides: Partial<TestUser> = {}): TestUser => ({
  id: 1,
  email: "ada@fixtures.school.test",
  firstName: "Ada",
  lastName: "Lovelace",
  role,
  ...overrides,
});

function signIn(account: TestUser) {
  store.dispatch(loginSuccess({ user: account, accessToken: "access-token", refreshToken: "refresh-token" }));
}

function renderHome(lng: SupportedLocale = "en") {
  return render(
    <HelmetProvider>
      <I18nextProvider i18n={createTestI18n(lng)}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter>
              <Routes>
                <Route path="/" element={<Home />} />
              </Routes>
            </MemoryRouter>
          </QueryClientProvider>
        </Provider>
      </I18nextProvider>
    </HelmetProvider>,
  );
}

afterEach(() => {
  act(() => {
    store.dispatch(resetAuth());
  });
  queryClient.clear();
});

describe("Home for a visitor", () => {
  it("introduces School App and offers to log in", () => {
    renderHome();

    expect(screen.getByRole("heading", { level: 1, name: "One place for school life" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Log in" })).toHaveAttribute("href", "/login");
  });

  it("describes the four audiences under one section heading", () => {
    renderHome();

    const section = screen.getByRole("region", { name: "Who it is for" });
    const titles = within(section).getAllByRole("heading", { level: 3 }).map((heading) => heading.textContent);
    expect(titles).toEqual(["Students", "Teachers", "Parents", "Staff and administrators"]);
  });

  it("shows nothing personal and no dashboard, profile or logout shortcut", () => {
    renderHome();

    expect(screen.queryByText(/Welcome,/)).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Go to your dashboard" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "My profile" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Log out" })).not.toBeInTheDocument();
  });
});

describe("Home for a signed-in user", () => {
  it.each([
    ["ADMIN", "Admin", "/admin/dashboard"],
    ["TEACHER", "Teacher", "/teacher/dashboard"],
    ["STUDENT", "Student", "/student/dashboard"],
    ["PARENT", "Parent", "/parent/dashboard"],
    ["STAFF", "Staff", "/staff/dashboard"],
  ])("greets a %s by name and links to their own dashboard", (role, roleLabel, dashboard) => {
    signIn(user(role));
    renderHome();

    expect(screen.getByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" })).toBeInTheDocument();
    expect(screen.getByText(roleLabel)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Go to your dashboard" })).toHaveAttribute("href", dashboard);
    expect(screen.getByRole("link", { name: "My profile" })).toHaveAttribute("href", "/profile");
    expect(screen.getByRole("button", { name: "Log out" })).toBeInTheDocument();
  });

  it("does not show the visitor landing page", () => {
    signIn(user("STUDENT"));
    renderHome();

    expect(screen.queryByRole("link", { name: "Log in" })).not.toBeInTheDocument();
    expect(screen.queryByText("Who it is for")).not.toBeInTheDocument();
  });

  it("falls back to a neutral name when the account has no name", () => {
    signIn(user("STUDENT", { firstName: "", lastName: "  " }));
    renderHome();

    expect(screen.getByRole("heading", { level: 1, name: "Welcome, User" })).toBeInTheDocument();
  });

  it("keeps a very long name in the heading so it can wrap", () => {
    const lastName = "Montgomery-Featherstonehaugh".repeat(4);
    signIn(user("PARENT", { lastName }));
    renderHome();

    const heading = screen.getByRole("heading", { level: 1 });
    expect(heading).toHaveTextContent(`Welcome, Ada ${lastName}`);
    expect(heading).toHaveClass("break-words");
  });

  it("offers no dashboard for an unknown role, only the profile and logout", () => {
    signIn(user("SUPERUSER"));
    renderHome();

    expect(screen.queryByRole("link", { name: "Go to your dashboard" })).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Your account role is not recognised");
    expect(screen.getByRole("link", { name: "My profile" })).toHaveAttribute("href", "/profile");
    expect(screen.getByRole("button", { name: "Log out" })).toBeInTheDocument();
  });

  it("ends the session on logout: auth state, stored tokens and cached data are cleared", async () => {
    setSessionEndedHandler(() => store.dispatch(resetAuth()));
    signIn(user("STUDENT"));
    queryClient.setQueryData(["private"], { secret: true });
    renderHome();

    await userEvent.setup().click(screen.getByRole("button", { name: "Log out" }));

    expect(store.getState().auth.user).toBeNull();
    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(queryClient.getQueryData(["private"])).toBeUndefined();
    expect(await screen.findByRole("link", { name: "Log in" })).toBeInTheDocument();
    expect(screen.queryByText(/Welcome,/)).not.toBeInTheDocument();
  });
});

describe("Home translations", () => {
  it.each(supportedLocales)("%s renders the visitor and signed-in pages without raw keys", (locale) => {
    const visitor = renderHome(locale);
    expect(visitor.container.textContent).not.toMatch(/\bhome\.[a-zA-Z]/);
    visitor.unmount();

    signIn(user("TEACHER"));
    const signedIn = renderHome(locale);
    expect(signedIn.container.textContent).not.toMatch(/\bhome\.[a-zA-Z]/);
  });
});
