// src/components/forms/InputField.tsx
import { useFormContext } from "react-hook-form";
import { Input } from "@/components/ui/input";
import { useTranslation } from "react-i18next";
import type { BaseField } from "./types";
import { cn } from "@/lib/utils";
import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";

type TextKinds = "text" | "password" | "number";

interface Props<K extends TextKinds = TextKinds> {
  field: BaseField<K>;
  inputType?: K;
}

export function InputField<K extends TextKinds = "text">({
  field,
  inputType,
}: Props<K>) {
  const {
    register,
    formState: { errors },
  } = useFormContext();
  const { t } = useTranslation();
  const [showPassword, setShowPassword] = useState(false);

  const err = errors[field.name]?.message as string | undefined;
  const Icon = field.icon;
  const isPasswordField = (inputType ?? field.type) === "password";
  const actualInputType = isPasswordField && showPassword ? "text" : (inputType ?? field.type);

  return (
    <div className="space-y-2">
      <label
        htmlFor={field.name}
        className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
      >
        {t(field.label)}
      </label>

      <div className="relative group">
        {Icon && (
          <Icon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground transition-colors group-focus-within:text-primary" />
        )}
        
        <Input
          id={field.name}
          type={actualInputType}
          placeholder={field.placeholder ? t(field.placeholder) : undefined}
          {...register(field.name)}
          {...field.props}
          className={cn(
            "h-11 transition-all duration-200 border border-border/60 bg-background/80 backdrop-blur-sm",
            "focus:border-primary/70 focus:bg-background focus:shadow-sm",
            "placeholder:text-muted-foreground placeholder:opacity-50",
            "hover:border-border/80",
            Icon && "pl-9",
            isPasswordField && "pr-10",
            err && "border-destructive/60 focus:border-destructive"
          )}
        />

        {isPasswordField && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="absolute right-2 top-1/2 -translate-y-1/2 h-7 w-7 p-0 hover:bg-transparent"
            onClick={() => setShowPassword(!showPassword)}
          >
            {showPassword ? (
              <EyeOff className="h-4 w-4 text-muted-foreground hover:text-foreground transition-colors" />
            ) : (
              <Eye className="h-4 w-4 text-muted-foreground hover:text-foreground transition-colors" />
            )}
          </Button>
        )}
      </div>

      {err && (
        <p className="flex items-center gap-1 text-sm text-destructive">
          <span className="h-1 w-1 rounded-full bg-destructive"></span>
          {t(err)}
        </p>
      )}
    </div>
  );
}
