import type { ReactNode } from 'react';
import { Loader2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/lib/utils';

interface LoadingStateProps {
  title?: ReactNode;
  description?: ReactNode;
  className?: string;
}

/** Generic loading message. Prefer a skeleton where the page layout should be preserved. */
export function LoadingState({ title, description, className }: LoadingStateProps) {
  const { t } = useTranslation();

  return (
    <div
      role="status"
      aria-live="polite"
      className={cn('flex flex-col items-center justify-center gap-3 px-4 py-12 text-center', className)}
    >
      <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <Loader2 className="size-6 animate-spin motion-reduce:animate-none" aria-hidden="true" />
      </div>
      <div className="space-y-1">
        <p className="text-base font-semibold text-foreground">{title ?? t('common.states.loading.title')}</p>
        {description && <p className="mx-auto max-w-md text-sm text-muted-foreground">{description}</p>}
      </div>
    </div>
  );
}
