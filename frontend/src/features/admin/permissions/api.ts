import { http } from "@/lib/http";

export type Role = "ADMIN" | "TEACHER" | "STUDENT" | "PARENT" | "STAFF";

export const listAllPermissions = () =>
  http.get<{ status: string; data: string[] }>("/admin/permissions").then((res) => res.data);

export const getRoleDefaults = (role: Role) =>
  http.get<{ status: string; data: string[] }>(`/admin/permissions/roles/${role}`).then((res) => res.data);

export const updateRoleDefaults = (role: Role, codes: string[]) =>
  http.put<{ status: string; data: null }>(`/admin/permissions/roles/${role}`, { codes }).then((res) => res);

export const getUserPermissions = (userId: number) =>
  http.get<{ status: string; data: string[] }>(`/admin/permissions/users/${userId}`).then((res) => res.data);

export const updateUserPermissions = (userId: number, codes: string[]) =>
  http.put<{ status: string; data: null }>(`/admin/permissions/users/${userId}`, { codes }).then((res) => res);


