import React from 'react';
import { Helmet } from 'react-helmet-async';

import { cn } from '@/lib/utils';

interface DashboardUser {
  id: string;
  name: string;
  email: string;
  role: string;
  avatar?: string;
  lastLogin?: Date;
}

interface DashboardNotification {
  id: string;
  title: string;
  message: string;
  type: 'info' | 'warning' | 'success' | 'error';
  isRead: boolean;
  timestamp: Date;
}

/**
 * The student shell owns the header, navigation, account menu and main landmark.
 * `user`, `notifications` and the `on*` handlers are still passed by the student pages
 * but are no longer rendered here.
 */
interface SimpleStudentLayoutProps {
  title: string;
  subtitle?: string;
  user?: DashboardUser;
  notifications?: DashboardNotification[];
  children: React.ReactNode;
  className?: string;
  onNotificationClick?: (notification: DashboardNotification) => void;
  onProfileClick?: () => void;
  onSettingsClick?: () => void;
  onLogout?: () => void;
}

export function SimpleStudentLayout({ title, subtitle, children, className }: SimpleStudentLayoutProps) {
  return (
    <div className={cn('space-y-6', className)}>
      <Helmet>
        <title>{title} - School Management System</title>
        <meta name="description" content={subtitle || `${title} dashboard for school management`} />
      </Helmet>

      <h1 className="text-2xl font-semibold text-foreground sm:text-3xl">{title}</h1>

      {children}
    </div>
  );
}
