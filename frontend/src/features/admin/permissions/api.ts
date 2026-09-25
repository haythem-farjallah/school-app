import { api } from "@/lib/api-client";

export type Role = "ADMIN" | "TEACHER" | "STUDENT" | "PARENT" | "STAFF";

interface PermissionResponse<T> {
  status: string;
  data: T;
}

export const listAllPermissions = () =>
  api.get<PermissionResponse<string[]>>("/admin/permissions").then((response) => response.data.data);

export const getRoleDefaults = (role: Role) =>
  api
    .get<PermissionResponse<string[]>>(`/admin/permissions/roles/${role}`)
    .then((response) => response.data.data);

export const updateRoleDefaults = (role: Role, codes: string[]) =>
  api
    .put<PermissionResponse<null>>(`/admin/permissions/roles/${role}`, { codes })
    .then((response) => response.data);

export const getUserPermissions = (userId: number) =>
  api
    .get<PermissionResponse<string[]>>(`/admin/permissions/users/${userId}`)
    .then((response) => response.data.data);

export const updateUserPermissions = (userId: number, codes: string[]) =>
  api
    .put<PermissionResponse<null>>(`/admin/permissions/users/${userId}`, { codes })
    .then((response) => response.data);

