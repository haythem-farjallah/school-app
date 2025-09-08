import { useState, useEffect } from 'react';
import { Clock, User, Activity, AlertCircle, CheckCircle, XCircle } from 'lucide-react';
import { RealTimeNotificationDto } from '@/types/websocket';
import { useAppSelector } from '@/stores/store';

interface AdminFeedItem extends RealTimeNotificationDto {
  id: string;
  receivedAt: Date;
}

/**
 * Admin Feed Component
 * Displays real-time activity feed for administrators
 */
export function AdminFeed() {
  const user = useAppSelector(state => state.auth.user);
  const [feedItems, setFeedItems] = useState<AdminFeedItem[]>([]);
  const [isExpanded, setIsExpanded] = useState(false);

  // Only show for admin users
  if (!user || user.role !== 'ADMIN') {
    return null;
  }

  // Listen for admin feed notifications through global event system
  useEffect(() => {
    const handleAdminFeed = (event: CustomEvent<RealTimeNotificationDto>) => {
      const notification = event.detail;
      console.log('📢 AdminFeed component received event:', event);
      console.log('📢 AdminFeed notification details:', notification);
      
      if (notification.type === 'ADMIN_FEED') {
        console.log('✅ AdminFeed processing ADMIN_FEED notification');
        const feedItem: AdminFeedItem = {
          ...notification,
          receivedAt: new Date()
        };
        
        setFeedItems(prev => {
          const updated = [feedItem, ...prev.slice(0, 49)];
          console.log('📢 AdminFeed updated items:', updated);
          return updated;
        }); // Keep last 50 items
      } else {
        console.log('❌ AdminFeed ignoring non-ADMIN_FEED notification:', notification.type);
      }
    };

    // Listen to custom events (we'll dispatch these from socket service)
    window.addEventListener('admin-feed', handleAdminFeed as EventListener);
    
    return () => {
      window.removeEventListener('admin-feed', handleAdminFeed as EventListener);
    };
  }, []);

  const getActivityIcon = (entityType: string) => {
    switch (entityType?.toLowerCase()) {
      case 'user':
      case 'student':
      case 'teacher':
      case 'parent':
        return <User className="w-4 h-4" />;
      case 'enrollment':
        return <CheckCircle className="w-4 h-4" />;
      case 'grade':
        return <Activity className="w-4 h-4" />;
      case 'system':
        return <AlertCircle className="w-4 h-4" />;
      default:
        return <Activity className="w-4 h-4" />;
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority?.toUpperCase()) {
      case 'HIGH':
        return 'text-red-600 bg-red-50 border-red-200';
      case 'MEDIUM':
        return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case 'LOW':
      default:
        return 'text-blue-600 bg-blue-50 border-blue-200';
    }
  };

  const formatTime = (date: Date) => {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700">
      <div className="p-4 border-b border-gray-200 dark:border-gray-700">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity className="w-5 h-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Admin Activity Feed
            </h3>
            {feedItems.length > 0 && (
              <span className="bg-blue-100 text-blue-800 text-xs font-medium px-2.5 py-0.5 rounded-full">
                {feedItems.length}
              </span>
            )}
          </div>
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
          >
            {isExpanded ? 'Collapse' : 'Expand'}
          </button>
        </div>
      </div>

      <div className={`transition-all duration-300 ${isExpanded ? 'max-h-96 overflow-y-auto' : 'max-h-48 overflow-y-auto'}`}>
        {feedItems.length === 0 ? (
          <div className="p-6 text-center text-gray-500 dark:text-gray-400">
            <Activity className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p>No recent activity</p>
            <p className="text-xs mt-1">Admin activities will appear here in real-time</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-200 dark:divide-gray-700">
            {feedItems.map((item) => (
              <div key={item.id} className="p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                <div className="flex items-start gap-3">
                  <div className={`p-2 rounded-full ${getPriorityColor(item.priority)}`}>
                    {getActivityIcon(item.entityType || '')}
                  </div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h4 className="text-sm font-medium text-gray-900 dark:text-white truncate">
                        {item.title}
                      </h4>
                      <div className="flex items-center gap-1 text-xs text-gray-500 dark:text-gray-400">
                        <Clock className="w-3 h-3" />
                        {formatTime(item.receivedAt)}
                      </div>
                    </div>
                    
                    {item.message && (
                      <p className="text-sm text-gray-600 dark:text-gray-300 mt-1">
                        {item.message}
                      </p>
                    )}
                    
                    <div className="flex items-center gap-4 mt-2 text-xs text-gray-500 dark:text-gray-400">
                      {item.performedBy && (
                        <span className="flex items-center gap-1">
                          <User className="w-3 h-3" />
                          {item.performedBy}
                        </span>
                      )}
                      
                      {item.entityType && (
                        <span className="px-2 py-1 bg-gray-100 dark:bg-gray-600 rounded-md">
                          {item.entityType}
                        </span>
                      )}
                      
                      <span className={`px-2 py-1 rounded-md text-xs ${getPriorityColor(item.priority)}`}>
                        {item.priority}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
