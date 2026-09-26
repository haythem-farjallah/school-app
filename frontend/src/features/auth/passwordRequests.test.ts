import { afterEach, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { api } from "@/lib/api-client";
import { apiUrl, server } from "@/test/server";
import { changePassword } from "./changePassword";
import { forgotPassword, resetPassword } from "./forgotPassword";

describe("password requests", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("changes a password through the api client and resolves an empty response as undefined", async () => {
    const post = vi.spyOn(api, "post");
    let body: unknown;
    server.use(
      http.post(apiUrl("/auth/change-password"), async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 200 });
      }),
    );

    const result = await changePassword({
      email: "student@school.test",
      oldPassword: "old-password",
      newPassword: "new-password",
    });

    expect(result).toBeUndefined();
    expect(post).toHaveBeenCalledOnce();
    expect(body).toEqual({ email: "student@school.test", oldPassword: "old-password", newPassword: "new-password" });
  });

  it("requests a password reset through the api client and resolves an empty response as undefined", async () => {
    const post = vi.spyOn(api, "post");
    let body: unknown;
    server.use(
      http.post(apiUrl("/auth/forgot-password"), async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 200 });
      }),
    );

    const result = await forgotPassword({ email: "student@school.test" });

    expect(result).toBeUndefined();
    expect(post).toHaveBeenCalledOnce();
    expect(body).toEqual({ email: "student@school.test" });
  });

  it("resets a password through the api client and resolves an empty response as undefined", async () => {
    const post = vi.spyOn(api, "post");
    let body: unknown;
    server.use(
      http.post(apiUrl("/auth/reset-password"), async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 200 });
      }),
    );

    const result = await resetPassword({
      email: "student@school.test",
      otp: "123456",
      newPassword: "new-password",
    });

    expect(result).toBeUndefined();
    expect(post).toHaveBeenCalledOnce();
    expect(body).toEqual({ email: "student@school.test", otp: "123456", newPassword: "new-password" });
  });
});
