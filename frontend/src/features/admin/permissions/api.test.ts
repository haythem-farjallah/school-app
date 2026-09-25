import { afterEach, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { api as apiClient } from "@/lib/api-client";
import { apiUrl, server } from "@/test/server";
import {
  getRoleDefaults,
  getUserPermissions,
  listAllPermissions,
  updateRoleDefaults,
  updateUserPermissions,
} from "./api";

describe("permissions api", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("returns the permission catalogue from the backend envelope", async () => {
    const get = vi.spyOn(apiClient, "get");
    server.use(
      http.get(apiUrl("/admin/permissions"), () =>
        HttpResponse.json({ status: "success", data: ["STUDENT_READ", "GRADE_READ"] }),
      ),
    );

    await expect(listAllPermissions()).resolves.toEqual(["STUDENT_READ", "GRADE_READ"]);
    expect(get).toHaveBeenCalledOnce();
  });

  it("returns role defaults from the backend envelope", async () => {
    const get = vi.spyOn(apiClient, "get");
    server.use(
      http.get(apiUrl("/admin/permissions/roles/TEACHER"), () =>
        HttpResponse.json({ status: "success", data: ["GRADE_READ"] }),
      ),
    );

    await expect(getRoleDefaults("TEACHER")).resolves.toEqual(["GRADE_READ"]);
    expect(get).toHaveBeenCalledOnce();
  });

  it("returns the update envelope after replacing role defaults", async () => {
    const put = vi.spyOn(apiClient, "put");
    let receivedBody: unknown;
    server.use(
      http.put(apiUrl("/admin/permissions/roles/TEACHER"), async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ status: "success", data: null });
      }),
    );

    await expect(updateRoleDefaults("TEACHER", ["GRADE_READ"])).resolves.toEqual({
      status: "success",
      data: null,
    });
    expect(receivedBody).toEqual({ codes: ["GRADE_READ"] });
    expect(put).toHaveBeenCalledOnce();
  });

  it("returns user permissions from the backend envelope", async () => {
    const get = vi.spyOn(apiClient, "get");
    server.use(
      http.get(apiUrl("/admin/permissions/users/42"), () =>
        HttpResponse.json({ status: "success", data: ["STUDENT_UPDATE"] }),
      ),
    );

    await expect(getUserPermissions(42)).resolves.toEqual(["STUDENT_UPDATE"]);
    expect(get).toHaveBeenCalledOnce();
  });

  it("returns the update envelope after replacing user permissions", async () => {
    const put = vi.spyOn(apiClient, "put");
    let receivedBody: unknown;
    server.use(
      http.put(apiUrl("/admin/permissions/users/42"), async ({ request }) => {
        receivedBody = await request.json();
        return HttpResponse.json({ status: "success", data: null });
      }),
    );

    await expect(updateUserPermissions(42, ["STUDENT_UPDATE"])).resolves.toEqual({
      status: "success",
      data: null,
    });
    expect(receivedBody).toEqual({ codes: ["STUDENT_UPDATE"] });
    expect(put).toHaveBeenCalledOnce();
  });
});
