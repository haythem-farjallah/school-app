import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClientProvider } from "@tanstack/react-query";
import { http, HttpResponse } from "msw";
import { HelmetProvider } from "react-helmet-async";
import { I18nextProvider } from "react-i18next";
import { Provider } from "react-redux";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { appRoutes } from "./AppRoutes";
import { queryClient } from "@/lib/query-client";
import { setSessionEndedHandler } from "@/lib/session";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { createTestI18n } from "@/test/i18n";
import { apiUrl, server } from "@/test/server";

// The dashboards load their own data; routing is what is under test here.
vi.mock("@/pages/admin/Dashboard", () => ({ default: () => <h1>Admin dashboard page</h1> }));
vi.mock("@/pages/student/Dashboard", () => ({ default: () => <h1>Student dashboard page</h1> }));
vi.mock("@/features/notifications/components/NotificationBell", () => ({ NotificationBell: () => null }));

const account = (role: string) => ({
  id: 1,
  email: "ada@fixtures.school.test",
  firstName: "Ada",
  lastName: "Lovelace",
  role,
});

function signIn(role: string) {
  store.dispatch(loginSuccess({ user: account(role), accessToken: "access-token", refreshToken: "refresh-token" }));
}

function renderApp(path: string) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  render(
    <HelmetProvider>
      <I18nextProvider i18n={createTestI18n()}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <RouterProvider router={router} />
          </QueryClientProvider>
        </Provider>
      </I18nextProvider>
    </HelmetProvider>,
  );
  return router;
}

function loginResponse(role: string, passwordChangeRequired: boolean) {
  return http.post(apiUrl("/auth/login"), () =>
    HttpResponse.json({
      status: "success",
      data: {
        accessToken: "access-token",
        refreshToken: "refresh-token",
        passwordChangeRequired,
        user: account(role),
      },
    }),
  );
}

// jsdom has no matchMedia; the sidebar reads it to tell mobile from desktop.
beforeEach(() => {
  vi.stubGlobal("matchMedia", (query: string) => ({
    matches: false,
    media: query,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  }));
});

afterEach(() => {
  vi.unstubAllGlobals();
  act(() => {
    store.dispatch(resetAuth());
  });
  queryClient.clear();
});

