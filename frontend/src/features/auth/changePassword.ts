import { http } from "@/lib/http";

export interface ChangePasswordRequest {
  email: string;
  oldPassword: string;
  newPassword: string;
}

/* API call for changing password */
export const changePassword = (values: ChangePasswordRequest) =>
  http.post<void>("/auth/change-password", values).then((r) => r.data);
