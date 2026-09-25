import { api } from "@/lib/api-client";

/** GET/PATCH /api/me/settings (ProfileSettingsDto) */
export interface ProfileSettings {
  language: string;
  theme: string;
  notificationsEnabled: boolean;
  darkMode: boolean;
}

/** GET/PATCH /api/me/profile response (UserProfileDto) */
export interface UserProfile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  telephone: string | null;
  address: string | null;
  profileTheme: string | null;
  profileLanguage: string | null;
  permissions: string[];
}

/** PATCH /api/me/profile request (UserProfileUpdateRequest) */
export interface UserProfileUpdate {
  telephone?: string;
  address?: string;
}

export async function getProfileSettings(): Promise<ProfileSettings> {
  const response = await api.get<ProfileSettings>("/me/settings");
  return response.data;
}

export async function updateProfileSettings(settings: Partial<ProfileSettings>): Promise<ProfileSettings> {
  const response = await api.patch<ProfileSettings>("/me/settings", settings);
  return response.data;
}

export async function getCurrentUserProfile(): Promise<UserProfile> {
  const response = await api.get<UserProfile>("/me/profile");
  return response.data;
}

export async function updateUserProfile(profile: UserProfileUpdate): Promise<UserProfile> {
  const response = await api.patch<UserProfile>("/me/profile", profile);
  return response.data;
}
