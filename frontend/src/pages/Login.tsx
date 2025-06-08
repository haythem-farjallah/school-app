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
import {
  loginSchema,
  loginFields,
  LoginValues,
} from "@/features/auth/loginForm.definition";

import { useLogin } from "@/hooks/useLogin";

export default function LoginPage() {
  const loginMut = useLogin(); // ← React-Query mutation

  /* ---------- Build recipe ---------- */
  const recipe: FormRecipe = {
    schema: loginSchema,
    fields: loginFields,
    onSubmit: (v) => loginMut.mutateAsync(v as LoginValues),
  };

  return (
    <div className="flex min-h-screen w-full items-center justify-center bg-gradient-to-br from-slate-50 via-background to-slate-100 dark:from-slate-950 dark:via-background dark:to-slate-900 px-4 py-12">
      <Card className="w-full max-w-md border border-border/40 bg-card/95 shadow-2xl backdrop-blur-xl">
        <CardHeader className="text-center space-y-3 pb-8">
          <CardTitle className="text-3xl font-bold tracking-tight bg-gradient-to-r from-primary to-primary/80 bg-clip-text text-transparent">
            Welcome Back
          </CardTitle>
          <CardDescription className="text-base text-muted-foreground">
            Sign in to your account to continue
          </CardDescription>
        </CardHeader>

        <CardContent className="px-8 pb-8">
          <AutoForm
            recipe={recipe}
            submitLabel="Sign In"
            submitClassName="bg-primary hover:bg-primary/90 text-white shadow-lg hover:shadow-xl"
          />
        </CardContent>

        <CardFooter className="flex flex-col gap-4 px-8 pb-8">
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <span className="w-full border-t border-border/40" />
            </div>
          </div>
          <p className="text-sm text-muted-foreground text-center">
            Forgot your password?{" "}
            <a
              href="/forgot-password"
              className="font-medium text-primary underline-offset-4 hover:underline transition-colors"
            >
              Reset it here
            </a>
          </p>
        </CardFooter>
      </Card>
    </div>
  );
}
