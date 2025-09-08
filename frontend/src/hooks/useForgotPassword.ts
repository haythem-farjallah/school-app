import { useNavigate } from "react-router-dom";
import { useMutationApi } from "@/hooks/useMutationApi";
import { forgotPassword, resetPassword } from "@/features/auth/forgotPassword";
import { notifySuccess } from "@/lib/notify";
import type { 
  ForgotPasswordRequest, 
  ResetPasswordRequest 
} from "@/features/auth/forgotPassword";

/**
 * Hook for sending forgot password request (Step 1)
 * Sends OTP to user's email
 */
export const useForgotPassword = () => {
  const navigate = useNavigate();

  return useMutationApi<void, ForgotPasswordRequest>(
    forgotPassword,
    {
      onSuccess: (_, variables) => {
        // Show success notification
        notifySuccess("Verification code sent to your email address. Please check your inbox.");
        
        // Navigate to reset password page with email pre-filled
        navigate("/reset-password", { 
          state: { email: variables.email },
          replace: true 
        });
      },
    }
  );
};

/**
 * Hook for resetting password with OTP (Step 2)
 * Validates OTP and sets new password
 */
export const useResetPassword = () => {
  const navigate = useNavigate();

  return useMutationApi<void, ResetPasswordRequest>(
    resetPassword,
    {
      onSuccess: () => {
        // Show success notification
        notifySuccess("Password reset successfully! You can now login with your new password.");
        
        // Navigate back to login page after successful password reset
        navigate("/login", { 
          state: { 
            message: "Password reset successfully! Please login with your new password." 
          },
          replace: true 
        });
      },
    }
  );
};
