import { token } from './token';

/**
 * Comprehensive logout utility that clears all user data
 * and resets the application to a clean state
 */
export const performLogout = () => {
  // Clear all tokens and localStorage
  token.clear();
  
  // Clear any query cache keys that might contain user data
  const queryKeys = [
    'user-profile',
    'user-settings', 
    'user-data',
    'auth-user',
    'react-query-cache'
  ];
  
  queryKeys.forEach(key => {
    localStorage.removeItem(key);
  });
  
  // Clear any cookies (if using any)
  document.cookie.split(";").forEach((c) => {
    const eqPos = c.indexOf("=");
    const name = eqPos > -1 ? c.substr(0, eqPos) : c;
    document.cookie = name + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/";
  });
  
  // Force garbage collection if available
  if (window.gc) {
    window.gc();
  }
  
  // Log the logout action for debugging
  console.log('User logged out successfully - all data cleared');
};

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