import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { configureStore } from "@reduxjs/toolkit";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import LoginPage from "./Login";
import authReducer from "@/stores/authSlice";
import { apiUrl, server } from "@/test/server";
import { store as appStore } from "@/stores/store";

function renderLoginPage() {
  const store = configureStore({ reducer: { auth: authReducer } });
  const queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });

  render(
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={["/login"]}>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<p>Home page</p>} />
            <Route path="/change-password" element={<p>Change password page</p>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    </Provider>,
  );
  return store;
}

function loginSuccess(passwordChangeRequired: boolean) {
  return http.post(apiUrl("/auth/login"), () =>
    HttpResponse.json({
      status: "success",
      data: {
        accessToken: "access-token",
        refreshToken: "refresh-token",
        passwordChangeRequired,
        user: { id: 1, email: "admin@fixtures.school.test", firstName: "Ada", lastName: "Admin", role: "ADMIN" },
      },
    }),
  );
}

function refusedCredentials() {
  return http.post(apiUrl("/auth/login"), () =>
    HttpResponse.json(
      { type: "about:blank", title: "Unauthorized", status: 401, detail: "Invalid credentials", instance: "/api/auth/login" },
      { status: 401 },
    ),
  );
}

/** Error notifications are shown from the application store. */
function errorMessages() {
  return appStore.getState().notification.list.filter((n) => n.type === "error").map((n) => n.message);
}

async function submitCredentials(email: string, password: string) {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Email Address"), email);
  await user.type(screen.getByLabelText("Password"), password);
  await user.click(screen.getByRole("button", { name: "Sign In" }));
}

describe("LoginPage", () => {
  it("stores the session and navigates home after a successful login", async () => {
    server.use(loginSuccess(false));
    const store = renderLoginPage();

    await submitCredentials("admin@fixtures.school.test", "secret-pass");

    expect(await screen.findByText("Home page")).toBeInTheDocument();
    expect(store.getState().auth.user?.email).toBe("admin@fixtures.school.test");
    expect(localStorage.getItem("accessToken")).toBe("access-token");
  });

  it("sends users who must change their password to the change-password page", async () => {
    server.use(loginSuccess(true));
    renderLoginPage();

    await submitCredentials("admin@fixtures.school.test", "secret-pass");

    expect(await screen.findByText("Change password page")).toBeInTheDocument();
  });

  it("does not call the backend when the email is invalid", async () => {
    let called = false;
    server.use(
      http.post(apiUrl("/auth/login"), () => {
        called = true;
        return HttpResponse.json({});
      }),
    );
    renderLoginPage();

    await submitCredentials("not-an-email", "secret-pass");

    expect(screen.getByRole("button", { name: "Sign In" })).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it("stays on the login page without a session and shows the reason when credentials are refused", async () => {
    server.use(refusedCredentials());
    const errorsBefore = errorMessages().length;
    const store = renderLoginPage();

    await submitCredentials("admin@fixtures.school.test", "wrong-pass");

    expect(await screen.findByRole("button", { name: "Sign In" })).toBeEnabled();
    expect(screen.queryByText("Home page")).not.toBeInTheDocument();
    expect(store.getState().auth.user).toBeNull();
    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(errorMessages().slice(errorsBefore)).toEqual(["Invalid credentials"]);
  });

  it("stays on the login page and drops a leftover session when credentials are refused", async () => {
    localStorage.setItem("accessToken", "expired-access-token");
    localStorage.setItem("refreshToken", "expired-refresh-token");
    server.use(refusedCredentials());
    renderLoginPage();

    await submitCredentials("admin@fixtures.school.test", "wrong-pass");

    expect(await screen.findByRole("button", { name: "Sign In" })).toBeEnabled();
    expect(screen.queryByText("Home page")).not.toBeInTheDocument();
    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(localStorage.getItem("refreshToken")).toBeNull();
  });
});
