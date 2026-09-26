import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClientProvider } from "@tanstack/react-query";
import { Provider } from "react-redux";
import { MemoryRouter, Outlet, Route, Routes } from "react-router-dom";
import { TopNavbar } from "./TopNavbar";
import { AppSidebar } from "./AppSidebar";
import { SidebarProvider } from "@/components/ui/sidebar";
import { RequireAuth } from "@/components/guards/RequireAuth";
import { queryClient } from "@/lib/query-client";
import { token } from "@/lib/token";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";

// The bell loads notifications over HTTP and WebSocket; the shell behaviour here does not involve it.
vi.mock("@/features/notifications/components/NotificationBell", () => ({ NotificationBell: () => null }));
// A stable t, as the initialised i18n instance provides; the search items depend on it.
vi.mock("react-i18next", () => {
  const t = (key: string) => key;
  return { useTranslation: () => ({ t }) };
});

const admin = { id: 1, email: "admin@fixtures.school.test", firstName: "Ada", lastName: "Admin", role: "ADMIN" };
const student = { id: 2, email: "student@fixtures.school.test", firstName: "Sam", lastName: "Student", role: "STUDENT" };

function signIn(user: typeof admin, permissions: string[] = []) {
  store.dispatch(
    loginSuccess({ user: { ...user, permissions }, accessToken: "access-token", refreshToken: "refresh-token" }),
  );
  queryClient.setQueryData(["userProfile"], user);
}

function renderShell(path = "/") {
  return render(
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/login" element={<p>Login page</p>} />
            <Route
              element={
                <RequireAuth>
                  <SidebarProvider>
                    <AppSidebar />
                    <TopNavbar />
                    <Outlet />
                  </SidebarProvider>
                </RequireAuth>
              }
            >
              <Route path="/" element={<p>Home page</p>} />
              <Route path="/student/results" element={<p>Results page</p>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </Provider>,
  );
}

async function openSearch() {
  const user = userEvent.setup();
  await user.keyboard("{Control>}k{/Control}");
  const dialog = await screen.findByRole("dialog", { name: "Search pages" });
  return { user, dialog };
}

describe("TopNavbar", () => {
  beforeEach(() => {
    // jsdom has neither; the sidebar primitive and its scroll area need them.
    vi.stubGlobal("matchMedia", (query: string) => ({
      matches: false,
      media: query,
      addEventListener: () => {},
      removeEventListener: () => {},
    }));
    vi.stubGlobal(
      "ResizeObserver",
      class {
        observe() {}
        unobserve() {}
        disconnect() {}
      },
    );
  });

  afterEach(() => {
    store.dispatch(resetAuth());
    queryClient.clear();
    vi.unstubAllGlobals();
    window.innerWidth = 1024;
  });

  it("signs out: ends the session and lets the route guard show the login page", async () => {
    signIn(admin);
    renderShell();

    const userEvents = userEvent.setup();
    await userEvents.click(screen.getByRole("button", { name: /Ada Admin/ }));
    await userEvents.click(await screen.findByRole("menuitem", { name: /Sign out/ }));

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(token.access).toBeNull();
    expect(token.refresh).toBeNull();
    expect(store.getState().auth.user).toBeNull();
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });

  it("searches only the current role's pages and never lists logout", async () => {
    signIn(student);
    renderShell();

    const { dialog } = await openSearch();
    const titles = within(dialog)
      .getAllByRole("option")
      .map((option) => option.textContent);

    expect(titles).toEqual(expect.arrayContaining([expect.stringContaining("Results"), expect.stringContaining("Learning Space")]));
    for (const adminPage of ["Students", "Teachers", "Classes"]) {
      expect(titles.some((title) => title?.startsWith(adminPage))).toBe(false);
    }
    expect(titles.some((title) => title?.includes("Logout"))).toBe(false);
  });

  it("offers permission-gated pages only to users holding the permission", async () => {
    signIn(admin);
    const { unmount } = renderShell();
    let { user, dialog } = await openSearch();
    await user.type(within(dialog).getByRole("combobox"), "Permissions");
    expect(within(dialog).queryByRole("option", { name: /Permissions/ })).not.toBeInTheDocument();
    unmount();

    signIn(admin, ["PERMISSIONS_MANAGE"]);
    renderShell();
    ({ user, dialog } = await openSearch());
    await user.type(within(dialog).getByRole("combobox"), "Permissions");
    expect(within(dialog).getByRole("option", { name: /Permissions/ })).toBeInTheDocument();
  });

  it("opens the keyboard-selected result", async () => {
    signIn(student);
    renderShell();

    const { user, dialog } = await openSearch();
    await user.type(within(dialog).getByRole("combobox"), "res");
    expect(within(dialog).getByRole("option", { selected: true })).toHaveTextContent("Results");
    await user.keyboard("{Enter}");

    expect(await screen.findByText("Results page")).toBeInTheDocument();
  });

  it("opens the mobile navigation from the menu trigger and closes it after choosing a page", async () => {
    window.innerWidth = 390;
    signIn(student);
    renderShell();

    const user = userEvent.setup();
    await user.click(await screen.findByRole("button", { name: "Open navigation menu" }));
    const sheet = await screen.findByRole("dialog", { name: "Sidebar" });
    await user.click(within(sheet).getByRole("link", { name: "Results" }));

    expect(await screen.findByText("Results page")).toBeInTheDocument();
    expect(screen.queryByRole("dialog", { name: "Sidebar" })).not.toBeInTheDocument();
  });
});
