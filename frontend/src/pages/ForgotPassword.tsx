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
import { ArrowLeft, Mail } from "lucide-react";
import { Link } from "react-router-dom";
import {
  forgotPasswordSchema,
  forgotPasswordFields,
  ForgotPasswordValues,
} from "@/features/auth/forgotPasswordForm.definition";
import { useForgotPassword } from "@/hooks/useForgotPassword";

export default function ForgotPasswordPage() {
  const forgotPasswordMut = useForgotPassword();

  /* ---------- Build recipe ---------- */
  const recipe: FormRecipe = {
    schema: forgotPasswordSchema,
    fields: forgotPasswordFields,
    onSubmit: (v) => forgotPasswordMut.mutateAsync(v as ForgotPasswordValues),
  };

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-gradient-to-br from-slate-50 via-background to-slate-100 dark:from-slate-950 dark:via-background dark:to-slate-900 px-4 py-12">
      <Card className="w-full max-w-md border border-border/40 bg-card/95 shadow-2xl backdrop-blur-xl">
        <CardHeader className="text-center space-y-3 pb-8">
          <div className="mx-auto w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            <Mail className="w-6 h-6 text-primary" />
          </div>
          <CardTitle className="text-2xl font-bold tracking-tight">
            Forgot Password?
          </CardTitle>
          <CardDescription className="text-base text-muted-foreground">
            Enter your email address and we'll send you a verification code to reset your password
          </CardDescription>
        </CardHeader>

        <CardContent className="px-8 pb-8">
          <AutoForm
            recipe={recipe}
            submitLabel={forgotPasswordMut.isPending ? "Sending..." : "Send Verification Code"}
            submitClassName="w-full bg-primary hover:bg-primary/90 text-white shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed"
            isLoading={forgotPasswordMut.isPending}
            disabled={forgotPasswordMut.isPending}
          />
          
          {forgotPasswordMut.isError && (
            <div className="mt-4 p-3 bg-destructive/10 border border-destructive/20 rounded-md">
              <p className="text-sm text-destructive">
                {forgotPasswordMut.error?.response?.data?.message || 
                 forgotPasswordMut.error?.message || 
                 "Failed to send verification code. Please try again."}
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
          
          <Button
            variant="ghost"
            className="w-full text-muted-foreground hover:text-foreground"
            asChild
          >
            <Link to="/login" className="flex items-center gap-2">
              <ArrowLeft className="w-4 h-4" />
              Back to Login
            </Link>
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
