import { api } from "@/lib/api-client";

/* ─── Request/Response Types ─────────────────────────────────────────── */

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  otp: string;
  newPassword: string;
}

/* ─── API Functions ─────────────────────────────────────────────────── */

/**
 * Step 1: Send forgot password request (generates and emails OTP)
 */
export const forgotPassword = (values: ForgotPasswordRequest) =>
  api.post<void>("/auth/forgot-password", values).then((response) => response.data);

/**
 * Step 2: Reset password with OTP and new password
 */
export const resetPassword = (values: ResetPasswordRequest) =>
  api.post<void>("/auth/reset-password", values).then((response) => response.data);
