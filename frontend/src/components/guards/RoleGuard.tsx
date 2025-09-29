import { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useUserRole, UserRole, usePermissions } from '../../hooks/useUserRole';
import { useAuth } from '../../hooks/useAuth';

interface RoleGuardProps {
  children: ReactNode;
  allowedRoles: UserRole[];
}

export const RoleGuard = ({ children, allowedRoles }: RoleGuardProps) => {
  const { isAuthenticated, user } = useAuth();
  const userRole = useUserRole();
  usePermissions(); // initialize selector for future fine-grained checks
  const location = useLocation();

  // If not authenticated, redirect to login
  if (!isAuthenticated || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // If authenticated but role doesn't match, redirect to appropriate dashboard
  if (!userRole || !allowedRoles.includes(userRole)) {
    const dashboardPath = `/${userRole.toLowerCase()}/dashboard`;
    return <Navigate to={dashboardPath} state={{ from: location }} replace />;
  }

  return <>{children}</>;
}; 