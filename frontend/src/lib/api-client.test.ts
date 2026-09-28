import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { AxiosError } from "axios";
import { api } from "./api-client";
import { token } from "./token";
import { queryClient } from "./query-client";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { apiUrl, server } from "@/test/server";

const user = { id: 10, email: "teacher@fixtures.school.test", firstName: "Theo", lastName: "Teacher", role: "TEACHER" };

// Every test gets its own session tokens, so no test sees a refresh made for another test's token.
let session = 0;
let accessToken = "";
let refreshToken = "";

function resetAuthDispatches(dispatch: ReturnType<typeof vi.spyOn>) {
  return dispatch.mock.calls.filter(([action]) => resetAuth.match(action)).length;
}

/** GET /me/profile answers 200 for `validToken` and 401 for any other bearer token. */
function serveProfileOnlyFor(validToken: string) {
  const authorizations: (string | null)[] = [];
  server.use(
    http.get(apiUrl("/me/profile"), ({ request }) => {
      const authorization = request.headers.get("Authorization");
      authorizations.push(authorization);
      return authorization === `Bearer ${validToken}`
        ? HttpResponse.json({ id: user.id })
        : new HttpResponse(null, { status: 401 });
    }),
  );
  return authorizations;
}

/** POST /auth/refresh with the given outcome; returns the refresh tokens it received. */
function serveRefresh(respond: () => Response | Promise<Response>) {
  const received: string[] = [];
  server.use(
    http.post(apiUrl("/auth/refresh"), async ({ request }) => {
      received.push(((await request.json()) as { refreshToken: string }).refreshToken);
      return respond();
    }),
  );
  return received;
}

function refreshed(newAccessToken: string) {
  return HttpResponse.json({ status: "success", data: { accessToken: newAccessToken } });
}

function expectSessionEnded() {
  expect(token.access).toBeNull();
  expect(token.refresh).toBeNull();
  expect(token.user).toBeNull();
  expect(store.getState().auth).toEqual({ user: null, accessToken: null, refreshToken: null });
  expect(queryClient.getQueryData(["userProfile"])).toBeUndefined();
}

