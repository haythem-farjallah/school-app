import { useNavigate } from "react-router-dom";
import { useMutationApi } from "@/hooks/useMutationApi";
import { changePassword, ChangePasswordRequest } from "@/features/auth/changePassword";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/lib/api-error";

/**
 * Hook for changing password after first login
 */
export const useChangePassword = () => {
  const navigate = useNavigate();

  return useMutationApi<void, ChangePasswordRequest>(
    changePassword,
    {
      onSuccess: () => {
        toast.success("Your password has been successfully changed. You can now access the system.");
        // Redirect to dashboard after successful password change
        navigate("/", { replace: true });
      },
      onError: (error) => {
        toast.error(getApiErrorMessage(error, "Failed to change password. Please try again."));
      },
    }
  );
};
