import { Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export const RootRedirect = () => {
  const { isAuthenticated, user } = useAuth();

  // If not authenticated, redirect to login
  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  // If authenticated, redirect to appropriate dashboard based on role
  const dashboardPath = `/${user.role.toLowerCase()}/dashboard`;
  return <Navigate to={dashboardPath} replace />;
};
