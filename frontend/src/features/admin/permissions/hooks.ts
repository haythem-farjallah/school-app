import { useQueryApi } from "@/hooks/useQueryApi";
import { useMutationApi } from "@/hooks/useMutationApi";
import { getRoleDefaults, getUserPermissions, listAllPermissions, updateRoleDefaults, updateUserPermissions, Role } from "./api";

export const usePermissionCatalogue = () =>
  useQueryApi(["permissions", "catalogue"], listAllPermissions, { staleTime: 60_000 });

export const useRoleDefaults = (role: Role) =>
  useQueryApi(["permissions", "role", role], () => getRoleDefaults(role), { enabled: !!role });

export const useUpdateRoleDefaults = () =>
  useMutationApi((vars: { role: Role; codes: string[] }) => updateRoleDefaults(vars.role, vars.codes));

export const useUserPerms = (userId?: number) =>
  useQueryApi(["permissions", "user", userId], () => getUserPermissions(userId as number), { enabled: !!userId });

export const useUpdateUserPerms = () =>
  useMutationApi((vars: { userId: number; codes: string[] }) => updateUserPermissions(vars.userId, vars.codes));


