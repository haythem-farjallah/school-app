import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { AutoForm } from "@/form/AutoForm";
import type { FormRecipe } from "@/form/types";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Shield } from "lucide-react";
import { Link } from "react-router-dom";
import {
  resetPasswordSchema,
  resetPasswordFields,
  ResetPasswordValues,
} from "@/features/auth/forgotPasswordForm.definition";
import { useResetPassword } from "@/hooks/useForgotPassword";

export default function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const resetPasswordMut = useResetPassword();
  
  // Get email from navigation state
  const email = location.state?.email;

  // Redirect to forgot password if no email is provided
  useEffect(() => {
    if (!email) {
      navigate("/forgot-password", { replace: true });
    }
  }, [email, navigate]);

  if (!email) {
    return null; // Will redirect
  }

  /* ---------- Build recipe with pre-filled email ---------- */
  const recipe: FormRecipe = {
    schema: resetPasswordSchema,
    fields: resetPasswordFields,
    defaultValues: { email },
    onSubmit: (v) => {
      const values = v as ResetPasswordValues;
      // Remove confirmPassword from the request
      const { confirmPassword, ...resetData } = values;
      return resetPasswordMut.mutateAsync(resetData);
    },
  };

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-gradient-to-br from-slate-50 via-background to-slate-100 dark:from-slate-950 dark:via-background dark:to-slate-900 px-4 py-12">
      <Card className="w-full max-w-md border border-border/40 bg-card/95 shadow-2xl backdrop-blur-xl">
        <CardHeader className="text-center space-y-3 pb-8">
          <div className="mx-auto w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            <Shield className="w-6 h-6 text-primary" />
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight">
            Reset Password
          </CardTitle>
          <CardDescription className="text-base text-muted-foreground">
            Enter the verification code sent to your email and create a new password
          </CardDescription>
        </CardHeader>

        <CardContent className="px-8 pb-8">
          <AutoForm
            recipe={recipe}
            submitLabel={resetPasswordMut.isPending ? "Resetting..." : "Reset Password"}
            submitClassName="w-full bg-primary hover:bg-primary/90 text-white shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
            isLoading={resetPasswordMut.isPending}
            disabled={resetPasswordMut.isPending}
          />
          
          {resetPasswordMut.isError && (
            <div className="mt-4 p-3 bg-destructive/10 border border-destructive/20 rounded-md">
              <p className="text-sm text-destructive">
                {resetPasswordMut.error?.response?.data?.message || 
                 resetPasswordMut.error?.message || 
                 "Failed to reset password. Please check your verification code and try again."}
              </p>
            </div>
          )}
        </CardContent>

        <CardFooter className="flex flex-col gap-4 px-8 pb-8">
          <div className="relative w-full">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t border-border/40" />
            </div>
          </div>
          
          <div className="flex flex-col gap-2 w-full">
            <Button
              variant="ghost"
              className="w-full text-muted-foreground hover:text-foreground"
              asChild
            >
              <Link to="/forgot-password" className="flex items-center gap-2">
                <ArrowLeft className="w-4 h-4" />
                Back to Email Entry
              </Link>
            </Button>
            
            <p className="text-xs text-muted-foreground text-center">
              Didn't receive the code?{" "}
              <Link 
                to="/forgot-password" 
                className="font-medium text-primary underline-offset-4 hover:underline"
              >
                Send again
              </Link>
            </p>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}
