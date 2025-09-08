import { useNavigate } from "react-router-dom";
import { useAppDispatch } from "@/stores/store";
import { loginSuccess } from "@/stores/authSlice";
import { useMutationApi } from "@/hooks/useMutationApi";
import type { LoginValues } from "@/features/auth/loginForm.definition";
import { loginUser, LoginResponse } from "@/features/auth/login";

/**
 * Thin wrapper around useMutationApi that
 *   • stores tokens in Redux
 *   • redirects on success
 */
export const useLogin = () => {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();

   return useMutationApi<LoginResponse, LoginValues>(
    loginUser,
    {
      onSuccess: ({ user, accessToken, refreshToken, passwordChangeRequired }) => {
        dispatch(loginSuccess({ user, accessToken, refreshToken }));
        
        if (passwordChangeRequired) {
          // Redirect to password change page if password change is required
          navigate("/change-password", { replace: true });
        } else {
          // Normal login flow - redirect to dashboard
          navigate("/", { replace: true });
        }
      },
    },
  );
}

