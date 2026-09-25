import { beforeEach, describe, expect, it } from "vitest";
import { http, HttpResponse } from "msw";
import { AxiosError } from "axios";
import {
  getCurrentUserProfile,
  getProfileSettings,
  updateProfileSettings,
  updateUserProfile,
  type ProfileSettings,
  type UserProfile,
} from "./profileSettings";
import { token } from "@/lib/token";
import { apiUrl, server } from "@/test/server";

const profile: UserProfile = {
  id: 10,
  firstName: "Theo",
  lastName: "Teacher",
  email: "teacher@fixtures.school.test",
  role: "TEACHER",
  telephone: null,
  address: null,
  profileTheme: "light",
  profileLanguage: "en",
  permissions: [],
};

const settings: ProfileSettings = {
  language: "fr",
  theme: "dark",
  notificationsEnabled: false,
  darkMode: true,
};

describe("profile and settings API", () => {
  beforeEach(() => {
    token.access = "access-token";
  });

  it("returns the current user's profile and sends the bearer token", async () => {
    let authorization: string | null = null;
    server.use(
      http.get(apiUrl("/me/profile"), ({ request }) => {
        authorization = request.headers.get("Authorization");
        return HttpResponse.json(profile);
      }),
    );

    await expect(getCurrentUserProfile()).resolves.toEqual(profile);
    expect(authorization).toBe("Bearer access-token");
  });

  it("returns the profile settings", async () => {
    server.use(http.get(apiUrl("/me/settings"), () => HttpResponse.json(settings)));

    await expect(getProfileSettings()).resolves.toEqual(settings);
  });

  it("sends the profile update and returns the updated profile", async () => {
    let body: unknown;
    server.use(
      http.patch(apiUrl("/me/profile"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ ...profile, telephone: "+1 555 0100", address: "1 Test Road" });
      }),
    );

    await expect(updateUserProfile({ telephone: "+1 555 0100", address: "1 Test Road" })).resolves.toEqual({
      ...profile,
      telephone: "+1 555 0100",
      address: "1 Test Road",
    });
    expect(body).toEqual({ telephone: "+1 555 0100", address: "1 Test Road" });
  });

  it("sends the settings update and returns the updated settings", async () => {
    let body: unknown;
    server.use(
      http.patch(apiUrl("/me/settings"), async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(settings);
      }),
    );

    await expect(updateProfileSettings({ language: "fr", darkMode: true })).resolves.toEqual(settings);
    expect(body).toEqual({ language: "fr", darkMode: true });
  });

  it("rejects on a server error instead of returning default values", async () => {
    server.use(
      http.get(apiUrl("/me/profile"), () => HttpResponse.json({ message: "UNEXPECTED_ERROR" }, { status: 500 })),
      http.get(apiUrl("/me/settings"), () => HttpResponse.json({ message: "UNEXPECTED_ERROR" }, { status: 500 })),
    );

    const profileError = await getCurrentUserProfile().catch((e) => e);
    const settingsError = await getProfileSettings().catch((e) => e);

    expect(profileError).toBeInstanceOf(AxiosError);
    expect((profileError as AxiosError).response?.status).toBe(500);
    expect(settingsError).toBeInstanceOf(AxiosError);
    expect((settingsError as AxiosError).response?.status).toBe(500);
    expect(token.access).toBe("access-token");
  });

  it("clears the session and rejects when the token is no longer accepted", async () => {
    token.refresh = "refresh-token";
    server.use(http.get(apiUrl("/me/profile"), () => new HttpResponse(null, { status: 401 })));

    const error = await getCurrentUserProfile().catch((e) => e);

    expect((error as AxiosError).response?.status).toBe(401);
    expect(token.access).toBeNull();
    expect(token.refresh).toBeNull();
  });
});
