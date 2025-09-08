import { useSelector } from 'react-redux';
import { RootState } from '@/stores/store';

export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT' | 'PARENT' | 'STAFF';

export const useUserRole = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  return user?.role as UserRole;
}; 

export const usePermissions = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  return (user?.permissions ?? []) as string[];
};