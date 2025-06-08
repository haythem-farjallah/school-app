const ACCESS  = "accessToken";
const REFRESH = "refreshToken";
const USER_DATA = "userData";

interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export const token = {
  get access() {
    return localStorage.getItem(ACCESS);
  },
  set access(v: string | null) {
    if (v) {
      localStorage.setItem(ACCESS, v);
    } else {
      localStorage.removeItem(ACCESS);
    }
  },

  get refresh() {
    return localStorage.getItem(REFRESH);
  },
  set refresh(v: string | null) {
    if (v) {
      localStorage.setItem(REFRESH, v);
    } else {
      localStorage.removeItem(REFRESH);
    }
  },

  get user(): User | null {
    const userData = localStorage.getItem(USER_DATA);
    return userData ? JSON.parse(userData) : null;
  },
  set user(v: User | null) {
    if (v) {
      localStorage.setItem(USER_DATA, JSON.stringify(v));
    } else {
      localStorage.removeItem(USER_DATA);
    }
  },

  clear() {
    // Clear specific tokens and user data
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
    localStorage.removeItem(USER_DATA);
    
    // Clear all user-related data that might be stored
    const keysToRemove = [
      'user',
      'userProfile', 
      'userSettings',
      'userPreferences',
      'authData',
      'persist:auth',
      'persist:root'
    ];
    
    keysToRemove.forEach(key => {
      localStorage.removeItem(key);
    });
    
    // Also clear sessionStorage to be thorough
    sessionStorage.clear();
  },
};
