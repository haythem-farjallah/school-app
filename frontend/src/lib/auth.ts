import { token } from './token';

// Re-export useAuth hook from hooks directory
export { useAuth, useUserRole, usePermissions } from '@/hooks/useAuth';

/**
 * Check if user is authenticated by validating tokens and user data
 */
export const isAuthenticated = (): boolean => {
  const accessToken = token.access;
  const refreshToken = token.refresh;
  const user = token.user;
  
  return !!(accessToken && refreshToken && user);
};

/**
 * Get current user data from localStorage
 */
export const getCurrentUser = () => {
  return token.user;
};

/**
 * Get current authentication status
 */
export const getAuthStatus = () => {
  const user = token.user;
  return {
    isAuthenticated: isAuthenticated(),
    hasAccessToken: !!token.access,
    hasRefreshToken: !!token.refresh,
    hasUserData: !!user,
    user: user,
    userRole: user?.role || null,
  };
}; 