describe("api client session handling", () => {
  beforeEach(() => {
    session += 1;
    accessToken = `access-${session}`;
    refreshToken = `refresh-${session}`;
    store.dispatch(loginSuccess({ user, accessToken, refreshToken }));
  });

  afterEach(() => {
    store.dispatch(resetAuth());
    token.clear();
    queryClient.clear();
    vi.restoreAllMocks();
  });

  it("leaves the session untouched after a successful request", async () => {
    serveProfileOnlyFor(accessToken);

    await expect(api.get("/me/profile")).resolves.toMatchObject({ status: 200 });

    expect(token.access).toBe(accessToken);
    expect(token.refresh).toBe(refreshToken);
    expect(store.getState().auth.user).toEqual(user);
  });

  it("refreshes the access token after a 401 and retries the request once with it", async () => {
    const authorizations = serveProfileOnlyFor("renewed-access");
    const refreshes = serveRefresh(() => refreshed("renewed-access"));

    const response = await api.get("/me/profile");

    expect(response.status).toBe(200);
    expect(refreshes).toEqual([refreshToken]);
    expect(authorizations).toEqual([`Bearer ${accessToken}`, "Bearer renewed-access"]);
    expect(token.access).toBe("renewed-access");
    expect(token.refresh).toBe(refreshToken);
    expect(store.getState().auth).toEqual({ user, accessToken: "renewed-access", refreshToken });
  });

  it("refreshes once when concurrent requests all return 401 and retries each of them", async () => {
    let releaseRefresh = undefined as (() => void) | undefined;
    const authorizations = serveProfileOnlyFor("renewed-access");
    const refreshes = serveRefresh(async () => {
      await new Promise<void>((resolve) => (releaseRefresh = resolve));
      return refreshed("renewed-access");
    });

    const requests = Promise.all([api.get("/me/profile"), api.get("/me/profile"), api.get("/me/profile")]);
    await vi.waitFor(() => expect(releaseRefresh).toBeDefined());
    releaseRefresh?.();
    const responses = await requests;

    expect(responses.map((response) => response.status)).toEqual([200, 200, 200]);
    expect(refreshes).toHaveLength(1);
    expect(authorizations.filter((a) => a === "Bearer renewed-access")).toHaveLength(3);
    expect(store.getState().auth.accessToken).toBe("renewed-access");
  });

  it("retries a 401 that arrives after the refresh for its token already completed, without refreshing again", async () => {
    let respondLate = undefined as ((response: Response) => void) | undefined;
    const refreshes = serveRefresh(() => refreshed("renewed-access"));
    server.use(
      http.get(apiUrl("/me/settings"), ({ request }) =>
        request.headers.get("Authorization") === "Bearer renewed-access"
          ? HttpResponse.json({ theme: "light" })
          : new Promise<Response>((resolve) => (respondLate = resolve)),
      ),
    );
    serveProfileOnlyFor("renewed-access");

    const late = api.get("/me/settings");
    await vi.waitFor(() => expect(respondLate).toBeDefined());
    await api.get("/me/profile");
    respondLate?.(new HttpResponse(null, { status: 401 }));

    await expect(late).resolves.toMatchObject({ status: 200 });
    expect(refreshes).toHaveLength(1);
  });

  it("ends the session when the refresh is rejected and still rejects the request", async () => {
    serveProfileOnlyFor("never-issued");
    const refreshes = serveRefresh(() => new HttpResponse(null, { status: 401 }));
    queryClient.setQueryData(["userProfile"], user);

    const error = await api.get("/me/profile").catch((e) => e);

    expect(error).toBeInstanceOf(AxiosError);
    expect((error as AxiosError).response?.status).toBe(401);
    expect(refreshes).toHaveLength(1);
    expectSessionEnded();
  });

  it.each([
    ["is rate limited", () => new HttpResponse(null, { status: 429 })],
    ["fails on the server", () => new HttpResponse(null, { status: 503 })],
    ["fails with an unexpected server error", () => new HttpResponse(null, { status: 500 })],
    ["cannot reach the server", () => HttpResponse.error()],
  ])("keeps the session when the refresh %s and still rejects the request", async (_outcome, respond) => {
    serveProfileOnlyFor("never-issued");
    const refreshes = serveRefresh(respond);
    const dispatch = vi.spyOn(store, "dispatch");
    queryClient.setQueryData(["userProfile"], user);

    const error = await api.get("/me/profile").catch((e) => e);

    expect(error).toBeInstanceOf(AxiosError);
    expect((error as AxiosError).response?.status).toBe(401);
    expect(refreshes).toHaveLength(1);
    expect(resetAuthDispatches(dispatch)).toBe(0);
    expect(token.access).toBe(accessToken);
    expect(token.refresh).toBe(refreshToken);
    expect(token.user).toEqual(user);
    expect(store.getState().auth).toEqual({ user, accessToken, refreshToken });
    expect(queryClient.getQueryData(["userProfile"])).toEqual(user);
  });

  it("shares one failed refresh between concurrent 401s and refreshes again on a later request", async () => {
    let refreshAvailable = false;
    const authorizations = serveProfileOnlyFor("renewed-access");
    const refreshes = serveRefresh(() =>
      refreshAvailable ? refreshed("renewed-access") : new HttpResponse(null, { status: 503 }),
    );

    const results = await Promise.allSettled([api.get("/me/profile"), api.get("/me/profile"), api.get("/me/profile")]);

    expect(results.map((result) => result.status)).toEqual(["rejected", "rejected", "rejected"]);
    expect(refreshes).toHaveLength(1);
    expect(token.access).toBe(accessToken);

    refreshAvailable = true;
    const response = await api.get("/me/profile");

    expect(response.status).toBe(200);
    expect(refreshes).toHaveLength(2);
    expect(authorizations.at(-1)).toBe("Bearer renewed-access");
    expect(store.getState().auth).toEqual({ user, accessToken: "renewed-access", refreshToken });
  });

  it("ends the session once when concurrent requests all return 401 and the refresh is rejected", async () => {
    serveProfileOnlyFor("never-issued");
    const refreshes = serveRefresh(() => new HttpResponse(null, { status: 401 }));
    const dispatch = vi.spyOn(store, "dispatch");

    const results = await Promise.allSettled([api.get("/me/profile"), api.get("/me/profile"), api.get("/me/profile")]);

    expect(results.map((result) => result.status)).toEqual(["rejected", "rejected", "rejected"]);
    expect(refreshes).toHaveLength(1);
    expect(resetAuthDispatches(dispatch)).toBe(1);
    expectSessionEnded();
  });

  it("ends the session without calling refresh when there is no refresh token", async () => {
    token.refresh = null;
    serveProfileOnlyFor("never-issued");
    const refreshes = serveRefresh(() => refreshed("renewed-access"));
    queryClient.setQueryData(["userProfile"], user);

    const error = await api.get("/me/profile").catch((e) => e);

    expect((error as AxiosError).response?.status).toBe(401);
    expect(refreshes).toHaveLength(0);
    expectSessionEnded();
  });

  it("ends the session when the retried request is rejected as well, without refreshing again", async () => {
    const authorizations = serveProfileOnlyFor("never-issued");
    const refreshes = serveRefresh(() => refreshed("renewed-access"));

    const error = await api.get("/me/profile").catch((e) => e);

    expect((error as AxiosError).response?.status).toBe(401);
    expect(refreshes).toHaveLength(1);
    expect(authorizations).toEqual([`Bearer ${accessToken}`, "Bearer renewed-access"]);
    expectSessionEnded();
  });

  it("does not refresh or end the session when a request sent without a token returns 401", async () => {
    store.dispatch(resetAuth());
    token.clear();
    server.use(http.post(apiUrl("/auth/login"), () => new HttpResponse(null, { status: 401 })));
    const refreshes = serveRefresh(() => refreshed("renewed-access"));
    const dispatch = vi.spyOn(store, "dispatch");

    const error = await api.post("/auth/login", { email: user.email, password: "wrong" }).catch((e) => e);

    expect((error as AxiosError).response?.status).toBe(401);
    expect(refreshes).toHaveLength(0);
    expect(resetAuthDispatches(dispatch)).toBe(0);
  });

  it("keeps a newer session when a 401 for an earlier token arrives late", async () => {
    let respond = undefined as ((response: Response) => void) | undefined;
    server.use(http.get(apiUrl("/me/profile"), () => new Promise<Response>((resolve) => (respond = resolve))));
    const refreshes = serveRefresh(() => refreshed("renewed-access"));

    const earlierRequest = api.get("/me/profile").catch((e) => e);
    await vi.waitFor(() => expect(respond).toBeDefined());
    store.dispatch(loginSuccess({ user, accessToken: "newer-access", refreshToken: "newer-refresh" }));
    respond?.(new HttpResponse(null, { status: 401 }));

    expect(((await earlierRequest) as AxiosError).response?.status).toBe(401);
    expect(refreshes).toHaveLength(0);
    expect(token.access).toBe("newer-access");
    expect(token.refresh).toBe("newer-refresh");
    expect(store.getState().auth).toEqual({ user, accessToken: "newer-access", refreshToken: "newer-refresh" });
  });

  it("keeps a newer session when a refresh started by the earlier session completes late", async () => {
    let finishRefresh = undefined as ((response: Response) => void) | undefined;
    serveProfileOnlyFor("never-issued");
    serveRefresh(() => new Promise<Response>((resolve) => (finishRefresh = resolve)));

    const earlierRequest = api.get("/me/profile").catch((e) => e);
    await vi.waitFor(() => expect(finishRefresh).toBeDefined());
    store.dispatch(loginSuccess({ user, accessToken: "newer-access", refreshToken: "newer-refresh" }));
    finishRefresh?.(refreshed("renewed-for-earlier-session"));

    expect(((await earlierRequest) as AxiosError).response?.status).toBe(401);
    expect(token.access).toBe("newer-access");
    expect(store.getState().auth).toEqual({ user, accessToken: "newer-access", refreshToken: "newer-refresh" });
  });

  it("keeps a newer session when a refresh started by the earlier session is rejected late", async () => {
    let finishRefresh = undefined as ((response: Response) => void) | undefined;
    serveProfileOnlyFor("never-issued");
    serveRefresh(() => new Promise<Response>((resolve) => (finishRefresh = resolve)));

    const earlierRequest = api.get("/me/profile").catch((e) => e);
    await vi.waitFor(() => expect(finishRefresh).toBeDefined());
    store.dispatch(loginSuccess({ user, accessToken: "newer-access", refreshToken: "newer-refresh" }));
    finishRefresh?.(new HttpResponse(null, { status: 401 }));

    expect(((await earlierRequest) as AxiosError).response?.status).toBe(401);
    expect(token.access).toBe("newer-access");
    expect(token.refresh).toBe("newer-refresh");
    expect(store.getState().auth.user).toEqual(user);
  });
});
