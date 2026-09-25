import { api } from "@/lib/api-client";
import type { LoginValues } from "@/features/auth/loginForm.definition";

/* server payload shape */
export interface LoginResponse {
  user: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
    permissions?: string[];
  };
  accessToken: string;
  refreshToken: string;
  passwordChangeRequired: boolean;
}

/** POST /api/auth/login response body (ApiSuccessResponse<LoginResponse>) */
interface LoginEnvelope {
  status: string;
  data: LoginResponse;
}

export async function loginUser(values: LoginValues): Promise<LoginResponse> {
  const response = await api.post<LoginEnvelope>("/auth/login", values);
  return response.data.data;
}
