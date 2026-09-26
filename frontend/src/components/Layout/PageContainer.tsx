import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface PageContainerProps {
  children: ReactNode;
  /** Use the full canvas width, e.g. for a wide timetable grid. */
  fullWidth?: boolean;
  className?: string;
}

/** Width and vertical rhythm of a page. Horizontal padding comes from the shell (Layout). */
export function PageContainer({ children, fullWidth = false, className }: PageContainerProps) {
  return (
    <div className={cn('mx-auto flex w-full flex-col gap-6 lg:gap-8', !fullWidth && 'max-w-screen-2xl', className)}>
      {children}
    </div>
  );
}
