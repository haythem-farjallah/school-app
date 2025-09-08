import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useUserRole, UserRole, usePermissions } from '../../hooks/useUserRole';

interface RoleGuardProps {
  children: ReactNode;
  allowedRoles: UserRole[];
}

export const RoleGuard = ({ children, allowedRoles }: RoleGuardProps) => {
  const userRole = useUserRole();
  usePermissions(); // initialize selector for future fine-grained checks
  const location = useLocation();

  if (!userRole || !allowedRoles.includes(userRole)) {
    // Redirect to the appropriate dashboard based on role
    const dashboardPath = userRole ? `/${userRole.toLowerCase()}/dashboard` : '/login';
    return <Navigate to={dashboardPath} state={{ from: location }} replace />;
  }

  return <>{children}</>;
}; 