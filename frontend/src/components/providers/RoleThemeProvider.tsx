import React, { createContext, useContext, useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { getRoleTheme, generateThemeCSS, getRoleClasses, type RoleTheme } from '@/lib/theme';

interface RoleThemeContextType {
  theme: RoleTheme;
  classes: ReturnType<typeof getRoleClasses>;
  role: string;
}

const RoleThemeContext = createContext<RoleThemeContextType | undefined>(undefined);

interface RoleThemeProviderProps {
  children: React.ReactNode;
}

export function RoleThemeProvider({ children }: RoleThemeProviderProps) {
  const { user } = useAuth();
  const role = user?.role || 'STUDENT';
  const theme = getRoleTheme(role);
  const classes = getRoleClasses(role);

  // Apply CSS custom properties to document root
  useEffect(() => {
    const root = document.documentElement;
    const cssText = generateThemeCSS(theme);
    
    // Create or update style element
    let styleElement = document.getElementById('role-theme-styles') as HTMLStyleElement;
    if (!styleElement) {
      styleElement = document.createElement('style');
      styleElement.id = 'role-theme-styles';
      document.head.appendChild(styleElement);
    }
    
    styleElement.textContent = `:root { ${cssText} }`;
    
    // Also set data attribute for role-based styling
    root.setAttribute('data-role', role.toLowerCase());
    
    return () => {
      // Cleanup on unmount
      if (styleElement) {
        styleElement.remove();
      }
      root.removeAttribute('data-role');
    };
  }, [theme, role]);

  const contextValue: RoleThemeContextType = {
    theme,
    classes,
    role
  };

  return (
    <RoleThemeContext.Provider value={contextValue}>
      {children}
    </RoleThemeContext.Provider>
  );
}

// Hook to use role theme
export function useRoleTheme() {
  const context = useContext(RoleThemeContext);
  if (context === undefined) {
    throw new Error('useRoleTheme must be used within a RoleThemeProvider');
  }
  return context;
}

// Higher-order component for role-based styling
export function withRoleTheme<P extends object>(
  Component: React.ComponentType<P>
) {
  return function ThemedComponent(props: P) {
    const { classes, theme, role } = useRoleTheme();
    
    return (
      <Component 
        {...props} 
        roleClasses={classes}
        roleTheme={theme}
        userRole={role}
      />
    );
  };
}
