import { api } from "@/lib/api-client";

export interface ChangePasswordRequest {
  email: string;
  oldPassword: string;
  newPassword: string;
}

/* API call for changing password */
export const changePassword = (values: ChangePasswordRequest) =>
  api.post<void>("/auth/change-password", values).then(() => undefined);
