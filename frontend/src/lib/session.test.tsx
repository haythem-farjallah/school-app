import { afterEach, describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { QueryClientProvider } from "@tanstack/react-query";
import { queryClient } from "./query-client";
import { terminateSession } from "./session";
import { token } from "./token";
import { store } from "@/stores/store";
import { loginSuccess, resetAuth } from "@/stores/authSlice";
import { useUserProfile } from "@/hooks/useProfileSettings";
import { apiUrl, server } from "@/test/server";

const alice = { id: 1, email: "alice@fixtures.school.test", firstName: "Alice", lastName: "Student", role: "STUDENT" };
const bob = { id: 2, email: "bob@fixtures.school.test", firstName: "Bob", lastName: "Student", role: "STUDENT" };
const profileByToken: Record<string, typeof alice> = { "Bearer alice-token": alice, "Bearer bob-token": bob };

function serveProfileForToken() {
  server.use(
    http.get(apiUrl("/me/profile"), ({ request }) => {
      const owner = profileByToken[request.headers.get("Authorization") ?? ""];
      return owner ? HttpResponse.json(owner) : new HttpResponse(null, { status: 401 });
    }),
  );
}

function ProfileName() {
  const { data } = useUserProfile();
  return <p>{data ? `Profile of ${data.firstName}` : "Loading profile"}</p>;
}

describe("terminateSession", () => {
  afterEach(() => {
    store.dispatch(resetAuth());
    queryClient.clear();
  });

  it("clears the stored tokens, the Redux auth state and the cached server data", () => {
    store.dispatch(loginSuccess({ user: alice, accessToken: "alice-token", refreshToken: "alice-refresh" }));
    queryClient.setQueryData(["userProfile"], alice);

    terminateSession();

    expect(token.access).toBeNull();
    expect(token.refresh).toBeNull();
    expect(store.getState().auth).toEqual({ user: null, accessToken: null, refreshToken: null });
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });

  it("does not show the previous account's cached data to the next user", async () => {
    serveProfileForToken();
    store.dispatch(loginSuccess({ user: alice, accessToken: "alice-token", refreshToken: "alice-refresh" }));
    const first = render(
      <QueryClientProvider client={queryClient}>
        <ProfileName />
      </QueryClientProvider>,
    );
    expect(await screen.findByText("Profile of Alice")).toBeInTheDocument();
    first.unmount();

    terminateSession();
    store.dispatch(loginSuccess({ user: bob, accessToken: "bob-token", refreshToken: "bob-refresh" }));
    render(
      <QueryClientProvider client={queryClient}>
        <ProfileName />
      </QueryClientProvider>,
    );

    expect(screen.queryByText("Profile of Alice")).not.toBeInTheDocument();
    expect(await screen.findByText("Profile of Bob")).toBeInTheDocument();
  });
});
