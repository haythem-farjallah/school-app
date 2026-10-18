import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, render, screen, within } from "@testing-library/react";
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
import { token } from "@/lib/token";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { createTestI18n } from "@/test/i18n";
import { studentTimetable } from "@/test/fixtures/student-timetable";
import { apiUrl, server } from "@/test/server";

// The dashboards load their own data; routing is what is under test here.
vi.mock("@/pages/admin/Dashboard", () => ({ default: () => <h1>Admin dashboard page</h1> }));
vi.mock("@/pages/teacher/Dashboard", () => ({ default: () => <h1>Teacher dashboard page</h1> }));
vi.mock("@/pages/parent/Dashboard", () => ({ default: () => <h1>Parent dashboard page</h1> }));
vi.mock("@/pages/staff/Dashboard", () => ({ default: () => <h1>Staff dashboard page</h1> }));
vi.mock("@/pages/student/Dashboard", () => ({ default: () => <h1>Student dashboard page</h1> }));
vi.mock("@/pages/student/Announcements", () => ({ default: () => <h1>Student announcements page</h1> }));
vi.mock("@/pages/Profile", () => ({ default: () => <h1>Profile page</h1> }));
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

// One header, one skip link and one main landmark, whichever shell wraps the page.
function expectSingleLandmarks() {
  expect(screen.getAllByRole("banner")).toHaveLength(1);
  expect(screen.getAllByRole("main")).toHaveLength(1);
  expect(screen.getAllByRole("link", { name: "Skip to main content" })).toHaveLength(1);
  expect(screen.getAllByRole("link", { name: "School App" })).toHaveLength(1);
}

const mainNavigation = () => within(screen.getByRole("navigation", { name: "Main navigation" }));

