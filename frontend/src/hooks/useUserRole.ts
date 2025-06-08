import { useSelector } from 'react-redux';
import { RootState } from '@/stores/store';

export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT' | 'PARENT';

export const useUserRole = () => {
  const user = useSelector((state: RootState) => state.auth.user);
  return user?.role as UserRole;
}; 