import { afterEach, describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { Provider } from "react-redux";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { RequireAuth } from "./RequireAuth";
import { api } from "@/lib/api-client";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { apiUrl, server } from "@/test/server";

const user = { id: 1, email: "admin@fixtures.school.test", firstName: "Ada", lastName: "Admin", role: "ADMIN" };

function renderProtectedPage() {
  render(
    <Provider store={store}>
      <MemoryRouter initialEntries={["/"]}>
        <Routes>
          <Route path="/login" element={<p>Login page</p>} />
          <Route path="/" element={<RequireAuth><p>Protected page</p></RequireAuth>} />
        </Routes>
      </MemoryRouter>
    </Provider>,
  );
}

describe("RequireAuth", () => {
  afterEach(() => {
    store.dispatch(resetAuth());
  });

  it("sends visitors without a session to the login page", () => {
    renderProtectedPage();

    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("moves to the login page when an API request ends the session with 401", async () => {
    store.dispatch(loginSuccess({ user, accessToken: "access-token", refreshToken: "refresh-token" }));
    server.use(http.get(apiUrl("/me/profile"), () => new HttpResponse(null, { status: 401 })));
    renderProtectedPage();
    expect(screen.getByText("Protected page")).toBeInTheDocument();

    await api.get("/me/profile").catch(() => undefined);

    expect(await screen.findByText("Login page")).toBeInTheDocument();
    expect(screen.queryByText("Protected page")).not.toBeInTheDocument();
  });
});
