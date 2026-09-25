import { afterEach, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { AxiosError } from "axios";
import { loginUser, type LoginResponse } from "./login";
import { api } from "@/lib/api-client";
import { apiUrl, server } from "@/test/server";

const loginResponse: LoginResponse = {
  accessToken: "access-token",
  refreshToken: "refresh-token",
  passwordChangeRequired: false,
  user: {
    id: 1,
    email: "admin@fixtures.school.test",
    firstName: "Ada",
    lastName: "Admin",
    role: "ADMIN",
    permissions: [],
  },
};

const refusedCredentials = () =>
  HttpResponse.json(
    { type: "about:blank", title: "Unauthorized", status: 401, detail: "Invalid credentials", instance: "/api/auth/login" },
    { status: 401 },
  );

describe("loginUser", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("posts the credentials through the api client and unwraps the backend envelope itself", async () => {
    const post = vi.spyOn(api, "post");
    let receivedBody: unknown;
    server.use(
      http.post(apiUrl("/auth/login"), async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ status: "success", data: loginResponse });
      }),
    );

    const result = await loginUser({ email: "admin@fixtures.school.test", password: "secret-pass" });

    expect(receivedBody).toEqual({ email: "admin@fixtures.school.test", password: "secret-pass" });
    expect(post).toHaveBeenCalledOnce();
    expect(result).toEqual(loginResponse);
  });

  it("rejects with the HTTP status when credentials are refused", async () => {
    server.use(
      http.post(apiUrl("/auth/login"), refusedCredentials),
    );

    const error = await loginUser({ email: "admin@fixtures.school.test", password: "wrong-pass" }).catch((e) => e);

    expect(error).toBeInstanceOf(AxiosError);
    expect((error as AxiosError).response?.status).toBe(401);
  });

  it("never calls a refresh endpoint when a leftover session is refused", async () => {
    localStorage.setItem("accessToken", "expired-access-token");
    localStorage.setItem("refreshToken", "expired-refresh-token");
    let refreshCalls = 0;
    server.use(
      http.post(apiUrl("/auth/login"), refusedCredentials),
      http.post(apiUrl("/auth/refresh-token"), () => {
        refreshCalls++;
        return new HttpResponse(null, { status: 404 });
      }),
    );

    const error = await loginUser({ email: "admin@fixtures.school.test", password: "wrong-pass" }).catch((e) => e);

    expect((error as AxiosError).response?.status).toBe(401);
    expect(refreshCalls).toBe(0);
    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(localStorage.getItem("refreshToken")).toBeNull();
  });
});
