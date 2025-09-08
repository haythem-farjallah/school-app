import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from "@/components/ui/card";
import {
  changePasswordSchema,
  changePasswordFields,
  ChangePasswordValues,
} from "@/features/auth/changePasswordForm.definition";
import { useChangePassword } from "@/hooks/useChangePassword";
import { useAppSelector } from "@/stores/store";
import { Navigate } from "react-router-dom";

export default function ChangePasswordPage() {
  const user = useAppSelector((state) => state.auth.user);
  const changePasswordMut = useChangePassword();

  // Redirect if user is not logged in
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  const recipe: FormRecipe = {
    schema: changePasswordSchema,
    fields: changePasswordFields,
    onSubmit: (values) => {
      const { oldPassword, newPassword } = values as ChangePasswordValues;
      return changePasswordMut.mutateAsync({
        email: user.email,
        oldPassword,
        newPassword,
      });
    },
  };

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-gradient-to-br from-slate-50 via-background to-slate-100 dark:from-slate-950 dark:via-background dark:to-slate-900 px-4 py-12">
      <Card className="w-full max-w-md border border-border/40 bg-card/95 shadow-2xl backdrop-blur-xl">
        <CardHeader className="text-center space-y-3 pb-8">
          <CardTitle className="text-2xl font-bold bg-gradient-to-r from-primary to-primary/80 bg-clip-text text-transparent">
            Change Password
          </CardTitle>
          <CardDescription className="text-muted-foreground">
            Please change your temporary password to continue
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <AutoForm
            recipe={recipe}
            loading={changePasswordMut.isPending}
            submitText="Change Password"
          />
        </CardContent>
      </Card>
    </div>
  );
}
