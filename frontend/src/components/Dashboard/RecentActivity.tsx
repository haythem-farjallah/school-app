import { useAppSelector } from '@/stores/store';
import { Clock, User, Activity, AlertCircle, CheckCircle, ExternalLink } from 'lucide-react';

/**
 * Recent Activity Component
 * Shows recent notifications from the Redux store as activity feed
 */
export function RecentActivity() {
  const notifications = useAppSelector(state => state.notification.list);

  // Get the last 5 notifications for recent activity (excluding errors)
  const recentNotifications = notifications
    .filter(n => n.type !== 'error') // Hide error notifications from activity feed
    .slice(0, 5);

  const getActivityIcon = (type: string, source?: string) => {
    // Show different icons for WebSocket vs other sources
    if (source === 'websocket') {
      switch (type) {
        case 'error':
          return <AlertCircle className="w-4 h-4 text-red-500" />;
        case 'success':
          return <CheckCircle className="w-4 h-4 text-green-500" />;
        case 'warning':
          return <Activity className="w-4 h-4 text-yellow-500" />;
        default:
          return <Activity className="w-4 h-4 text-blue-500" />;
      }
    }
    
    // Default icons for non-websocket notifications
    switch (type) {
      case 'error':
        return <div className="w-2 h-2 bg-red-500 rounded-full"></div>;
      case 'success':
        return <div className="w-2 h-2 bg-green-500 rounded-full"></div>;
      case 'warning':
        return <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>;
      default:
        return <div className="w-2 h-2 bg-blue-500 rounded-full"></div>;
    }
  };

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / (1000 * 60));
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-xl font-semibold text-gray-900">Recent Activity</h3>
        {notifications.filter(n => n.type !== 'error').length > 0 && (
          <span className="text-sm text-gray-500">
            {notifications.filter(n => n.type !== 'error').length} activities
          </span>
        )}
      </div>

      <div className="space-y-3">
        {recentNotifications.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <Activity className="w-8 h-8 mx-auto mb-2 opacity-50" />
            <p className="text-sm">No recent activity</p>
            <p className="text-xs mt-1">Activities will appear here as they happen</p>
          </div>
        ) : (
          recentNotifications.map((notification) => (
            <div
              key={notification.id}
              className="flex items-start gap-3 p-3 rounded-lg bg-gray-50 hover:bg-gray-100 transition-colors group"
            >
              {/* Activity Icon */}
              <div className="flex-shrink-0 mt-0.5">
                {getActivityIcon(notification.type, notification.source)}
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <p className="text-sm text-gray-700 font-medium">
                      {notification.title}
                    </p>
                    
                    {notification.message && (
                      <p className="text-xs text-gray-600 mt-1 line-clamp-2">
                        {notification.message}
                      </p>
                    )}
                    
                    <div className="flex items-center gap-2 mt-2">
                      <div className="flex items-center gap-1 text-xs text-gray-500">
                        <Clock className="w-3 h-3" />
                        {notification.timestamp && formatTime(notification.timestamp)}
                      </div>
                      
                      {notification.source === 'websocket' && (
                        <span className="text-xs bg-green-100 text-green-800 px-1.5 py-0.5 rounded">
                          Live
                        </span>
                      )}
                      
                      {!notification.read && (
                        <span className="text-xs bg-blue-100 text-blue-800 px-1.5 py-0.5 rounded">
                          New
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Action Link */}
                  {notification.actionUrl && (
                    <button
                      onClick={() => window.location.href = notification.actionUrl!}
                      className="opacity-0 group-hover:opacity-100 p-1 text-gray-400 hover:text-gray-600 transition-all"
                      title="Go to"
                    >
                      <ExternalLink className="w-3 h-3" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Show more link if there are more notifications */}
      {notifications.length > 5 && (
        <div className="mt-4 pt-3 border-t border-gray-200 text-center">
          <button className="text-sm text-blue-600 hover:text-blue-800 transition-colors">
            View all activity ({notifications.length})
          </button>
        </div>
      )}
    </div>
  );
}
