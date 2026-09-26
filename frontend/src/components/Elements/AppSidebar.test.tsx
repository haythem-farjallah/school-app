import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClientProvider } from "@tanstack/react-query";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { I18nextProvider } from "react-i18next";
import { AppSidebar } from "./AppSidebar";
import { SidebarProvider } from "@/components/ui/sidebar";
import { RequireAuth } from "@/components/guards/RequireAuth";
import { menuConfig } from "@/config/menuConfig";
import { queryClient } from "@/lib/query-client";
import { token } from "@/lib/token";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { createTestI18n } from "@/test/i18n";


describe("AppSidebar logout", () => {
  beforeEach(() => {
    // jsdom has neither; the sidebar and its scroll area need them.
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
  });

  it.each(["TEACHER", "STUDENT", "PARENT"])("ends the %s session and the guard shows the login page", async (role) => {
    store.dispatch(
      loginSuccess({
        user: { id: 1, email: "user@fixtures.school.test", firstName: "Uma", lastName: "User", role },
        accessToken: "access-token",
        refreshToken: "refresh-token",
      }),
    );
    queryClient.setQueryData(["userProfile"], { id: 1 });
    render(
      <I18nextProvider i18n={createTestI18n()}>
        <Provider store={store}>
          <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={["/"]}>
              <Routes>
                <Route path="/login" element={<p>Login page</p>} />
                <Route
                  path="/"
                  element={
                    <RequireAuth>
                      <SidebarProvider>
                        <AppSidebar />
                      </SidebarProvider>
                    </RequireAuth>
                  }
                />
              </Routes>
            </MemoryRouter>
          </QueryClientProvider>
        </Provider>
      </I18nextProvider>,
    );

    await userEvent.setup().click(screen.getByRole("button", { name: "Logout" }));

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(token.access).toBeNull();
    expect(token.refresh).toBeNull();
    expect(store.getState().auth.user).toBeNull();
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });

  it("uses the logout action, not a /logout link, in every role's menu", () => {
    const items = Object.values(menuConfig).flatMap((sections) => sections.flatMap((section) => section.items));

    expect(items.filter((item) => "href" in item && item.href === "/logout")).toEqual([]);
    expect(items.filter((item) => item.label === "navigation.logout").every((item) => "action" in item)).toBe(true);
  });
});
