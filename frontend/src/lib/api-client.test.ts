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

function resetAuthDispatches(dispatch: ReturnType<typeof vi.spyOn>) {
  return dispatch.mock.calls.filter(([action]) => resetAuth.match(action)).length;
}

describe("api client session handling", () => {
  beforeEach(() => {
    store.dispatch(loginSuccess({ user, accessToken: "access-token", refreshToken: "refresh-token" }));
  });

  afterEach(() => {
    store.dispatch(resetAuth());
    vi.restoreAllMocks();
  });

  it("leaves the session untouched after a successful request", async () => {
    server.use(http.get(apiUrl("/me/profile"), () => HttpResponse.json({ id: 10 })));

    await expect(api.get("/me/profile")).resolves.toMatchObject({ status: 200 });

    expect(token.access).toBe("access-token");
    expect(token.refresh).toBe("refresh-token");
    expect(store.getState().auth.user).toEqual(user);
  });

  it("ends the session when an authenticated request returns 401 and still rejects the request", async () => {
    server.use(http.get(apiUrl("/me/profile"), () => new HttpResponse(null, { status: 401 })));
    queryClient.setQueryData(["userProfile"], user);

    const error = await api.get("/me/profile").catch((e) => e);

    expect(error).toBeInstanceOf(AxiosError);
    expect((error as AxiosError).response?.status).toBe(401);
    expect(token.access).toBeNull();
    expect(token.refresh).toBeNull();
    expect(store.getState().auth).toEqual({ user: null, accessToken: null, refreshToken: null });
    expect(queryClient.getQueryData(["userProfile"])).toBeUndefined();
  });

  it("ends the session once when concurrent requests all return 401", async () => {
    server.use(
      http.get(apiUrl("/me/profile"), () => new HttpResponse(null, { status: 401 })),
      http.get(apiUrl("/me/settings"), () => new HttpResponse(null, { status: 401 })),
    );
    const dispatch = vi.spyOn(store, "dispatch");

    const results = await Promise.allSettled([api.get("/me/profile"), api.get("/me/settings"), api.get("/me/profile")]);

    expect(results.map((result) => result.status)).toEqual(["rejected", "rejected", "rejected"]);
    expect(resetAuthDispatches(dispatch)).toBe(1);
    expect(store.getState().auth.user).toBeNull();
  });

  it("does not end the session when a request sent without a token returns 401", async () => {
    store.dispatch(resetAuth());
    token.clear();
    server.use(http.post(apiUrl("/auth/login"), () => new HttpResponse(null, { status: 401 })));
    const dispatch = vi.spyOn(store, "dispatch");

    const error = await api.post("/auth/login", { email: user.email, password: "wrong" }).catch((e) => e);

    expect((error as AxiosError).response?.status).toBe(401);
    expect(resetAuthDispatches(dispatch)).toBe(0);
  });

  it("keeps a newer session when a 401 for an earlier token arrives late", async () => {
    let respond = undefined as ((response: Response) => void) | undefined;
    server.use(http.get(apiUrl("/me/profile"), () => new Promise<Response>((resolve) => (respond = resolve))));

    const earlierRequest = api.get("/me/profile").catch((e) => e);
    await vi.waitFor(() => expect(respond).toBeDefined());
    store.dispatch(loginSuccess({ user, accessToken: "newer-token", refreshToken: "newer-refresh" }));
    respond?.(new HttpResponse(null, { status: 401 }));

    expect(((await earlierRequest) as AxiosError).response?.status).toBe(401);
    expect(token.access).toBe("newer-token");
    expect(store.getState().auth.user).toEqual(user);
  });
});
