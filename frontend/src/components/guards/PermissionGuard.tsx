import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { usePermissions } from '@/hooks/useUserRole';

export const PermissionGuard = ({ children, required }: { children: ReactNode; required: string; }) => {
  const perms = usePermissions();
  const location = useLocation();
  if (!perms.includes(required)) {
    return <Navigate to={location.state?.from ?? '/admin/dashboard'} replace />;
  }
  return <>{children}</>;
};


