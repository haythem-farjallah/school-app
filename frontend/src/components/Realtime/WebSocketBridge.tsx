import { useEffect, useState } from 'react';
import { useAppSelector } from '@/stores/store';
import { 
  connectWebSocket, 
  disconnectWebSocket, 
  getConnectionStatus, 
  onConnectionStatusChange,
  isConnected 
} from '@/lib/socket';
import { ConnectionStatus } from '@/types/websocket';

/**
 * WebSocket Bridge Component
 * Manages WebSocket connection lifecycle based on user authentication
 */
export function WebSocketBridge() {
  const user = useAppSelector(state => state.auth.user);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>(getConnectionStatus());

  // Set up connection status listener
  useEffect(() => {
    onConnectionStatusChange(setConnectionStatus);
  }, []);

  // Manage WebSocket connection based on user authentication
  useEffect(() => {
    if (user && user.id && user.role) {
      console.log('🔗 User authenticated, connecting WebSocket...', {
        userId: user.id,
        userRole: user.role,
        userEmail: user.email
      });
      
      // Small delay to ensure token is available in localStorage
      const timer = setTimeout(() => {
        connectWebSocket({
          id: user.id,
          role: user.role,
          email: user.email
        });
      }, 100);

      // Cleanup on unmount or user change
      return () => {
        clearTimeout(timer);
        console.log('🔌 User changed/unmounted, disconnecting WebSocket...');
        disconnectWebSocket();
      };
    } else {
      // User logged out or not authenticated
      console.log('👤 No authenticated user, disconnecting WebSocket...');
      disconnectWebSocket();
    }
  }, [user?.id, user?.role]); // Re-run when user ID or role changes

  // Log connection status changes for debugging
  useEffect(() => {
    if (connectionStatus.connected) {
      console.log('✅ WebSocket Bridge: Connected');
    } else if (connectionStatus.reconnecting) {
      console.log('🔄 WebSocket Bridge: Reconnecting...');
    } else if (connectionStatus.error) {
      console.error('❌ WebSocket Bridge: Error -', connectionStatus.error);
    } else {
      console.log('🔌 WebSocket Bridge: Disconnected');
    }
  }, [connectionStatus]);

  // Development-only status indicator
  if (import.meta.env.DEV && user) {
    return (
      <div className="fixed bottom-4 right-4 z-50 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg p-2 text-xs">
        <div className="flex items-center gap-2">
          <div 
            className={`w-2 h-2 rounded-full ${
              connectionStatus.connected 
                ? 'bg-green-500' 
                : connectionStatus.reconnecting 
                ? 'bg-yellow-500 animate-pulse' 
                : 'bg-red-500'
            }`}
          />
          <span className="text-gray-600 dark:text-gray-300">
            WebSocket: {
              connectionStatus.connected 
                ? 'Connected' 
                : connectionStatus.reconnecting 
                ? 'Reconnecting...' 
                : connectionStatus.error 
                ? 'Error' 
                : 'Disconnected'
            }
          </span>
        </div>
        {connectionStatus.error && (
          <div className="text-red-500 text-xs mt-1">
            {connectionStatus.error}
          </div>
        )}
      </div>
    );
  }

  // In production, render nothing (invisible bridge)
  return null;
}
