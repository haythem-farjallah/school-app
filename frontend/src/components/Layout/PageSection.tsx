import { useId, type ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface PageSectionProps {
  title?: ReactNode;
  description?: ReactNode;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}

/** A titled part of a page. Structural only: wrap the content in a Card where it is conceptually a card. */
export function PageSection({ title, description, actions, children, className }: PageSectionProps) {
  const titleId = useId();

  return (
    <section aria-labelledby={title ? titleId : undefined} className={cn('flex flex-col gap-4', className)}>
      {(title || description || actions) && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div className="min-w-0 space-y-1">
            {title && (
              <h2 id={titleId} className="text-lg font-semibold text-foreground">
                {title}
              </h2>
            )}
            {description && <p className="text-sm text-muted-foreground">{description}</p>}
          </div>
          {actions && <div className="flex shrink-0 flex-wrap items-center gap-2">{actions}</div>}
        </div>
      )}
      {children}
    </section>
  );
}
