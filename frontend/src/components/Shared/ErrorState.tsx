import type { ReactNode } from 'react';
import { AlertTriangle, RotateCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

interface ErrorStateProps {
  title?: ReactNode;
  description?: ReactNode;
  /** Shows a retry button that calls this. */
  onRetry?: () => void;
  /** Any further action, e.g. a link home. */
  action?: ReactNode;
  className?: string;
}

export function ErrorState({ title, description, onRetry, action, className }: ErrorStateProps) {
  const { t } = useTranslation();

  return (
    <div role="alert" className={cn('flex flex-col items-center justify-center gap-3 px-4 py-12 text-center', className)}>
      <div className="flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
        <AlertTriangle className="size-6" aria-hidden="true" />
      </div>
      <div className="space-y-1">
        <p className="text-base font-semibold text-foreground">{title ?? t('common.states.error.title')}</p>
        <p className="mx-auto max-w-md text-sm text-muted-foreground">
          {description ?? t('common.states.error.description')}
        </p>
      </div>
      {(onRetry || action) && (
        <div className="mt-2 flex flex-wrap items-center justify-center gap-2">
          {onRetry && (
            <Button type="button" onClick={onRetry}>
              <RotateCcw aria-hidden="true" />
              {t('common.actions.retry')}
            </Button>
          )}
          {action}
        </div>
      )}
    </div>
  );
}
