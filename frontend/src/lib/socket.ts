import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_URL } from '@/lib/env';
import { token } from '@/lib/token';
import { store } from '@/stores/store';
import { addNotification } from '@/stores/notificationSlice';
import toast from 'react-hot-toast';
import { RealTimeNotificationDto, NotificationSeverity, ConnectionStatus } from '@/types/websocket';

// WebSocket URL - replace /api with /ws
const WS_URL = (import.meta.env.VITE_WS_URL as string) || API_URL.replace(/\/api$/, '') + '/ws';

let client: Client | null = null;
let connectionStatus: ConnectionStatus = { connected: false, reconnecting: false };
let connectionStatusCallback: ((status: ConnectionStatus) => void) | null = null;

/**
 * Map notification priority to severity for UI
 */
function mapPriorityToSeverity(priority: string): NotificationSeverity {
  switch (priority?.toUpperCase()) {
    case 'HIGH':
      return 'error';
    case 'MEDIUM':
      return 'warning';
    case 'LOW':
    default:
      return 'info';
  }
}

/**
 * Handle incoming WebSocket messages
 */
function onMessage(message: IMessage) {
  try {
    const notification: RealTimeNotificationDto = JSON.parse(message.body);
    
    console.log('📨 WebSocket notification received:', notification);
    
    const severity = mapPriorityToSeverity(notification.priority);
    const title = notification.title || 'Notification';
    const msg = notification.message || '';
    
    // Dispatch to Redux store for persistent notifications
    store.dispatch(addNotification({
      type: severity,
      title,
      message: msg,
      ttl: notification.priority === 'HIGH' ? 0 : 5000, // High priority stays until dismissed
      actionUrl: notification.actionUrl,
      source: 'websocket'
    }));
    
    // Notifications will only appear in the bell - no toasts
    
    // Dispatch custom events for specific notification types
    if (notification.type === 'ADMIN_FEED') {
      console.log('🔧 Admin feed update:', notification);
      console.log('🔧 Dispatching admin-feed event with detail:', notification);
      // Dispatch custom event for admin feed component
      window.dispatchEvent(new CustomEvent('admin-feed', { detail: notification }));
    } else if (notification.type === 'SYSTEM_ALERT') {
      console.log('🚨 System alert:', notification);
    } else {
      console.log('📦 Other notification type:', notification.type, notification);
    }
    
  } catch (error) {
    console.error('❌ Failed to parse WebSocket message:', error);
  }
}

/**
 * Update connection status and notify listeners
 */
function updateConnectionStatus(status: Partial<ConnectionStatus>) {
  connectionStatus = { ...connectionStatus, ...status };
  if (connectionStatusCallback) {
    connectionStatusCallback(connectionStatus);
  }
}

/**
 * Connect to WebSocket and subscribe to relevant topics
 */
export function connectWebSocket(user?: { id: number; role: string; email?: string }) {
  if (client?.connected) {
    console.log('🔗 WebSocket already connected');
    return;
  }
  
  if (!user) {
    console.log('👤 No user provided, skipping WebSocket connection');
    return;
  }
  
  console.log('🔗 Connecting to WebSocket...', { 
    url: WS_URL, 
    user: { id: user.id, role: user.role, email: user.email } 
  });
  
  updateConnectionStatus({ reconnecting: true, error: undefined });
  
  client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    connectHeaders: token.access ? { 
      Authorization: `Bearer ${token.access}`,
      'X-User-ID': user.id.toString(),
      'X-User-Role': user.role
    } : {},
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    
    onConnect: (frame) => {
      console.log('✅ WebSocket connected:', frame);
      updateConnectionStatus({ connected: true, reconnecting: false });
      
      if (!client) return;
      
      try {
        // Subscribe to system-wide alerts (for all users)
        client.subscribe('/topic/system-alerts', onMessage);
        console.log('📡 Subscribed to /topic/system-alerts');
        
        // Subscribe to admin feeds (admin only)
        if (user.role === 'ADMIN') {
          client.subscribe('/topic/admin-feeds', onMessage);
          console.log('📡 Subscribed to /topic/admin-feeds');
        }
        
        // Subscribe to role-specific notifications
        const roleChannel = `/topic/notifications/${user.role.toLowerCase()}`;
        client.subscribe(roleChannel, onMessage);
        console.log(`📡 Subscribed to ${roleChannel}`);
        
        // Subscribe to personal notification queue
        const personalChannel = `/queue/user/${user.id}/notifications`;
        client.subscribe(personalChannel, onMessage);
        console.log(`📡 Subscribed to ${personalChannel}`);
        
        // Send a test message to confirm connection
        if (client.connected) {
          client.publish({
            destination: '/app/test',
            body: JSON.stringify({
              message: 'Frontend connection test',
              userId: user.id,
              role: user.role,
              timestamp: new Date().toISOString()
            })
          });
        }
        
      } catch (error) {
        console.error('❌ Failed to subscribe to WebSocket topics:', error);
      }
    },
    
    onStompError: (frame) => {
      console.error('❌ STOMP error:', frame);
      updateConnectionStatus({ 
        connected: false, 
        reconnecting: false, 
        error: frame.headers?.message || 'STOMP connection error' 
      });
    },
    
    onWebSocketError: (error) => {
      console.error('❌ WebSocket error:', error);
      updateConnectionStatus({ 
        connected: false, 
        reconnecting: false, 
        error: 'WebSocket connection error' 
      });
    },
    
    onDisconnect: () => {
      console.log('🔌 WebSocket disconnected');
      updateConnectionStatus({ connected: false, reconnecting: false });
    }
  });
  
  client.activate();
}

/**
 * Disconnect from WebSocket
 */
export function disconnectWebSocket() {
  if (!client) {
    console.log('🔌 WebSocket already disconnected');
    return;
  }
  
  console.log('🔌 Disconnecting WebSocket...');
  updateConnectionStatus({ connected: false, reconnecting: false });
  
  client.deactivate();
  client = null;
}

/**
 * Get current connection status
 */
export function getConnectionStatus(): ConnectionStatus {
  return connectionStatus;
}

/**
 * Set callback for connection status changes
 */
export function onConnectionStatusChange(callback: (status: ConnectionStatus) => void) {
  connectionStatusCallback = callback;
}

/**
 * Send a test message (for debugging)
 */
export function sendTestMessage(message: string) {
  if (!client?.connected) {
    console.warn('⚠️ Cannot send message: WebSocket not connected');
    return;
  }
  
  client.publish({
    destination: '/app/test',
    body: JSON.stringify({
      message,
      timestamp: new Date().toISOString()
    })
  });
}

/**
 * Check if WebSocket is connected
 */
export function isConnected(): boolean {
  return client?.connected || false;
}
