import { useUserRole } from "@/hooks/useUserRole";

/** Admin and staff share the students pages; each stays under its own route base. */
export function useStudentsPath(): string {
  return useUserRole() === "STAFF" ? "/staff/students" : "/admin/students";
}
