import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { 
  getProfileSettings, 
  updateProfileSettings, 
  getCurrentUserProfile, 
  updateUserProfile,
  ProfileSettingsDto,
  UserProfileDto 
} from "@/features/auth/profileSettings";
import toast from "react-hot-toast";

// Hook for profile settings
export const useProfileSettings = () => {
  return useQuery({
    queryKey: ["profileSettings"],
    queryFn: getProfileSettings,
  });
};

export const useUpdateProfileSettings = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: updateProfileSettings,
    onSuccess: (data) => {
      queryClient.setQueryData(["profileSettings"], data);
      toast.success("Settings updated successfully!");
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Failed to update settings");
    },
  });
};

// Hook for user profile
export const useUserProfile = () => {
  return useQuery({
    queryKey: ["userProfile"],
    queryFn: getCurrentUserProfile,
  });
};

export const useUpdateUserProfile = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: updateUserProfile,
    onSuccess: (data) => {
      queryClient.setQueryData(["userProfile"], data);
      // Also update the auth store if needed
      toast.success("Profile updated successfully!");
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Failed to update profile");
    },
  });
};