const currentPages = () =>
  mainNavigation()
    .getAllByRole("link")
    .filter((link) => link.getAttribute("aria-current") === "page")
    .map((link) => link.textContent);

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

  it("never shows the private student links to a visitor", async () => {
    renderApp("/");

    await screen.findByRole("heading", { level: 1 });
    for (const name of ["Dashboard", "Announcements", "My Profile"]) {
      expect(screen.queryByRole("link", { name })).not.toBeInTheDocument();
    }
    expect(screen.queryByRole("button", { name: "Open navigation menu" })).not.toBeInTheDocument();
    expectSingleLandmarks();
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
    ["TEACHER", "/teacher/dashboard"],
    ["PARENT", "/parent/dashboard"],
    ["STAFF", "/staff/dashboard"],
  ])("keeps a signed-in %s on / without the student navigation", async (role, dashboard) => {
    signIn(role);
    const router = renderApp("/");

    expect(await screen.findByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
    expect(screen.getByRole("link", { name: "Go to your dashboard" })).toHaveAttribute("href", dashboard);
    expect(screen.queryByRole("navigation", { name: "Main navigation" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Account menu/ })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Open navigation menu" })).not.toBeInTheDocument();
    expectSingleLandmarks();
  });

  it("keeps a signed-in student on / with the student navigation around the personal home", async () => {
    signIn("STUDENT");
    const router = renderApp("/");

    expect(await screen.findByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
    expect(screen.getByRole("link", { name: "Go to your dashboard" })).toHaveAttribute("href", "/student/dashboard");
    expect(screen.getByRole("link", { name: "My profile" })).toHaveAttribute("href", "/profile");
    expect(mainNavigation().getAllByRole("link").map((link) => link.textContent)).toEqual([
      "Home",
      "Dashboard",
      "Announcements",
      "My Timetable",
    ]);
    expect(currentPages()).toEqual(["Home"]);
    expect(screen.getByRole("main")).toContainElement(screen.getByRole("heading", { level: 1 }));
    expect(screen.queryByRole("button", { name: /sidebar/i })).not.toBeInTheDocument();
    expectSingleLandmarks();
  });

  it("marks only Home as current in the mobile menu of a student on /", async () => {
    signIn("STUDENT");
    renderApp("/");

    await userEvent.setup().click(await screen.findByRole("button", { name: "Open navigation menu" }));

    const menu = within(await screen.findByRole("menu"));
    expect(
      menu.getAllByRole("menuitem").filter((item) => item.getAttribute("aria-current") === "page").map((item) => item.textContent),
    ).toEqual(["Home"]);
  });

  it("moves a student between Home, Dashboard and Announcements in both directions keeping the shell", async () => {
    signIn("STUDENT");
    const router = renderApp("/");
    await screen.findByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" });
    const user = userEvent.setup();

    const open = async (link: string, heading: string, path: string, current: string) => {
      await user.click(mainNavigation().getByRole("link", { name: link }));
      expect(await screen.findByRole("heading", { name: heading })).toBeInTheDocument();
      expect(router.state.location.pathname).toBe(path);
      expect(currentPages()).toEqual([current]);
      expectSingleLandmarks();
    };

    await open("Dashboard", "Student dashboard page", "/student/dashboard", "Dashboard");
    await open("Home", "Welcome, Ada Lovelace", "/", "Home");
    await open("Announcements", "Student announcements page", "/student/announcements", "Announcements");
    await open("Home", "Welcome, Ada Lovelace", "/", "Home");
    await open("Announcements", "Student announcements page", "/student/announcements", "Announcements");
    await open("Dashboard", "Student dashboard page", "/student/dashboard", "Dashboard");
  });

  it("opens the student's profile from the account menu on Home and from the Home shortcut", async () => {
    signIn("STUDENT");
    const router = renderApp("/");
    await screen.findByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" });
    const user = userEvent.setup();

    await user.click(screen.getByRole("button", { name: "Account menu, Ada Lovelace" }));
    await user.click(await screen.findByRole("menuitem", { name: "My Profile" }));
    expect(await screen.findByRole("heading", { name: "Profile page" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/profile");
    expect(mainNavigation().getAllByRole("link")).toHaveLength(4);

    await act(() => router.navigate("/"));
    await user.click(await screen.findByRole("link", { name: "My profile" }));
    expect(await screen.findByRole("heading", { name: "Profile page" })).toBeInTheDocument();
    expectSingleLandmarks();
  });

  it("logs a student out from the account menu on Home and reveals the public home", async () => {
    setSessionEndedHandler(() => store.dispatch(resetAuth()));
    signIn("STUDENT");
    queryClient.setQueryData(["private"], { secret: true });
    const router = renderApp("/");
    await screen.findByRole("heading", { level: 1, name: "Welcome, Ada Lovelace" });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Account menu, Ada Lovelace" }));
    await user.click(await screen.findByRole("menuitem", { name: "Log out" }));

    expect(await screen.findByRole("heading", { level: 1, name: "One place for school life" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/");
    expect(token.access).toBeNull();
    expect(token.refresh).toBeNull();
    expect(store.getState().auth.user).toBeNull();
    expect(queryClient.getQueryData(["private"])).toBeUndefined();
    expect(screen.queryByRole("navigation", { name: "Main navigation" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Account menu/ })).not.toBeInTheDocument();
    expect(screen.queryByText(/Welcome,/)).not.toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "Log in" })[0]).toHaveAttribute("href", "/login");
    expectSingleLandmarks();
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

describe("student timetable routes", () => {
  it.each(["/student/my-timetable", "/student/schedule"])("shows the authenticated timetable at %s with the canonical URL", async (path) => {
    signIn("STUDENT");
    server.use(http.get(apiUrl("/v1/timetables/me"), () => HttpResponse.json(studentTimetable)));
    const router = renderApp(path);
    expect(await screen.findByText("Mathematics")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/student/my-timetable");
    expect(mainNavigation().getByRole("link", { name: "My Timetable" })).toHaveAttribute("aria-current", "page");
  });

  it.each(["/student/my-timetable", "/student/schedule"])("sends visitors at %s to login", async (path) => {
    const router = renderApp(path);
    await vi.waitFor(() => expect(router.state.location.pathname).toBe("/login"));
    expect(screen.queryByRole("heading", { name: "My Timetable" })).not.toBeInTheDocument();
  });

  it.each(["ADMIN", "TEACHER", "PARENT", "STAFF"])("keeps %s out of both student timetable paths without requesting /me", async (role) => {
    signIn(role);
    let requests = 0;
    server.use(http.get(apiUrl("/v1/timetables/me"), () => {
      requests++;
      return HttpResponse.json(studentTimetable);
    }));
    const router = renderApp("/student/my-timetable");
    const destination = `/${role.toLowerCase()}/dashboard`;
    await vi.waitFor(() => expect(router.state.location.pathname).toBe(destination));
    await act(() => router.navigate("/student/schedule"));
    await vi.waitFor(() => expect(router.state.location.pathname).toBe(destination));
    expect(requests).toBe(0);
    expect(screen.queryByRole("heading", { name: "My Timetable" })).not.toBeInTheDocument();
  });
});

it("ends a rejected student timetable session through the existing refresh handling", async () => {
  setSessionEndedHandler(() => store.dispatch(resetAuth()));
  signIn("STUDENT");
  server.use(
    http.get(apiUrl("/v1/timetables/me"), () => new HttpResponse(null, { status: 401 })),
    http.post(apiUrl("/auth/refresh"), () => new HttpResponse(null, { status: 401 })),
  );
  const router = renderApp("/student/my-timetable");
  await vi.waitFor(() => expect(router.state.location.pathname).toBe("/login"));
  expect(token.access).toBeNull();
  expect(token.refresh).toBeNull();
  expect(screen.queryByText("No timetable slots to display")).not.toBeInTheDocument();
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

describe("student shell", () => {
  it("gives a student the top navigation and no sidebar or staff top bar", async () => {
    signIn("STUDENT");
    renderApp("/student/dashboard");

    expect(await screen.findByRole("heading", { name: "Student dashboard page" })).toBeInTheDocument();
    const nav = within(screen.getByRole("navigation", { name: "Main navigation" }));
    expect(nav.getByRole("link", { name: "Dashboard" })).toHaveAttribute("aria-current", "page");
    expect(nav.getAllByRole("link").map((link) => link.textContent)).toEqual(["Home", "Dashboard", "Announcements", "My Timetable"]);
    expect(screen.queryByRole("button", { name: /sidebar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Search pages" })).not.toBeInTheDocument();
    expect(screen.getAllByRole("main")).toHaveLength(1);
  });

  it("opens the existing announcements route from the navigation", async () => {
    signIn("STUDENT");
    const router = renderApp("/student/dashboard");
    await screen.findByRole("heading", { name: "Student dashboard page" });

    await userEvent.setup().click(
      within(screen.getByRole("navigation", { name: "Main navigation" })).getByRole("link", { name: "Announcements" }),
    );

    expect(await screen.findByRole("heading", { name: "Student announcements page" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/student/announcements");
    expect(
      within(screen.getByRole("navigation", { name: "Main navigation" })).getByRole("link", { name: "Announcements" }),
    ).toHaveAttribute("aria-current", "page");
  });

  it("opens the student's own profile at /profile from the account menu", async () => {
    signIn("STUDENT");
    const router = renderApp("/student/dashboard");
    await screen.findByRole("heading", { name: "Student dashboard page" });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Account menu, Ada Lovelace" }));
    await user.click(await screen.findByRole("menuitem", { name: "My Profile" }));

    expect(await screen.findByRole("heading", { name: "Profile page" })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/profile");
    expect(screen.getByRole("navigation", { name: "Main navigation" })).toBeInTheDocument();
  });

  it("logs out to the login page and leaves the public home reachable", async () => {
    setSessionEndedHandler(() => store.dispatch(resetAuth()));
    signIn("STUDENT");
    queryClient.setQueryData(["private"], { secret: true });
    const router = renderApp("/student/dashboard");
    await screen.findByRole("heading", { name: "Student dashboard page" });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Account menu, Ada Lovelace" }));
    await user.click(await screen.findByRole("menuitem", { name: "Log out" }));

    await vi.waitFor(() => expect(router.state.location.pathname).toBe("/login"));
    expect(queryClient.getQueryData(["private"])).toBeUndefined();
    expect(screen.queryByRole("navigation", { name: "Main navigation" })).not.toBeInTheDocument();

    await act(() => router.navigate("/"));
    expect(await screen.findByRole("heading", { level: 1, name: "One place for school life" })).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: "Main navigation" })).not.toBeInTheDocument();
  });
});

describe("shell of the other roles", () => {
  it.each(["ADMIN", "TEACHER", "PARENT", "STAFF"])("keeps the sidebar and top bar for %s", async (role) => {
    signIn(role);
    renderApp("/profile");

    expect(await screen.findByRole("heading", { name: "Profile page" })).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "Main navigation" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Collapse sidebar" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Search pages" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Skip to main content" })).not.toBeInTheDocument();
  });

  it.each(["ADMIN", "TEACHER", "PARENT", "STAFF"])("lets %s open their profile settings and sign out from the account menu", async (role) => {
    setSessionEndedHandler(() => store.dispatch(resetAuth()));
    signIn(role);
    const router = renderApp("/profile");
    await screen.findByRole("heading", { name: "Profile page" });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Account menu, Ada Lovelace" }));
    expect(await screen.findByRole("menuitem", { name: "Profile settings" })).toBeInTheDocument();
    await user.click(screen.getByRole("menuitem", { name: "Sign out" }));

    await vi.waitFor(() => expect(router.state.location.pathname).toBe("/login"));
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
