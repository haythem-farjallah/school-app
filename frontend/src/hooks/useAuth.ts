import { useSelector } from 'react-redux';
import { RootState } from '@/stores/store';

export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT' | 'PARENT' | 'STAFF';

interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  permissions?: string[];
}

interface AuthHook {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
}

export const useAuth = (): AuthHook => {
  const { user, accessToken, refreshToken } = useSelector((state: RootState) => state.auth);
  
  return {
    user: user as User | null,
    accessToken,
    refreshToken,
    isAuthenticated: !!(user && accessToken),
  };
};

export const useUserRole = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  return user?.role as UserRole;
}; 

export const usePermissions = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  return (user?.permissions ?? []) as string[];
};
