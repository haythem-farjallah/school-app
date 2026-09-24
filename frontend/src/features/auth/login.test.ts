import { describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { AxiosError } from "axios";
import { loginUser, type LoginResponse } from "./login";
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

describe("loginUser", () => {
  it("posts the credentials and returns the login payload from the backend envelope", async () => {
    let receivedBody: unknown;
    server.use(
      http.post(apiUrl("/auth/login"), async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ status: "success", data: loginResponse });
      }),
    );

    const result = await loginUser({ email: "admin@fixtures.school.test", password: "secret-pass" });

    expect(receivedBody).toEqual({ email: "admin@fixtures.school.test", password: "secret-pass" });
    expect(result).toEqual(loginResponse);
  });

  it("rejects with the HTTP status when credentials are refused", async () => {
    server.use(
      http.post(apiUrl("/auth/login"), () =>
        HttpResponse.json({ status: "UNAUTHORIZED", message: "Invalid credentials" }, { status: 401 }),
      ),
    );

    const error = await loginUser({ email: "admin@fixtures.school.test", password: "wrong-pass" }).catch((e) => e);

    expect(error).toBeInstanceOf(AxiosError);
    expect((error as AxiosError).response?.status).toBe(401);
  });
});
