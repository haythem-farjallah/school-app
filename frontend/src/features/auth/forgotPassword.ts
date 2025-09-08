import { http } from "@/lib/http";

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
  http.post<void>("/auth/forgot-password", values, { skipAuth: true });

/**
 * Step 2: Reset password with OTP and new password
 */
export const resetPassword = (values: ResetPasswordRequest) =>
  http.post<void>("/auth/reset-password", values, { skipAuth: true });
