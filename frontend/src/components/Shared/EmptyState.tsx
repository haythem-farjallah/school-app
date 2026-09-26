import type { ComponentType, ReactNode } from 'react';
import { Inbox } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/lib/utils';

interface EmptyStateProps {
  icon?: ComponentType<{ className?: string }>;
  title?: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({ icon: Icon = Inbox, title, description, action, className }: EmptyStateProps) {
  const { t } = useTranslation();

  return (
    <div className={cn('flex flex-col items-center justify-center gap-3 px-4 py-12 text-center', className)}>
      <div className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
        <Icon className="size-6" aria-hidden="true" />
      </div>
      <div className="space-y-1">
        <p className="text-base font-semibold text-foreground">{title ?? t('common.states.empty.title')}</p>
        <p className="mx-auto max-w-md text-sm text-muted-foreground">
          {description ?? t('common.states.empty.description')}
        </p>
      </div>
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
