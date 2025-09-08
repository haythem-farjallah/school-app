import { http } from "@/lib/http";

export interface ProfileSettingsDto {
  language: string;
  theme: string;
  notificationsEnabled: boolean;
  darkMode: boolean;
}

export interface UserProfileDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone?: string;
  birthday?: string;
  gender?: string;
  address?: string;
  role: string;
  image?: string;
}

export const DEFAULT_PROFILE_SETTINGS: ProfileSettingsDto = {
  language: "en",
  theme: "light",
  notificationsEnabled: true,
  darkMode: false,
};

export const EMPTY_USER_PROFILE: UserProfileDto = {
  id: 0,
  firstName: "",
  lastName: "",
  email: "",
  role: "USER",
  telephone: "",
  address: "",
};

/* API calls for profile settings */
export const getProfileSettings = async () => {
  try {
    const r = await http.get<ProfileSettingsDto>("/me/settings");
    return r.data ?? DEFAULT_PROFILE_SETTINGS;
  } catch (e) {
    return DEFAULT_PROFILE_SETTINGS;
  }
};

export const updateProfileSettings = (settings: Partial<ProfileSettingsDto>) =>
  http.patch<ProfileSettingsDto>("/me/settings", settings).then((r) => r.data);

export const getCurrentUserProfile = async () => {
  try {
    const r = await http.get<UserProfileDto>("/me/profile");
    return r.data ?? EMPTY_USER_PROFILE;
  } catch (e) {
    return EMPTY_USER_PROFILE;
  }
};

export const updateUserProfile = (profile: Partial<UserProfileDto>) =>
  http.patch<UserProfileDto>("/me/profile", profile).then((r) => r.data);
