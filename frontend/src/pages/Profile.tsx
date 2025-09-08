import { useState } from "react";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
} from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";

import {
  User,
  Settings,
  Lock,
  Bell,
  Palette,
  Globe,
  Shield,

  Mail,
  Phone,
  MapPin,
} from "lucide-react";

// Form definitions
import {
  profileSettingsSchema,
  profileSettingsFields,
  ProfileSettingsValues,
  personalInfoSchema,
  personalInfoFields,
  PersonalInfoValues,
} from "@/features/auth/profileSettingsForm.definition";
import {
  changePasswordSchema,
  changePasswordFields,
  ChangePasswordValues,
} from "@/features/auth/changePasswordForm.definition";

// Hooks
import { useProfileSettings, useUpdateProfileSettings, useUserProfile, useUpdateUserProfile } from "@/hooks/useProfileSettings";
import { useChangePassword } from "@/hooks/useChangePassword";
import { useAuth } from "@/hooks/useAuth";
import { getRoleClasses } from "@/lib/theme";

const SettingsPage = () => {
  const { user } = useAuth();
  const roleClasses = getRoleClasses(user?.role);
  const isStudent = user?.role === "STUDENT";
  const isTeacher = user?.role === "TEACHER";
  const [activeTab, setActiveTab] = useState("contact");

  // Data fetching
  const { data: profileSettings, isLoading: settingsLoading } = useProfileSettings();
  const { data: userProfile, isLoading: profileLoading } = useUserProfile();

  // Mutations
  const updateSettingsMut = useUpdateProfileSettings();
  const updateProfileMut = useUpdateUserProfile();
  const changePasswordMut = useChangePassword();

  // Create a filtered list of fields for teachers
  const teacherPersonalInfoFields = personalInfoFields.filter(field => 
    ['telephone', 'address'].includes(field.name)
  );

  // Form recipes
  const personalInfoRecipe: FormRecipe = {
    schema: personalInfoSchema,
    fields: isTeacher ? teacherPersonalInfoFields : personalInfoFields,
    onSubmit: (values) => {
      return updateProfileMut.mutateAsync(values as PersonalInfoValues);
    },
  };

  const settingsRecipe: FormRecipe = {
    schema: profileSettingsSchema,
    fields: profileSettingsFields,
    onSubmit: (values) => {
      return updateSettingsMut.mutateAsync(values as ProfileSettingsValues);
    },
  };

  const passwordRecipe: FormRecipe = {
    schema: changePasswordSchema,
    fields: changePasswordFields,
    onSubmit: (values) => {
      const { oldPassword, newPassword } = values as ChangePasswordValues;
      return changePasswordMut.mutateAsync({
        email: user?.email || "",
        oldPassword,
        newPassword,
      });
    },
  };

  if (profileLoading || settingsLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
      <div className={`container mx-auto py-8 px-4 max-w-4xl`}>
        {/* Header */}
        <div className="mb-8">
          <Card className="border-0 shadow-xl bg-white/80 backdrop-blur-sm">
            <CardContent className="p-8">
              <div className="flex items-center gap-6 mb-6">
                <div className="relative">
                  <Avatar className={`h-24 w-24 ring-4 ${roleClasses.primaryLight.replace('bg-','ring-')} shadow-lg`}>
                    <AvatarImage src={userProfile?.image} />
                    <AvatarFallback className={`text-xl ${roleClasses.primaryBg} text-white`}>
                      {user?.firstName?.[0]}{user?.lastName?.[0]}
                    </AvatarFallback>
                  </Avatar>
                </div>
                <div className="flex-1">
                  <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent mb-2">
                    {user?.firstName} {user?.lastName}
                  </h1>
                  <div className="flex items-center gap-2 mb-3">
                    <Mail className="h-4 w-4 text-slate-500" />
                    <p className="text-slate-600 font-medium">{user?.email}</p>
                  </div>
                  <Badge 
                    variant="secondary" 
                    className={`${roleClasses.badge} font-semibold px-3 py-1`}
                  >
                    {user?.role}
                  </Badge>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Tabs */}
        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-8">
          <div className="flex justify-center w-full">
            <TabsList className="relative flex bg-gradient-to-r from-white/90 via-slate-50/90 to-white/90 backdrop-blur-md shadow-lg border border-slate-200/50 p-1.5 rounded-2xl gap-1 w-fit">
              <TabsTrigger 
                value="contact" 
                className={`relative flex items-center gap-2 px-5 py-3 rounded-xl font-medium text-sm transition-all duration-200 ease-out
                  data-[state=active]:bg-gradient-to-r data-[state=active]:from-blue-500 data-[state=active]:to-indigo-600 data-[state=active]:text-white data-[state=active]:shadow-md data-[state=active]:shadow-blue-500/25
                  data-[state=active]:transform data-[state=active]:scale-[1.02]
                  data-[state=inactive]:text-slate-700 data-[state=inactive]:hover:text-slate-900 
                  data-[state=inactive]:hover:bg-gradient-to-r data-[state=inactive]:hover:from-white data-[state=inactive]:hover:to-slate-50
                  data-[state=inactive]:hover:shadow-sm data-[state=inactive]:hover:scale-[1.01]
                  whitespace-nowrap focus-visible:ring-0 focus-visible:ring-offset-0 z-10`}
              >
                <User className="h-4 w-4 transition-transform duration-200" />
                <span className="hidden sm:inline">Contact Info</span>
                <span className="sm:hidden">Contact</span>
              </TabsTrigger>
              <TabsTrigger 
                value="password" 
                className={`relative flex items-center gap-2 px-5 py-3 rounded-xl font-medium text-sm transition-all duration-200 ease-out
                  data-[state=active]:bg-gradient-to-r data-[state=active]:from-red-500 data-[state=active]:to-pink-600 data-[state=active]:text-white data-[state=active]:shadow-md data-[state=active]:shadow-red-500/25
                  data-[state=active]:transform data-[state=active]:scale-[1.02]
                  data-[state=inactive]:text-slate-700 data-[state=inactive]:hover:text-slate-900 
                  data-[state=inactive]:hover:bg-gradient-to-r data-[state=inactive]:hover:from-white data-[state=inactive]:hover:to-slate-50
                  data-[state=inactive]:hover:shadow-sm data-[state=inactive]:hover:scale-[1.01]
                  whitespace-nowrap focus-visible:ring-0 focus-visible:ring-offset-0 z-10`}
              >
                <Lock className="h-4 w-4 transition-transform duration-200" />
                <span>Password</span>
              </TabsTrigger>
              {!isStudent && (
                <>
                  <TabsTrigger 
                    value="settings" 
                    className={`relative flex items-center gap-2 px-5 py-3 rounded-xl font-medium text-sm transition-all duration-200 ease-out
                      data-[state=active]:bg-gradient-to-r data-[state=active]:from-green-500 data-[state=active]:to-emerald-600 data-[state=active]:text-white data-[state=active]:shadow-md data-[state=active]:shadow-green-500/25
                      data-[state=active]:transform data-[state=active]:scale-[1.02]
                      data-[state=inactive]:text-slate-700 data-[state=inactive]:hover:text-slate-900 
                      data-[state=inactive]:hover:bg-gradient-to-r data-[state=inactive]:hover:from-white data-[state=inactive]:hover:to-slate-50
                      data-[state=inactive]:hover:shadow-sm data-[state=inactive]:hover:scale-[1.01]
                      whitespace-nowrap focus-visible:ring-0 focus-visible:ring-offset-0 z-10`}
                  >
                    <Settings className="h-4 w-4 transition-transform duration-200" />
                    <span className="hidden sm:inline">Preferences</span>
                    <span className="sm:hidden">Settings</span>
                  </TabsTrigger>
                  <TabsTrigger 
                    value="notifications" 
                    className={`relative flex items-center gap-2 px-5 py-3 rounded-xl font-medium text-sm transition-all duration-200 ease-out
                      data-[state=active]:bg-gradient-to-r data-[state=active]:from-orange-500 data-[state=active]:to-yellow-600 data-[state=active]:text-white data-[state=active]:shadow-md data-[state=active]:shadow-orange-500/25
                      data-[state=active]:transform data-[state=active]:scale-[1.02]
                      data-[state=inactive]:text-slate-700 data-[state=inactive]:hover:text-slate-900 
                      data-[state=inactive]:hover:bg-gradient-to-r data-[state=inactive]:hover:from-white data-[state=inactive]:hover:to-slate-50
                      data-[state=inactive]:hover:shadow-sm data-[state=inactive]:hover:scale-[1.01]
                      whitespace-nowrap focus-visible:ring-0 focus-visible:ring-offset-0 z-10`}
                  >
                    <Bell className="h-4 w-4 transition-transform duration-200" />
                    <span className="hidden sm:inline">Notifications</span>
                    <span className="sm:hidden">Alerts</span>
                  </TabsTrigger>
                </>
              )}
            </TabsList>
          </div>

          {/* Contact Information Tab */}
          <TabsContent value="contact">
            <Card className="border-0 shadow-xl bg-white/80 backdrop-blur-sm">
              <CardHeader className={`${roleClasses.gradient} rounded-t-lg`}>
                <CardTitle className="flex items-center gap-3 text-xl">
                  <div className={`p-2 ${roleClasses.primaryBg} rounded-lg`}>
                    <User className="h-5 w-5 text-white" />
                  </div>
                  Contact Information
                </CardTitle>
              </CardHeader>
              <CardContent className="p-8">
                <div className="space-y-6">
                  {/* Basic Info Grid */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className={`p-4 ${roleClasses.gradient} rounded-xl border ${roleClasses.primaryBorder}/50`}>
                      <div className="flex items-center gap-2 mb-2">
                        <User className="h-4 w-4 text-slate-600" />
                        <p className="text-sm font-medium text-slate-600">Full Name</p>
                      </div>
                      <p className="text-lg font-semibold text-slate-900">{user?.firstName} {user?.lastName}</p>
                    </div>
                    <div className={`p-4 ${roleClasses.gradient} rounded-xl border ${roleClasses.primaryBorder}/50`}>
                      <div className="flex items-center gap-2 mb-2">
                        <Mail className="h-4 w-4 text-slate-600" />
                        <p className="text-sm font-medium text-slate-600">Email Address</p>
                      </div>
                      <p className="text-lg font-semibold text-slate-900">{user?.email}</p>
                    </div>
                  </div>

                  {/* Additional Contact Info (Read-only for all) */}
                  {userProfile && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      {userProfile.telephone && (
                        <div className={`p-4 ${roleClasses.gradient} rounded-xl border ${roleClasses.primaryBorder}/50`}>
                          <div className="flex items-center gap-2 mb-2">
                            <Phone className="h-4 w-4 text-slate-600" />
                            <p className="text-sm font-medium text-slate-600">Phone Number</p>
                          </div>
                          <p className="text-lg font-semibold text-slate-900">{userProfile.telephone}</p>
                        </div>
                      )}
                      {userProfile.address && (
                        <div className={`p-4 ${roleClasses.gradient} rounded-xl border ${roleClasses.primaryBorder}/50`}>
                          <div className="flex items-center gap-2 mb-2">
                            <MapPin className="h-4 w-4 text-slate-600" />
                            <p className="text-sm font-medium text-slate-600">Address</p>
                          </div>
                          <p className="text-lg font-semibold text-slate-900">{userProfile.address}</p>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Notice */}
                  <div className="p-4 bg-gradient-to-r from-amber-50 to-orange-50 border border-amber-200 rounded-xl">
                    <p className="text-sm text-amber-800 font-medium">
                      {isStudent 
                        ? "Your contact information is managed by the administration. Contact your administrator if you need to update these details."
                        : isTeacher 
                        ? "You can only update your phone and address. For other changes, please contact an administrator."
                        : "Your name and email cannot be changed. Contact your administrator if you need to update these details."
                      }
                    </p>
                  </div>

                  {/* Update Form - Only for non-students */}
                  {!isStudent && (
                    <div className="pt-4 border-t border-slate-200">
                      <AutoForm
                        recipe={personalInfoRecipe}
                        loading={updateProfileMut.isPending}
                        submitText="Update Contact Info"
                        defaultValues={userProfile}
                      />
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Password Tab */}
          <TabsContent value="password">
            <Card className="border-0 shadow-xl bg-white/80 backdrop-blur-sm">
              <CardHeader className="bg-gradient-to-r from-red-50 to-pink-50 rounded-t-lg">
                <CardTitle className="flex items-center gap-3 text-xl">
                  <div className="p-2 bg-gradient-to-r from-red-500 to-pink-600 rounded-lg">
                    <Lock className="h-5 w-5 text-white" />
                  </div>
                  Change Password
                </CardTitle>
              </CardHeader>
              <CardContent className="p-8">
                <div className="space-y-6">
                  {/* Security Notice */}
                  <div className={`p-4 ${roleClasses.gradient} border ${roleClasses.primaryBorder} rounded-xl`}>
                    <div className="flex items-center gap-2 mb-2">
                      <Shield className={`h-4 w-4 ${roleClasses.icon}`} />
                      <p className={`text-sm font-medium ${roleClasses.primary}`}>{`Security Guidelines`}</p>
                    </div>
                    <ul className={`text-sm ${roleClasses.primary} space-y-1 ml-6`}>
                      <li>• Use at least 8 characters</li>
                      <li>• Include uppercase and lowercase letters</li>
                      <li>• Add numbers and special characters</li>
                      <li>• Avoid common words or personal information</li>
                    </ul>
                  </div>

                  {/* Password Form */}
                  <div className="pt-4">
                    <AutoForm
                      recipe={passwordRecipe}
                      loading={changePasswordMut.isPending}
                      submitText="Change Password"
                    />
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Settings/Preferences Tab - Only for non-students */}
          {!isStudent && (
            <TabsContent value="settings">
              <div className="grid gap-6">
                <Card className="border-0 shadow-xl bg-white/80 backdrop-blur-sm">
                  <CardHeader className="bg-gradient-to-r from-green-50 to-emerald-50 rounded-t-lg">
                    <CardTitle className="flex items-center gap-3 text-xl">
                      <div className="p-2 bg-gradient-to-r from-green-500 to-emerald-600 rounded-lg">
                        <Palette className="h-5 w-5 text-white" />
                      </div>
                      Appearance & Language
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="p-8">
                    <AutoForm
                      recipe={settingsRecipe}
                      loading={updateSettingsMut.isPending}
                      submitText="Save Preferences"
                      defaultValues={profileSettings}
                    />
                  </CardContent>
                </Card>

                {/* Additional Settings Cards */}
                <Card className="border-0 shadow-xl bg-white/80 backdrop-blur-sm">
                  <CardHeader className="bg-gradient-to-r from-purple-50 to-violet-50 rounded-t-lg">
                    <CardTitle className="flex items-center gap-3 text-xl">
                      <div className="p-2 bg-gradient-to-r from-purple-500 to-violet-600 rounded-lg">
                        <Globe className="h-5 w-5 text-white" />
                      </div>
                      Regional Settings
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="p-8 space-y-6">
                    <div className="flex items-center justify-between p-4 bg-gradient-to-br from-slate-50 to-purple-50 rounded-xl border border-slate-200/50">
                      <div>
                        <p className="font-medium text-slate-900">Time Zone</p>
                        <p className="text-sm text-slate-600">Automatically detected</p>
                      </div>
                      <Badge variant="outline" className="bg-white">UTC+1 (Paris)</Badge>
                    </div>
                    <div className="flex items-center justify-between p-4 bg-gradient-to-br from-slate-50 to-purple-50 rounded-xl border border-slate-200/50">
                      <div>
                        <p className="font-medium text-slate-900">Date Format</p>
                        <p className="text-sm text-slate-600">How dates are displayed</p>
                      </div>
                      <Badge variant="outline" className="bg-white">DD/MM/YYYY</Badge>
                    </div>
                  </CardContent>
                </Card>
              </div>
            </TabsContent>
          )}

          {/* Notifications Tab - Only for non-students */}
          {!isStudent && (
            <TabsContent value="notifications">
              <Card className="border-0 shadow-xl bg-white/80 backdrop-blur-sm">
                <CardHeader className="bg-gradient-to-r from-orange-50 to-yellow-50 rounded-t-lg">
                  <CardTitle className="flex items-center gap-3 text-xl">
                    <div className="p-2 bg-gradient-to-r from-orange-500 to-yellow-600 rounded-lg">
                      <Bell className="h-5 w-5 text-white" />
                    </div>
                    Notification Preferences
                  </CardTitle>
                </CardHeader>
                <CardContent className="p-8 space-y-6">
                  <div className="flex items-center justify-between p-4 bg-gradient-to-br from-slate-50 to-orange-50 rounded-xl border border-slate-200/50">
                    <div>
                      <p className="font-medium text-slate-900">Email Notifications</p>
                      <p className="text-sm text-slate-600">Receive notifications via email</p>
                    </div>
                    <Badge variant={profileSettings?.notificationsEnabled ? "default" : "secondary"}>
                      {profileSettings?.notificationsEnabled ? "Enabled" : "Disabled"}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between p-4 bg-gradient-to-br from-slate-50 to-orange-50 rounded-xl border border-slate-200/50">
                    <div>
                      <p className="font-medium text-slate-900">Grade Notifications</p>
                      <p className="text-sm text-slate-600">Get notified when grades are posted</p>
                    </div>
                    <Button variant="outline" size="sm" className="bg-white hover:bg-slate-50">
                      Configure
                    </Button>
                  </div>
                  <div className="flex items-center justify-between p-4 bg-gradient-to-br from-slate-50 to-orange-50 rounded-xl border border-slate-200/50">
                    <div>
                      <p className="font-medium text-slate-900">Assignment Reminders</p>
                      <p className="text-sm text-slate-600">Reminders for upcoming assignments</p>
                    </div>
                    <Button variant="outline" size="sm" className="bg-white hover:bg-slate-50">
                      Configure
                    </Button>
                  </div>
                  <div className="flex items-center justify-between p-4 bg-gradient-to-br from-slate-50 to-orange-50 rounded-xl border border-slate-200/50">
                    <div>
                      <p className="font-medium text-slate-900">System Announcements</p>
                      <p className="text-sm text-slate-600">Important system updates</p>
                    </div>
                    <Button variant="outline" size="sm" className="bg-white hover:bg-slate-50">
                      Configure
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          )}
        </Tabs>
      </div>
    </div>
  );
};

export default SettingsPage;
