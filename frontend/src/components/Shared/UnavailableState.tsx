import { useId, type ReactNode } from 'react';
import { CircleSlash } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/lib/utils';

interface UnavailableStateProps {
  title?: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  className?: string;
}

/** For functionality that is deliberately not available. Never implies that anything was done. */
export function UnavailableState({ title, description, action, className }: UnavailableStateProps) {
  const { t } = useTranslation();
  const titleId = useId();

  return (
    <section
      aria-labelledby={titleId}
      className={cn('flex flex-col items-center justify-center gap-3 px-4 py-12 text-center', className)}
    >
      <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <CircleSlash className="size-6" aria-hidden="true" />
      </div>
      <div className="space-y-1">
        <p id={titleId} className="text-base font-semibold text-foreground">
          {title ?? t('common.states.unavailable.title')}
        </p>
        <p className="mx-auto max-w-md text-sm text-muted-foreground">
          {description ?? t('common.states.unavailable.description')}
        </p>
      </div>
      {action && <div className="mt-2">{action}</div>}
    </section>
  );
}
