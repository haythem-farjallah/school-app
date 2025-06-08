import { http } from "@/lib/http";
import type { LoginValues } from "@/features/auth/loginForm.definition";

/* server payload shape */
export interface LoginResponse {
  user: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
  };
  accessToken:  string;
  refreshToken: string;
}

/* plain axios call – no hooks here */
export const loginUser = (values: LoginValues) =>
  http.post<LoginResponse>("/auth/login", values).then((r) => r.data);
