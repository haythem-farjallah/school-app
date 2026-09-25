import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClientProvider } from "@tanstack/react-query";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { TopNavbar } from "./TopNavbar";
import { RequireAuth } from "@/components/guards/RequireAuth";
import { queryClient } from "@/lib/query-client";
import { token } from "@/lib/token";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";

// The bell loads notifications over HTTP and WebSocket; logout does not involve it.
vi.mock("@/features/notifications/components/NotificationBell", () => ({ NotificationBell: () => null }));
// A stable t, as the initialised i18n instance provides; the search effect depends on it.
vi.mock("react-i18next", () => {
  const t = (key: string) => key;
  return { useTranslation: () => ({ t }) };
});

const user = { id: 1, email: "admin@fixtures.school.test", firstName: "Ada", lastName: "Admin", role: "ADMIN" };

describe("TopNavbar sign out", () => {
  afterEach(() => {
    store.dispatch(resetAuth());
    queryClient.clear();
  });

  it("ends the session and lets the route guard show the login page", async () => {
    store.dispatch(loginSuccess({ user, accessToken: "access-token", refreshToken: "refresh-token" }));
    queryClient.setQueryData(["userProfile"], user);
    render(
      <Provider store={store}>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={["/"]}>
            <Routes>
              <Route path="/login" element={<p>Login page</p>} />
              <Route path="/" element={<RequireAuth><TopNavbar /></RequireAuth>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      </Provider>,
    );

    const userEvents = userEvent.setup();
    await userEvents.click(screen.getByRole("button", { name: /Ada Admin/ }));
    await userEvents.click(await screen.findByRole("menuitem", { name: /Sign out/ }));

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(token.access).toBeNull();
    expect(token.refresh).toBeNull();
    expect(store.getState().auth.user).toBeNull();
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });
});
