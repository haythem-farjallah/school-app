// Role-based theme system
export interface RoleTheme {
  primary: string;
  primaryHover: string;
  primaryLight: string;
  primaryDark: string;
  secondary: string;
  accent: string;
  background: string;
  surface: string;
  text: string;
  textSecondary: string;
  border: string;
  icon: string;
  gradient: string;
}

export const roleThemes: Record<string, RoleTheme> = {
  ADMIN: {
    primary: '#dc2626', // Red
    primaryHover: '#b91c1c',
    primaryLight: '#fef2f2',
    primaryDark: '#991b1b',
    secondary: '#f87171',
    accent: '#fbbf24',
    background: '#ffffff',
    surface: '#f9fafb',
    text: '#111827',
    textSecondary: '#6b7280',
    border: '#e5e7eb',
    icon: '#374151',
    gradient: 'from-red-50 to-rose-50'
  },
  TEACHER: {
    primary: '#059669', // Green
    primaryHover: '#047857',
    primaryLight: '#f0fdf4',
    primaryDark: '#065f46',
    secondary: '#34d399',
    accent: '#10b981',
    background: '#ffffff',
    surface: '#f9fafb',
    text: '#111827',
    textSecondary: '#6b7280',
    border: '#e5e7eb',
    icon: '#374151',
    gradient: 'from-green-50 to-emerald-50'
  },
  STUDENT: {
    primary: '#2563eb', // Blue
    primaryHover: '#1d4ed8',
    primaryLight: '#eff6ff',
    primaryDark: '#1e40af',
    secondary: '#60a5fa',
    accent: '#3b82f6',
    background: '#ffffff',
    surface: '#f9fafb',
    text: '#111827',
    textSecondary: '#6b7280',
    border: '#e5e7eb',
    icon: '#374151',
    gradient: 'from-blue-50 to-indigo-50'
  },
  STAFF: {
    primary: '#7c3aed', // Purple
    primaryHover: '#6d28d9',
    primaryLight: '#faf5ff',
    primaryDark: '#5b21b6',
    secondary: '#a78bfa',
    accent: '#8b5cf6',
    background: '#ffffff',
    surface: '#f9fafb',
    text: '#111827',
    textSecondary: '#6b7280',
    border: '#e5e7eb',
    icon: '#374151',
    gradient: 'from-purple-50 to-violet-50'
  },
  PARENT: {
    primary: '#ea580c', // Orange
    primaryHover: '#c2410c',
    primaryLight: '#fff7ed',
    primaryDark: '#9a3412',
    secondary: '#fb923c',
    accent: '#f97316',
    background: '#ffffff',
    surface: '#f9fafb',
    text: '#111827',
    textSecondary: '#6b7280',
    border: '#e5e7eb',
    icon: '#374151',
    gradient: 'from-orange-50 to-amber-50'
  }
};

// Get theme for current user role
export function getRoleTheme(role?: string): RoleTheme {
  if (!role) return roleThemes.STUDENT; // Default fallback
  return roleThemes[role.toUpperCase()] || roleThemes.STUDENT;
}

// Generate CSS custom properties for a role theme
export function generateThemeCSS(theme: RoleTheme): string {
  return `
    --color-primary: ${theme.primary};
    --color-primary-hover: ${theme.primaryHover};
    --color-primary-light: ${theme.primaryLight};
    --color-primary-dark: ${theme.primaryDark};
    --color-secondary: ${theme.secondary};
    --color-accent: ${theme.accent};
    --color-background: ${theme.background};
    --color-surface: ${theme.surface};
    --color-text: ${theme.text};
    --color-text-secondary: ${theme.textSecondary};
    --color-border: ${theme.border};
    --color-icon: ${theme.icon};
  `;
}

// Tailwind CSS classes for role themes
export function getRoleClasses(role?: string) {
  const roleUpper = role?.toUpperCase();
  
  switch (roleUpper) {
    case 'ADMIN':
      return {
        primary: 'text-red-600',
        primaryBg: 'bg-red-600',
        primaryHover: 'hover:bg-red-700',
        primaryLight: 'bg-red-50',
        primaryBorder: 'border-red-200',
        gradient: 'bg-gradient-to-r from-red-50 to-rose-50',
        icon: 'text-red-600',
        badge: 'bg-red-100 text-red-800',
        button: 'bg-red-600 hover:bg-red-700 text-white'
      };
    case 'TEACHER':
      return {
        primary: 'text-green-600',
        primaryBg: 'bg-green-600',
        primaryHover: 'hover:bg-green-700',
        primaryLight: 'bg-green-50',
        primaryBorder: 'border-green-200',
        gradient: 'bg-gradient-to-r from-green-50 to-emerald-50',
        icon: 'text-green-600',
        badge: 'bg-green-100 text-green-800',
        button: 'bg-green-600 hover:bg-green-700 text-white'
      };
    case 'STUDENT':
      return {
        primary: 'text-blue-600',
        primaryBg: 'bg-blue-600',
        primaryHover: 'hover:bg-blue-700',
        primaryLight: 'bg-blue-50',
        primaryBorder: 'border-blue-200',
        gradient: 'bg-gradient-to-r from-blue-50 to-indigo-50',
        icon: 'text-blue-600',
        badge: 'bg-blue-100 text-blue-800',
        button: 'bg-blue-600 hover:bg-blue-700 text-white'
      };
    case 'STAFF':
      return {
        primary: 'text-purple-600',
        primaryBg: 'bg-purple-600',
        primaryHover: 'hover:bg-purple-700',
        primaryLight: 'bg-purple-50',
        primaryBorder: 'border-purple-200',
        gradient: 'bg-gradient-to-r from-purple-50 to-violet-50',
        icon: 'text-purple-600',
        badge: 'bg-purple-100 text-purple-800',
        button: 'bg-purple-600 hover:bg-purple-700 text-white'
      };
    case 'PARENT':
      return {
        primary: 'text-orange-600',
        primaryBg: 'bg-orange-600',
        primaryHover: 'hover:bg-orange-700',
        primaryLight: 'bg-orange-50',
        primaryBorder: 'border-orange-200',
        gradient: 'bg-gradient-to-r from-orange-50 to-amber-50',
        icon: 'text-orange-600',
        badge: 'bg-orange-100 text-orange-800',
        button: 'bg-orange-600 hover:bg-orange-700 text-white'
      };
    default:
      return {
        primary: 'text-gray-600',
        primaryBg: 'bg-gray-600',
        primaryHover: 'hover:bg-gray-700',
        primaryLight: 'bg-gray-50',
        primaryBorder: 'border-gray-200',
        gradient: 'bg-gradient-to-r from-gray-50 to-slate-50',
        icon: 'text-gray-600',
        badge: 'bg-gray-100 text-gray-800',
        button: 'bg-gray-600 hover:bg-gray-700 text-white'
      };
  }
}

// Hook to get current user's theme
export function useRoleTheme() {
  // This would typically get the user role from your auth context
  // For now, returning a function that takes role as parameter
  return (role?: string) => ({
    theme: getRoleTheme(role),
    classes: getRoleClasses(role)
  });
}