describe("public home", () => {
  it("shows the landing page to a visitor without redirecting to the login page", async () => {
    const router = renderApp("/");

    expect(await screen.findByRole("heading", { level: 1, name: "One place for school life" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
    for (const link of screen.getAllByRole("link", { name: "Log in" })) {
      expect(link).toHaveAttribute("href", "/login");
    }
  });

  it("does not mount the application sidebar or top bar", async () => {
    renderApp("/");

    await screen.findByRole("heading", { level: 1 });
    expect(screen.queryByRole("navigation", { name: "Main navigation" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Account menu/ })).not.toBeInTheDocument();
  });

  it("offers a skip link to the main content", async () => {
    renderApp("/");

    const skipLink = await screen.findByRole("link", { name: "Skip to main content" });
    expect(skipLink).toHaveAttribute("href", "#main-content");
    expect(screen.getByRole("main")).toHaveAttribute("id", "main-content");
  });

  it("opens the login page from the Log in action", async () => {
    const router = renderApp("/");

    await userEvent.setup().click((await screen.findAllByRole("link", { name: "Log in" }))[0]);

    expect(router.state.location.pathname).toBe("/login");
  });
});

describe("personalised home", () => {
  it.each([
    ["ADMIN", "/admin/dashboard"],
    ["STUDENT", "/student/dashboard"],
  ])("keeps a signed-in %s on / and links to their dashboard", async (role, dashboard) => {
    signIn(role);
    const router = renderApp("/");

    expect(await screen.findByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
    expect(screen.getByRole("link", { name: "Go to your dashboard" })).toHaveAttribute("href", dashboard);
    expect(screen.queryByRole("navigation", { name: "Main navigation" })).not.toBeInTheDocument();
  });

  it("follows the dashboard link into the protected application shell", async () => {
    signIn("STUDENT");
    const router = renderApp("/");

    await userEvent.setup().click(await screen.findByRole("link", { name: "Go to your dashboard" }));

    expect(await screen.findByRole("heading", { name: "Student dashboard page" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/student/dashboard");
    expect(screen.getByRole("navigation", { name: "Main navigation" })).toBeInTheDocument();
  });

  it("returns to the public landing page after logout", async () => {
    setSessionEndedHandler(() => store.dispatch(resetAuth()));
    signIn("STUDENT");
    queryClient.setQueryData(["private"], { secret: true });
    const router = renderApp("/");

    await userEvent.setup().click(await screen.findByRole("button", { name: "Log out" }));

    expect(await screen.findByRole("heading", { level: 1, name: "One place for school life" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
    expect(queryClient.getQueryData(["private"])).toBeUndefined();
    expect(screen.queryByText(/Welcome,/)).not.toBeInTheDocument();
  });
});

describe("session restoration", () => {
  it("shows the personalised home on first render when a stored session exists", async () => {
    localStorage.setItem("accessToken", "stored-access");
    localStorage.setItem("refreshToken", "stored-refresh");
    localStorage.setItem("userData", JSON.stringify(account("TEACHER")));
    vi.resetModules();
    const [{ appRoutes: freshRoutes }, { store: freshStore }, { queryClient: freshClient }, { createTestI18n: freshI18n }] =
      await Promise.all([
        import("./AppRoutes"),
        import("@/stores/store"),
        import("@/lib/query-client"),
        import("@/test/i18n"),
      ]);

    const router = createMemoryRouter(freshRoutes, { initialEntries: ["/"] });
    render(
      <HelmetProvider>
        <I18nextProvider i18n={freshI18n()}>
          <Provider store={freshStore}>
            <QueryClientProvider client={freshClient}>
              <RouterProvider router={router} />
            </QueryClientProvider>
          </Provider>
        </I18nextProvider>
      </HelmetProvider>,
    );

    expect(await screen.findByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Go to your dashboard" })).toHaveAttribute("href", "/teacher/dashboard");
    expect(screen.queryByRole("link", { name: "Log in" })).not.toBeInTheDocument();
  });
});

describe("login destination", () => {
  async function logIn() {
    const user = userEvent.setup();
    await user.type(await screen.findByLabelText("Email Address"), "ada@fixtures.school.test");
    await user.type(screen.getByLabelText("Password"), "secret-pass");
    await user.click(screen.getByRole("button", { name: "Sign In" }));
  }

  it("lands on the personalised home after a normal login", async () => {
    server.use(loginResponse("STUDENT", false));
    const router = renderApp("/login");

    await logIn();

    expect(await screen.findByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
  });

  it("sends a user who must change their password to the change-password page first", async () => {
    server.use(loginResponse("STUDENT", true));
    const router = renderApp("/login");

    await logIn();

    await vi.waitFor(() => expect(router.state.location.pathname).toBe("/change-password"));
    expect(screen.queryByText(/Welcome, Ada/)).not.toBeInTheDocument();
  });
});

describe("protected routes", () => {
  it.each(["/admin/dashboard", "/student/dashboard", "/profile"])(
    "send a visitor opening %s to the login page",
    async (path) => {
      const router = renderApp(path);

      await vi.waitFor(() => expect(router.state.location.pathname).toBe("/login"));
      expect(screen.queryByText(/Student dashboard page|Admin dashboard page/)).not.toBeInTheDocument();
    },
  );

  it("keeps a student out of the admin area", async () => {
    signIn("STUDENT");
    const router = renderApp("/admin/dashboard");

    expect(await screen.findByRole("heading", { name: "Student dashboard page" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/student/dashboard");
    expect(screen.queryByText("Admin dashboard page")).not.toBeInTheDocument();
  });

  it("does not let an unknown role into any role area", async () => {
    signIn("SUPERUSER");
    renderApp("/admin/dashboard");

    expect(screen.queryByText("Admin dashboard page")).not.toBeInTheDocument();
    expect(screen.queryByText("Student dashboard page")).not.toBeInTheDocument();
  });
});
