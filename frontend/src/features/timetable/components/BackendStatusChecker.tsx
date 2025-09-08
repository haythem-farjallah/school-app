import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../components/ui/card';
import { Button } from '../../../components/ui/button';
import { Badge } from '../../../components/ui/badge';
import { 
  Server, 
  Database, 
  CheckCircle, 
  XCircle, 
  RefreshCw,
  AlertTriangle,
  Wifi,
  WifiOff
} from 'lucide-react';
import { http } from '../../../lib/http';

export function BackendStatusChecker() {
  const [status, setStatus] = useState({
    backend: 'checking' as 'online' | 'offline' | 'checking',
    database: 'checking' as 'online' | 'offline' | 'checking',
    lastCheck: new Date(),
    error: null as string | null
  });

  const checkBackendStatus = async () => {
    setStatus(prev => ({ ...prev, backend: 'checking', database: 'checking', error: null }));
    
    try {
      // Try to hit a simple health endpoint or any basic endpoint
      const response = await http.get('/v1/periods');
      
      setStatus({
        backend: 'online',
        database: 'online',
        lastCheck: new Date(),
        error: null
      });
    } catch (error: any) {
      console.error('Backend status check failed:', error);
      
      let backendStatus: 'online' | 'offline' = 'offline';
      let databaseStatus: 'online' | 'offline' = 'offline';
      let errorMessage = 'Unknown error';
      
      if (error?.code === 'NETWORK_ERROR' || error?.message?.includes('Network Error')) {
        errorMessage = 'Backend server is not running or not accessible';
      } else if (error?.response?.status === 500) {
        backendStatus = 'online';
        errorMessage = 'Backend is running but database connection failed';
      } else if (error?.response?.status === 404) {
        backendStatus = 'online';
        databaseStatus = 'online';
        errorMessage = 'Endpoint not found (but backend is running)';
      } else {
        errorMessage = error?.response?.data?.message || error?.message || 'Connection failed';
      }
      
      setStatus({
        backend: backendStatus,
        database: databaseStatus,
        lastCheck: new Date(),
        error: errorMessage
      });
    }
  };

  useEffect(() => {
    checkBackendStatus();
  }, []);

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'online':
        return <CheckCircle className="w-5 h-5 text-green-600" />;
      case 'offline':
        return <XCircle className="w-5 h-5 text-red-600" />;
      case 'checking':
        return <RefreshCw className="w-5 h-5 text-yellow-600 animate-spin" />;
      default:
        return <AlertTriangle className="w-5 h-5 text-gray-400" />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'online':
        return 'text-green-600';
      case 'offline':
        return 'text-red-600';
      case 'checking':
        return 'text-yellow-600';
      default:
        return 'text-gray-400';
    }
  };

  return (
    <Card className="border-blue-200 bg-blue-50">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-blue-800">
          <Server className="w-5 h-5" />
          Backend Status Check
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* Status Grid */}
          <div className="grid grid-cols-2 gap-4">
            <div className="flex items-center gap-2">
              {status.backend === 'online' ? <Wifi className="w-4 h-4 text-green-600" /> : <WifiOff className="w-4 h-4 text-red-600" />}
              <span className="text-sm font-medium">Backend Server:</span>
              <Badge variant={status.backend === 'online' ? 'default' : 'destructive'}>
                {status.backend}
              </Badge>
            </div>
            
            <div className="flex items-center gap-2">
              <Database className="w-4 h-4" />
              <span className="text-sm font-medium">Database:</span>
              <Badge variant={status.database === 'online' ? 'default' : 'destructive'}>
                {status.database}
              </Badge>
            </div>
          </div>

          {/* Error Message */}
          {status.error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded">
              <div className="flex items-center gap-2 mb-1">
                <XCircle className="w-4 h-4 text-red-600" />
                <span className="font-medium text-red-800">Connection Error</span>
              </div>
              <p className="text-sm text-red-700">{status.error}</p>
            </div>
          )}

          {/* Success Message */}
          {status.backend === 'online' && status.database === 'online' && (
            <div className="p-3 bg-green-50 border border-green-200 rounded">
              <div className="flex items-center gap-2">
                <CheckCircle className="w-4 h-4 text-green-600" />
                <span className="font-medium text-green-800">All Systems Online</span>
              </div>
              <p className="text-sm text-green-700">Backend and database are working properly.</p>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-600">
              Last checked: {status.lastCheck.toLocaleTimeString()}
            </span>
            <Button 
              size="sm" 
              variant="outline" 
              onClick={checkBackendStatus}
              disabled={status.backend === 'checking'}
            >
              {status.backend === 'checking' ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <RefreshCw className="w-4 h-4" />
              )}
              Recheck
            </Button>
          </div>

          {/* Troubleshooting */}
          {status.backend === 'offline' && (
            <div className="p-3 bg-yellow-50 border border-yellow-200 rounded">
              <div className="font-medium text-yellow-800 mb-2">🔧 Troubleshooting Steps:</div>
              <ol className="text-sm text-yellow-700 list-decimal list-inside space-y-1">
                <li>Check if backend is running: <code className="bg-yellow-100 px-1 rounded">mvn spring-boot:run</code></li>
                <li>Verify backend URL in frontend config</li>
                <li>Check for CORS issues in browser console</li>
                <li>Ensure database is running and accessible</li>
                <li>Check for migration conflicts in backend logs</li>
              </ol>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
