import { useNavigate } from "react-router-dom";
import { useMutationApi } from "@/hooks/useMutationApi";
import { changePassword, ChangePasswordRequest } from "@/features/auth/changePassword";
import toast from "react-hot-toast";
import { getApiErrorMessage } from "@/lib/api-error";
import { terminateSession } from "@/lib/session";

/**
 * Hook for changing the password, on first login or from the profile. A successful change
 * revokes every token of the account, so the session ends and the user signs in again.
 */
export const useChangePassword = () => {
  const navigate = useNavigate();

  return useMutationApi<void, ChangePasswordRequest>(
    changePassword,
    {
      onSuccess: () => {
        toast.success("Password changed successfully. Please sign in again.");
        terminateSession();
        navigate("/login", { replace: true });
      },
      onError: (error) => {
        toast.error(getApiErrorMessage(error, "Failed to change password. Please try again."));
      },
    }
  );
};
