import { useNavigate } from "react-router-dom";
import { useAppDispatch } from "@/stores/store";
import { loginSuccess } from "@/stores/authSlice";
import { useMutationApi } from "@/hooks/useMutationApi";
import { getApiErrorMessage } from "@/lib/api-error";
import { notifyError } from "@/lib/notify";
import type { LoginValues } from "@/features/auth/loginForm.definition";
import { loginUser, LoginResponse } from "@/features/auth/login";

/**
 * Thin wrapper around useMutationApi that
 *   • stores tokens in Redux
 *   • redirects on success
 *   • shows the reason when the login is refused
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
      onError: (error) => notifyError(getApiErrorMessage(error)),
    },
  );
}

