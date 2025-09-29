import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { 
  Home, 
  Users, 
  Calendar, 
  CheckSquare, 
  MessageSquare, 
  Phone, 
  Bell, 
  Settings,
  BookOpen,
  TrendingUp,
  AlertTriangle,
  Award
} from 'lucide-react';

interface ParentSidebarProps {
  isCollapsed?: boolean;
  onToggle?: () => void;
}

interface NavItem {
  id: string;
  label: string;
  icon: React.ComponentType<any>;
  path: string;
  badge?: number;
  isActive?: boolean;
}

export const ParentSidebar: React.FC<ParentSidebarProps> = ({ 
  isCollapsed = false, 
  onToggle 
}) => {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems: NavItem[] = [
    {
      id: 'dashboard',
      label: 'Dashboard',
      icon: Home,
      path: '/parent/dashboard'
    },
    {
      id: 'children',
      label: 'My Children',
      icon: Users,
      path: '/parent/children'
    },
    {
      id: 'timetables',
      label: 'Timetables',
      icon: Calendar,
      path: '/parent/timetables'
    },
    {
      id: 'attendance',
      label: 'Attendance',
      icon: CheckSquare,
      path: '/parent/attendance'
    },
    {
      id: 'grades',
      label: 'Grades & Reports',
      icon: BookOpen,
      path: '/parent/grades'
    },
    {
      id: 'messages',
      label: 'Messages',
      icon: MessageSquare,
      path: '/parent/messages',
      badge: 3
    },
    {
      id: 'meetings',
      label: 'Meetings',
      icon: Phone,
      path: '/parent/meetings'
    },
    {
      id: 'notifications',
      label: 'Notifications',
      icon: Bell,
      path: '/parent/notifications',
      badge: 5
    },
    {
      id: 'statistics',
      label: 'Statistics',
      icon: TrendingUp,
      path: '/parent/statistics'
    },
    {
      id: 'alerts',
      label: 'Alerts',
      icon: AlertTriangle,
      path: '/parent/alerts',
      badge: 2
    },
    {
      id: 'achievements',
      label: 'Achievements',
      icon: Award,
      path: '/parent/achievements'
    },
    {
      id: 'settings',
      label: 'Settings',
      icon: Settings,
      path: '/parent/settings'
    }
  ];

  const handleNavigation = (path: string) => {
    navigate(path);
  };

  const isActivePath = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  return (
    <div className={`bg-white border-r border-gray-200 transition-all duration-300 ${
      isCollapsed ? 'w-16' : 'w-64'
    }`}>
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          {!isCollapsed && (
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg flex items-center justify-center">
                <Users className="h-5 w-5 text-white" />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-gray-900">Parent Portal</h2>
                <p className="text-xs text-gray-500">School Management</p>
              </div>
            </div>
          )}
          {isCollapsed && (
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg flex items-center justify-center mx-auto">
              <Users className="h-5 w-5 text-white" />
            </div>
          )}
        </div>
      </div>

      {/* Navigation */}
      <nav className="p-4 space-y-2">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = isActivePath(item.path);
          
          return (
            <button
              key={item.id}
              onClick={() => handleNavigation(item.path)}
              className={`w-full flex items-center space-x-3 px-3 py-2.5 rounded-lg text-left transition-all duration-200 group ${
                isActive
                  ? 'bg-blue-50 text-blue-700 border border-blue-200'
                  : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900'
              }`}
            >
              <Icon className={`h-5 w-5 flex-shrink-0 ${
                isActive ? 'text-blue-600' : 'text-gray-500 group-hover:text-gray-700'
              }`} />
              
              {!isCollapsed && (
                <>
                  <span className="flex-1 text-sm font-medium">{item.label}</span>
                  {item.badge && (
                    <span className="bg-red-500 text-white text-xs rounded-full px-2 py-0.5 min-w-[20px] text-center">
                      {item.badge}
                    </span>
                  )}
                </>
              )}
              
              {isCollapsed && item.badge && (
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </nav>

      {/* Quick Stats */}
      {!isCollapsed && (
        <div className="p-4 border-t border-gray-200">
          <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-3">
            <h3 className="text-sm font-medium text-gray-900 mb-2">Quick Stats</h3>
            <div className="space-y-2">
              <div className="flex justify-between items-center text-xs">
                <span className="text-gray-600">Children</span>
                <span className="font-medium text-gray-900">2</span>
              </div>
              <div className="flex justify-between items-center text-xs">
                <span className="text-gray-600">Avg Attendance</span>
                <span className="font-medium text-green-600">94.5%</span>
              </div>
              <div className="flex justify-between items-center text-xs">
                <span className="text-gray-600">Unread Messages</span>
                <span className="font-medium text-blue-600">3</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Footer */}
      <div className="p-4 border-t border-gray-200">
        {!isCollapsed ? (
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
              <span className="text-sm font-medium text-gray-600">P</span>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-gray-900 truncate">Parent Name</p>
              <p className="text-xs text-gray-500 truncate">parent@email.com</p>
            </div>
          </div>
        ) : (
          <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center mx-auto">
            <span className="text-sm font-medium text-gray-600">P</span>
          </div>
        )}
      </div>
    </div>
  );
};
