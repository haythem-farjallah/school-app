import { useState } from 'react';
import { Send, Wifi, WifiOff, TestTube } from 'lucide-react';
import { sendTestMessage, isConnected, getConnectionStatus } from '@/lib/socket';
import { useAppSelector } from '@/stores/store';

/**
 * WebSocket Test Panel - Development tool for testing WebSocket connectivity
 * Only shows in development mode for admin users
 */
export function WebSocketTestPanel() {
  const user = useAppSelector(state => state.auth.user);
  const [testMessage, setTestMessage] = useState('Hello from frontend!');
  const [isExpanded, setIsExpanded] = useState(false);
  const connectionStatus = getConnectionStatus();

  // Only show in development mode for admin users
  if (!import.meta.env.DEV || !user || user.role !== 'ADMIN') {
    return null;
  }

  const handleSendTest = () => {
    if (!isConnected()) {
      alert('WebSocket not connected');
      return;
    }
    
    sendTestMessage(testMessage);
    setTestMessage('Hello from frontend!');
  };

  const triggerTestNotification = () => {
    // This would be replaced with actual backend triggers in a real scenario
    const mockNotification = {
      id: `test-${Date.now()}`,
      type: 'ADMIN_FEED',
      title: 'Test Admin Feed',
      message: 'This is a test notification to verify the admin feed is working',
      priority: 'MEDIUM' as const,
      timestamp: new Date().toISOString(),
      performedBy: user.email || 'Test User',
      entityType: 'TEST',
      entityId: 1
    };

    // Dispatch test event
    window.dispatchEvent(new CustomEvent('admin-feed', { detail: mockNotification }));
  };

  return (
    <div className="fixed bottom-20 right-4 z-40 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg max-w-xs">
      <div className="p-3 border-b border-gray-200 dark:border-gray-700">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TestTube className="w-4 h-4 text-gray-600" />
            <span className="text-sm font-medium text-gray-900 dark:text-white">
              WebSocket Test
            </span>
          </div>
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="text-xs text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
          >
            {isExpanded ? 'Hide' : 'Show'}
          </button>
        </div>
      </div>

      {isExpanded && (
        <div className="p-3 space-y-3">
          <div className="flex items-center gap-2 text-xs">
            {connectionStatus.connected ? (
              <>
                <Wifi className="w-3 h-3 text-green-500" />
                <span className="text-green-600">Connected</span>
              </>
            ) : (
              <>
                <WifiOff className="w-3 h-3 text-red-500" />
                <span className="text-red-600">
                  {connectionStatus.reconnecting ? 'Reconnecting...' : 'Disconnected'}
                </span>
              </>
            )}
          </div>

          <div className="space-y-2">
            <label className="block text-xs font-medium text-gray-700 dark:text-gray-300">
              Test Message:
            </label>
            <input
              type="text"
              value={testMessage}
              onChange={(e) => setTestMessage(e.target.value)}
              className="w-full px-2 py-1 text-xs border border-gray-300 rounded dark:border-gray-600 dark:bg-gray-700 dark:text-white"
              placeholder="Enter test message..."
            />
          </div>

          <div className="flex gap-2">
            <button
              onClick={handleSendTest}
              disabled={!connectionStatus.connected}
              className="flex items-center gap-1 px-2 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Send className="w-3 h-3" />
              Send
            </button>
            
            <button
              onClick={triggerTestNotification}
              className="flex items-center gap-1 px-2 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700"
            >
              <TestTube className="w-3 h-3" />
              Test Feed
            </button>
          </div>

          <div className="text-xs text-gray-500 dark:text-gray-400">
            <p>User: {user.email}</p>
            <p>Role: {user.role}</p>
            <p>ID: {user.id}</p>
          </div>
        </div>
      )}
    </div>
  );
}
