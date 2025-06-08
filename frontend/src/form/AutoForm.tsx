import { FormProvider, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { motion } from "framer-motion";
import type { FormRecipe } from "./types";
import { Button } from "@/components/ui/button";
import { useTranslation } from "react-i18next";
import { renderField } from "./renderField";
import type { z } from "zod";
import { cx } from "class-variance-authority";
import { Loader2 } from "lucide-react";

type RollbackContext = {
  rollback?: () => void;
};

interface Props<R extends FormRecipe> {
  recipe: R;
  defaultValues?: z.infer<R["schema"]>;
  submitLabel?: string;
  animate?: boolean;
  submitClassName?: string;
}

export const AutoForm = <R extends FormRecipe>({
  recipe,
  defaultValues,
  submitLabel = "form.submit",
  submitClassName,
  animate = true,
}: Props<R>) => {
  type FormValues = z.infer<R["schema"]>;
  const { t } = useTranslation();
  const qc = useQueryClient();

  const methods = useForm<FormValues>({
    resolver: zodResolver(recipe.schema),
    defaultValues,
  });

  const mutation = useMutation<unknown, unknown, unknown, RollbackContext>({
    mutationFn: recipe.onSubmit,
    onMutate: async () => {
      return {};
    },
    onError: (_e, _v, ctx) => ctx?.rollback?.(),
    onSuccess: () => methods.reset(),
    onSettled: () => qc.invalidateQueries(),
  });

  const FormWrapper = animate ? motion.form : "form";

  return (
    <FormProvider {...methods}>
      <FormWrapper
        onSubmit={methods.handleSubmit((v) => mutation.mutateAsync(v))}
        {...(animate && {
          initial: { opacity: 0, y: 12 },
          animate: { opacity: 1, y: 0 },
          exit: { opacity: 0, y: -12 },
          transition: { duration: 0.25 },
        })}
        className="space-y-6"
      >
        <div className="space-y-4">
          {recipe.fields.map(renderField)}
        </div>

        <Button
          type="submit"
          disabled={mutation.isPending}
          className={cx(
            "w-full h-11 font-semibold transition-all duration-200",
            "bg-primary hover:bg-primary/90 text-white",
            "disabled:opacity-50 disabled:cursor-not-allowed",
            "focus:ring-2 focus:ring-primary focus:ring-offset-2",
            submitClassName
          )}
        >
          {mutation.isPending ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Signing in...
            </>
          ) : (
            t(submitLabel)
          )}
        </Button>
      </FormWrapper>
    </FormProvider>
  );
};
